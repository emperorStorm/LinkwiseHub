package com.linkwisehub.modules.ai.document.service;

import com.linkwisehub.modules.ai.document.dto.AiDocumentChunkRespDto;
import com.linkwisehub.modules.ai.document.dto.AiDocumentRespDto;

import java.util.List;

public interface DocumentQueryService {
    List<AiDocumentRespDto> listDocuments();

    AiDocumentRespDto getDocument(Long id);

    List<AiDocumentChunkRespDto> listChunks(Long documentId);

    void deleteDocument(Long id);
}
