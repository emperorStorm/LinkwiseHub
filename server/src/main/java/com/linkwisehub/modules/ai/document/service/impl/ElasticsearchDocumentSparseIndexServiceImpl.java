package com.linkwisehub.modules.ai.document.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.linkwisehub.config.AiSearchElasticsearchProperties;
import com.linkwisehub.modules.ai.document.dto.AiDocumentChunkIndexDto;
import com.linkwisehub.modules.ai.document.dto.DocumentSparseSearchResult;
import com.linkwisehub.modules.ai.document.dto.SparseIndexRebuildRespDto;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentChunkMapper;
import com.linkwisehub.modules.ai.document.service.DocumentSparseIndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 基于 Elasticsearch BM25 的稀疏检索索引服务。
 */
@Slf4j
@Service
public class ElasticsearchDocumentSparseIndexServiceImpl implements DocumentSparseIndexService {

    private static final String PUBLISH_PUBLISHED = "PUBLISHED";
    private static final int ACTIVE_STATUS = 1;

    private final AiDocumentChunkMapper chunkMapper;
    private final AiSearchElasticsearchProperties properties;
    private final WebClient webClient;

    private volatile boolean indexReady;

    public ElasticsearchDocumentSparseIndexServiceImpl(AiDocumentChunkMapper chunkMapper,
                                                       AiSearchElasticsearchProperties properties) {
        this.chunkMapper = chunkMapper;
        this.properties = properties;
        this.webClient = buildWebClient(properties);
    }

