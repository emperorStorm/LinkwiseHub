package com.linkwisehub.modules.ai.document.dto;

import com.linkwisehub.modules.ai.document.entity.AiDocumentSplitConfig;
import lombok.Data;

/**
 * 文档 RAG 索引请求对象，统一承载切片与向量索引元数据。
 */
@Data
public class DocumentRagIndexRequest {
    private Long documentId;
    private Long categoryId;
    private String title;
    private String fileName;
    private String fileType;
    private String sourceType;
    private String text;
    private AiDocumentSplitConfig splitConfig;
}
