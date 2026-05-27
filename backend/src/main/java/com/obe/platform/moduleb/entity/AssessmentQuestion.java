package com.obe.platform.moduleb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@TableName("assessment_question")
public class AssessmentQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assessmentId;
    private String name;
    private BigDecimal maxScore;
    private Integer sortOrder;

    @TableField(exist = false)
    private List<Long> objectiveIds;
}
