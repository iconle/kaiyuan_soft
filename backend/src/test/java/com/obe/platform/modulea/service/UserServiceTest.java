package com.obe.platform.modulea.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.SysUser;
import com.obe.platform.modulea.mapper.SysRoleMapper;
import com.obe.platform.modulea.mapper.SysUserMapper;
import com.obe.platform.modulea.mapper.TeachingClassMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final TeachingClassMapper teachingClassMapper = mock(TeachingClassMapper.class);
    private final UserService userService = new UserService(userMapper, roleMapper, passwordEncoder, teachingClassMapper);

    @Test
    void disableUserRejectsCurrentUser() {
        assertThatThrownBy(() -> userService.disableUser(7L, 7L))
                .isInstanceOf(BizException.class)
                .hasMessage("不能禁用当前登录用户");

        verify(userMapper, never()).selectById(7L);
    }

    @Test
    void listUsersHandlesEmptyPage() {
        when(userMapper.selectPage(any(), any())).thenReturn(new Page<SysUser>(1, 10));

        userService.listUsers(1, 10, "missing", null);

        verify(roleMapper, never()).selectBatchIds(any());
    }

    @Test
    void enableUserRestoresStatus() {
        SysUser user = new SysUser();
        user.setId(8L);
        user.setStatus(0);
        when(userMapper.selectById(8L)).thenReturn(user);

        userService.enableUser(8L);

        assertThat(user.getStatus()).isEqualTo(1);
        verify(userMapper).updateById(user);
    }

    @Test
    void deleteUserRejectsCurrentUser() {
        assertThatThrownBy(() -> userService.deleteUser(7L, 7L))
                .isInstanceOf(BizException.class)
                .hasMessage("不能删除当前登录用户");

        verify(userMapper, never()).selectById(7L);
    }

    @Test
    void deleteUserRejectsTeacherWithTeachingClasses() {
        when(userMapper.selectById(8L)).thenReturn(new SysUser());
        when(teachingClassMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> userService.deleteUser(8L, 7L))
                .isInstanceOf(BizException.class)
                .hasMessage("该教师已关联教学班级，请先调整教学班级主讲教师后再删除");

        verify(userMapper, never()).deleteById(8L);
    }
}
