package com.obe.platform.modulea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.dto.LoginRequest;
import com.obe.platform.modulea.dto.LoginResponse;
import com.obe.platform.modulea.dto.PasswordChangeRequest;
import com.obe.platform.modulea.entity.SysRole;
import com.obe.platform.modulea.entity.SysUser;
import com.obe.platform.modulea.mapper.SysRoleMapper;
import com.obe.platform.modulea.mapper.SysUserMapper;
import com.obe.platform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用");
        }

        SysRole role = roleMapper.selectById(user.getRoleId());
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(),
                role.getRoleCode(), user.getRealName());

        return new LoginResponse(token, user.getId(), user.getUsername(),
                user.getRealName(), role.getRoleCode(), role.getRoleName());
    }

    public void changePassword(PasswordChangeRequest request) {
        Long userId = getCurrentUserId();
        SysUser user = userMapper.selectById(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BizException("旧密码错误");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
