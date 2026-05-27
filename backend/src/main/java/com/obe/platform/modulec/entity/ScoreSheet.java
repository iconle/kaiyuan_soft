package com.obe.platform.modulec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("score_sheet")
public class ScoreSheet {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classId;
    private String status;
    private LocalDateTime lockedAt;
    private Long lockedBy;
}
