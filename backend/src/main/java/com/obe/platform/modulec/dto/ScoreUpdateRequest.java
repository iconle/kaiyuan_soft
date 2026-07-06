package com.obe.platform.modulec.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreUpdateRequest {

    @NotNull(message = "学生 ID 不能为空")
    private Long studentId;

    @NotNull(message = "考核点 ID 不能为空")
    private Long assessmentId;

    private Long questionId;

    @NotNull(message = "分数不能为空")
    @DecimalMin(value = "0.0", inclusive = true, message = "分数不能为负数")
    private BigDecimal score;
}
