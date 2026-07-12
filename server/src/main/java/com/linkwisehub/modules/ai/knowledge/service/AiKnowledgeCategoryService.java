package com.linkwisehub.modules.ai.knowledge.service;

import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeCategoryRespDto;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeCategorySaveReqDto;

import java.util.List;

public interface AiKnowledgeCategoryService {
    List<AiKnowledgeCategoryRespDto> listTree();

    List<Long> listCategoryAndChildrenIds(Long categoryId);

    AiKnowledgeCategoryRespDto create(AiKnowledgeCategorySaveReqDto reqDto);

    AiKnowledgeCategoryRespDto update(Long id, AiKnowledgeCategorySaveReqDto reqDto);

    void delete(Long id);
}
