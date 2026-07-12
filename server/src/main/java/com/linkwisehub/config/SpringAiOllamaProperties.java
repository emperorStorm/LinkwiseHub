package com.linkwisehub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring AI Ollama RAG 配置。
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.ollama")
public class SpringAiOllamaProperties {

    private String baseUrl = "http://127.0.0.1:11434";

    private int timeout = 600000;

    private Init init = new Init();

    private Embedding embedding = new Embedding();

    @Data
    public static class Init {

        private String pullModelStrategy = "never";
    }

    @Data
    public static class Embedding {

        private Options options = new Options();
    }

    @Data
    public static class Options {

        private String model = "bge-m3";

        private Integer dimensions;
    }
}
