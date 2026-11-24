package com.gcc.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 提交问题反馈的请求参数。 */
public record CreateIssueRequest(
        @NotBlank(message = "问题标题不能为空")
        @Size(max = 120, message = "问题标题长度不能超过 120 个字符")
        String title,

        @NotBlank(message = "问题分类不能为空")
        @Size(max = 40, message = "问题分类长度不能超过 40 个字符")
        String category,

        @NotBlank(message = "问题描述不能为空")
        @Size(max = 3000, message = "问题描述长度不能超过 3000 个字符")
        String description,

        @Size(max = 40, message = "游戏版本长度不能超过 40 个字符")
        String gameVersion,

        @Size(max = 2000, message = "复现步骤长度不能超过 2000 个字符")
        String steps) {
}
