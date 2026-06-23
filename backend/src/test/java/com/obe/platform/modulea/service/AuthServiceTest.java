package com.obe.platform.modulea.service;

import com.obe.platform.common.BizException;
import com.obe.platform.modulea.dto.CurrentUserResponse;
import com.obe.platform.modulea.entity.SysRole;
import com.obe.platform.modulea.entity.SysUser;
import com.obe.platform.modulea.mapper.SysRoleMapper;
import com.obe.platform.modulea.mapper.SysUserMapper;
import com.obe.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final AuthService authService = new AuthService(userMapper, roleMapper, passwordEncoder, jwtTokenProvider);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserReturnsUserAndRoleInfo() {
        setCurrentUser(3L);
        SysUser user = user(3L, 2L, 1);
        SysRole role = role(2L, "TEACHER", "主讲教师");
        when(userMapper.selectById(3L)).thenReturn(user);
        when(roleMapper.selectById(2L)).thenReturn(role);

        CurrentUserResponse response = authService.getCurrentUser();

        assertThat(response.getUserId()).isEqualTo(3L);
        assertThat(response.getUsername()).isEqualTo("teacher_wang");
        assertThat(response.getRealName()).isEqualTo("王老师");
        assertThat(response.getRoleCode()).isEqualTo("TEACHER");
        assertThat(response.getRoleName()).isEqualTo("主讲教师");
    }

    @Test
    void getCurrentUserRejectsMissingUser() {
        setCurrentUser(99L);
        when(userMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(BizException.class)
                .hasMessage("当前用户不存在或登录已失效");
    }

    @Test
    void getCurrentUserRejectsDisabledUser() {
        setCurrentUser(3L);
        when(userMapper.selectById(3L)).thenReturn(user(3L, 2L, 0));

        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(BizException.class)
                .hasMessage("账号已被禁用");
    }

    private void setCurrentUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null));
    }

    private SysUser user(Long id, Long roleId, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("teacher_wang");
        user.setRealName("王老师");
        user.setRoleId(roleId);
        user.setStatus(status);
        return user;
    }

    private SysRole role(Long id, String roleCode, String roleName) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        return role;
    }
}
