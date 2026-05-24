package com.obe.platform.modulea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("admin_class_student")
public class AdminClassStudent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminClassId;
    private Long studentId;
}
