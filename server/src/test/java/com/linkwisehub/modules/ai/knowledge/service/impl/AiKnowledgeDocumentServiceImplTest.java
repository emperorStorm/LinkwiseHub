package com.linkwisehub.modules.ai.knowledge.service.impl;

import com.linkwisehub.config.AiDocumentProcessingProperties;
import com.linkwisehub.modules.ai.document.dto.DocumentRagIndexRequest;
import com.linkwisehub.modules.ai.document.dto.DocumentStorageResult;
import com.linkwisehub.modules.ai.document.entity.AiDocument;
import com.linkwisehub.modules.ai.document.entity.AiDocumentChunk;
import com.linkwisehub.modules.ai.document.entity.AiDocumentSplitConfig;
import com.linkwisehub.modules.ai.document.enums.DocumentParseStrategy;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentChunkMapper;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentMapper;
import com.linkwisehub.modules.ai.document.service.DocumentProcessingJobService;
import com.linkwisehub.modules.ai.document.service.DocumentRagIndexService;
import com.linkwisehub.modules.ai.document.service.DocumentSparseIndexService;
import com.linkwisehub.modules.ai.document.service.DocumentSplitConfigService;
import com.linkwisehub.modules.ai.document.service.DocumentStorageService;
import com.linkwisehub.modules.ai.document.service.TextCleanService;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeDocumentReqDto;
import com.linkwisehub.modules.ai.knowledge.entity.AiKnowledgeCategory;
import com.linkwisehub.modules.ai.knowledge.mapper.AiKnowledgeCategoryMapper;
import com.linkwisehub.modules.ai.knowledge.service.AiKnowledgeCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiKnowledgeDocumentServiceImplTest {

    private AiDocumentMapper documentMapper;
    private AiKnowledgeCategoryMapper categoryMapper;
    private DocumentStorageService documentStorageService;
    private DocumentSplitConfigService documentSplitConfigService;
    private TextCleanService textCleanService;
    private DocumentRagIndexService documentRagIndexService;
    private DocumentProcessingJobService processingJobService;
    private AiKnowledgeDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        documentMapper = mock(AiDocumentMapper.class);
        AiDocumentChunkMapper chunkMapper = mock(AiDocumentChunkMapper.class);
        categoryMapper = mock(AiKnowledgeCategoryMapper.class);
        AiKnowledgeCategoryService categoryService = mock(AiKnowledgeCategoryService.class);
        documentStorageService = mock(DocumentStorageService.class);
        documentSplitConfigService = mock(DocumentSplitConfigService.class);
        textCleanService = mock(TextCleanService.class);
        documentRagIndexService = mock(DocumentRagIndexService.class);
        DocumentSparseIndexService documentSparseIndexService = mock(DocumentSparseIndexService.class);
        processingJobService = mock(DocumentProcessingJobService.class);
        AiDocumentProcessingProperties processingProperties = new AiDocumentProcessingProperties();

        service = new AiKnowledgeDocumentServiceImpl(
                documentMapper,
                chunkMapper,
                categoryMapper,
                categoryService,
                documentStorageService,
                documentSplitConfigService,
                textCleanService,
                documentRagIndexService,
                documentSparseIndexService,
                processingJobService,
                processingProperties
        );
    }

    @Test
    void createWithFileOnlySubmitsUnifiedAsyncParseJob() {
        MockMultipartFile file = file();
        when(categoryMapper.selectById(1L)).thenReturn(activeCategory());
        when(documentStorageService.upload(file, "pdf")).thenReturn(new DocumentStorageResult("knowledge-base", "doc.pdf"));
        mockDocumentPersistence(file, "FILE");

        service.create(reqDto(""), file);

        verify(processingJobService).submit(any(AiDocument.class), any());
        verify(processingJobService).submit(any(AiDocument.class), eq(DocumentParseStrategy.AUTO));
        verify(documentRagIndexService, never()).index(any());
    }

    @Test
    void createWithContentAndFileDefersAttachmentFallbackToJobService() {
        MockMultipartFile file = file();
        when(categoryMapper.selectById(1L)).thenReturn(activeCategory());
        when(documentStorageService.upload(file, "pdf")).thenReturn(new DocumentStorageResult("knowledge-base", "doc.pdf"));
        mockDocumentPersistence(file, "MIXED");

        service.create(reqDto("<p>正文内容</p>"), file);

        verify(processingJobService).submit(any(AiDocument.class), any());
        verify(documentRagIndexService, never()).index(any());
    }

    @Test
    void createWithContentOnlyIndexesImmediately() {
        when(categoryMapper.selectById(1L)).thenReturn(activeCategory());
        when(textCleanService.clean(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentSplitConfigService.getEffectiveConfig()).thenReturn(splitConfig());
        when(documentRagIndexService.index(any(DocumentRagIndexRequest.class))).thenReturn(List.of(chunk()));
        mockDocumentPersistence(null, "CONTENT");

        service.create(reqDto("<p>正文内容</p>"), null);

        verify(documentRagIndexService).index(any(DocumentRagIndexRequest.class));
        verify(processingJobService, never()).submit(any(), any());
    }

    private void mockDocumentPersistence(MockMultipartFile file, String sourceType) {
        when(documentMapper.insert(any(AiDocument.class))).thenAnswer(invocation -> {
            AiDocument document = invocation.getArgument(0);
            document.setId(10L);
            return 1;
        });
        when(documentMapper.selectById(10L)).thenAnswer(invocation -> {
            AiDocument document = new AiDocument();
            document.setId(10L);
            document.setCategoryId(1L);
            document.setTitle("测试文档");
            document.setFileName(file == null ? "测试文档.txt" : "sample.pdf");
            document.setFileType(file == null ? "txt" : "pdf");
            document.setFileSize(file == null ? 0L : file.getSize());
            document.setStorageBucket(file == null ? "" : "knowledge-base");
            document.setStoragePath(file == null ? "" : "doc.pdf");
            document.setSourceType(sourceType);
            document.setPublishStatus("DRAFT");
            document.setStatus(1);
            return document;
        });
    }

    private AiKnowledgeDocumentReqDto reqDto(String contentHtml) {
        AiKnowledgeDocumentReqDto reqDto = new AiKnowledgeDocumentReqDto();
        reqDto.setCategoryId(1L);
        reqDto.setTitle("测试文档");
        reqDto.setContentHtml(contentHtml);
        reqDto.setPublishStatus("DRAFT");
        return reqDto;
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "sample.pdf", "application/pdf", "bad pdf".getBytes());
    }

    private AiKnowledgeCategory activeCategory() {
        AiKnowledgeCategory category = new AiKnowledgeCategory();
        category.setId(1L);
        category.setStatus(1);
        category.setName("默认知识库");
        return category;
    }

    private AiDocumentSplitConfig splitConfig() {
        AiDocumentSplitConfig config = new AiDocumentSplitConfig();
        config.setId(1L);
        config.setTargetChunkLength(800);
        return config;
    }

    private AiDocumentChunk chunk() {
        AiDocumentChunk chunk = new AiDocumentChunk();
        chunk.setId(1L);
        chunk.setDocumentId(10L);
        chunk.setChunkIndex(1);
        chunk.setContent("正文内容");
        chunk.setStatus(1);
        return chunk;
    }
}
