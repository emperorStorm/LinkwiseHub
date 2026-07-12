package com.linkwisehub.modules.ai.document.service.impl;

import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.modules.ai.document.dto.DocumentRagIndexRequest;
import com.linkwisehub.modules.ai.document.dto.AiDocumentUploadRespDto;
import com.linkwisehub.modules.ai.document.dto.DocumentStorageResult;
import com.linkwisehub.modules.ai.document.entity.AiDocument;
import com.linkwisehub.modules.ai.document.entity.AiDocumentChunk;
import com.linkwisehub.modules.ai.document.entity.AiDocumentSplitConfig;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentMapper;
import com.linkwisehub.modules.ai.document.service.DocumentParseService;
import com.linkwisehub.modules.ai.document.service.DocumentRagIndexService;
import com.linkwisehub.modules.ai.document.service.DocumentSplitConfigService;
import com.linkwisehub.modules.ai.document.service.DocumentStorageService;
import com.linkwisehub.modules.ai.document.service.DocumentUploadService;
import com.linkwisehub.modules.ai.document.service.TextCleanService;
import com.linkwisehub.modules.ai.document.support.DocumentFileType;
import com.linkwisehub.modules.ai.knowledge.entity.AiKnowledgeCategory;
import com.linkwisehub.modules.ai.knowledge.mapper.AiKnowledgeCategoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档上传编排服务，负责校验、MinIO 存储、解析、分片和状态落库。
 */
@Slf4j
@Service
public class DocumentUploadServiceImpl implements DocumentUploadService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String TEST_CATEGORY_NAME = "测试数据";
    private static final String PUBLISH_PUBLISHED = "PUBLISHED";
    private static final String SOURCE_TYPE_FILE = "FILE";

    private final AiDocumentMapper documentMapper;
    private final AiKnowledgeCategoryMapper categoryMapper;
    private final DocumentParseService documentParseService;
    private final DocumentStorageService documentStorageService;
    private final DocumentSplitConfigService documentSplitConfigService;
    private final TextCleanService textCleanService;
    private final DocumentRagIndexService documentRagIndexService;

    public DocumentUploadServiceImpl(AiDocumentMapper documentMapper,
                                     AiKnowledgeCategoryMapper categoryMapper,
                                     DocumentParseService documentParseService,
                                     DocumentStorageService documentStorageService,
                                     DocumentSplitConfigService documentSplitConfigService,
                                     TextCleanService textCleanService,
                                     DocumentRagIndexService documentRagIndexService) {
        this.documentMapper = documentMapper;
        this.categoryMapper = categoryMapper;
        this.documentParseService = documentParseService;
        this.documentStorageService = documentStorageService;
        this.documentSplitConfigService = documentSplitConfigService;
        this.textCleanService = textCleanService;
        this.documentRagIndexService = documentRagIndexService;
    }

    @Override
    @Transactional
    public AiDocumentUploadRespDto uploadAndParse(MultipartFile file) {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String fileType = getFileType(originalName);
        DocumentStorageResult storageResult = documentStorageService.upload(file, fileType);

        AiDocument document = new AiDocument();
        document.setTitle(getFileTitle(originalName));
        document.setCategoryId(resolveTestCategoryId());
        document.setFileName(originalName);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setStorageBucket(storageResult.getBucketName());
        document.setStoragePath(storageResult.getObjectName());
        document.setParseStatus(STATUS_PROCESSING);
        document.setChunkCount(0);
        document.setPublishStatus(PUBLISH_PUBLISHED);
        document.setSourceType(SOURCE_TYPE_FILE);
        document.setStatus(1);
        documentMapper.insert(document);

        try {
            String parsedText = documentParseService.parse(file.getInputStream(), fileType);
            String cleanText = textCleanService.clean(parsedText);
            if (cleanText.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "文件内容为空");
            }

            AiDocumentSplitConfig splitConfig = documentSplitConfigService.getEffectiveConfig();
            List<AiDocumentChunk> chunks = documentRagIndexService.index(buildRagIndexRequest(document, cleanText, splitConfig));
            if (chunks.isEmpty()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未生成有效 Chunk");
            }

            documentMapper.updateParseResult(document.getId(), STATUS_SUCCESS, chunks.size(), null);
            return new AiDocumentUploadRespDto(document.getId(), originalName, STATUS_SUCCESS, chunks.size());
        } catch (RuntimeException e) {
            documentRagIndexService.deleteIndexByDocumentId(document.getId());
            documentStorageService.deleteQuietly(storageResult.getBucketName(), storageResult.getObjectName());
            documentMapper.updateParseResult(document.getId(), STATUS_FAILED, 0, e.getMessage());
            throw e;
        } catch (Exception e) {
            documentRagIndexService.deleteIndexByDocumentId(document.getId());
            documentStorageService.deleteQuietly(storageResult.getBucketName(), storageResult.getObjectName());
            documentMapper.updateParseResult(document.getId(), STATUS_FAILED, 0, e.getMessage());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "解析文件失败: " + e.getMessage());
        }
    }

    /**
     * 校验文件大小、空文件和扩展名，避免无效文件进入解析链路。
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文件大小不能超过 10MB");
        }
        getFileType(file.getOriginalFilename());
    }

    /**
     * 获取并校验文件扩展名。
     */
    private String getFileType(String fileName) {
        return DocumentFileType.resolveFileType(fileName);
    }

    private String getFileTitle(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "未命名文档";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
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

    private Long resolveTestCategoryId() {
        AiKnowledgeCategory category = categoryMapper.selectRootByName(TEST_CATEGORY_NAME);
        if (category != null) {
            return category.getId();
        }
        AiKnowledgeCategory newCategory = new AiKnowledgeCategory();
        newCategory.setParentId(0L);
        newCategory.setName(TEST_CATEGORY_NAME);
        newCategory.setSort(0);
        newCategory.setStatus(1);
        categoryMapper.insert(newCategory);
        return newCategory.getId();
    }
}
