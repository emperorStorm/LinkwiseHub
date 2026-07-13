package com.linkwisehub.modules.ai.document.support;

import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 文档解析支持格式定义，供上传校验和解析服务统一复用。
 */
public final class DocumentFileType {

    public static final String TXT = "txt";
    public static final String MD = "md";
    public static final String DOC = "doc";
    public static final String DOCX = "docx";
    public static final String PPT = "ppt";
    public static final String PPTX = "pptx";
    public static final String XLS = "xls";
    public static final String XLSX = "xlsx";
    public static final String PDF = "pdf";
    public static final String PNG = "png";
    public static final String JPG = "jpg";
    public static final String JPEG = "jpeg";
    public static final String WEBP = "webp";
    public static final String TIFF = "tiff";

    private static final Set<String> LEGACY_TYPES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            TXT, MD, DOC, DOCX, PPT, PPTX, XLS, XLSX, PDF
    )));
    private static final Set<String> MINERU_TYPES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            PDF, DOCX, PPTX, XLSX, PNG, JPG, JPEG, WEBP, TIFF
    )));
    private static final Set<String> SUPPORTED_TYPES;
    private static final String SUPPORTED_MESSAGE = "支持 txt、md、doc、docx、ppt、pptx、xls、xlsx、pdf 和常见图片文件";

    static {
        LinkedHashSet<String> supportedTypes = new LinkedHashSet<>(LEGACY_TYPES);
        supportedTypes.addAll(MINERU_TYPES);
        SUPPORTED_TYPES = Collections.unmodifiableSet(supportedTypes);
    }

    private DocumentFileType() {
    }

    public static String resolveFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, SUPPORTED_MESSAGE);
        }
        String fileType = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!isSupported(fileType)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, SUPPORTED_MESSAGE);
        }
        return fileType;
    }

    public static boolean isSupported(String fileType) {
        return fileType != null && SUPPORTED_TYPES.contains(fileType.toLowerCase(Locale.ROOT));
    }

    public static boolean isLegacySupported(String fileType) {
        return fileType != null && LEGACY_TYPES.contains(fileType.toLowerCase(Locale.ROOT));
    }

    public static boolean isMineruSupported(String fileType) {
        return fileType != null && MINERU_TYPES.contains(fileType.toLowerCase(Locale.ROOT));
    }

    public static String getSupportedMessage() {
        return SUPPORTED_MESSAGE;
    }
}
