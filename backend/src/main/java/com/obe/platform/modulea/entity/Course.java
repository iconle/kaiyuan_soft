package com.obe.platform.modulea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private BigDecimal credit;
    private Integer hoursTheory;
    private Integer hoursExperiment;
    private String category;
    private Long majorId;
}
