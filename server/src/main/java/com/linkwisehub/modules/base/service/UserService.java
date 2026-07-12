package com.linkwisehub.modules.base.service;

import com.linkwisehub.modules.base.entity.User;
import java.util.List;

public interface UserService {
    List<User> getAll();
    User getById(Long id);
    User getByUsername(String username);
    void create(User user);
    void update(User user);
    void delete(Long id);
    List<User> getByOrganizationId(Long organizationId);
}
