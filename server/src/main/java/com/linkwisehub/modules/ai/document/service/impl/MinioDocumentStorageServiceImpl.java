package com.linkwisehub.modules.ai.document.service.impl;

import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.modules.ai.document.dto.DocumentStorageResult;
import com.linkwisehub.modules.ai.document.service.DocumentStorageService;
import com.linkwisehub.modules.ai.document.support.DocumentFileType;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

/**
 * AI 文档 MinIO 存储服务，统一把知识库相关文件放入专用 bucket。
 */
@Slf4j
@Service
public class MinioDocumentStorageServiceImpl implements DocumentStorageService {

    @Value("${ai.document.bucket-name:knowledge-base}")
    private String bucketName;

    private final MinioClient minioClient;

    public MinioDocumentStorageServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public DocumentStorageResult upload(MultipartFile file, String fileType) {
        ensureBucketExists();
        String objectName = buildObjectName(fileType);
        try {
            // 使用文件原始输入流直传 MinIO，避免在 JVM 中创建额外大对象。
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(resolveContentType(file.getContentType(), fileType))
                    .build());
            return new DocumentStorageResult(bucketName, objectName);
        } catch (Exception e) {
            log.error("上传 AI 文档到 MinIO 失败: bucket={}, object={}", bucketName, objectName, e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "上传文件到 MinIO 失败: " + e.getMessage());
        }
    }

    @Override
    public void overwrite(String bucketName, String objectName, InputStream inputStream, long size, String contentType) {
        String targetBucket = bucketName == null || bucketName.trim().isEmpty() ? this.bucketName : bucketName;
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文件存储路径不能为空");
        }
        try (InputStream in = inputStream) {
            ensureBucketExists(targetBucket);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(objectName)
                    .stream(in, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("覆盖 MinIO AI 文档失败: bucket={}, object={}", targetBucket, objectName, e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "保存 OnlyOffice 文件失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream getObject(String bucketName, String objectName) {
        String targetBucket = bucketName == null || bucketName.trim().isEmpty() ? this.bucketName : bucketName;
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文件存储路径不能为空");
        }
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("读取 MinIO AI 文档失败: bucket={}, object={}", targetBucket, objectName, e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "读取知识库文件失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteQuietly(String bucketName, String objectName) {
        String targetBucket = bucketName == null || bucketName.trim().isEmpty() ? this.bucketName : bucketName;
        if (objectName == null || objectName.trim().isEmpty()) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            // 删除文档时数据库软删优先，MinIO 清理失败只记录日志，避免影响业务状态。
            log.warn("删除 MinIO AI 文档失败: bucket={}, object={}", targetBucket, objectName, e);
        }
    }

    /**
     * 确保知识库 bucket 存在；MinIO bucket 名使用英文，避免 S3 兼容规则不接受中文。
     */
    private void ensureBucketExists() {
        ensureBucketExists(bucketName);
    }

    private void ensureBucketExists(String targetBucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(targetBucket)
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(targetBucket)
                        .build());
            }
        } catch (Exception e) {
            log.error("检查或创建知识库 MinIO bucket 失败: {}", targetBucket, e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "知识库文件桶不可用: " + e.getMessage());
        }
    }

    /**
     * 按日期组织对象路径，便于后续知识库文件管理和清理。
     */
    private String buildObjectName(String fileType) {
        LocalDate today = LocalDate.now();
        return String.format("documents/%d/%02d/%02d/%s.%s",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(), UUID.randomUUID(), fileType);
    }

    /**
     * 规范化文件类型，避免浏览器上传时 content-type 为空。
     */
    private String resolveContentType(String contentType, String fileType) {
        if (contentType != null && !contentType.trim().isEmpty()) {
            return contentType;
        }
        return switch (fileType) {
            case DocumentFileType.MD -> "text/markdown";
            case DocumentFileType.DOC -> "application/msword";
            case DocumentFileType.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case DocumentFileType.PPT -> "application/vnd.ms-powerpoint";
            case DocumentFileType.PPTX -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case DocumentFileType.XLS -> "application/vnd.ms-excel";
            case DocumentFileType.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case DocumentFileType.PDF -> "application/pdf";
            default -> "text/plain";
        };
    }
}
