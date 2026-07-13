package com.linkwisehub.modules.ai.document.enums;

import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.modules.ai.document.support.DocumentFileType;

import java.util.Locale;
import java.util.Set;

public enum DocumentParseStrategy {
    LEGACY,
    MINERU,
    AUTO;

    private static final Set<String> MINERU_AUTO_TYPES = Set.of(
            DocumentFileType.PDF,
            DocumentFileType.DOCX,
            DocumentFileType.PPTX,
            DocumentFileType.XLSX,
            DocumentFileType.PNG,
            DocumentFileType.JPG,
            DocumentFileType.JPEG,
            DocumentFileType.WEBP,
            DocumentFileType.TIFF
    );

    public boolean usesMineru(String fileType) {
        if (this == MINERU) {
            return DocumentFileType.isMineruSupported(fileType);
        }
        return this == AUTO && MINERU_AUTO_TYPES.contains(normalize(fileType));
    }

    public static DocumentParseStrategy resolve(String value, DocumentParseStrategy defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue == null ? LEGACY : defaultValue;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "解析策略仅支持 LEGACY、MINERU、AUTO");
        }
    }

    private static String normalize(String fileType) {
        return fileType == null ? "" : fileType.toLowerCase(Locale.ROOT);
    }
}
