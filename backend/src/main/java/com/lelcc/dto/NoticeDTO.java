package com.lelcc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 公告新增/编辑请求体。
 * status 单独走「发布/下架」接口，这里也允许带上（保存即发布时用）。
 */
public record NoticeDTO(
        @NotBlank(message = "请输入公告标题")
        @Size(max = 100, message = "公告标题不能超过 100 个字")
        String title,

        @Size(max = 20000, message = "公告正文不能超过 20000 个字")
        String content,

        @NotNull(message = "请选择公告类型")
        @Min(value = 1, message = "公告类型只能为 1（公告）或 2（版本更新说明）")
        @Max(value = 2, message = "公告类型只能为 1（公告）或 2（版本更新说明）")
        Integer type,

        @Min(value = 0, message = "排序不能小于 0")
        Integer sort,

        /** 1发布 0草稿；为空按 1 处理 */
        @Min(value = 0, message = "状态只能为 1（发布）或 0（草稿）")
        @Max(value = 1, message = "状态只能为 1（发布）或 0（草稿）")
        Integer status) {
}
