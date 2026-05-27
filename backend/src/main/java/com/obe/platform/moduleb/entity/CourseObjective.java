package com.obe.platform.moduleb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("course_objective")
public class CourseObjective {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long outlineId;
    private String objNo;
    private String dimension;
    private String description;
}
