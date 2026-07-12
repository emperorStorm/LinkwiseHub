package com.linkwisehub.modules.ai.knowledge.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.config.OnlyOfficeProperties;
import com.linkwisehub.modules.ai.document.entity.AiDocument;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentMapper;
import com.linkwisehub.modules.ai.document.service.DocumentStorageService;
import com.linkwisehub.modules.ai.document.support.DocumentFileType;
import com.linkwisehub.modules.ai.knowledge.dto.OnlyOfficePreviewConfigRespDto;
import com.linkwisehub.modules.ai.knowledge.service.AiKnowledgeDocumentService;
import com.linkwisehub.modules.ai.knowledge.service.OnlyOfficePreviewService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class OnlyOfficePreviewServiceImpl implements OnlyOfficePreviewService {

    private static final long FILE_TOKEN_TTL_SECONDS = 10 * 60;
    private static final String SECRET_PLACEHOLDER = "PLEASE_REPLACE_WITH_ONLYOFFICE_JWT_SECRET";
    private static final String MODE_EDIT = "edit";
    private static final String MODE_VIEW = "view";

    private final OnlyOfficeProperties onlyOfficeProperties;
    private final AiDocumentMapper documentMapper;
    private final DocumentStorageService documentStorageService;
    private final AiKnowledgeDocumentService knowledgeDocumentService;
    private final OnlyOfficeJwtSupport jwtSupport;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public OnlyOfficePreviewServiceImpl(OnlyOfficeProperties onlyOfficeProperties,
                                        AiDocumentMapper documentMapper,
                                        DocumentStorageService documentStorageService,
                                        AiKnowledgeDocumentService knowledgeDocumentService,
                                        OnlyOfficeJwtSupport jwtSupport) {
        this.onlyOfficeProperties = onlyOfficeProperties;
        this.documentMapper = documentMapper;
        this.documentStorageService = documentStorageService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.jwtSupport = jwtSupport;
    }

    @Override
    public OnlyOfficePreviewConfigRespDto buildPreviewConfig(Long documentId, String mode) {
        AiDocument document = getPreviewableDocument(documentId);
        String secret = getJwtSecret();
        String safeMode = normalizeMode(mode);
        boolean editable = MODE_EDIT.equals(safeMode) && isEditableFile(document.getFileType());
        long now = Instant.now().getEpochSecond();
        Map<String, Object> filePayload = new LinkedHashMap<>();
        filePayload.put("documentId", document.getId());
        filePayload.put("scope", "knowledge-file");
        filePayload.put("exp", now + FILE_TOKEN_TTL_SECONDS);
        String fileToken = jwtSupport.createToken(filePayload, secret);

        Map<String, Object> documentConfig = new LinkedHashMap<>();
        documentConfig.put("fileType", document.getFileType());
        documentConfig.put("key", buildDocumentKey(document, safeMode));
        documentConfig.put("title", document.getFileName());
        documentConfig.put("url", normalizeUrl(onlyOfficeProperties.getPublicBackendUrl())
                + "/api/ai/knowledge/documents/" + document.getId() + "/file?token=" + fileToken);

        Map<String, Object> permissions = new LinkedHashMap<>();
        permissions.put("edit", editable);
        permissions.put("download", true);
        permissions.put("print", true);
        documentConfig.put("permissions", permissions);

        Map<String, Object> editorConfig = new LinkedHashMap<>();
        editorConfig.put("mode", editable ? MODE_EDIT : MODE_VIEW);
        editorConfig.put("lang", "zh-CN");
        editorConfig.put("callbackUrl", normalizeUrl(onlyOfficeProperties.getPublicBackendUrl())
                + "/api/ai/knowledge/onlyoffice/callback");

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", "oa-preview");
        user.put("name", "OA 知识库");
        editorConfig.put("user", user);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("type", "desktop");
        config.put("documentType", resolveDocumentType(document.getFileType()));
        config.put("document", documentConfig);
        config.put("editorConfig", editorConfig);
        config.put("token", jwtSupport.createToken(config, secret));

        OnlyOfficePreviewConfigRespDto dto = new OnlyOfficePreviewConfigRespDto();
        dto.setDocumentServerApiUrl(normalizeUrl(onlyOfficeProperties.getDocumentServerUrl())
                + "/web-apps/apps/api/documents/api.js");
        dto.setConfig(config);
        return dto;
    }

    @Override
    public PreviewFile openPreviewFile(Long documentId, String token) {
        JSONObject payload = jwtSupport.verifyToken(token, getJwtSecret());
        Long tokenDocumentId = payload.getLong("documentId");
        String scope = payload.getString("scope");
        if (!documentId.equals(tokenDocumentId) || !"knowledge-file".equals(scope)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "预览文件令牌与文档不匹配");
        }
        AiDocument document = getPreviewableDocument(documentId);
        InputStream inputStream = documentStorageService.getObject(document.getStorageBucket(), document.getStoragePath());
        return new PreviewFile(document.getFileName(), resolveContentType(document.getFileType()), inputStream);
    }

    @Override
    public PreviewFile openDownloadFile(Long documentId) {
        AiDocument document = getPreviewableDocument(documentId);
        InputStream inputStream = documentStorageService.getObject(document.getStorageBucket(), document.getStoragePath());
        return new PreviewFile(document.getFileName(), resolveContentType(document.getFileType()), inputStream);
    }

    @Override
    public void handleCallback(Map<String, Object> callbackBody) {
        Integer status = getInteger(callbackBody.get("status"));
        if (status == null || status != 2 && status != 6) {
            return;
        }
        Long documentId = parseDocumentId(String.valueOf(callbackBody.get("key")));
        String url = callbackBody.get("url") == null ? "" : String.valueOf(callbackBody.get("url"));
        if (documentId == null || url.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "OnlyOffice 回调缺少文档标识或保存地址");
        }
        AiDocument document = getPreviewableDocument(documentId);
        byte[] bytes = downloadEditedFile(url);
        documentStorageService.overwrite(
                document.getStorageBucket(),
                document.getStoragePath(),
                new ByteArrayInputStream(bytes),
                bytes.length,
                resolveContentType(document.getFileType()));
        knowledgeDocumentService.rebuildAfterAttachmentChanged(documentId, bytes.length);
    }

    private AiDocument getPreviewableDocument(Long documentId) {
        AiDocument document = documentMapper.selectById(documentId);
        if (document == null || document.getStatus() == null || document.getStatus() != 1 || document.getSourceType() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "知识文档不存在");
        }
        if (!hasStoredFile(document)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前知识文档没有可预览附件");
        }
        if (!DocumentFileType.isSupported(document.getFileType())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "当前文件类型不支持预览");
        }
        return document;
    }

    private boolean hasStoredFile(AiDocument document) {
        return document.getFileName() != null && !document.getFileName().isBlank()
                && document.getFileType() != null && !document.getFileType().isBlank()
                && document.getStoragePath() != null && !document.getStoragePath().isBlank();
    }

    private String getJwtSecret() {
        String secret = onlyOfficeProperties.getJwtSecret();
        if (secret == null || secret.isBlank() || SECRET_PLACEHOLDER.equals(secret.trim())) {
            throw new BusinessException(ErrorCode.AI_CONFIG_MISSING, "OnlyOffice JWT secret 不能为空或占位值");
        }
        return secret.trim();
    }

    private String resolveDocumentType(String fileType) {
        String type = fileType.toLowerCase(Locale.ROOT);
        if (DocumentFileType.XLS.equals(type) || DocumentFileType.XLSX.equals(type)) {
            return "cell";
        }
        if (DocumentFileType.PPT.equals(type) || DocumentFileType.PPTX.equals(type)) {
            return "slide";
        }
        if (DocumentFileType.PDF.equals(type)) {
            return "pdf";
        }
        return "word";
    }

    private String buildDocumentKey(AiDocument document, String mode) {
        String rawKey = document.getId() + ":" + document.getUpdateTime() + ":" + document.getStoragePath();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 16 && i < digest.length; i++) {
                builder.append(String.format("%02x", digest[i]));
            }
            return "knowledge-" + document.getId() + "-" + mode + "-" + builder;
        } catch (Exception e) {
            return "knowledge-" + document.getId() + "-" + mode + "-" + Math.abs(rawKey.hashCode());
        }
    }

    private String normalizeMode(String mode) {
        return MODE_EDIT.equalsIgnoreCase(mode) ? MODE_EDIT : MODE_VIEW;
    }

    private boolean isEditableFile(String fileType) {
        String type = fileType.toLowerCase(Locale.ROOT);
        return !DocumentFileType.PDF.equals(type);
    }

    private Long parseDocumentId(String key) {
        if (key == null || !key.startsWith("knowledge-")) {
            return null;
        }
        String[] parts = key.split("-");
        if (parts.length < 3) {
            return null;
        }
        try {
            return Long.valueOf(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private byte[] downloadEditedFile(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "OnlyOffice 文件下载失败: HTTP " + response.statusCode());
            }
            return response.body();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "OnlyOffice 文件下载失败: " + e.getMessage());
        }
    }

    private String resolveContentType(String fileType) {
        return switch (fileType.toLowerCase(Locale.ROOT)) {
            case DocumentFileType.MD -> "text/markdown; charset=UTF-8";
            case DocumentFileType.DOC -> "application/msword";
            case DocumentFileType.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case DocumentFileType.PPT -> "application/vnd.ms-powerpoint";
            case DocumentFileType.PPTX -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case DocumentFileType.XLS -> "application/vnd.ms-excel";
            case DocumentFileType.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case DocumentFileType.PDF -> "application/pdf";
            default -> "text/plain; charset=UTF-8";
        };
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException(ErrorCode.AI_CONFIG_MISSING, "OnlyOffice 服务地址配置不能为空");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
