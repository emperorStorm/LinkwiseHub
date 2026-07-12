package com.linkwisehub.modules.ai.embedding;

import com.linkwisehub.config.SpringAiOllamaProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * LinkwiseHub 本地 Ollama embedding 模型适配器。
 */
public class LinkwiseHubOllamaEmbeddingModel implements EmbeddingModel {

    private static final String EMBED_PATH = "/api/embed";

    private final WebClient webClient;

    private final String defaultModel;

    private final Integer dimensions;

    private final Duration timeout;

    public LinkwiseHubOllamaEmbeddingModel(SpringAiOllamaProperties properties,
                                           WebClient.Builder webClientBuilder) {
        SpringAiOllamaProperties.Options options = properties.getEmbedding().getOptions();
        this.defaultModel = options.getModel();
        this.dimensions = options.getDimensions();
        this.timeout = Duration.ofMillis(properties.getTimeout());
        this.webClient = webClientBuilder
            .baseUrl(properties.getBaseUrl())
            .build();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> input = request == null ? List.of() : request.getInstructions();
        if (CollectionUtils.isEmpty(input)) {
            return new EmbeddingResponse(List.of());
        }

        String model = resolveModel(request);
        OllamaEmbedResponse response = webClient.post()
            .uri(EMBED_PATH)
            .bodyValue(new OllamaEmbedRequest(model, input))
            .retrieve()
            .bodyToMono(OllamaEmbedResponse.class)
            .block(timeout);

        if (response == null || CollectionUtils.isEmpty(response.getEmbeddings())) {
            throw new IllegalStateException("Ollama embedding 返回为空");
        }
        if (response.getEmbeddings().size() != input.size()) {
            throw new IllegalStateException("Ollama embedding 返回数量与输入数量不一致");
        }

        List<Embedding> embeddings = new ArrayList<>(response.getEmbeddings().size());
        for (int i = 0; i < response.getEmbeddings().size(); i++) {
            embeddings.add(new Embedding(toFloatArray(response.getEmbeddings().get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        if (document == null) {
            return new float[0];
        }
        return embed(getEmbeddingContent(document));
    }

    @Override
    public int dimensions() {
        if (dimensions != null && dimensions > 0) {
            return dimensions;
        }
        return EmbeddingModel.super.dimensions();
    }

    private String resolveModel(EmbeddingRequest request) {
        if (request != null && request.getOptions() != null
            && StringUtils.hasText(request.getOptions().getModel())) {
            return request.getOptions().getModel();
        }
        if (!StringUtils.hasText(defaultModel)) {
            throw new IllegalStateException("Ollama embedding model 未配置");
        }
        return defaultModel;
    }

    private float[] toFloatArray(List<Double> values) {
        if (CollectionUtils.isEmpty(values)) {
            return new float[0];
        }
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = Objects.requireNonNullElse(values.get(i), 0.0d).floatValue();
        }
        return result;
    }

    private record OllamaEmbedRequest(String model, List<String> input) {
    }

    @Data
    private static class OllamaEmbedResponse {

        private List<List<Double>> embeddings;
    }
}
