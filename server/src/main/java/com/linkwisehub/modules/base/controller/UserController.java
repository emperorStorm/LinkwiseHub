package com.linkwisehub.modules.base.controller;

import com.linkwisehub.common.ApiResponse;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotRespDto;
import com.linkwisehub.modules.base.entity.User;
import com.linkwisehub.modules.base.service.UserService;
import com.linkwisehub.modules.base.service.UserOutboundCallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/base/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserOutboundCallService userOutboundCallService;

    @GetMapping
    public List<User> getAll() {
        return userService.getAll();
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping
    public void create(@RequestBody User user) {
        userService.create(user);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.update(user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping("/organization/{organizationId}")
    public List<User> getByOrganizationId(@PathVariable Long organizationId) {
        return userService.getByOrganizationId(organizationId);
    }

    /**
     * 根据用户手机号发起阿里智能外呼。
     */
    @PostMapping("/{id}/outbound-call")
    public ApiResponse<AliOutboundBotRespDto> outboundCall(@PathVariable Long id) {
        return ApiResponse.success("外呼任务已提交", userOutboundCallService.startOutboundCall(id));
    }
}
