package com.linkwisehub.modules.base.controller;

import com.linkwisehub.modules.base.entity.Role;
import com.linkwisehub.modules.base.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/base/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public List<Role> getAll() {
        return roleService.getAll();
    }

    @GetMapping("/{id}")
    public Role getById(@PathVariable Long id) {
        return roleService.getById(id);
    }

    @PostMapping
    public void create(@RequestBody Role role) {
        roleService.create(role);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        roleService.update(role);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }
}
