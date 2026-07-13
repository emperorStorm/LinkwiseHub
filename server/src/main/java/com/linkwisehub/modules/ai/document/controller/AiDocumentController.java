package com.linkwisehub.modules.ai.document.controller;

import com.linkwisehub.common.ApiResponse;
import com.linkwisehub.config.AiDocumentProcessingProperties;
import com.linkwisehub.modules.ai.document.dto.AiDocumentChunkRespDto;
import com.linkwisehub.modules.ai.document.dto.AiDocumentRespDto;
import com.linkwisehub.modules.ai.document.dto.AiDocumentSplitConfigRespDto;
import com.linkwisehub.modules.ai.document.dto.AiDocumentSplitConfigSaveReqDto;
import com.linkwisehub.modules.ai.document.dto.AiDocumentUploadRespDto;
import com.linkwisehub.modules.ai.document.dto.DocumentParseJobRespDto;
import com.linkwisehub.modules.ai.document.dto.DocumentParseRetryReqDto;
import com.linkwisehub.modules.ai.document.dto.SparseIndexRebuildRespDto;
import com.linkwisehub.modules.ai.document.dto.VectorIndexRebuildRespDto;
import com.linkwisehub.modules.ai.document.enums.DocumentParseStrategy;
import com.linkwisehub.modules.ai.document.service.DocumentProcessingJobService;
import com.linkwisehub.modules.ai.document.service.DocumentQueryService;
import com.linkwisehub.modules.ai.document.service.DocumentRagIndexService;
import com.linkwisehub.modules.ai.document.service.DocumentSplitConfigService;
import com.linkwisehub.modules.ai.document.service.DocumentSparseIndexService;
import com.linkwisehub.modules.ai.document.service.DocumentUploadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 文档分片控制器，提供上传解析、RAG 切片索引和 Chunk 预览接口。
 */
@RestController
@RequestMapping("/api/ai/documents")
public class AiDocumentController {

    private final DocumentUploadService documentUploadService;
    private final DocumentQueryService documentQueryService;
    private final DocumentSplitConfigService documentSplitConfigService;
    private final DocumentSparseIndexService documentSparseIndexService;
    private final DocumentRagIndexService documentRagIndexService;
    private final DocumentProcessingJobService processingJobService;
    private final AiDocumentProcessingProperties processingProperties;

    public AiDocumentController(DocumentUploadService documentUploadService,
                                DocumentQueryService documentQueryService,
                                DocumentSplitConfigService documentSplitConfigService,
                                DocumentSparseIndexService documentSparseIndexService,
                                DocumentRagIndexService documentRagIndexService,
                                DocumentProcessingJobService processingJobService,
                                AiDocumentProcessingProperties processingProperties) {
        this.documentUploadService = documentUploadService;
        this.documentQueryService = documentQueryService;
        this.documentSplitConfigService = documentSplitConfigService;
        this.documentSparseIndexService = documentSparseIndexService;
        this.documentRagIndexService = documentRagIndexService;
        this.processingJobService = processingJobService;
        this.processingProperties = processingProperties;
    }

    @GetMapping("/split-config")
    public ResponseEntity<ApiResponse<AiDocumentSplitConfigRespDto>> getSplitConfig() {
        return ResponseEntity.ok(ApiResponse.success(documentSplitConfigService.getConfig()));
    }

    @PutMapping("/split-config")
    public ResponseEntity<ApiResponse<AiDocumentSplitConfigRespDto>> saveSplitConfig(@Valid @RequestBody AiDocumentSplitConfigSaveReqDto reqDto) {
        return ResponseEntity.ok(ApiResponse.success("配置已保存，仅影响后续上传文档", documentSplitConfigService.saveConfig(reqDto)));
    }

    @PostMapping("/sparse-index/rebuild")
    public ResponseEntity<ApiResponse<SparseIndexRebuildRespDto>> rebuildSparseIndex() {
        return ResponseEntity.ok(ApiResponse.success("ES 稀疏索引重建完成", documentSparseIndexService.rebuildAll()));
    }

    @PostMapping("/vector-index/rebuild")
    public ResponseEntity<ApiResponse<VectorIndexRebuildRespDto>> rebuildVectorIndex() {
        return ResponseEntity.ok(ApiResponse.success("Qdrant 向量索引重建完成", documentRagIndexService.rebuildVectorIndex()));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<AiDocumentUploadRespDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "strategy", required = false) String strategy) {
        AiDocumentUploadRespDto result = documentUploadService.uploadAndParse(file, strategy);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("文档已进入解析队列", result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AiDocumentRespDto>>> listDocuments() {
        return ResponseEntity.ok(ApiResponse.success(documentQueryService.listDocuments()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AiDocumentRespDto>> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(documentQueryService.getDocument(id)));
    }

    @GetMapping("/{id}/chunks")
    public ResponseEntity<ApiResponse<List<AiDocumentChunkRespDto>>> listChunks(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(documentQueryService.listChunks(id)));
    }

    @GetMapping("/{id}/parse-job")
    public ResponseEntity<ApiResponse<DocumentParseJobRespDto>> getParseJob(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(processingJobService.getLatest(id)));
    }

    @PostMapping("/{id}/parse/retry")
    public ResponseEntity<ApiResponse<DocumentParseJobRespDto>> retryParse(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) DocumentParseRetryReqDto reqDto) {
        String requestedStrategy = reqDto == null ? null : reqDto.getStrategy();
        DocumentParseStrategy strategy = DocumentParseStrategy.resolve(requestedStrategy, processingProperties.getStrategy());
        DocumentParseJobRespDto result = processingJobService.retry(id, strategy);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("文档已重新提交解析", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        documentQueryService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success("文档删除成功"));
    }
}
