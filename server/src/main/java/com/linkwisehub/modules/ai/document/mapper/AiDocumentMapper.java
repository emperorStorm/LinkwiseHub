package com.linkwisehub.modules.ai.document.mapper;

import com.linkwisehub.modules.ai.document.entity.AiDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiDocumentMapper {
    List<AiDocument> selectAll();

    List<AiDocument> selectDocumentProcessList();

    List<AiDocument> selectKnowledgeDocuments(@Param("categoryIds") List<Long> categoryIds,
                                              @Param("keyword") String keyword,
                                              @Param("publishStatus") String publishStatus);

    AiDocument selectById(@Param("id") Long id);

    int insert(AiDocument document);

    int updateKnowledgeDocument(AiDocument document);

    int updateParseResult(@Param("id") Long id,
                          @Param("parseStatus") String parseStatus,
                          @Param("chunkCount") Integer chunkCount,
                          @Param("errorMessage") String errorMessage);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updatePublishStatus(@Param("id") Long id, @Param("publishStatus") String publishStatus);

    int countByCategoryId(@Param("categoryIds") List<Long> categoryIds);
}
