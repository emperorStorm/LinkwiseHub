package com.linkwisehub.config;

import com.linkwisehub.modules.ai.embedding.LinkwiseHubOllamaEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 手动注册 RAG 向量化所需的 Ollama EmbeddingModel。
 */
@Configuration
@EnableConfigurationProperties(SpringAiOllamaProperties.class)
public class SpringAiOllamaEmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel(SpringAiOllamaProperties properties,
                                         WebClient.Builder webClientBuilder) {
        return new LinkwiseHubOllamaEmbeddingModel(properties, webClientBuilder);
    }
}
