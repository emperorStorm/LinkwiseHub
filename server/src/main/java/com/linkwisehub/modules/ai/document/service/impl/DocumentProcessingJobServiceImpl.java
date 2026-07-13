package com.linkwisehub.modules.ai.document.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.config.AiDocumentProcessingProperties;
import com.linkwisehub.modules.ai.document.client.AiDocumentProcessingClient;
import com.linkwisehub.modules.ai.document.dto.DocumentParseJobRespDto;
import com.linkwisehub.modules.ai.document.dto.DocumentRagIndexRequest;
import com.linkwisehub.modules.ai.document.dto.ParsedDocumentBlock;
import com.linkwisehub.modules.ai.document.entity.AiDocument;
import com.linkwisehub.modules.ai.document.entity.AiDocumentChunk;
import com.linkwisehub.modules.ai.document.entity.AiDocumentProcessingJob;
import com.linkwisehub.modules.ai.document.enums.DocumentParseStrategy;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentMapper;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentProcessingJobMapper;
import com.linkwisehub.modules.ai.document.service.DocumentParseService;
import com.linkwisehub.modules.ai.document.service.DocumentProcessingJobService;
import com.linkwisehub.modules.ai.document.service.DocumentRagIndexService;
import com.linkwisehub.modules.ai.document.service.DocumentSplitConfigService;
import com.linkwisehub.modules.ai.document.service.DocumentStorageService;
import com.linkwisehub.modules.ai.document.service.TextCleanService;
import com.linkwisehub.modules.ai.document.support.DocumentFileType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DocumentProcessingJobServiceImpl implements DocumentProcessingJobService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final AiDocumentProcessingJobMapper jobMapper;
    private final AiDocumentMapper documentMapper;
    private final AiDocumentProcessingClient processingClient;
    private final AiDocumentProcessingProperties properties;
    private final DocumentStorageService storageService;
    private final DocumentParseService parseService;
    private final TextCleanService textCleanService;
    private final DocumentSplitConfigService splitConfigService;
    private final DocumentRagIndexService ragIndexService;
    private final ObjectMapper objectMapper;
    private final String workerId = UUID.randomUUID().toString();

    public DocumentProcessingJobServiceImpl(AiDocumentProcessingJobMapper jobMapper,
                                            AiDocumentMapper documentMapper,
                                            AiDocumentProcessingClient processingClient,
                                            AiDocumentProcessingProperties properties,
                                            DocumentStorageService storageService,
                                            DocumentParseService parseService,
                                            TextCleanService textCleanService,
                                            DocumentSplitConfigService splitConfigService,
                                            DocumentRagIndexService ragIndexService,
                                            ObjectMapper objectMapper) {
        this.jobMapper = jobMapper;
        this.documentMapper = documentMapper;
        this.processingClient = processingClient;
        this.properties = properties;
        this.storageService = storageService;
        this.parseService = parseService;
        this.textCleanService = textCleanService;
        this.splitConfigService = splitConfigService;
        this.ragIndexService = ragIndexService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiDocumentProcessingJob submit(AiDocument document, DocumentParseStrategy strategy) {
        validateDocument(document, strategy);
        cancelActive(document.getId());
        AiDocumentProcessingJob job = new AiDocumentProcessingJob();
        job.setDocumentId(document.getId());
        job.setTaskType("DOCUMENT_PARSE");
        job.setParseEngine(strategy.name());
        job.setStatus("PENDING");
        job.setProgress(0);
        job.setRetryCount(0);
        job.setNextPollTime(LocalDateTime.now());
        job.setVersion(0);
        jobMapper.insert(job);
        if (strategy.usesMineru(document.getFileType())) {
            trySubmit(job, document);
        }
        return jobMapper.selectById(job.getId());
    }

    @Override
    public DocumentParseJobRespDto getLatest(Long documentId) {
        ensureActiveDocument(documentId);
        return toRespDto(jobMapper.selectLatestByDocumentId(documentId));
    }

    @Override
    public DocumentParseJobRespDto retry(Long documentId, DocumentParseStrategy strategy) {
        AiDocument document = ensureActiveDocument(documentId);
        validateDocument(document, strategy);
        documentMapper.updateParseResult(documentId, STATUS_PROCESSING, 0, null);
        return toRespDto(submit(document, strategy));
    }

    @Override
    public void cancelActive(Long documentId) {
        if (documentId != null) {
            jobMapper.cancelActiveByDocumentId(documentId, LocalDateTime.now());
        }
    }

    @Scheduled(fixedDelayString = "${ai.document.processing.scheduler-delay-ms:3000}")
    public void pollPendingJobs() {
        if (!properties.isSchedulerEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<AiDocumentProcessingJob> jobs = jobMapper.selectPollable(now, properties.getPollBatchSize());
        for (AiDocumentProcessingJob job : jobs) {
            long lockSeconds = Math.max(60, properties.getRequestTimeoutSeconds() + 60L);
            if (jobMapper.claim(job.getId(), job.getVersion(), workerId, now.plusSeconds(lockSeconds)) == 0) {
                continue;
            }
            try {
                processJob(jobMapper.selectById(job.getId()));
            } catch (Exception e) {
                log.warn("处理 MinerU 文档任务失败: jobId={}, documentId={}", job.getId(), job.getDocumentId(), e);
                scheduleRetry(jobMapper.selectById(job.getId()), e.getMessage());
            }
        }
    }

    private void processJob(AiDocumentProcessingJob job) {
        if (isTimedOut(job)) {
            failJob(job, "文档解析超过 " + properties.getTaskTimeoutMinutes() + " 分钟");
            return;
        }
        AiDocument document = ensureActiveDocument(job.getDocumentId());
        DocumentParseStrategy strategy = DocumentParseStrategy.resolve(job.getParseEngine(), DocumentParseStrategy.LEGACY);
        if (strategy.usesMineru(document.getFileType())) {
            processRemoteJob(job, document);
            return;
        }
        processLegacyJob(job, document);
    }

    private void processRemoteJob(AiDocumentProcessingJob job, AiDocument document) {
        if (!StringUtils.hasText(job.getProviderTaskId())) {
            trySubmit(job, document);
            return;
        }
        AiDocumentProcessingClient.RemoteTask remoteTask = processingClient.getStatus(job.getProviderTaskId());
        if ("FAILED".equals(remoteTask.status())) {
            scheduleRetry(job, remoteTask.errorMessage());
            return;
        }
        if (!"SUCCESS".equals(remoteTask.status())) {
            jobMapper.updateRemoteStatus(job.getId(), remoteTask.status(), remoteTask.progress(), null,
                    LocalDateTime.now().plusSeconds(nextPollSeconds(job)), parseRemoteTime(remoteTask.startedAt()));
            return;
        }
        materializeAndIndex(job, document);
    }

    private void processLegacyJob(AiDocumentProcessingJob job, AiDocument document) {
        jobMapper.markRunning(job.getId(), LocalDateTime.now());
        String parsedText;
        try (InputStream inputStream = storageService.getObject(document.getStorageBucket(), document.getStoragePath())) {
            parsedText = parseService.parse(inputStream, document.getFileType());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_PARSE_ERROR, "旧解析器读取文档失败: " + e.getMessage());
        }
        String contentText = htmlToText(document.getContentHtml());
        String combinedText = contentText.isBlank()
                ? parsedText
                : "# " + document.getTitle() + "\n\n" + contentText + "\n\n" + parsedText;
        String cleanText = textCleanService.clean(combinedText);
        if (!StringUtils.hasText(cleanText)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文件内容为空");
        }
        DocumentRagIndexRequest request = new DocumentRagIndexRequest();
        request.setDocumentId(document.getId());
        request.setCategoryId(document.getCategoryId());
        request.setTitle(document.getTitle());
        request.setFileName(document.getFileName());
        request.setFileType(document.getFileType());
        request.setSourceType(document.getSourceType());
        request.setText(cleanText);
        request.setSplitConfig(splitConfigService.getEffectiveConfig());
        List<AiDocumentChunk> chunks = ragIndexService.index(request);
        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_PARSE_ERROR, "旧解析器未生成有效 Chunk");
        }
        documentMapper.updateParseResult(document.getId(), STATUS_SUCCESS, chunks.size(), null);
        jobMapper.finish(job.getId(), STATUS_SUCCESS, 100, null, LocalDateTime.now());
    }

    private void trySubmit(AiDocumentProcessingJob job, AiDocument document) {
        try {
            AiDocumentProcessingClient.RemoteTask task = processingClient.submit(document);
            jobMapper.updateSubmission(job.getId(), task.taskId(), "SUBMITTED", task.progress(),
                    job.getRetryCount(), null, LocalDateTime.now().plusSeconds(properties.getInitialPollSeconds()));
        } catch (Exception e) {
            scheduleRetry(job, e.getMessage());
        }
    }

    private void materializeAndIndex(AiDocumentProcessingJob job, AiDocument document) {
        AiDocumentProcessingClient.MaterializedResult result = processingClient.materialize(
                job.getProviderTaskId(), document.getId());
        AiDocumentProcessingClient.Artifacts artifacts = result.artifacts();
        jobMapper.updateArtifacts(job.getId(), artifacts.bucket(), artifacts.manifestObject(),
                artifacts.markdownObject(), artifacts.blocksObject());
        List<ParsedDocumentBlock> blocks = readBlocks(artifacts.bucket(), artifacts.blocksObject());
        appendKnowledgeContent(document, blocks);
        DocumentRagIndexRequest request = new DocumentRagIndexRequest();
        request.setDocumentId(document.getId());
        request.setCategoryId(document.getCategoryId());
        request.setTitle(document.getTitle());
        request.setFileName(document.getFileName());
        request.setFileType(document.getFileType());
        request.setSourceType(document.getSourceType());
        request.setBlocks(blocks);
        request.setSplitConfig(splitConfigService.getEffectiveConfig());
        List<AiDocumentChunk> chunks = ragIndexService.index(request);
        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_PARSE_ERROR, "MinerU 解析结果未生成有效 Chunk");
        }
        documentMapper.updateParseResult(document.getId(), STATUS_SUCCESS, chunks.size(), null);
        jobMapper.finish(job.getId(), STATUS_SUCCESS, 100, null, LocalDateTime.now());
    }

    private List<ParsedDocumentBlock> readBlocks(String bucket, String objectName) {
        try (InputStream inputStream = storageService.getObject(bucket, objectName)) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_PARSE_ERROR, "读取 MinerU 标准化结果失败: " + e.getMessage());
        }
    }

    private void appendKnowledgeContent(AiDocument document, List<ParsedDocumentBlock> blocks) {
        String text = htmlToText(document.getContentHtml());
        if (!text.isBlank()) {
            ParsedDocumentBlock contentBlock = new ParsedDocumentBlock();
            contentBlock.setIndex(0);
            contentBlock.setBlockType("rich_text");
            contentBlock.setContent("# " + document.getTitle() + "\n\n" + text);
            contentBlock.setTitle(document.getTitle());
            blocks.add(0, contentBlock);
        }
    }

    private String htmlToText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String normalized = html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>|</div>|</li>", "\n");
        return HTML_TAG_PATTERN.matcher(normalized).replaceAll("")
                .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&amp;", "&").replace("&quot;", "\"").trim();
    }

    private void scheduleRetry(AiDocumentProcessingJob job, String errorMessage) {
        int retryCount = (job.getRetryCount() == null ? 0 : job.getRetryCount()) + 1;
        if (retryCount >= properties.getMaxAttempts()) {
            failJob(job, errorMessage);
            return;
        }
        jobMapper.updateSubmission(job.getId(), null, "RETRY_WAIT", 0, retryCount, limitError(errorMessage),
                LocalDateTime.now().plusSeconds(backoffSeconds(retryCount)));
    }

    private void failJob(AiDocumentProcessingJob job, String errorMessage) {
        String safeError = limitError(errorMessage);
        AiDocument document = documentMapper.selectById(job.getDocumentId());
        if (!indexKnowledgeContentFallback(document, safeError)) {
            documentMapper.updateParseResult(job.getDocumentId(), STATUS_FAILED, 0, safeError);
        }
        jobMapper.finish(job.getId(), STATUS_FAILED, 0, safeError, LocalDateTime.now());
    }

    private boolean indexKnowledgeContentFallback(AiDocument document, String warning) {
        if (document == null || !StringUtils.hasText(document.getContentHtml())) {
            return false;
        }
        String content = textCleanService.clean("# " + document.getTitle() + "\n\n" + htmlToText(document.getContentHtml()));
        if (!StringUtils.hasText(content)) {
            return false;
        }
        try {
            DocumentRagIndexRequest request = new DocumentRagIndexRequest();
            request.setDocumentId(document.getId());
            request.setCategoryId(document.getCategoryId());
            request.setTitle(document.getTitle());
            request.setFileName(document.getFileName());
            request.setFileType(document.getFileType());
            request.setSourceType(document.getSourceType());
            request.setText(content);
            request.setSplitConfig(splitConfigService.getEffectiveConfig());
            List<AiDocumentChunk> chunks = ragIndexService.index(request);
            documentMapper.updateParseResult(document.getId(), STATUS_SUCCESS, chunks.size(), warning);
            return true;
        } catch (Exception e) {
            log.warn("知识文档正文降级索引失败: documentId={}", document.getId(), e);
            return false;
        }
    }

    private int nextPollSeconds(AiDocumentProcessingJob job) {
        LocalDateTime startedAt = job.getStartedAt() == null ? job.getCreateTime() : job.getStartedAt();
        long elapsedSeconds = startedAt == null ? 0 : Math.max(0, Duration.between(startedAt, LocalDateTime.now()).toSeconds());
        int multiplier = elapsedSeconds < 30 ? 1 : elapsedSeconds < 120 ? 2 : elapsedSeconds < 300 ? 3 : 5;
        return Math.min(properties.getMaxPollSeconds(), properties.getInitialPollSeconds() * multiplier);
    }

    private int backoffSeconds(int retryCount) {
        int multiplier = 1 << Math.min(retryCount - 1, 4);
        return Math.min(properties.getMaxPollSeconds(), properties.getInitialPollSeconds() * multiplier);
    }

    private boolean isTimedOut(AiDocumentProcessingJob job) {
        return job.getCreateTime() != null
                && job.getCreateTime().plusMinutes(properties.getTaskTimeoutMinutes()).isBefore(LocalDateTime.now());
    }

    private void validateDocument(AiDocument document, DocumentParseStrategy strategy) {
        if (strategy == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "解析策略不能为空");
        }
        if (strategy.usesMineru(document.getFileType())) {
            return;
        }
        if (strategy == DocumentParseStrategy.MINERU) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该文件类型不支持 MINERU 解析");
        }
        if (!DocumentFileType.isLegacySupported(document.getFileType())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该文件类型不支持 LEGACY 解析");
        }
    }

    private AiDocument ensureActiveDocument(Long documentId) {
        AiDocument document = documentMapper.selectById(documentId);
        if (document == null || document.getStatus() == null || document.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "文档不存在");
        }
        return document;
    }

    private DocumentParseJobRespDto toRespDto(AiDocumentProcessingJob job) {
        if (job == null) {
            return null;
        }
        DocumentParseJobRespDto dto = new DocumentParseJobRespDto();
        dto.setJobId(job.getId());
        dto.setDocumentId(job.getDocumentId());
        dto.setParseEngine(job.getParseEngine());
        dto.setStatus(job.getStatus());
        dto.setProgress(job.getProgress());
        dto.setRetryCount(job.getRetryCount());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setStartedAt(job.getStartedAt());
        dto.setFinishedAt(job.getFinishedAt());
        dto.setCreateTime(job.getCreateTime());
        return dto;
    }

    private String limitError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "AI 文档解析失败";
        }
        return errorMessage.length() <= 1000 ? errorMessage : errorMessage.substring(0, 1000);
    }

    private LocalDateTime parseRemoteTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(value);
            } catch (Exception invalidTime) {
                return null;
            }
        }
    }
}
