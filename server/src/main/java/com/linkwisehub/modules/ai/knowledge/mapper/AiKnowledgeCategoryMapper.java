package com.linkwisehub.modules.ai.knowledge.mapper;

import com.linkwisehub.modules.ai.knowledge.entity.AiKnowledgeCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiKnowledgeCategoryMapper {
    List<AiKnowledgeCategory> selectAll();

    AiKnowledgeCategory selectById(@Param("id") Long id);

    AiKnowledgeCategory selectRootByName(@Param("name") String name);

    int countByParentId(@Param("parentId") Long parentId);

    int insert(AiKnowledgeCategory category);

    int update(AiKnowledgeCategory category);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
