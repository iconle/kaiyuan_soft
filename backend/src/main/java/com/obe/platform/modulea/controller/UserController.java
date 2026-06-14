package com.obe.platform.modulea.controller;

import com.obe.platform.common.PageResult;
import com.obe.platform.common.Result;
import com.obe.platform.modulea.dto.UserCreateRequest;
import com.obe.platform.modulea.dto.UserUpdateRequest;
import com.obe.platform.modulea.entity.SysRole;
import com.obe.platform.modulea.entity.SysUser;
import com.obe.platform.modulea.mapper.SysRoleMapper;
import com.obe.platform.modulea.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SysRoleMapper roleMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId) {
        return Result.ok(userService.listUsers(page, size, keyword, roleId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> create(@RequestBody UserCreateRequest request) {
        userService.createUser(request);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        userService.updateUser(id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> disable(@PathVariable Long id) {
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.disableUser(id, currentUserId);
        return Result.ok();
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return Result.ok();
    }

    @GetMapping("/roles")
    public Result<List<SysRole>> listRoles() {
        return Result.ok(roleMapper.selectList(null));
    }
}
