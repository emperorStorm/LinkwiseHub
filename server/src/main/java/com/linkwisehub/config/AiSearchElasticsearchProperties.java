package com.linkwisehub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 本地知识库 ES BM25 检索配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.search.elasticsearch")
public class AiSearchElasticsearchProperties {
    private boolean enabled = true;
    private String uris = "http://10.211.55.4:9200";
    private String username = "elastic";
    private String password = "";
    private String indexName = "lwh_document_chunks_bm25";
    private int topK = 20;
    private double titleBoost = 2.0D;
    private double contentBoost = 1.0D;
    private int rebuildBatchSize = 200;
}
