package com.obe.platform.modulec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime calcTime;
    private Long triggeredBy;
}
