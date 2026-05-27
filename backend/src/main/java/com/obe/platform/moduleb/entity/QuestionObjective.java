package com.obe.platform.moduleb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question_objective")
public class QuestionObjective {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private Long objectiveId;
}
