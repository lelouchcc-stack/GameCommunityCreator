package com.lelcc.dto;

import jakarta.validation.constraints.NotNull;

/** 进度阶段排序项：批量调整顺序时数组里的一项。 */
public record StageSortDTO(
        @NotNull(message = "阶段 ID 不能为空")
        Long id,

        @NotNull(message = "排序不能为空")
        Integer sort) {
}
