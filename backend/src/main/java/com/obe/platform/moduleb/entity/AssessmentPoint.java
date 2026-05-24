package com.obe.platform.moduleb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@TableName("assessment_point")
public class AssessmentPoint {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long outlineId;
    private String name;
    private BigDecimal maxScore;
    private BigDecimal weightPercent;
    private Long objectiveId;
    private Integer sortOrder;

    @TableField(exist = false)
    private java.util.List<Long> objectiveIds;
}
