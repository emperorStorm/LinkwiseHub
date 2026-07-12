package com.linkwisehub.modules.ai.document.service;

import com.linkwisehub.modules.ai.document.dto.DocumentStorageResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface DocumentStorageService {
    DocumentStorageResult upload(MultipartFile file, String fileType);

    void overwrite(String bucketName, String objectName, InputStream inputStream, long size, String contentType);

    InputStream getObject(String bucketName, String objectName);

    void deleteQuietly(String bucketName, String objectName);
}
