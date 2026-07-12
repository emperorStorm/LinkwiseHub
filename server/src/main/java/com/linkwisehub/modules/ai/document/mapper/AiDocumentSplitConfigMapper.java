package com.linkwisehub.modules.ai.document.mapper;

import com.linkwisehub.modules.ai.document.entity.AiDocumentSplitConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiDocumentSplitConfigMapper {
    AiDocumentSplitConfig selectActive();

    int insertDefault(AiDocumentSplitConfig config);

    int update(AiDocumentSplitConfig config);
}
