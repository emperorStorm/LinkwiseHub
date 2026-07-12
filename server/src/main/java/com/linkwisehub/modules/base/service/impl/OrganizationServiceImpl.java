package com.linkwisehub.modules.base.service.impl;

import com.linkwisehub.modules.base.entity.Organization;
import com.linkwisehub.modules.base.mapper.OrganizationMapper;
import com.linkwisehub.modules.base.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    @Autowired
    private OrganizationMapper organizationMapper;

    @Override
    public List<Organization> getAll() {
        return organizationMapper.selectAll();
    }

    @Override
    public Organization getById(Long id) {
        return organizationMapper.selectById(id);
    }

    @Override
    @Transactional
    public void create(Organization organization) {
        organizationMapper.insert(organization);
    }

    @Override
    @Transactional
    public void update(Organization organization) {
        organizationMapper.update(organization);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        organizationMapper.delete(id);
    }

    @Override
    public List<Organization> getByParentId(Long parentId) {
        return organizationMapper.selectByParentId(parentId);
    }
}
