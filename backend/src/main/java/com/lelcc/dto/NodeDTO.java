package com.lelcc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 页面节点保存项。整页保存时一次提交整棵树：
 * id 为空表示新增，已有 id 表示更新，未出现在列表里的旧节点视为删除。
 */
public record NodeDTO(
        Long id,

        Long parentId,

        @NotBlank(message = "请选择组件类型")
        String typeCode,

        @Size(max = 50, message = "节点名称不能超过 50 个字")
        String nodeName,

        @Min(value = 0, message = "排序不能小于 0")
        Integer sort,

        Integer visible,

        Map<String, Object> styleJson,

        Map<String, Object> contentJson,

        Map<String, Object> behaviorJson,

        @Size(max = 200, message = "备注不能超过 200 个字")
        String remark) {
}
