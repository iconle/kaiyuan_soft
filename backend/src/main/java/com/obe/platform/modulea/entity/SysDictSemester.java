package com.obe.platform.modulea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_dict_semester")
public class SysDictSemester {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String academicYear;
    private Integer semester;
    private String label;
}
