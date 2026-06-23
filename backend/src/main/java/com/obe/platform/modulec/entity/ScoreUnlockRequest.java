package com.obe.platform.modulec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("score_unlock_request")
public class ScoreUnlockRequest {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sheetId;
    private Long classId;
    private Long requesterId;
    private String reason;
    private String status;
    private Long reviewerId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;

    @TableField(exist = false)
    private String requesterName;
    @TableField(exist = false)
    private String className;
}