    @Override
    public void indexDocument(Long documentId) {
        if (!isEnabled() || documentId == null) {
            return;
        }
        List<AiDocumentChunkIndexDto> chunks = chunkMapper.selectIndexChunksByDocumentId(documentId);
        indexChunks(chunks);
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (!isEnabled() || documentId == null) {
            return;
        }
        try {
            ensureIndex();
            JSONObject body = new JSONObject();
            JSONObject query = new JSONObject();
            JSONObject term = new JSONObject();
            term.put("documentId", documentId);
            query.put("term", term);
            body.put("query", query);
            webClient.post()
                    .uri("/{index}/_delete_by_query?conflicts=proceed&refresh=true", properties.getIndexName())
                    .bodyValue(body.toJSONString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.warn("删除 ES 稀疏索引失败: documentId={}", documentId, e);
        }
    }

    @Override
    public void updatePublishStatus(Long documentId, String publishStatus) {
        if (!isEnabled() || documentId == null) {
            return;
        }
        try {
            ensureIndex();
            JSONObject body = new JSONObject();
            JSONObject script = new JSONObject();
            JSONObject params = new JSONObject();
            params.put("publishStatus", publishStatus);
            script.put("source", "ctx._source.publishStatus = params.publishStatus");
            script.put("params", params);
            body.put("script", script);

            JSONObject query = new JSONObject();
            JSONObject term = new JSONObject();
            term.put("documentId", documentId);
            query.put("term", term);
            body.put("query", query);

            webClient.post()
                    .uri("/{index}/_update_by_query?conflicts=proceed&refresh=true", properties.getIndexName())
                    .bodyValue(body.toJSONString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.warn("更新 ES 稀疏索引发布状态失败: documentId={}", documentId, e);
        }
    }

    @Override
    public List<DocumentSparseSearchResult> search(String question) {
        if (!isEnabled() || !StringUtils.hasText(question)) {
            return List.of();
        }
        try {
            ensureIndex();
            JSONObject body = buildSearchBody(question.trim());
            String response = webClient.post()
                    .uri("/{index}/_search", properties.getIndexName())
                    .bodyValue(body.toJSONString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return parseSearchResults(response);
        } catch (Exception e) {
            log.warn("ES BM25 检索失败，降级为纯向量检索", e);
            return List.of();
        }
    }

    @Override
    public SparseIndexRebuildRespDto rebuildAll() {
        if (!isEnabled()) {
            return new SparseIndexRebuildRespDto(false, 0);
        }
        ensureIndex();
        int indexedCount = 0;
        int offset = 0;
        int batchSize = Math.max(50, properties.getRebuildBatchSize());
        while (true) {
            List<AiDocumentChunkIndexDto> chunks = chunkMapper.selectIndexChunksPage(offset, batchSize);
            if (chunks == null || chunks.isEmpty()) {
                break;
            }
            indexChunks(chunks);
            indexedCount += chunks.size();
            offset += chunks.size();
            if (chunks.size() < batchSize) {
                break;
            }
        }
        return new SparseIndexRebuildRespDto(true, indexedCount);
    }

    private void indexChunks(List<AiDocumentChunkIndexDto> chunks) {
        if (!isEnabled() || chunks == null || chunks.isEmpty()) {
            return;
        }
        try {
            ensureIndex();
            StringBuilder bulkBody = new StringBuilder(chunks.size() * 512);
            for (AiDocumentChunkIndexDto chunk : chunks) {
                if (chunk.getChunkId() == null || !StringUtils.hasText(chunk.getContent())) {
                    continue;
                }
                JSONObject indexLine = new JSONObject();
                JSONObject index = new JSONObject();
                index.put("_index", properties.getIndexName());
                index.put("_id", chunk.getChunkId().toString());
                indexLine.put("index", index);
                bulkBody.append(indexLine.toJSONString()).append('\n');
                bulkBody.append(buildIndexDocument(chunk).toJSONString()).append('\n');
            }
            if (bulkBody.length() == 0) {
                return;
            }
            webClient.post()
                    .uri("/_bulk?refresh=true")
                    .contentType(MediaType.APPLICATION_NDJSON)
                    .bodyValue(bulkBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.warn("写入 ES 稀疏索引失败，主链路继续执行", e);
        }
    }

    private void ensureIndex() {
        if (!isEnabled() || indexReady) {
            return;
        }
        synchronized (this) {
            if (indexReady) {
                return;
            }
            try {
                Boolean exists = webClient.head()
                        .uri("/{index}", properties.getIndexName())
                        .exchangeToMono(response -> response.statusCode().is2xxSuccessful()
                                ? reactor.core.publisher.Mono.just(true)
                                : reactor.core.publisher.Mono.just(false))
                        .block();
                if (!Boolean.TRUE.equals(exists)) {
                    webClient.put()
                            .uri("/{index}", properties.getIndexName())
                            .bodyValue(buildIndexMapping().toJSONString())
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();
                }
                indexReady = true;
            } catch (Exception e) {
                throw new IllegalStateException("初始化 ES 稀疏索引失败", e);
            }
        }
    }

    private JSONObject buildIndexMapping() {
        JSONObject root = new JSONObject();
        JSONObject mappings = new JSONObject();
        JSONObject propertiesJson = new JSONObject();
        putField(propertiesJson, "chunkId", "long");
        putField(propertiesJson, "documentId", "long");
        putField(propertiesJson, "categoryId", "long");
        putField(propertiesJson, "publishStatus", "keyword");
        putField(propertiesJson, "status", "integer");
        putField(propertiesJson, "title", "text");
        putField(propertiesJson, "fileName", "keyword");
        putField(propertiesJson, "content", "text");
        putField(propertiesJson, "sourceType", "keyword");
        putField(propertiesJson, "chunkIndex", "integer");
        putField(propertiesJson, "updateTime", "date");
        mappings.put("properties", propertiesJson);
        root.put("mappings", mappings);
        return root;
    }

    private void putField(JSONObject propertiesJson, String name, String type) {
        JSONObject field = new JSONObject();
        field.put("type", type);
        propertiesJson.put(name, field);
    }

    private JSONObject buildIndexDocument(AiDocumentChunkIndexDto chunk) {
        JSONObject document = new JSONObject();
        document.put("chunkId", chunk.getChunkId());
        document.put("documentId", chunk.getDocumentId());
        document.put("categoryId", chunk.getCategoryId());
        document.put("publishStatus", chunk.getPublishStatus());
        document.put("status", resolveSearchableStatus(chunk));
        document.put("title", resolveTitle(chunk));
        document.put("fileName", safeString(chunk.getFileName()));
        document.put("content", safeString(chunk.getContent()));
        document.put("sourceType", safeString(chunk.getSourceType()));
        document.put("chunkIndex", chunk.getChunkIndex());
        if (chunk.getUpdateTime() != null) {
            document.put("updateTime", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(chunk.getUpdateTime()));
        }
        return document;
    }

    private int resolveSearchableStatus(AiDocumentChunkIndexDto chunk) {
        return Integer.valueOf(ACTIVE_STATUS).equals(chunk.getDocumentStatus())
                && Integer.valueOf(ACTIVE_STATUS).equals(chunk.getChunkStatus()) ? ACTIVE_STATUS : 0;
    }

    private JSONObject buildSearchBody(String question) {
        JSONObject body = new JSONObject();
        body.put("size", Math.max(1, properties.getTopK()));
        JSONObject bool = new JSONObject();
        JSONArray filter = new JSONArray();
        filter.add(termFilter("status", ACTIVE_STATUS));
        filter.add(termFilter("publishStatus", PUBLISH_PUBLISHED));
        bool.put("filter", filter);

        JSONObject multiMatch = new JSONObject();
        multiMatch.put("query", question);
        JSONArray fields = new JSONArray();
        fields.add("title^" + properties.getTitleBoost());
        fields.add("content^" + properties.getContentBoost());
        multiMatch.put("fields", fields);
        JSONObject must = new JSONObject();
        must.put("multi_match", multiMatch);
        bool.put("must", must);
        JSONObject query = new JSONObject();
        query.put("bool", bool);
        body.put("query", query);
        return body;
    }

    private JSONObject termFilter(String field, Object value) {
        JSONObject term = new JSONObject();
        term.put(field, value);
        JSONObject filter = new JSONObject();
        filter.put("term", term);
        return filter;
    }

    private List<DocumentSparseSearchResult> parseSearchResults(String response) {
        if (!StringUtils.hasText(response)) {
            return List.of();
        }
        JSONObject root = JSONObject.parseObject(response);
        JSONObject hitsRoot = root.getJSONObject("hits");
        if (hitsRoot == null) {
            return List.of();
        }
        JSONArray hits = hitsRoot.getJSONArray("hits");
        if (hits == null || hits.isEmpty()) {
            return List.of();
        }
        List<DocumentSparseSearchResult> results = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            JSONObject hit = hits.getJSONObject(i);
            JSONObject source = hit.getJSONObject("_source");
            if (source == null) {
                continue;
            }
            DocumentSparseSearchResult result = new DocumentSparseSearchResult();
            result.setChunkId(source.getLong("chunkId"));
            result.setDocumentId(source.getLong("documentId"));
            result.setTitle(source.getString("title"));
            result.setContent(source.getString("content"));
            result.setScore(hit.getDouble("_score"));
            result.setRank(i + 1);
            results.add(result);
        }
        return results;
    }

    private WebClient buildWebClient(AiSearchElasticsearchProperties properties) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(trimTrailingSlash(properties.getUris()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (StringUtils.hasText(properties.getUsername()) && StringUtils.hasText(properties.getPassword())) {
            String token = properties.getUsername() + ":" + properties.getPassword();
            String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }
        return builder.build();
    }

    private String trimTrailingSlash(String uri) {
        if (!StringUtils.hasText(uri)) {
            return "http://10.211.55.6:9200";
        }
        String value = uri.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String resolveTitle(AiDocumentChunkIndexDto chunk) {
        if (StringUtils.hasText(chunk.getSourceTitle())) {
            return chunk.getSourceTitle().trim();
        }
        if (StringUtils.hasText(chunk.getTitle())) {
            return chunk.getTitle().trim();
        }
        return safeString(chunk.getFileName());
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private boolean isEnabled() {
        return properties.isEnabled();
    }
}
