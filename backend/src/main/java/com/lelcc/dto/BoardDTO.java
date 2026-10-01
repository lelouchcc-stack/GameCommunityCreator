package com.lelcc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 板块新增/编辑请求体。 */
public record BoardDTO(
        @NotBlank(message = "请输入板块名称")
        @Size(max = 50, message = "板块名称不能超过 50 个字")
        String name,

        @Size(max = 500, message = "板块简介不能超过 500 个字")
        String intro,

        @Size(max = 255, message = "封面地址不能超过 255 个字符")
        String cover,

        @Min(value = 0, message = "排序不能小于 0")
        Integer sort) {
}
