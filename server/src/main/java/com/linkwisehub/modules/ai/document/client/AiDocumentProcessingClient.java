package com.linkwisehub.modules.ai.document.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.config.AiDocumentProcessingProperties;
import com.linkwisehub.modules.ai.document.entity.AiDocument;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AiDocumentProcessingClient {

    private static final String TOKEN_HEADER = "X-Service-Token";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final AiDocumentProcessingProperties properties;
    private final WebClient webClient;

    public AiDocumentProcessingClient(AiDocumentProcessingProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder.baseUrl(properties.getServiceBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public RemoteTask submit(AiDocument document) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("bucket", document.getStorageBucket());
        source.put("object_key", document.getStoragePath());
        source.put("file_name", document.getFileName());
        source.put("file_type", document.getFileType());
        Map<String, Object> payload = Map.of("document_id", document.getId(), "source", source);
        return post("/internal/v1/document-parses", payload, RemoteTask.class);
    }

    public RemoteTask getStatus(String taskId) {
        return execute(webClient.get()
                .uri("/internal/v1/document-parses/{taskId}", taskId)
                .header(TOKEN_HEADER, properties.getServiceToken())
                .header(REQUEST_ID_HEADER, UUID.randomUUID().toString()), RemoteTask.class);
    }

    public MaterializedResult materialize(String taskId, Long documentId) {
        return post("/internal/v1/document-parses/" + taskId + "/materialize",
                Map.of("document_id", documentId), MaterializedResult.class);
    }

    private <T> T post(String uri, Object body, Class<T> responseType) {
        return execute(webClient.post()
                .uri(uri)
                .header(TOKEN_HEADER, properties.getServiceToken())
                .header(REQUEST_ID_HEADER, UUID.randomUUID().toString())
                .bodyValue(body), responseType);
    }

    private <T> T execute(WebClient.RequestHeadersSpec<?> request, Class<T> responseType) {
        if (properties.getServiceToken() == null || properties.getServiceToken().isBlank()) {
            throw new BusinessException(ErrorCode.AI_CONFIG_MISSING, "AI_SERVICE_TOKEN 未配置");
        }
        try {
            T response = request.retrieve().bodyToMono(responseType)
                    .block(Duration.ofSeconds(properties.getRequestTimeoutSeconds()));
            if (response == null) {
                throw new BusinessException(ErrorCode.AI_PARSE_ERROR, "AI 服务返回空响应");
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 文档服务调用失败: " + e.getMessage());
        }
    }

    public record RemoteTask(@JsonProperty("task_id") String taskId,
                             String status,
                             Integer progress,
                             @JsonProperty("error_message") String errorMessage,
                             @JsonProperty("created_at") String createdAt,
                             @JsonProperty("started_at") String startedAt,
                             @JsonProperty("completed_at") String completedAt) {
    }

    public record MaterializedResult(@JsonProperty("task_id") String taskId, String status, Artifacts artifacts) {
    }

    public record Artifacts(String bucket,
                            @JsonProperty("manifest_object") String manifestObject,
                            @JsonProperty("markdown_object") String markdownObject,
                            @JsonProperty("blocks_object") String blocksObject,
                            @JsonProperty("asset_prefix") String assetPrefix,
                            @JsonProperty("block_count") Integer blockCount) {
    }
}
