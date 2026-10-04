package com.lelcc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 页面新增/编辑请求体。 */
public record PageDTO(
        @NotBlank(message = "请输入页面标识")
        @Pattern(regexp = "^[a-z][a-z0-9_-]{1,29}$", message = "页面标识只能用小写字母、数字、下划线或中划线，且以字母开头")
        String pageKey,

        @NotBlank(message = "请输入页面名称")
        @Size(max = 50, message = "页面名称不能超过 50 个字")
        String pageName,

        @Size(max = 100, message = "路由地址不能超过 100 个字符")
        String routePath,

        Integer isHome,

        Integer status,

        @Size(max = 200, message = "备注不能超过 200 个字")
        String remark) {
}
