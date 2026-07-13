package com.linkwisehub.modules.ai.document.service.impl;

import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.modules.ai.document.dto.AiDocumentChunkRespDto;
import com.linkwisehub.modules.ai.document.dto.AiDocumentRespDto;
import com.linkwisehub.modules.ai.document.entity.AiDocument;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentChunkMapper;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentMapper;
import com.linkwisehub.modules.ai.document.service.DocumentQueryService;
import com.linkwisehub.modules.ai.document.service.DocumentProcessingJobService;
import com.linkwisehub.modules.ai.document.service.DocumentRagIndexService;
import com.linkwisehub.modules.ai.document.service.DocumentStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档查询服务，统一处理文档和 Chunk 的可见状态。
 */
@Service
public class DocumentQueryServiceImpl implements DocumentQueryService {

    private final AiDocumentMapper documentMapper;
    private final AiDocumentChunkMapper chunkMapper;
    private final DocumentStorageService documentStorageService;
    private final DocumentRagIndexService documentRagIndexService;
    private final DocumentProcessingJobService processingJobService;

    public DocumentQueryServiceImpl(AiDocumentMapper documentMapper,
                                    AiDocumentChunkMapper chunkMapper,
                                    DocumentStorageService documentStorageService,
                                    DocumentRagIndexService documentRagIndexService,
                                    DocumentProcessingJobService processingJobService) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.documentStorageService = documentStorageService;
        this.documentRagIndexService = documentRagIndexService;
        this.processingJobService = processingJobService;
    }

    @Override
    public List<AiDocumentRespDto> listDocuments() {
        return documentMapper.selectAll().stream()
                .map(this::toRespDto)
                .collect(Collectors.toList());
    }

    @Override
    public AiDocumentRespDto getDocument(Long id) {
        return toRespDto(getActiveDocument(id));
    }

    @Override
    public List<AiDocumentChunkRespDto> listChunks(Long documentId) {
        getActiveDocument(documentId);
        return chunkMapper.selectByDocumentId(documentId);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        AiDocument document = getActiveDocument(id);
        processingJobService.cancelActive(id);
        documentRagIndexService.deleteIndexByDocumentId(id);
        chunkMapper.updateStatusByDocumentId(id, 0);
        documentMapper.updateStatus(id, 0);
        documentStorageService.deleteQuietly(document.getStorageBucket(), document.getStoragePath());
    }

    /**
     * 查询可用文档，统一处理不存在和已删除状态。
     */
    private AiDocument getActiveDocument(Long id) {
        AiDocument document = documentMapper.selectById(id);
        if (document == null || document.getStatus() == null || document.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "文档不存在");
        }
        return document;
    }

    /**
     * 实体转响应对象，避免把存储路径暴露给前端。
     */
    private AiDocumentRespDto toRespDto(AiDocument document) {
        AiDocumentRespDto dto = new AiDocumentRespDto();
        dto.setId(document.getId());
        dto.setTitle(resolveTitle(document));
        dto.setFileName(document.getFileName());
        dto.setFileType(document.getFileType());
        dto.setFileSize(document.getFileSize());
        dto.setParseStatus(document.getParseStatus());
        dto.setChunkCount(document.getChunkCount());
        dto.setErrorMessage(document.getErrorMessage());
        dto.setSourceType(document.getSourceType());
        dto.setPublishStatus(document.getPublishStatus());
        dto.setCreateTime(document.getCreateTime());
        dto.setUpdateTime(document.getUpdateTime());
        return dto;
    }

    private String resolveTitle(AiDocument document) {
        if (document.getTitle() != null && !document.getTitle().trim().isEmpty()) {
            return document.getTitle();
        }
        String fileName = document.getFileName();
        if (fileName == null || fileName.trim().isEmpty()) {
            return "未命名文档";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
}
