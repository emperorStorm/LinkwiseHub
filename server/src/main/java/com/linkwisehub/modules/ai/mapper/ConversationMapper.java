package com.linkwisehub.modules.ai.mapper;

import com.linkwisehub.modules.ai.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ConversationMapper {
    List<Conversation> selectAll();
    List<Conversation> selectByUserId(@Param("userId") Long userId);
    Conversation selectById(@Param("id") Long id);
    int insert(Conversation conversation);
    int updateTitle(@Param("id") Long id, @Param("title") String title);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int delete(@Param("id") Long id);
}
