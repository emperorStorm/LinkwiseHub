package com.linkwisehub.modules.ai.document.service;

import com.linkwisehub.modules.ai.document.dto.DocumentParseJobRespDto;
import com.linkwisehub.modules.ai.document.entity.AiDocument;
import com.linkwisehub.modules.ai.document.entity.AiDocumentProcessingJob;
import com.linkwisehub.modules.ai.document.enums.DocumentParseStrategy;

public interface DocumentProcessingJobService {
    AiDocumentProcessingJob submit(AiDocument document, DocumentParseStrategy strategy);

    DocumentParseJobRespDto getLatest(Long documentId);

    DocumentParseJobRespDto retry(Long documentId, DocumentParseStrategy strategy);

    void cancelActive(Long documentId);
}
