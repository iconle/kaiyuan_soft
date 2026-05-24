package com.obe.platform.modulea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("macro_support_matrix")
public class MacroSupportMatrix {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long indicatorId;
    private String supportLevel;
    private BigDecimal weight;
}
