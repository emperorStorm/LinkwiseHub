package com.linkwisehub.modules.ai.document.mapper;

import com.linkwisehub.modules.ai.document.dto.AiDocumentChunkRespDto;
import com.linkwisehub.modules.ai.document.dto.AiDocumentChunkIndexDto;
import com.linkwisehub.modules.ai.document.entity.AiDocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiDocumentChunkMapper {
    List<AiDocumentChunkRespDto> selectByDocumentId(@Param("documentId") Long documentId);

    List<String> selectVectorIdsByDocumentId(@Param("documentId") Long documentId);

    List<AiDocumentChunkRespDto> selectPublishedByVectorIds(@Param("vectorIds") List<String> vectorIds);

    List<AiDocumentChunkRespDto> selectPublishedByChunkIds(@Param("chunkIds") List<Long> chunkIds);

    List<AiDocumentChunkIndexDto> selectIndexChunksByDocumentId(@Param("documentId") Long documentId);

    List<AiDocumentChunkIndexDto> selectIndexChunksPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    int batchInsert(@Param("chunks") List<AiDocumentChunk> chunks);

    int updateVectorIdByChunkId(@Param("chunkId") Long chunkId, @Param("vectorId") String vectorId);

    int updateStatusByDocumentId(@Param("documentId") Long documentId, @Param("status") Integer status);
}
