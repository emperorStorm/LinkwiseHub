package com.linkwisehub.modules.ai.knowledge.service.impl;

import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.config.AiDocumentProcessingProperties;
import com.linkwisehub.modules.ai.document.dto.AiDocumentChunkRespDto;
import com.linkwisehub.modules.ai.document.dto.DocumentParseJobRespDto;
import com.linkwisehub.modules.ai.document.dto.DocumentRagIndexRequest;
import com.linkwisehub.modules.ai.document.dto.DocumentStorageResult;
import com.linkwisehub.modules.ai.document.entity.AiDocument;
import com.linkwisehub.modules.ai.document.entity.AiDocumentChunk;
import com.linkwisehub.modules.ai.document.entity.AiDocumentSplitConfig;
import com.linkwisehub.modules.ai.document.enums.DocumentParseStrategy;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentChunkMapper;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentMapper;
import com.linkwisehub.modules.ai.document.service.DocumentProcessingJobService;
import com.linkwisehub.modules.ai.document.service.DocumentRagIndexService;
import com.linkwisehub.modules.ai.document.service.DocumentSplitConfigService;
import com.linkwisehub.modules.ai.document.service.DocumentSparseIndexService;
import com.linkwisehub.modules.ai.document.service.DocumentStorageService;
import com.linkwisehub.modules.ai.document.service.TextCleanService;
import com.linkwisehub.modules.ai.document.support.DocumentFileType;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeDocumentReqDto;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeDocumentRespDto;
import com.linkwisehub.modules.ai.knowledge.entity.AiKnowledgeCategory;
import com.linkwisehub.modules.ai.knowledge.mapper.AiKnowledgeCategoryMapper;
import com.linkwisehub.modules.ai.knowledge.service.AiKnowledgeCategoryService;
import com.linkwisehub.modules.ai.knowledge.service.AiKnowledgeDocumentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiKnowledgeDocumentServiceImpl implements AiKnowledgeDocumentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String PUBLISH_DRAFT = "DRAFT";
    private static final String PUBLISH_PUBLISHED = "PUBLISHED";
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final AiDocumentMapper documentMapper;
    private final AiDocumentChunkMapper chunkMapper;
    private final AiKnowledgeCategoryMapper categoryMapper;
    private final AiKnowledgeCategoryService categoryService;
    private final DocumentStorageService documentStorageService;
    private final DocumentSplitConfigService documentSplitConfigService;
    private final TextCleanService textCleanService;
    private final DocumentRagIndexService documentRagIndexService;
    private final DocumentSparseIndexService documentSparseIndexService;
    private final DocumentProcessingJobService processingJobService;
    private final AiDocumentProcessingProperties processingProperties;

    public AiKnowledgeDocumentServiceImpl(AiDocumentMapper documentMapper,
                                          AiDocumentChunkMapper chunkMapper,
                                          AiKnowledgeCategoryMapper categoryMapper,
                                          AiKnowledgeCategoryService categoryService,
                                          DocumentStorageService documentStorageService,
                                          DocumentSplitConfigService documentSplitConfigService,
                                          TextCleanService textCleanService,
                                          DocumentRagIndexService documentRagIndexService,
                                          DocumentSparseIndexService documentSparseIndexService,
                                          DocumentProcessingJobService processingJobService,
                                          AiDocumentProcessingProperties processingProperties) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.categoryMapper = categoryMapper;
        this.categoryService = categoryService;
        this.documentStorageService = documentStorageService;
        this.documentSplitConfigService = documentSplitConfigService;
        this.textCleanService = textCleanService;
        this.documentRagIndexService = documentRagIndexService;
        this.documentSparseIndexService = documentSparseIndexService;
        this.processingJobService = processingJobService;
        this.processingProperties = processingProperties;
    }

    @Override
    public List<AiKnowledgeDocumentRespDto> listDocuments(Long categoryId, String keyword, String publishStatus) {
        List<Long> categoryIds = categoryId == null ? null : categoryService.listCategoryAndChildrenIds(categoryId);
        String safeStatus = normalizePublishStatus(publishStatus, false);
        Map<Long, String> categoryNameMap = categoryMapper.selectAll().stream()
                .collect(Collectors.toMap(AiKnowledgeCategory::getId, AiKnowledgeCategory::getName, (left, right) -> left));
        List<AiDocument> documents = documentMapper.selectKnowledgeDocuments(categoryIds, trimToNull(keyword), safeStatus);
        Map<Long, DocumentParseJobRespDto> parseJobs = processingJobService.getLatestByDocumentIds(
                documents.stream().map(AiDocument::getId).collect(Collectors.toList()));
        return documents.stream()
                .map(document -> toRespDto(document, categoryNameMap.get(document.getCategoryId()), parseJobs.get(document.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public AiKnowledgeDocumentRespDto getDocument(Long id) {
        AiDocument document = ensureActiveKnowledgeDocument(id);
        return toRespDto(document, getCategoryName(document.getCategoryId()), processingJobService.getLatest(document.getId()));
    }

    @Override
    @Transactional
    public AiKnowledgeDocumentRespDto create(AiKnowledgeDocumentReqDto reqDto, MultipartFile file) {
        ensureActiveCategory(reqDto.getCategoryId());
        ParsedDocumentInput parsedInput = buildParsedInput(reqDto, file, null);
        AiDocument document = new AiDocument();
        try {
            fillDocument(document, reqDto, parsedInput);
            document.setStatus(1);
            documentMapper.insert(document);
            processDocument(document, parsedInput);
            return toRespDto(documentMapper.selectById(document.getId()), getCategoryName(document.getCategoryId()),
                    processingJobService.getLatest(document.getId()));
        } catch (RuntimeException e) {
            documentRagIndexService.deleteIndexByDocumentId(document.getId());
            if (parsedInput.isFileReplaced()) {
                documentStorageService.deleteQuietly(parsedInput.getStorageBucket(), parsedInput.getStoragePath());
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public AiKnowledgeDocumentRespDto update(Long id, AiKnowledgeDocumentReqDto reqDto, MultipartFile file) {
        AiDocument oldDocument = ensureActiveKnowledgeDocument(id);
        ensureActiveCategory(reqDto.getCategoryId());
        ParsedDocumentInput parsedInput = buildParsedInput(reqDto, file, oldDocument);
        String oldBucket = oldDocument.getStorageBucket();
        String oldPath = oldDocument.getStoragePath();

        try {
            fillDocument(oldDocument, reqDto, parsedInput);
            documentRagIndexService.deleteIndexByDocumentId(id);
            chunkMapper.updateStatusByDocumentId(id, 0);
            documentMapper.updateKnowledgeDocument(oldDocument);
            processDocument(oldDocument, parsedInput);
            if (parsedInput.isFileReplaced() && oldPath != null) {
                documentStorageService.deleteQuietly(oldBucket, oldPath);
            }
        } catch (RuntimeException e) {
            documentRagIndexService.deleteIndexByDocumentId(id);
            if (parsedInput.isFileReplaced()) {
                documentStorageService.deleteQuietly(parsedInput.getStorageBucket(), parsedInput.getStoragePath());
            }
            throw e;
        }
        return toRespDto(documentMapper.selectById(id), getCategoryName(reqDto.getCategoryId()),
                processingJobService.getLatest(id));
    }

    @Override
    @Transactional
    public AiKnowledgeDocumentRespDto updatePublishStatus(Long id, String publishStatus) {
        AiDocument document = ensureActiveKnowledgeDocument(id);
        String safeStatus = normalizePublishStatus(publishStatus, true);
        documentMapper.updatePublishStatus(id, safeStatus);
        documentSparseIndexService.updatePublishStatus(id, safeStatus);
        document.setPublishStatus(safeStatus);
        return toRespDto(document, getCategoryName(document.getCategoryId()), processingJobService.getLatest(id));
    }

    @Override
    public List<AiDocumentChunkRespDto> listChunks(Long id) {
        ensureActiveKnowledgeDocument(id);
        return chunkMapper.selectByDocumentId(id);
    }

    @Override
    @Transactional
    public AiKnowledgeDocumentRespDto deleteAttachment(Long id) {
        AiDocument document = ensureActiveKnowledgeDocument(id);
        if (!hasStoredFile(document)) {
            return toRespDto(document, getCategoryName(document.getCategoryId()), processingJobService.getLatest(id));
        }
        String contentText = htmlToText(document.getContentHtml());
        if (contentText.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "删除附件前需要保留正文内容");
        }
        String oldBucket = document.getStorageBucket();
        String oldPath = document.getStoragePath();
        document.setFileName("");
        document.setFileType("txt");
        document.setFileSize(0L);
        document.setStoragePath("");
        document.setParseStatus(STATUS_PROCESSING);
        document.setChunkCount(0);
        document.setErrorMessage(null);
        document.setSourceType("CONTENT");

        documentRagIndexService.deleteIndexByDocumentId(id);
        processingJobService.cancelActive(id);
        chunkMapper.updateStatusByDocumentId(id, 0);
        documentMapper.updateKnowledgeDocument(document);
        saveChunks(document, combineText(document.getTitle(), contentText, ""));
        documentStorageService.deleteQuietly(oldBucket, oldPath);
        return toRespDto(documentMapper.selectById(id), getCategoryName(document.getCategoryId()),
                processingJobService.getLatest(id));
    }

    @Override
    @Transactional
    public void rebuildAfterAttachmentChanged(Long id, long fileSize) {
        AiDocument document = ensureActiveKnowledgeDocument(id);
        if (!hasStoredFile(document)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前知识文档没有可重建的附件");
        }
        DocumentParseStrategy strategy = processingProperties.getStrategy();
        validateParseStrategy(document.getFileType(), strategy);
        document.setFileSize(fileSize);
        document.setParseStatus(STATUS_PROCESSING);
        document.setChunkCount(0);
        document.setErrorMessage(null);
        documentRagIndexService.deleteIndexByDocumentId(id);
        chunkMapper.updateStatusByDocumentId(id, 0);
        documentMapper.updateKnowledgeDocument(document);
        processingJobService.submit(document, strategy);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AiDocument document = ensureActiveKnowledgeDocument(id);
        processingJobService.cancelActive(id);
        documentRagIndexService.deleteIndexByDocumentId(id);
        chunkMapper.updateStatusByDocumentId(id, 0);
        documentMapper.updateStatus(id, 0);
        documentStorageService.deleteQuietly(document.getStorageBucket(), document.getStoragePath());
    }

    private ParsedDocumentInput buildParsedInput(AiKnowledgeDocumentReqDto reqDto, MultipartFile file, AiDocument oldDocument) {
        String title = reqDto.getTitle() == null ? "" : reqDto.getTitle().trim();
        String contentHtml = reqDto.getContentHtml() == null ? "" : reqDto.getContentHtml();
        String contentText = htmlToText(contentHtml);
        boolean hasContent = !contentText.isBlank();
        boolean hasFile = file != null && !file.isEmpty();
        if (!hasContent && !hasFile && oldDocument == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "内容和文件至少填写一个");
        }

        String fileName = oldDocument == null ? title + ".txt" : oldDocument.getFileName();
        String fileType = oldDocument == null ? "txt" : oldDocument.getFileType();
        long fileSize = oldDocument == null || oldDocument.getFileSize() == null ? 0L : oldDocument.getFileSize();
        String storageBucket = oldDocument == null ? "" : oldDocument.getStorageBucket();
        String storagePath = oldDocument == null ? "" : oldDocument.getStoragePath();
        boolean fileReplaced = false;
        boolean asyncParse = false;
        DocumentParseStrategy strategy = processingProperties.getStrategy();

        if (hasFile) {
            validateFile(file);
            fileName = file.getOriginalFilename();
            fileType = getFileType(fileName);
            fileSize = file.getSize();
            validateParseStrategy(fileType, strategy);
            asyncParse = true;
            DocumentStorageResult storageResult = documentStorageService.upload(file, fileType);
            storageBucket = storageResult.getBucketName();
            storagePath = storageResult.getObjectName();
            fileReplaced = true;
        } else if (oldDocument != null && hasStoredFile(oldDocument)) {
            validateParseStrategy(oldDocument.getFileType(), strategy);
            asyncParse = true;
        }

        String combinedText = combineText(title, contentText, "");
        if (combinedText.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "内容和文件至少填写一个");
        }

        ParsedDocumentInput input = new ParsedDocumentInput();
        input.setContentHtml(contentHtml);
        input.setCombinedText(combinedText);
        input.setFileName(fileName);
        input.setFileType(fileType);
        input.setFileSize(fileSize);
        input.setStorageBucket(storageBucket);
        input.setStoragePath(storagePath);
        input.setSourceType(resolveSourceType(hasContent, hasFile || oldDocument != null && hasStoredFile(oldDocument)));
        input.setFileReplaced(fileReplaced);
        input.setAsyncParse(asyncParse);
        return input;
    }

    private void processDocument(AiDocument document, ParsedDocumentInput input) {
        if (input.isAsyncParse()) {
            processingJobService.submit(document, processingProperties.getStrategy());
            return;
        }
        saveChunks(document, input.getCombinedText());
    }

    private void fillDocument(AiDocument document, AiKnowledgeDocumentReqDto reqDto, ParsedDocumentInput input) {
        document.setCategoryId(reqDto.getCategoryId());
        document.setTitle(reqDto.getTitle().trim());
        document.setFileName(input.getFileName());
        document.setFileType(input.getFileType());
        document.setFileSize(input.getFileSize());
        document.setStorageBucket(input.getStorageBucket());
        document.setStoragePath(input.getStoragePath());
        document.setParseStatus(STATUS_PROCESSING);
        document.setChunkCount(0);
        document.setErrorMessage(null);
        document.setContentHtml(input.getContentHtml());
        document.setPublishStatus(normalizePublishStatus(reqDto.getPublishStatus(), true));
        document.setSourceType(input.getSourceType());
    }

    private void saveChunks(AiDocument document, String combinedText) {
        String cleanText = textCleanService.clean(combinedText);
        if (cleanText.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "知识文档内容为空");
        }
        AiDocumentSplitConfig splitConfig = documentSplitConfigService.getEffectiveConfig();
        List<AiDocumentChunk> chunks = documentRagIndexService.index(buildRagIndexRequest(document, cleanText, splitConfig));
        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未生成有效 Chunk");
        }
        documentMapper.updateParseResult(document.getId(), STATUS_SUCCESS, chunks.size(), null);
    }

    private DocumentRagIndexRequest buildRagIndexRequest(AiDocument document, String cleanText, AiDocumentSplitConfig splitConfig) {
        DocumentRagIndexRequest request = new DocumentRagIndexRequest();
        request.setDocumentId(document.getId());
        request.setCategoryId(document.getCategoryId());
        request.setTitle(document.getTitle());
        request.setFileName(document.getFileName());
        request.setFileType(document.getFileType());
        request.setSourceType(document.getSourceType());
        request.setText(cleanText);
        request.setSplitConfig(splitConfig);
        return request;
    }

    private AiDocument ensureActiveKnowledgeDocument(Long id) {
        AiDocument document = documentMapper.selectById(id);
        if (document == null || document.getStatus() == null || document.getStatus() != 1 || document.getSourceType() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "知识文档不存在");
        }
        return document;
    }

    private void ensureActiveCategory(Long id) {
        AiKnowledgeCategory category = categoryMapper.selectById(id);
        if (category == null || category.getStatus() == null || category.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "分类不存在");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文件大小不能超过 10MB");
        }
        getFileType(file.getOriginalFilename());
    }

    private String getFileType(String fileName) {
        return DocumentFileType.resolveFileType(fileName);
    }

    private void validateParseStrategy(String fileType, DocumentParseStrategy strategy) {
        if (strategy == DocumentParseStrategy.MINERU && !DocumentFileType.isMineruSupported(fileType)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该文件类型不支持 MINERU 解析");
        }
        if (!strategy.usesMineru(fileType) && !DocumentFileType.isLegacySupported(fileType)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "图片附件需要使用 MINERU 或 AUTO 解析策略");
        }
    }

    private String htmlToText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String normalized = html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</div>", "\n")
                .replaceAll("(?i)</li>", "\n");
        String withoutTags = HTML_TAG_PATTERN.matcher(normalized).replaceAll("");
        return withoutTags.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .trim();
    }

    private String combineText(String title, String contentText, String fileText) {
        StringBuilder builder = new StringBuilder();
        if (!title.isBlank()) {
            builder.append("# ").append(title).append("\n\n");
        }
        if (contentText != null && !contentText.isBlank()) {
            builder.append(contentText.trim()).append("\n\n");
        }
        if (fileText != null && !fileText.isBlank()) {
            builder.append(fileText.trim());
        }
        return builder.toString().trim();
    }

    private String resolveSourceType(boolean hasContent, boolean hasFile) {
        if (hasContent && hasFile) {
            return "MIXED";
        }
        return hasContent ? "CONTENT" : "FILE";
    }

    private boolean hasStoredFile(AiDocument document) {
        return document.getFileName() != null
                && document.getFileType() != null
                && document.getStoragePath() != null
                && !document.getStoragePath().isBlank();
    }

    private String normalizePublishStatus(String publishStatus, boolean defaultDraft) {
        if (publishStatus == null || publishStatus.isBlank()) {
            return defaultDraft ? PUBLISH_DRAFT : null;
        }
        String status = publishStatus.trim().toUpperCase(Locale.ROOT);
        if (!PUBLISH_DRAFT.equals(status) && !PUBLISH_PUBLISHED.equals(status)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "发布状态无效");
        }
        return status;
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String getCategoryName(Long categoryId) {
        AiKnowledgeCategory category = categoryMapper.selectById(categoryId);
        return category == null ? "" : category.getName();
    }

    private AiKnowledgeDocumentRespDto toRespDto(AiDocument document, String categoryName, DocumentParseJobRespDto parseJob) {
        AiKnowledgeDocumentRespDto dto = new AiKnowledgeDocumentRespDto();
        dto.setId(document.getId());
        dto.setCategoryId(document.getCategoryId());
        dto.setCategoryName(categoryName);
        dto.setTitle(document.getTitle());
        dto.setFileName(document.getFileName());
        dto.setFileType(document.getFileType());
        dto.setFileSize(document.getFileSize());
        dto.setParseStatus(document.getParseStatus());
        dto.setChunkCount(document.getChunkCount());
        dto.setErrorMessage(document.getErrorMessage());
        if (parseJob != null) {
            dto.setParseEngine(parseJob.getParseEngine());
            dto.setParseJobStatus(parseJob.getStatus());
            dto.setParseProgress(parseJob.getProgress());
            dto.setParseRetryCount(parseJob.getRetryCount());
            dto.setParseJobErrorMessage(parseJob.getErrorMessage());
        }
        dto.setContentHtml(document.getContentHtml());
        dto.setPublishStatus(document.getPublishStatus());
        dto.setSourceType(document.getSourceType());
        dto.setCreateTime(document.getCreateTime());
        dto.setUpdateTime(document.getUpdateTime());
        return dto;
    }

    private static class ParsedDocumentInput {
        private String contentHtml;
        private String combinedText;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String storageBucket;
        private String storagePath;
        private String sourceType;
        private boolean fileReplaced;
        private boolean asyncParse;

        public String getContentHtml() {
            return contentHtml;
        }

        public void setContentHtml(String contentHtml) {
            this.contentHtml = contentHtml;
        }

        public String getCombinedText() {
            return combinedText;
        }

        public void setCombinedText(String combinedText) {
            this.combinedText = combinedText;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }

        public String getStorageBucket() {
            return storageBucket;
        }

        public void setStorageBucket(String storageBucket) {
            this.storageBucket = storageBucket;
        }

        public String getStoragePath() {
            return storagePath;
        }

        public void setStoragePath(String storagePath) {
            this.storagePath = storagePath;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public boolean isFileReplaced() {
            return fileReplaced;
        }

        public void setFileReplaced(boolean fileReplaced) {
            this.fileReplaced = fileReplaced;
        }

        public boolean isAsyncParse() {
            return asyncParse;
        }

        public void setAsyncParse(boolean asyncParse) {
            this.asyncParse = asyncParse;
        }
    }
}
