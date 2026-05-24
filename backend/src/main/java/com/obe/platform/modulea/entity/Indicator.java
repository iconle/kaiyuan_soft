package com.obe.platform.modulea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("indicator")
public class Indicator {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long gradReqId;
    private String indicatorNo;
    private String content;
}
