package com.obe.platform.modulea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("teaching_class")
public class TeachingClass {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long teacherId;
    private Long semesterId;
    private String className;

    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private String teacherName;
}
