package com.linkwisehub.modules.base.service.impl;

import com.linkwisehub.modules.base.entity.User;
import com.linkwisehub.modules.base.mapper.UserMapper;
import com.linkwisehub.modules.base.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public List<User> getAll() {
        return userMapper.selectAll();
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    @Transactional
    public void create(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(passwordEncoder.encode("123456"));
        }
        userMapper.insert(user);
    }

    @Override
    @Transactional
    public void update(User user) {
        User existingUser = userMapper.selectById(user.getId());
        if (existingUser != null) {
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                user.setPassword(existingUser.getPassword());
            }
        }
        userMapper.update(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        userMapper.delete(id);
    }

    @Override
    public List<User> getByOrganizationId(Long organizationId) {
        return userMapper.selectByOrganizationId(organizationId);
    }
}
