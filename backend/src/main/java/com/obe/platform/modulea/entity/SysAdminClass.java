package com.obe.platform.modulea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_admin_class")
public class SysAdminClass {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long majorId;
    private String className;
    private Integer enrollmentYear;

    @TableField(exist = false)
    private String majorName;

    @TableField(exist = false)
    private Integer studentCount;
}
