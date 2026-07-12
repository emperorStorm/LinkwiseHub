package com.linkwisehub.modules.base.service.impl;

import com.linkwisehub.modules.base.entity.Role;
import com.linkwisehub.modules.base.mapper.RoleMapper;
import com.linkwisehub.modules.base.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Override
    public List<Role> getAll() {
        return roleMapper.selectAll();
    }

    @Override
    public Role getById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    @Transactional
    public void create(Role role) {
        roleMapper.insert(role);
    }

    @Override
    @Transactional
    public void update(Role role) {
        roleMapper.update(role);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        roleMapper.delete(id);
    }
}
