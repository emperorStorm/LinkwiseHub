package com.linkwisehub.modules.ai.knowledge.service.impl;

import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentMapper;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeCategoryRespDto;
import com.linkwisehub.modules.ai.knowledge.dto.AiKnowledgeCategorySaveReqDto;
import com.linkwisehub.modules.ai.knowledge.entity.AiKnowledgeCategory;
import com.linkwisehub.modules.ai.knowledge.mapper.AiKnowledgeCategoryMapper;
import com.linkwisehub.modules.ai.knowledge.service.AiKnowledgeCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiKnowledgeCategoryServiceImpl implements AiKnowledgeCategoryService {

    private static final long ROOT_PARENT_ID = 0L;

    private final AiKnowledgeCategoryMapper categoryMapper;
    private final AiDocumentMapper documentMapper;

    public AiKnowledgeCategoryServiceImpl(AiKnowledgeCategoryMapper categoryMapper,
                                          AiDocumentMapper documentMapper) {
        this.categoryMapper = categoryMapper;
        this.documentMapper = documentMapper;
    }

    @Override
    public List<AiKnowledgeCategoryRespDto> listTree() {
        List<AiKnowledgeCategory> categories = categoryMapper.selectAll();
        Map<Long, AiKnowledgeCategoryRespDto> dtoMap = categories.stream()
                .map(this::toRespDto)
                .collect(Collectors.toMap(AiKnowledgeCategoryRespDto::getId, item -> item, (left, right) -> left));
        List<AiKnowledgeCategoryRespDto> roots = new ArrayList<>();
        for (AiKnowledgeCategory category : categories) {
            AiKnowledgeCategoryRespDto dto = dtoMap.get(category.getId());
            Long parentId = normalizeParentId(category.getParentId());
            if (ROOT_PARENT_ID == parentId || !dtoMap.containsKey(parentId)) {
                roots.add(dto);
            } else {
                dtoMap.get(parentId).getChildren().add(dto);
            }
        }
        return roots;
    }

    @Override
    public List<Long> listCategoryAndChildrenIds(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            return List.of();
        }
        ensureActiveCategory(categoryId);
        List<AiKnowledgeCategory> categories = categoryMapper.selectAll();
        Map<Long, List<Long>> childMap = buildChildIdMap(categories);
        List<Long> ids = new ArrayList<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            Long id = queue.removeFirst();
            ids.add(id);
            for (Long childId : childMap.getOrDefault(id, List.of())) {
                queue.addLast(childId);
            }
        }
        return ids;
    }

    @Override
    @Transactional
    public AiKnowledgeCategoryRespDto create(AiKnowledgeCategorySaveReqDto reqDto) {
        Long parentId = normalizeParentId(reqDto.getParentId());
        if (parentId > 0) {
            ensureActiveCategory(parentId);
        }
        AiKnowledgeCategory category = new AiKnowledgeCategory();
        category.setParentId(parentId);
        category.setName(reqDto.getName().trim());
        category.setSort(reqDto.getSort() == null ? 0 : reqDto.getSort());
        category.setStatus(1);
        categoryMapper.insert(category);
        return toRespDto(categoryMapper.selectById(category.getId()));
    }

    @Override
    @Transactional
    public AiKnowledgeCategoryRespDto update(Long id, AiKnowledgeCategorySaveReqDto reqDto) {
        AiKnowledgeCategory category = ensureActiveCategory(id);
        Long parentId = normalizeParentId(reqDto.getParentId());
        if (id.equals(parentId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "父级分类不能选择自己");
        }
        if (parentId > 0) {
            ensureActiveCategory(parentId);
            if (listCategoryAndChildrenIds(id).contains(parentId)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "父级分类不能选择自己的子级");
            }
        }
        category.setParentId(parentId);
        category.setName(reqDto.getName().trim());
        category.setSort(reqDto.getSort() == null ? 0 : reqDto.getSort());
        categoryMapper.update(category);
        return toRespDto(categoryMapper.selectById(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ensureActiveCategory(id);
        if (categoryMapper.countByParentId(id) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该分类下存在子分类，不能删除");
        }
        if (documentMapper.countByCategoryId(List.of(id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该分类下存在知识文档，不能删除");
        }
        categoryMapper.updateStatus(id, 0);
    }

    private AiKnowledgeCategory ensureActiveCategory(Long id) {
        AiKnowledgeCategory category = categoryMapper.selectById(id);
        if (category == null || category.getStatus() == null || category.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "分类不存在");
        }
        return category;
    }

    private Map<Long, List<Long>> buildChildIdMap(List<AiKnowledgeCategory> categories) {
        Map<Long, List<Long>> childMap = new HashMap<>();
        Set<Long> ids = categories.stream().map(AiKnowledgeCategory::getId).collect(Collectors.toSet());
        for (AiKnowledgeCategory category : categories) {
            Long parentId = normalizeParentId(category.getParentId());
            if (parentId > 0 && ids.contains(parentId)) {
                childMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(category.getId());
            }
        }
        return childMap;
    }

    private AiKnowledgeCategoryRespDto toRespDto(AiKnowledgeCategory category) {
        AiKnowledgeCategoryRespDto dto = new AiKnowledgeCategoryRespDto();
        dto.setId(category.getId());
        dto.setParentId(normalizeParentId(category.getParentId()));
        dto.setName(category.getName());
        dto.setSort(category.getSort());
        dto.setCreateTime(category.getCreateTime());
        dto.setUpdateTime(category.getUpdateTime());
        return dto;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId < 0 ? ROOT_PARENT_ID : parentId;
    }
}
