package com.linkwisehub.modules.ai.document.service;

import com.linkwisehub.modules.ai.document.dto.DocumentSparseSearchResult;
import com.linkwisehub.modules.ai.document.dto.SparseIndexRebuildRespDto;

import java.util.List;

public interface DocumentSparseIndexService {

    void indexDocument(Long documentId);

    void deleteByDocumentId(Long documentId);

    void updatePublishStatus(Long documentId, String publishStatus);

    List<DocumentSparseSearchResult> search(String question);

    SparseIndexRebuildRespDto rebuildAll();
}
