package com.lelcc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 标签新增/编辑请求体。boardId 为空表示全局标签。 */
public record TagDTO(
        Long boardId,

        @NotBlank(message = "请输入标签名")
        @Size(max = 30, message = "标签名不能超过 30 个字")
        String tagName,

        @NotNull(message = "请选择标签类型")
        @Min(value = 1, message = "标签类型只能为 1 或 2")
        @Max(value = 2, message = "标签类型只能为 1 或 2")
        Integer tagType,

        @Min(value = 0, message = "排序不能小于 0")
        Integer sort) {
}
