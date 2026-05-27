package com.obe.platform.modulec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("student_score")
public class StudentScore {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sheetId;
    private Long studentId;
    private Long assessmentId;
    private Long questionId;
    private BigDecimal score;
}
