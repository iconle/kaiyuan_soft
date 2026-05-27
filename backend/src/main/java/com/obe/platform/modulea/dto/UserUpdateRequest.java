package com.obe.platform.modulea.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotNull(message = "角色不能为空")
    private Long roleId;

    private Long collegeId;
    private Integer status;
}
