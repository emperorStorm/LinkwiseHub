package com.linkwisehub.modules.ai.mapper;

import com.linkwisehub.modules.ai.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MessageMapper {
    List<Message> selectByConversationId(@Param("conversationId") Long conversationId);
    Message selectById(@Param("id") Long id);
    int insert(Message message);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int deleteByConversationId(@Param("conversationId") Long conversationId);
}
