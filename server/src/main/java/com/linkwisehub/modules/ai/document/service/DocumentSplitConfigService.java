package com.linkwisehub.modules.ai.document.service;

import com.linkwisehub.modules.ai.document.dto.AiDocumentSplitConfigRespDto;
import com.linkwisehub.modules.ai.document.dto.AiDocumentSplitConfigSaveReqDto;
import com.linkwisehub.modules.ai.document.entity.AiDocumentSplitConfig;

public interface DocumentSplitConfigService {
    AiDocumentSplitConfigRespDto getConfig();

    AiDocumentSplitConfig getEffectiveConfig();

    AiDocumentSplitConfigRespDto saveConfig(AiDocumentSplitConfigSaveReqDto reqDto);
}
