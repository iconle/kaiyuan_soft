package com.obe.platform.moduleb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("objective_indicator_weight")
public class ObjectiveIndicatorWeight {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long objectiveId;
    private Long indicatorId;
    private BigDecimal weight;
}
