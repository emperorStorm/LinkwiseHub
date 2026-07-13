package com.linkwisehub.modules.ai.document.service.impl;

import com.alibaba.fastjson.JSON;
import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.modules.ai.document.dto.AiDocumentChunkIndexDto;
import com.linkwisehub.modules.ai.document.dto.DocumentRagIndexRequest;
import com.linkwisehub.modules.ai.document.dto.ParsedDocumentBlock;
import com.linkwisehub.modules.ai.document.dto.VectorIndexRebuildRespDto;
import com.linkwisehub.modules.ai.document.entity.AiDocumentChunk;
import com.linkwisehub.modules.ai.document.entity.AiDocumentSplitConfig;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentChunkMapper;
import com.linkwisehub.modules.ai.document.service.DocumentRagIndexService;
import com.linkwisehub.modules.ai.document.service.DocumentSparseIndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 基于 Spring AI Alibaba/Spring AI 的 RAG 索引服务，统一负责切片和 Qdrant 写入。
 */
@Slf4j
@Service
public class DocumentRagIndexServiceImpl implements DocumentRagIndexService {

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int MIN_CHUNK_SIZE = 200;
    private static final int MAX_CHUNK_SIZE = 2000;
    private static final int MIN_CHUNK_SIZE_CHARS = 100;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 20;
    private static final int MAX_NUM_CHUNKS = 10000;
    private static final int REBUILD_BATCH_SIZE = 100;
    private static final String VECTOR_STATUS_SUCCESS = "SUCCESS";

    private final AiDocumentChunkMapper chunkMapper;
    private final VectorStore vectorStore;
    private final DocumentSparseIndexService documentSparseIndexService;

    public DocumentRagIndexServiceImpl(AiDocumentChunkMapper chunkMapper,
                                       VectorStore vectorStore,
                                       DocumentSparseIndexService documentSparseIndexService) {
        this.chunkMapper = chunkMapper;
        this.vectorStore = vectorStore;
        this.documentSparseIndexService = documentSparseIndexService;
    }

    @Override
    public List<AiDocumentChunk> index(DocumentRagIndexRequest request) {
        validateRequest(request);
        List<Document> splitDocuments = splitDocuments(request);
        if (splitDocuments.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未生成有效 Chunk");
        }

        List<AiDocumentChunk> chunks = new ArrayList<>(splitDocuments.size());
        List<Document> vectorDocuments = new ArrayList<>(splitDocuments.size());
        List<String> vectorIds = new ArrayList<>(splitDocuments.size());
        int chunkIndex = 1;
        for (Document splitDocument : splitDocuments) {
            String content = splitDocument.getText();
            if (!StringUtils.hasText(content)) {
                continue;
            }
            String vectorId = buildVectorId(request.getDocumentId(), chunkIndex);
            Map<String, Object> metadata = buildMetadata(request, splitDocument, chunkIndex, vectorId);
            Map<String, Object> vectorMetadata = normalizeMetadataForQdrant(metadata);
            chunks.add(buildChunk(request, content.trim(), chunkIndex, vectorId, metadata));
            vectorIds.add(vectorId);
            vectorDocuments.add(Document.builder()
                    .id(vectorId)
                    .text(content.trim())
                    .metadata(vectorMetadata)
                    .build());
            chunkIndex++;
        }
        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未生成有效 Chunk");
        }

