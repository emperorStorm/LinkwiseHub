package com.linkwisehub.modules.ai.document.mapper;

import com.linkwisehub.modules.ai.document.entity.AiDocumentProcessingJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AiDocumentProcessingJobMapper {
    int insert(AiDocumentProcessingJob job);

    AiDocumentProcessingJob selectById(@Param("id") Long id);

    AiDocumentProcessingJob selectLatestByDocumentId(@Param("documentId") Long documentId);

    List<AiDocumentProcessingJob> selectPollable(@Param("now") LocalDateTime now, @Param("limit") Integer limit);

    int claim(@Param("id") Long id,
              @Param("version") Integer version,
              @Param("workerId") String workerId,
              @Param("lockedUntil") LocalDateTime lockedUntil);

    int updateSubmission(@Param("id") Long id,
                         @Param("providerTaskId") String providerTaskId,
                         @Param("status") String status,
                         @Param("progress") Integer progress,
                         @Param("retryCount") Integer retryCount,
                         @Param("errorMessage") String errorMessage,
                         @Param("nextPollTime") LocalDateTime nextPollTime);

    int updateRemoteStatus(@Param("id") Long id,
                           @Param("status") String status,
                           @Param("progress") Integer progress,
                           @Param("errorMessage") String errorMessage,
                           @Param("nextPollTime") LocalDateTime nextPollTime,
                           @Param("startedAt") LocalDateTime startedAt);

    int markRunning(@Param("id") Long id,
                    @Param("startedAt") LocalDateTime startedAt);

    int updateArtifacts(@Param("id") Long id,
                        @Param("resultBucket") String resultBucket,
                        @Param("manifestPath") String manifestPath,
                        @Param("markdownPath") String markdownPath,
                        @Param("blocksPath") String blocksPath);

    int finish(@Param("id") Long id,
               @Param("status") String status,
               @Param("progress") Integer progress,
               @Param("errorMessage") String errorMessage,
               @Param("finishedAt") LocalDateTime finishedAt);

    int cancelActiveByDocumentId(@Param("documentId") Long documentId,
                                 @Param("finishedAt") LocalDateTime finishedAt);
}
