package com.linkwisehub.modules.ai.document.dto;

import lombok.Data;

/**
 * 文档对象存储结果，记录 bucket 和 objectName 便于后续定位文件。
 */
@Data
public class DocumentStorageResult {
    private final String bucketName;
    private final String objectName;
}
