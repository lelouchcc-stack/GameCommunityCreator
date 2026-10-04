package com.lelcc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** 组件类型新增/编辑请求体。 */
public record ComponentTypeDTO(
        @NotBlank(message = "请输入组件名称")
        @Size(max = 50, message = "组件名称不能超过 50 个字")
        String typeName,

        @NotBlank(message = "请选择组件分类")
        String category,

        @NotBlank(message = "请选择渲染内核")
        String renderKind,

        Integer isContainer,

        List<String> styleSchema,

        List<String> contentSchema,

        List<String> behaviorSchema,

        Map<String, Object> defaultStyle,

        Map<String, Object> defaultContent,

        String previewIcon,

        @Min(value = 0, message = "排序不能小于 0")
        Integer sort,

        @Size(max = 200, message = "备注不能超过 200 个字")
        String remark) {
}
