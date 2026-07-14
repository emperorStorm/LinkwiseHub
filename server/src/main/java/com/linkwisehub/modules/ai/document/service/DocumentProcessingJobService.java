package com.linkwisehub.modules.ai.document.service;

import com.linkwisehub.modules.ai.document.dto.DocumentParseJobRespDto;
import com.linkwisehub.modules.ai.document.entity.AiDocument;
import com.linkwisehub.modules.ai.document.entity.AiDocumentProcessingJob;
import com.linkwisehub.modules.ai.document.enums.DocumentParseStrategy;

import java.util.List;
import java.util.Map;

public interface DocumentProcessingJobService {
    AiDocumentProcessingJob submit(AiDocument document, DocumentParseStrategy strategy);

    DocumentParseJobRespDto getLatest(Long documentId);

    Map<Long, DocumentParseJobRespDto> getLatestByDocumentIds(List<Long> documentIds);

    DocumentParseJobRespDto retry(Long documentId, DocumentParseStrategy strategy);

    void cancelActive(Long documentId);
}
