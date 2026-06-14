package com.obe.platform.modulea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("student")
public class Student {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentNo;
    private String name;
    private Long collegeId;
    private Long majorId;
    private Integer enrollmentYear;
    private Long adminClassId;

    @TableField(exist = false)
    private String collegeName;
    @TableField(exist = false)
    private String majorName;
    @TableField(exist = false)
    private String adminClassName;
}
