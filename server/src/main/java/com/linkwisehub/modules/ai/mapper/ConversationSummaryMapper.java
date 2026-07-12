package com.linkwisehub.modules.ai.mapper;

import com.linkwisehub.modules.ai.entity.ConversationSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConversationSummaryMapper {
    ConversationSummary selectByConversationId(@Param("conversationId") Long conversationId);
    int insert(ConversationSummary summary);
    int update(ConversationSummary summary);
    int deleteByConversationId(@Param("conversationId") Long conversationId);
}
