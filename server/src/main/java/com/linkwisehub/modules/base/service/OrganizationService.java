package com.linkwisehub.modules.base.service;

import com.linkwisehub.modules.base.entity.Organization;
import java.util.List;

public interface OrganizationService {
    List<Organization> getAll();
    Organization getById(Long id);
    void create(Organization organization);
    void update(Organization organization);
    void delete(Long id);
    List<Organization> getByParentId(Long parentId);
}
