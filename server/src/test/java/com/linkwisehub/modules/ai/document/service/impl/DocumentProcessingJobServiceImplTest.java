package com.linkwisehub.modules.ai.document.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwisehub.config.AiDocumentProcessingProperties;
import com.linkwisehub.modules.ai.document.client.AiDocumentProcessingClient;
import com.linkwisehub.modules.ai.document.entity.AiDocument;
import com.linkwisehub.modules.ai.document.entity.AiDocumentChunk;
import com.linkwisehub.modules.ai.document.entity.AiDocumentProcessingJob;
import com.linkwisehub.modules.ai.document.enums.DocumentParseStrategy;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentMapper;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentProcessingJobMapper;
import com.linkwisehub.modules.ai.document.service.DocumentParseService;
import com.linkwisehub.modules.ai.document.service.DocumentRagIndexService;
import com.linkwisehub.modules.ai.document.service.DocumentSplitConfigService;
import com.linkwisehub.modules.ai.document.service.DocumentStorageService;
import com.linkwisehub.modules.ai.document.service.TextCleanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentProcessingJobServiceImplTest {

    private AiDocumentProcessingJobMapper jobMapper;
    private AiDocumentMapper documentMapper;
    private AiDocumentProcessingClient processingClient;
    private DocumentStorageService storageService;
    private DocumentParseService parseService;
    private TextCleanService textCleanService;
    private DocumentSplitConfigService splitConfigService;
    private DocumentRagIndexService ragIndexService;
    private DocumentProcessingJobServiceImpl service;

    @BeforeEach
    void setUp() {
        jobMapper = mock(AiDocumentProcessingJobMapper.class);
        documentMapper = mock(AiDocumentMapper.class);
        processingClient = mock(AiDocumentProcessingClient.class);
        storageService = mock(DocumentStorageService.class);
        parseService = mock(DocumentParseService.class);
        textCleanService = mock(TextCleanService.class);
        splitConfigService = mock(DocumentSplitConfigService.class);
        ragIndexService = mock(DocumentRagIndexService.class);
        AiDocumentProcessingProperties properties = new AiDocumentProcessingProperties();
        service = new DocumentProcessingJobServiceImpl(
                jobMapper,
                documentMapper,
                processingClient,
                properties,
                storageService,
                parseService,
                textCleanService,
                splitConfigService,
                ragIndexService,
                new ObjectMapper()
        );
    }

    @Test
    void submitCreatesTrackedMineruJob() {
        AiDocument document = document();
        doAnswer(invocation -> {
            AiDocumentProcessingJob job = invocation.getArgument(0);
            job.setId(10L);
            return 1;
        }).when(jobMapper).insert(any(AiDocumentProcessingJob.class));
        when(processingClient.submit(document)).thenReturn(
                new AiDocumentProcessingClient.RemoteTask("remote-1", "PENDING", 0, null, null, null, null));
        when(jobMapper.selectById(10L)).thenAnswer(invocation -> {
            AiDocumentProcessingJob job = new AiDocumentProcessingJob();
            job.setId(10L);
            job.setDocumentId(1L);
            job.setParseEngine("AUTO");
            job.setStatus("SUBMITTED");
            return job;
        });

        AiDocumentProcessingJob result = service.submit(document, DocumentParseStrategy.AUTO);

        assertEquals(10L, result.getId());
        verify(jobMapper).cancelActiveByDocumentId(eq(1L), any());
        verify(jobMapper).updateSubmission(eq(10L), eq("remote-1"), eq("SUBMITTED"), eq(0), eq(0),
                eq(null), any());
    }

    @Test
    void submitFailureSchedulesRetryWithoutDroppingDocument() {
        AiDocument document = document();
        doAnswer(invocation -> {
            AiDocumentProcessingJob job = invocation.getArgument(0);
            job.setId(11L);
            return 1;
        }).when(jobMapper).insert(any(AiDocumentProcessingJob.class));
        when(processingClient.submit(document)).thenThrow(new IllegalStateException("unavailable"));
        when(jobMapper.selectById(11L)).thenAnswer(invocation -> {
            AiDocumentProcessingJob job = new AiDocumentProcessingJob();
            job.setId(11L);
            job.setDocumentId(1L);
            job.setStatus("RETRY_WAIT");
            return job;
        });

        AiDocumentProcessingJob result = service.submit(document, DocumentParseStrategy.AUTO);

        assertEquals("RETRY_WAIT", result.getStatus());
        verify(jobMapper).updateSubmission(eq(11L), eq(null), eq("RETRY_WAIT"), eq(0), eq(1),
                eq("unavailable"), any());
    }

    @Test
    void submitLegacyCreatesPendingJobWithoutCallingAiService() {
        AiDocument document = document();
        document.setFileType("txt");
        doAnswer(invocation -> {
            AiDocumentProcessingJob job = invocation.getArgument(0);
            job.setId(12L);
            return 1;
        }).when(jobMapper).insert(any(AiDocumentProcessingJob.class));
        when(jobMapper.selectById(12L)).thenAnswer(invocation -> {
            AiDocumentProcessingJob job = new AiDocumentProcessingJob();
            job.setId(12L);
            job.setDocumentId(1L);
            job.setParseEngine("LEGACY");
            job.setStatus("PENDING");
            return job;
        });

        AiDocumentProcessingJob result = service.submit(document, DocumentParseStrategy.LEGACY);

        assertEquals("PENDING", result.getStatus());
        verify(processingClient, never()).submit(any());
    }

    @Test
    void pollingLegacyJobParsesAndIndexesWithJavaParser() {
        AiDocument document = document();
        document.setFileType("txt");
        AiDocumentProcessingJob job = new AiDocumentProcessingJob();
        job.setId(13L);
        job.setDocumentId(1L);
        job.setParseEngine("LEGACY");
        job.setStatus("PENDING");
        job.setVersion(0);
        when(jobMapper.selectPollable(any(), any())).thenReturn(List.of(job));
        when(jobMapper.claim(eq(13L), eq(0), any(), any())).thenReturn(1);
        when(jobMapper.selectById(13L)).thenReturn(job);
        when(documentMapper.selectById(1L)).thenReturn(document);
        when(storageService.getObject(any(), any())).thenReturn(new ByteArrayInputStream("legacy".getBytes()));
        when(parseService.parse(any(), eq("txt"))).thenReturn("legacy");
        when(textCleanService.clean("legacy")).thenReturn("legacy");
        when(ragIndexService.index(any())).thenReturn(List.of(new AiDocumentChunk()));

        service.pollPendingJobs();

        verify(jobMapper).markRunning(eq(13L), any());
        verify(documentMapper).updateParseResult(1L, "SUCCESS", 1, null);
        verify(jobMapper).finish(eq(13L), eq("SUCCESS"), eq(100), eq(null), any());
        verify(processingClient, never()).getStatus(any());
    }

    @Test
    void retryValidatesStrategyBeforeChangingDocumentStatus() {
        AiDocument document = document();
        document.setFileType("png");
        when(documentMapper.selectById(1L)).thenReturn(document);

        assertThrows(RuntimeException.class, () -> service.retry(1L, DocumentParseStrategy.LEGACY));

        verify(documentMapper, never()).updateParseResult(any(), any(), any(), any());
    }

    @Test
    void batchLookupReturnsLatestTaskSummaryByDocumentId() {
        AiDocumentProcessingJob first = new AiDocumentProcessingJob();
        first.setDocumentId(1L);
        first.setParseEngine("AUTO");
        first.setStatus("RUNNING");
        first.setProgress(50);
        AiDocumentProcessingJob second = new AiDocumentProcessingJob();
        second.setDocumentId(2L);
        second.setParseEngine("LEGACY");
        second.setStatus("SUCCESS");
        second.setProgress(100);
        when(jobMapper.selectLatestByDocumentIds(List.of(1L, 2L))).thenReturn(List.of(first, second));

        Map<Long, String> statuses = service.getLatestByDocumentIds(List.of(1L, 2L)).entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getStatus()));

        assertEquals(Map.of(1L, "RUNNING", 2L, "SUCCESS"), statuses);
        verify(jobMapper).selectLatestByDocumentIds(List.of(1L, 2L));
    }

    private AiDocument document() {
        AiDocument document = new AiDocument();
        document.setId(1L);
        document.setFileName("sample.pdf");
        document.setFileType("pdf");
        document.setStorageBucket("knowledge-base");
        document.setStoragePath("documents/sample.pdf");
        document.setStatus(1);
        return document;
    }
}
