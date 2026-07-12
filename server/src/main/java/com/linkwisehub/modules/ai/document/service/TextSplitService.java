package com.linkwisehub.modules.ai.document.service;

import com.linkwisehub.modules.ai.document.entity.AiDocumentChunk;
import com.linkwisehub.modules.ai.document.entity.AiDocumentSplitConfig;

import java.util.List;

public interface TextSplitService {
    List<AiDocumentChunk> split(Long documentId, String fileName, String fileType, String text, AiDocumentSplitConfig config);
}
