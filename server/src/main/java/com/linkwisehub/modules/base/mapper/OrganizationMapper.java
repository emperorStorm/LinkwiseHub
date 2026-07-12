package com.linkwisehub.modules.base.mapper;

import com.linkwisehub.modules.base.entity.Organization;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface OrganizationMapper {
    List<Organization> selectAll();
    Organization selectById(Long id);
    int insert(Organization organization);
    int update(Organization organization);
    int delete(Long id);
    List<Organization> selectByParentId(Long parentId);
}
