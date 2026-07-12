package com.linkwisehub.modules.ai.knowledge.service;

import com.linkwisehub.modules.ai.document.dto.AiDocumentChunkRespDto;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeDocumentReqDto;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeDocumentRespDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AiKnowledgeDocumentService {
    List<AiKnowledgeDocumentRespDto> listDocuments(Long categoryId, String keyword, String publishStatus);

    AiKnowledgeDocumentRespDto getDocument(Long id);

    AiKnowledgeDocumentRespDto create(AiKnowledgeDocumentReqDto reqDto, MultipartFile file);

    AiKnowledgeDocumentRespDto update(Long id, AiKnowledgeDocumentReqDto reqDto, MultipartFile file);

    AiKnowledgeDocumentRespDto updatePublishStatus(Long id, String publishStatus);

    List<AiDocumentChunkRespDto> listChunks(Long id);

    AiKnowledgeDocumentRespDto deleteAttachment(Long id);

    void rebuildAfterAttachmentChanged(Long id, long fileSize);

    void delete(Long id);
}
