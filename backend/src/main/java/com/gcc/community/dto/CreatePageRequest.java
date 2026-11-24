package com.gcc.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 新建页面的请求参数。 */
public record CreatePageRequest(
        @NotBlank(message = "页面名称不能为空")
        @Size(max = 40, message = "页面名称长度不能超过 40 个字符")
        String name,

        // 页面类型，home 不能新建
        @NotBlank(message = "页面类型不能为空")
        String pageType,

        // 页面标识，自定义页面才需要传
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,62}$",
                message = "页面标识只能由小写字母、数字和中划线组成，长度 1–63 且以字母或数字开头")
        String slug,

        // 模板，不传就按页面类型取默认的
        String templateKey) {
}
