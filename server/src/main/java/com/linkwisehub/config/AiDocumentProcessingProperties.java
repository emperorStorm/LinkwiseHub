package com.linkwisehub.config;

import com.linkwisehub.modules.ai.document.enums.DocumentParseStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.document.processing")
public class AiDocumentProcessingProperties {

    private DocumentParseStrategy strategy = DocumentParseStrategy.AUTO;
    private String serviceBaseUrl = "http://127.0.0.1:8090";
    private String serviceToken = "";
    private boolean schedulerEnabled = true;
    private int requestTimeoutSeconds = 120;
    private int pollBatchSize = 20;
    private int initialPollSeconds = 3;
    private int maxPollSeconds = 15;
    private int taskTimeoutMinutes = 60;
    private int maxAttempts = 3;
}
