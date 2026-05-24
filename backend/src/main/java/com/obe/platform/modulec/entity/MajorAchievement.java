package com.obe.platform.modulec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("major_achievement")
public class MajorAchievement {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long majorId;
    private Long indicatorId;
    private Long semesterId;
    private BigDecimal achievement;
    private LocalDateTime calcTime;
    private Long triggeredBy;
}
