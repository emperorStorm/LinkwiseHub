package com.linkwisehub.modules.ai.service.impl;

import com.linkwisehub.modules.ai.document.dto.AiDocumentChunkRespDto;
import com.linkwisehub.modules.ai.document.dto.DocumentSparseSearchResult;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentChunkMapper;
import com.linkwisehub.modules.ai.document.service.DocumentSparseIndexService;
import com.linkwisehub.modules.ai.service.LocalKnowledgeSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地知识库检索服务，先向量召回，再回查数据库确认文档仍为已发布状态。
 */
@Slf4j
@Service
public class LocalKnowledgeSearchServiceImpl implements LocalKnowledgeSearchService {

    private static final int TOP_K = 6;
    private static final int CANDIDATE_TOP_K = 20;
    private static final int RRF_K = 60;
    private static final double SIMILARITY_THRESHOLD = 0.2D;

    private final VectorStore vectorStore;
    private final AiDocumentChunkMapper chunkMapper;
    private final DocumentSparseIndexService documentSparseIndexService;

    public LocalKnowledgeSearchServiceImpl(VectorStore vectorStore,
                                           AiDocumentChunkMapper chunkMapper,
                                           DocumentSparseIndexService documentSparseIndexService) {
        this.vectorStore = vectorStore;
        this.chunkMapper = chunkMapper;
        this.documentSparseIndexService = documentSparseIndexService;
    }

    @Override
    public List<SearchResult> search(String question) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        List<Candidate> vectorCandidates = searchVectorCandidates(question);
        List<Candidate> sparseCandidates = searchSparseCandidates(question);
        List<Candidate> mergedCandidates = mergeCandidates(vectorCandidates, sparseCandidates);
        if (mergedCandidates.isEmpty()) {
            return List.of();
        }
        List<SearchResult> results = new ArrayList<>(Math.min(TOP_K, mergedCandidates.size()));
        for (Candidate candidate : mergedCandidates) {
            if (candidate.chunk != null && StringUtils.hasText(candidate.chunk.getContent())) {
                results.add(new SearchResult(candidate.chunk.getId(), resolveTitle(candidate.chunk), candidate.chunk.getContent().trim()));
            }
            if (results.size() >= TOP_K) {
                break;
            }
        }
        return results;
    }

    private List<Candidate> searchVectorCandidates(String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question.trim())
                .topK(CANDIDATE_TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();
        List<Document> documents;
        try {
            documents = vectorStore.similaritySearch(request);
        } catch (RuntimeException e) {
            log.warn("本地知识库向量检索失败", e);
            return List.of();
        }
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<String> vectorIds = new ArrayList<>(documents.size());
        for (Document document : documents) {
            if (document != null && StringUtils.hasText(document.getId())) {
                vectorIds.add(document.getId());
            }
        }
        if (vectorIds.isEmpty()) {
            return List.of();
        }

        List<AiDocumentChunkRespDto> chunks = chunkMapper.selectPublishedByVectorIds(vectorIds);
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        Map<String, AiDocumentChunkRespDto> chunkMap = new LinkedHashMap<>(chunks.size());
        for (AiDocumentChunkRespDto chunk : chunks) {
            chunkMap.put(chunk.getVectorId(), chunk);
        }
        List<Candidate> results = new ArrayList<>(chunks.size());
        int rank = 1;
        for (String vectorId : vectorIds) {
            AiDocumentChunkRespDto chunk = chunkMap.get(vectorId);
            if (chunk != null && StringUtils.hasText(chunk.getContent())) {
                results.add(Candidate.vector(chunk, rank));
                rank++;
            }
        }
        return results;
    }

    private List<Candidate> searchSparseCandidates(String question) {
        List<DocumentSparseSearchResult> sparseResults = documentSparseIndexService.search(question);
        if (sparseResults == null || sparseResults.isEmpty()) {
            return List.of();
        }
        List<Long> chunkIds = new ArrayList<>(sparseResults.size());
        for (DocumentSparseSearchResult result : sparseResults) {
            if (result != null && result.getChunkId() != null) {
                chunkIds.add(result.getChunkId());
            }
        }
        if (chunkIds.isEmpty()) {
            return List.of();
        }
        List<AiDocumentChunkRespDto> chunks = chunkMapper.selectPublishedByChunkIds(chunkIds);
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        Map<Long, AiDocumentChunkRespDto> chunkMap = new HashMap<>(chunks.size());
        for (AiDocumentChunkRespDto chunk : chunks) {
            chunkMap.put(chunk.getId(), chunk);
        }
        List<Candidate> candidates = new ArrayList<>(chunks.size());
        int rank = 1;
        for (Long chunkId : chunkIds) {
            AiDocumentChunkRespDto chunk = chunkMap.get(chunkId);
            if (chunk != null && StringUtils.hasText(chunk.getContent())) {
                candidates.add(Candidate.sparse(chunk, rank));
                rank++;
            }
        }
        return candidates;
    }

    private List<Candidate> mergeCandidates(List<Candidate> vectorCandidates, List<Candidate> sparseCandidates) {
        Map<Long, Candidate> mergedMap = new LinkedHashMap<>();
        addCandidates(mergedMap, vectorCandidates, true);
        addCandidates(mergedMap, sparseCandidates, false);
        return mergedMap.values().stream()
                .sorted(Comparator.comparingDouble(Candidate::getScore).reversed())
                .toList();
    }

    private void addCandidates(Map<Long, Candidate> mergedMap, List<Candidate> candidates, boolean vector) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (Candidate candidate : candidates) {
            if (candidate == null || candidate.chunk == null || candidate.chunk.getId() == null) {
                continue;
            }
            Candidate merged = mergedMap.computeIfAbsent(candidate.chunk.getId(), key -> new Candidate(candidate.chunk));
            double score = 1.0D / (RRF_K + candidate.rank);
            if (vector) {
                merged.vectorRank = candidate.rank;
            } else {
                merged.sparseRank = candidate.rank;
            }
            merged.score += score;
        }
    }

    private String resolveTitle(AiDocumentChunkRespDto chunk) {
        if (StringUtils.hasText(chunk.getSourceTitle())) {
            return chunk.getSourceTitle().trim();
        }
        return StringUtils.hasText(chunk.getDocumentName()) ? chunk.getDocumentName().trim() : "本地知识库";
    }

    private static class Candidate {
        private final AiDocumentChunkRespDto chunk;
        private int rank;
        private int vectorRank;
        private int sparseRank;
        private double score;

        private Candidate(AiDocumentChunkRespDto chunk) {
            this.chunk = chunk;
        }

        private static Candidate vector(AiDocumentChunkRespDto chunk, int rank) {
            Candidate candidate = new Candidate(chunk);
            candidate.rank = rank;
            candidate.vectorRank = rank;
            candidate.score = 1.0D / (RRF_K + rank);
            return candidate;
        }

        private static Candidate sparse(AiDocumentChunkRespDto chunk, int rank) {
            Candidate candidate = new Candidate(chunk);
            candidate.rank = rank;
            candidate.sparseRank = rank;
            candidate.score = 1.0D / (RRF_K + rank);
            return candidate;
        }

        private double getScore() {
            return score;
        }
    }
}
