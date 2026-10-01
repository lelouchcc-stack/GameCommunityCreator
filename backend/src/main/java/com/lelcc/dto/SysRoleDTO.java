package com.lelcc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 角色新增/编辑请求体。 */
public record SysRoleDTO(
        @NotBlank(message = "请输入角色名称")
        @Size(max = 50, message = "角色名称不能超过 50 个字")
        String roleName,

        @NotBlank(message = "请输入角色标识")
        @Size(max = 50, message = "角色标识不能超过 50 个字符")
        String roleKey,

        @Min(value = 0, message = "排序不能小于 0")
        Integer sort,

        Integer status,

        @Size(max = 200, message = "备注不能超过 200 个字")
        String remark) {
}
