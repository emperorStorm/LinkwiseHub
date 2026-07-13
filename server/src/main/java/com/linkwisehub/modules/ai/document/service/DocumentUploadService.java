package com.linkwisehub.modules.ai.document.service;

import com.linkwisehub.modules.ai.document.dto.AiDocumentUploadRespDto;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentUploadService {
    AiDocumentUploadRespDto uploadAndParse(MultipartFile file);

    AiDocumentUploadRespDto uploadAndParse(MultipartFile file, String strategy);
}
