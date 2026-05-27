package com.obe.platform.modulec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("obj_achievement")
public class ObjAchievement {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classId;
    private Long objectiveId;
    private BigDecimal achievement;
    private LocalDateTime calcTime;
}
