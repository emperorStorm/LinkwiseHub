package com.linkwisehub.modules.ai.knowledge.service;

import com.linkwisehub.modules.ai.knowledge.dto.OnlyOfficePreviewConfigRespDto;

import java.io.InputStream;
import java.util.Map;

public interface OnlyOfficePreviewService {
    OnlyOfficePreviewConfigRespDto buildPreviewConfig(Long documentId, String mode);

    PreviewFile openPreviewFile(Long documentId, String token);

    PreviewFile openDownloadFile(Long documentId);

    void handleCallback(Map<String, Object> callbackBody);

    class PreviewFile {
        private final String fileName;
        private final String contentType;
        private final InputStream inputStream;

        public PreviewFile(String fileName, String contentType, InputStream inputStream) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.inputStream = inputStream;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public InputStream getInputStream() {
            return inputStream;
        }
    }
}
