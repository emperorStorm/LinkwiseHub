package com.linkwisehub.modules.base.controller;

import com.linkwisehub.modules.base.entity.Organization;
import com.linkwisehub.modules.base.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/base/organization")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @GetMapping
    public List<Organization> getAll() {
        return organizationService.getAll();
    }

    @GetMapping("/{id}")
    public Organization getById(@PathVariable Long id) {
        return organizationService.getById(id);
    }

    @PostMapping
    public void create(@RequestBody Organization organization) {
        organizationService.create(organization);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody Organization organization) {
        organization.setId(id);
        organizationService.update(organization);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        organizationService.delete(id);
    }

    @GetMapping("/parent/{parentId}")
    public List<Organization> getByParentId(@PathVariable Long parentId) {
        return organizationService.getByParentId(parentId);
    }
}
