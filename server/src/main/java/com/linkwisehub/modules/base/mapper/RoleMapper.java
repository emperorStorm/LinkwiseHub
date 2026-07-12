package com.linkwisehub.modules.base.mapper;

import com.linkwisehub.modules.base.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface RoleMapper {
    List<Role> selectAll();
    Role selectById(Long id);
    int insert(Role role);
    int update(Role role);
    int delete(Long id);
}
