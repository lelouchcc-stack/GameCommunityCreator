package com.gcc.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 创建社区项目的请求参数。 */
public record CreateProjectRequest(
        @NotBlank(message = "社区名称不能为空")
        @Size(max = 80, message = "社区名称长度不能超过 80 个字符")
        String name,

        @NotBlank(message = "项目标识不能为空")
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,62}$",
                message = "项目标识只能由小写字母、数字和中划线组成，长度 2–63 且以字母或数字开头")
        String slug,

        @NotBlank(message = "游戏名称不能为空")
        @Size(max = 80, message = "游戏名称长度不能超过 80 个字符")
        String gameName,

        @Size(max = 500, message = "社区简介长度不能超过 500 个字符")
        String description) {
}
