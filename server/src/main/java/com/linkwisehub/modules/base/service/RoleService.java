package com.linkwisehub.modules.base.service;

import com.linkwisehub.modules.base.entity.Role;
import java.util.List;

public interface RoleService {
    List<Role> getAll();
    Role getById(Long id);
    void create(Role role);
    void update(Role role);
    void delete(Long id);
}
