package com.linkwisehub.modules.base.mapper;

import com.linkwisehub.modules.base.entity.User;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface UserMapper {
    List<User> selectAll();
    User selectById(Long id);
    User selectByUsername(String username);
    int insert(User user);
    int update(User user);
    int delete(Long id);
    List<User> selectByOrganizationId(Long organizationId);
}
