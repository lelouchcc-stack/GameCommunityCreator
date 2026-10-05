package com.lelcc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 进度阶段新增/编辑请求体。 */
public record ProgressStageDTO(
        @NotBlank(message = "请输入阶段名")
        @Size(max = 50, message = "阶段名不能超过 50 个字")
        String stageName,

        @Size(max = 200, message = "阶段说明不能超过 200 个字")
        String description,

        @Min(value = 0, message = "排序不能小于 0")
        Integer sort) {
}
