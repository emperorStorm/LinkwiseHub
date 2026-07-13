package com.linkwisehub.modules.ai.document.service.impl;

import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.config.AiDocumentProcessingProperties;
import com.linkwisehub.modules.ai.document.dto.AiDocumentUploadRespDto;
import com.linkwisehub.modules.ai.document.dto.DocumentStorageResult;
import com.linkwisehub.modules.ai.document.entity.AiDocument;
import com.linkwisehub.modules.ai.document.entity.AiDocumentProcessingJob;
import com.linkwisehub.modules.ai.document.enums.DocumentParseStrategy;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentMapper;
import com.linkwisehub.modules.ai.document.service.DocumentProcessingJobService;
import com.linkwisehub.modules.ai.document.service.DocumentStorageService;
import com.linkwisehub.modules.ai.document.service.DocumentUploadService;
import com.linkwisehub.modules.ai.document.support.DocumentFileType;
import com.linkwisehub.modules.ai.knowledge.entity.AiKnowledgeCategory;
import com.linkwisehub.modules.ai.knowledge.mapper.AiKnowledgeCategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档上传编排服务，负责校验、MinIO 存储、解析、分片和状态落库。
 */
@Service
public class DocumentUploadServiceImpl implements DocumentUploadService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String TEST_CATEGORY_NAME = "测试数据";
    private static final String PUBLISH_PUBLISHED = "PUBLISHED";
    private static final String SOURCE_TYPE_FILE = "FILE";

    private final AiDocumentMapper documentMapper;
    private final AiKnowledgeCategoryMapper categoryMapper;
    private final DocumentStorageService documentStorageService;
    private final DocumentProcessingJobService processingJobService;
    private final AiDocumentProcessingProperties processingProperties;

    public DocumentUploadServiceImpl(AiDocumentMapper documentMapper,
                                     AiKnowledgeCategoryMapper categoryMapper,
                                     DocumentStorageService documentStorageService,
                                     DocumentProcessingJobService processingJobService,
                                     AiDocumentProcessingProperties processingProperties) {
        this.documentMapper = documentMapper;
        this.categoryMapper = categoryMapper;
        this.documentStorageService = documentStorageService;
        this.processingJobService = processingJobService;
        this.processingProperties = processingProperties;
    }

    @Override
    public AiDocumentUploadRespDto uploadAndParse(MultipartFile file) {
        return uploadAndParse(file, null);
    }

    @Override
    @Transactional
    public AiDocumentUploadRespDto uploadAndParse(MultipartFile file, String strategyValue) {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String fileType = getFileType(originalName);
        DocumentParseStrategy strategy = DocumentParseStrategy.resolve(strategyValue, processingProperties.getStrategy());
        if (strategy == DocumentParseStrategy.MINERU && !DocumentFileType.isMineruSupported(fileType)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该文件类型不支持 MINERU 解析");
        }
        if (!strategy.usesMineru(fileType) && !DocumentFileType.isLegacySupported(fileType)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "图片文件需要使用 MINERU 或 AUTO 解析策略");
        }
        DocumentStorageResult storageResult = documentStorageService.upload(file, fileType);

        try {
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

            AiDocumentProcessingJob job = processingJobService.submit(document, strategy);
            return new AiDocumentUploadRespDto(document.getId(), job.getId(), originalName, STATUS_PROCESSING, 0);
        } catch (RuntimeException e) {
            documentStorageService.deleteQuietly(storageResult.getBucketName(), storageResult.getObjectName());
            throw e;
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