        deleteIndexByDocumentId(request.getDocumentId());
        chunkMapper.updateStatusByDocumentId(request.getDocumentId(), 0);
        try {
            vectorStore.add(vectorDocuments);
            chunkMapper.batchInsert(chunks);
            documentSparseIndexService.indexDocument(request.getDocumentId());
        } catch (RuntimeException e) {
            deleteVectorIdsQuietly(vectorIds);
            documentSparseIndexService.deleteByDocumentId(request.getDocumentId());
            chunkMapper.updateStatusByDocumentId(request.getDocumentId(), 0);
            throw e;
        }
        return chunks;
    }

    @Override
    public void deleteIndexByDocumentId(Long documentId) {
        if (documentId == null) {
            return;
        }
        documentSparseIndexService.deleteByDocumentId(documentId);
        List<String> vectorIds = chunkMapper.selectVectorIdsByDocumentId(documentId);
        if (vectorIds == null || vectorIds.isEmpty()) {
            return;
        }
        List<String> validVectorIds = vectorIds.stream()
                .filter(this::isValidUuid)
                .collect(Collectors.toList());
        int invalidCount = vectorIds.size() - validVectorIds.size();
        if (invalidCount > 0) {
            log.warn("跳过历史非法 Qdrant 向量ID: documentId={}, invalidCount={}", documentId, invalidCount);
        }
        if (validVectorIds.isEmpty()) {
            return;
        }
        try {
            vectorStore.delete(validVectorIds);
        } catch (Exception e) {
            log.warn("删除 Qdrant 文档向量失败: documentId={}, vectorCount={}", documentId, validVectorIds.size(), e);
        }
    }

    @Override
    public VectorIndexRebuildRespDto rebuildVectorIndex() {
        int indexedCount = 0;
        int skippedCount = 0;
        int offset = 0;
        while (true) {
            List<AiDocumentChunkIndexDto> chunks = chunkMapper.selectIndexChunksPage(offset, REBUILD_BATCH_SIZE);
            if (chunks == null || chunks.isEmpty()) {
                break;
            }
            RebuildBatchResult result = rebuildVectorBatch(chunks);
            indexedCount += result.indexedCount;
            skippedCount += result.skippedCount;
            offset += chunks.size();
            if (chunks.size() < REBUILD_BATCH_SIZE) {
                break;
            }
        }
        return new VectorIndexRebuildRespDto(indexedCount, skippedCount);
    }

    private RebuildBatchResult rebuildVectorBatch(List<AiDocumentChunkIndexDto> chunks) {
        List<Document> vectorDocuments = new ArrayList<>(chunks.size());
        int skippedCount = 0;
        for (AiDocumentChunkIndexDto chunk : chunks) {
            if (!isValidIndexChunk(chunk)) {
                skippedCount++;
                continue;
            }
            String vectorId = resolveVectorId(chunk);
            Map<String, Object> metadata = buildRebuildMetadata(chunk, vectorId);
            vectorDocuments.add(Document.builder()
                    .id(vectorId)
                    .text(chunk.getContent().trim())
                    .metadata(normalizeMetadataForQdrant(metadata))
                    .build());
        }
        if (!vectorDocuments.isEmpty()) {
            vectorStore.add(vectorDocuments);
        }
        return new RebuildBatchResult(vectorDocuments.size(), skippedCount);
    }

    private void deleteVectorIdsQuietly(List<String> vectorIds) {
        if (vectorIds == null || vectorIds.isEmpty()) {
            return;
        }
        List<String> validVectorIds = vectorIds.stream()
                .filter(this::isValidUuid)
                .collect(Collectors.toList());
        if (validVectorIds.isEmpty()) {
            return;
        }
        try {
            vectorStore.delete(validVectorIds);
        } catch (Exception e) {
            log.warn("补偿删除 Qdrant 向量失败: vectorCount={}", validVectorIds.size(), e);
        }
    }

    private boolean isValidIndexChunk(AiDocumentChunkIndexDto chunk) {
        return chunk != null
                && chunk.getChunkId() != null
                && chunk.getDocumentId() != null
                && chunk.getChunkIndex() != null
                && StringUtils.hasText(chunk.getContent());
    }

    private String resolveVectorId(AiDocumentChunkIndexDto chunk) {
        if (isValidUuid(chunk.getVectorId())) {
            return chunk.getVectorId();
        }
        String vectorId = buildVectorId(chunk.getDocumentId(), chunk.getChunkIndex());
        chunkMapper.updateVectorIdByChunkId(chunk.getChunkId(), vectorId);
        return vectorId;
    }

    private Map<String, Object> buildRebuildMetadata(AiDocumentChunkIndexDto chunk, String vectorId) {
        Map<String, Object> metadata = new HashMap<>(8);
        metadata.put("documentId", chunk.getDocumentId());
        if (chunk.getCategoryId() != null) {
            metadata.put("categoryId", chunk.getCategoryId());
        }
        metadata.put("title", safeString(chunk.getTitle()));
        metadata.put("fileName", safeString(chunk.getFileName()));
        metadata.put("sourceType", safeString(chunk.getSourceType()));
        metadata.put("chunkIndex", chunk.getChunkIndex());
        metadata.put("vectorId", vectorId);
        metadata.put("businessVectorId", buildBusinessVectorId(chunk.getDocumentId(), chunk.getChunkIndex()));
        return metadata;
    }

    private List<Document> splitDocuments(DocumentRagIndexRequest request) {
        if (request.getBlocks() != null && !request.getBlocks().isEmpty()) {
            return splitParsedBlocks(request);
        }
        Document sourceDocument = Document.builder()
                .id("oa-doc-" + request.getDocumentId())
                .text(request.getText().trim())
                .metadata(buildSourceMetadata(request))
                .build();
        return buildSplitter(request.getSplitConfig()).split(sourceDocument);
    }

    private List<Document> splitParsedBlocks(DocumentRagIndexRequest request) {
        List<Document> documents = new ArrayList<>();
        TokenTextSplitter splitter = buildSplitter(request.getSplitConfig());
        for (ParsedDocumentBlock block : request.getBlocks()) {
            if (block == null || !StringUtils.hasText(block.getContent())) {
                continue;
            }
            Map<String, Object> metadata = buildSourceMetadata(request);
            metadata.put("sourcePage", block.getPage() == null ? 1 : block.getPage());
            metadata.put("blockType", safeString(block.getBlockType()));
            metadata.put("sourceTitle", StringUtils.hasText(block.getTitle()) ? block.getTitle() : resolveSourceTitle(request));
            if (block.getMetadata() != null) {
                metadata.putAll(block.getMetadata());
            }
            Document source = Document.builder()
                    .id("oa-doc-" + request.getDocumentId() + "-block-" + block.getIndex())
                    .text(block.getContent().trim())
                    .metadata(metadata)
                    .build();
            documents.addAll(splitter.split(source));
        }
        return documents;
    }

    private TokenTextSplitter buildSplitter(AiDocumentSplitConfig splitConfig) {
        return TokenTextSplitter.builder()
                .withChunkSize(resolveChunkSize(splitConfig))
                .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
                .withMinChunkLengthToEmbed(MIN_CHUNK_LENGTH_TO_EMBED)
                .withMaxNumChunks(MAX_NUM_CHUNKS)
                .withKeepSeparator(true)
                .build();
    }

    private AiDocumentChunk buildChunk(DocumentRagIndexRequest request, String content, Integer chunkIndex,
                                      String vectorId, Map<String, Object> metadata) {
        AiDocumentChunk chunk = new AiDocumentChunk();
        chunk.setDocumentId(request.getDocumentId());
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setContentLength(content.length());
        chunk.setSourceTitle(stringMetadata(metadata, "sourceTitle", resolveSourceTitle(request)));
        chunk.setSourcePage(integerMetadata(metadata, "sourcePage", 1));
        chunk.setSourceParagraph(chunkIndex);
        chunk.setMetadataJson(JSON.toJSONString(metadata));
        chunk.setVectorId(vectorId);
        chunk.setVectorStatus(VECTOR_STATUS_SUCCESS);
        chunk.setVectorErrorMessage(null);
        chunk.setStatus(1);
        return chunk;
    }

    private Map<String, Object> buildSourceMetadata(DocumentRagIndexRequest request) {
        Map<String, Object> metadata = new HashMap<>(8);
        metadata.put("documentId", request.getDocumentId());
        if (request.getCategoryId() != null) {
            metadata.put("categoryId", request.getCategoryId());
        }
        metadata.put("title", safeString(request.getTitle()));
        metadata.put("fileName", safeString(request.getFileName()));
        metadata.put("fileType", safeString(request.getFileType()));
        metadata.put("sourceType", safeString(request.getSourceType()));
        return metadata;
    }

    private Map<String, Object> buildMetadata(DocumentRagIndexRequest request, Document splitDocument,
                                               Integer chunkIndex, String vectorId) {
        Map<String, Object> metadata = buildSourceMetadata(request);
        if (splitDocument.getMetadata() != null) {
            metadata.putAll(splitDocument.getMetadata());
        }
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("vectorId", vectorId);
        metadata.put("businessVectorId", buildBusinessVectorId(request.getDocumentId(), chunkIndex));
        return metadata;
    }

    private Map<String, Object> normalizeMetadataForQdrant(Map<String, Object> metadata) {
        Map<String, Object> normalized = new HashMap<>(metadata.size());
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            normalized.put(entry.getKey(), normalizePayloadValue(entry.getValue()));
        }
        return normalized;
    }

    private Object normalizePayloadValue(Object value) {
        if (value instanceof Long) {
            return value.toString();
        }
        return value;
    }

    private String buildVectorId(Long documentId, Integer chunkIndex) {
        String sourceId = buildBusinessVectorId(documentId, chunkIndex);
        return UUID.nameUUIDFromBytes(sourceId.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private boolean isValidUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String buildBusinessVectorId(Long documentId, Integer chunkIndex) {
        return "oa-doc-" + documentId + "-chunk-" + chunkIndex;
    }

    private String resolveSourceTitle(DocumentRagIndexRequest request) {
        if (StringUtils.hasText(request.getTitle())) {
            return request.getTitle().trim();
        }
        return safeString(request.getFileName());
    }

    private int resolveChunkSize(AiDocumentSplitConfig config) {
        if (config == null || config.getTargetChunkLength() == null) {
            return DEFAULT_CHUNK_SIZE;
        }
        return Math.max(MIN_CHUNK_SIZE, Math.min(MAX_CHUNK_SIZE, config.getTargetChunkLength()));
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String stringMetadata(Map<String, Object> metadata, String key, String defaultValue) {
        Object value = metadata.get(key);
        return value == null || value.toString().isBlank() ? defaultValue : value.toString();
    }

    private Integer integerMetadata(Map<String, Object> metadata, String key, Integer defaultValue) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? defaultValue : Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void validateRequest(DocumentRagIndexRequest request) {
        if (request == null || request.getDocumentId() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文档ID不能为空");
        }
        boolean hasText = StringUtils.hasText(request.getText());
        boolean hasBlocks = request.getBlocks() != null && request.getBlocks().stream()
                .anyMatch(block -> block != null && StringUtils.hasText(block.getContent()));
        if (!hasText && !hasBlocks) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文档内容为空");
        }
    }

    private static class RebuildBatchResult {
        private final int indexedCount;
        private final int skippedCount;

        private RebuildBatchResult(int indexedCount, int skippedCount) {
            this.indexedCount = indexedCount;
            this.skippedCount = skippedCount;
        }
    }
}
