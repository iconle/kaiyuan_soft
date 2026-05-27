package com.obe.platform.modulea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

@Data
@TableName("grad_requirement")
public class GradRequirement {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long majorId;
    private Integer reqNo;
    private String title;
    private String content;

    @TableField(exist = false)
    private List<Indicator> indicators;
}
