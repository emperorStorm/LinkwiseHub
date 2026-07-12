package com.linkwisehub.modules.ai.knowledge.controller;

import com.linkwisehub.common.ApiResponse;
import com.linkwisehub.modules.ai.document.dto.AiDocumentChunkRespDto;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeCategoryRespDto;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeCategorySaveReqDto;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeDocumentReqDto;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeDocumentRespDto;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgePublishStatusReqDto;
import com.linkwisehub.modules.ai.knowledge.dto.OnlyOfficePreviewConfigRespDto;
import com.linkwisehub.modules.ai.knowledge.service.AiKnowledgeCategoryService;
import com.linkwisehub.modules.ai.knowledge.service.AiKnowledgeDocumentService;
import com.linkwisehub.modules.ai.knowledge.service.OnlyOfficePreviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/knowledge")
public class AiKnowledgeController {

    private final AiKnowledgeCategoryService categoryService;
    private final AiKnowledgeDocumentService documentService;
    private final OnlyOfficePreviewService onlyOfficePreviewService;

    public AiKnowledgeController(AiKnowledgeCategoryService categoryService,
                                 AiKnowledgeDocumentService documentService,
                                 OnlyOfficePreviewService onlyOfficePreviewService) {
        this.categoryService = categoryService;
        this.documentService = documentService;
        this.onlyOfficePreviewService = onlyOfficePreviewService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<AiKnowledgeCategoryRespDto>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.listTree()));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<AiKnowledgeCategoryRespDto>> createCategory(@Valid @RequestBody AiKnowledgeCategorySaveReqDto reqDto) {
        return ResponseEntity.ok(ApiResponse.success("分类创建成功", categoryService.create(reqDto)));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<AiKnowledgeCategoryRespDto>> updateCategory(@PathVariable Long id,
                                                                                 @Valid @RequestBody AiKnowledgeCategorySaveReqDto reqDto) {
        return ResponseEntity.ok(ApiResponse.success("分类保存成功", categoryService.update(id, reqDto)));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("分类删除成功"));
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<AiKnowledgeDocumentRespDto>>> listDocuments(@RequestParam(required = false) Long categoryId,
                                                                                      @RequestParam(required = false) String keyword,
                                                                                      @RequestParam(required = false) String publishStatus) {
        return ResponseEntity.ok(ApiResponse.success(documentService.listDocuments(categoryId, keyword, publishStatus)));
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<AiKnowledgeDocumentRespDto>> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(documentService.getDocument(id)));
    }

    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<AiKnowledgeDocumentRespDto>> createDocument(@Valid @ModelAttribute AiKnowledgeDocumentReqDto reqDto,
                                                                                 @RequestParam(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("知识文档创建成功", documentService.create(reqDto, file)));
    }

    @PutMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<AiKnowledgeDocumentRespDto>> updateDocument(@PathVariable Long id,
                                                                                 @Valid @ModelAttribute AiKnowledgeDocumentReqDto reqDto,
                                                                                 @RequestParam(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("知识文档保存成功", documentService.update(id, reqDto, file)));
    }

    @PutMapping("/documents/{id}/publish-status")
    public ResponseEntity<ApiResponse<AiKnowledgeDocumentRespDto>> updatePublishStatus(@PathVariable Long id,
                                                                                      @Valid @RequestBody AiKnowledgePublishStatusReqDto reqDto) {
        return ResponseEntity.ok(ApiResponse.success("发布状态已更新", documentService.updatePublishStatus(id, reqDto.getPublishStatus())));
    }

    @GetMapping("/documents/{id}/chunks")
    public ResponseEntity<ApiResponse<List<AiDocumentChunkRespDto>>> listChunks(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(documentService.listChunks(id)));
    }

    @GetMapping("/documents/{id}/preview-config")
    public ResponseEntity<ApiResponse<OnlyOfficePreviewConfigRespDto>> getPreviewConfig(@PathVariable Long id,
                                                                                       @RequestParam(defaultValue = "view") String mode) {
        return ResponseEntity.ok(ApiResponse.success(onlyOfficePreviewService.buildPreviewConfig(id, mode)));
    }

    @GetMapping("/documents/{id}/file")
    public ResponseEntity<StreamingResponseBody> getPreviewFile(@PathVariable Long id,
                                                                @RequestParam String token) {
        OnlyOfficePreviewService.PreviewFile file = onlyOfficePreviewService.openPreviewFile(id, token);
        return buildFileResponse(file, true);
    }

    @GetMapping("/documents/{id}/attachment")
    public ResponseEntity<StreamingResponseBody> downloadAttachment(@PathVariable Long id) {
        OnlyOfficePreviewService.PreviewFile file = onlyOfficePreviewService.openDownloadFile(id);
        return buildFileResponse(file, false);
    }

    @DeleteMapping("/documents/{id}/attachment")
    public ResponseEntity<ApiResponse<AiKnowledgeDocumentRespDto>> deleteAttachment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("附件已删除", documentService.deleteAttachment(id)));
    }

    @PostMapping("/onlyoffice/callback")
    public ResponseEntity<Map<String, Integer>> onlyOfficeCallback(@RequestBody(required = false) Map<String, Object> callbackBody) {
        onlyOfficePreviewService.handleCallback(callbackBody == null ? Map.of() : callbackBody);
        return ResponseEntity.ok(Map.of("error", 0));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("知识文档删除成功"));
    }

    private ResponseEntity<StreamingResponseBody> buildFileResponse(OnlyOfficePreviewService.PreviewFile file, boolean inline) {
        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = file.getInputStream()) {
                inputStream.transferTo(outputStream);
            }
        };
        String dispositionType = inline ? "inline" : "attachment";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename*=UTF-8''" + encodeFileName(file.getFileName()))
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(body);
    }

    private String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
