package com.obe.platform.modulea.service;

import com.obe.platform.common.BizException;
import com.obe.platform.modulea.mapper.SysRoleMapper;
import com.obe.platform.modulea.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UserServiceTest {

    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserService userService = new UserService(userMapper, roleMapper, passwordEncoder);

    @Test
    void disableUserRejectsCurrentUser() {
        assertThatThrownBy(() -> userService.disableUser(7L, 7L))
                .isInstanceOf(BizException.class)
                .hasMessage("不能禁用当前登录用户");

        verify(userMapper, never()).selectById(7L);
    }
}
