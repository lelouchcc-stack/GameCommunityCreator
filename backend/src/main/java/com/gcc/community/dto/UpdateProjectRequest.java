package com.gcc.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 更新社区项目的请求参数，slug 不能改。 */
public record UpdateProjectRequest(
        @NotBlank(message = "社区名称不能为空")
        @Size(max = 80, message = "社区名称长度不能超过 80 个字符")
        String name,

        @NotBlank(message = "游戏名称不能为空")
        @Size(max = 80, message = "游戏名称长度不能超过 80 个字符")
        String gameName,

        @Size(max = 500, message = "社区简介长度不能超过 500 个字符")
        String description) {
}
