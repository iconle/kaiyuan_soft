package com.obe.platform.moduleb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("assessment_objective")
public class AssessmentObjective {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assessmentId;
    private Long objectiveId;
}
