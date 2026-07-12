package com.linkwisehub.modules.ai.document.dto;

import lombok.Data;

/**
 * ES BM25 稀疏检索结果。
 */
@Data
public class DocumentSparseSearchResult {
    private Long chunkId;
    private Long documentId;
    private String title;
    private String content;
    private Integer rank;
    private Double score;
}
