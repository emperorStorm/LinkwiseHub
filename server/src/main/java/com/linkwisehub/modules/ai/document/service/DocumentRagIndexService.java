package com.linkwisehub.modules.ai.document.service;

import com.linkwisehub.modules.ai.document.dto.DocumentRagIndexRequest;
import com.linkwisehub.modules.ai.document.dto.VectorIndexRebuildRespDto;
import com.linkwisehub.modules.ai.document.entity.AiDocumentChunk;

import java.util.List;

public interface DocumentRagIndexService {
    List<AiDocumentChunk> index(DocumentRagIndexRequest request);

    void deleteIndexByDocumentId(Long documentId);

    VectorIndexRebuildRespDto rebuildVectorIndex();
}
