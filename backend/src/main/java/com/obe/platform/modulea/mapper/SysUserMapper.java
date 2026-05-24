package com.obe.platform.modulea.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.obe.platform.modulea.entity.SysUser;
import org.apache.ibatis.annotations.Select;

public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT u.*, r.role_code, r.role_name " +
            "FROM sys_user u LEFT JOIN sys_role r ON u.role_id = r.id " +
            "WHERE u.username = #{username} AND u.status = 1")
    SysUser selectWithRole(String username);
}
