package com.lelcc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** 预设（原子团）新增/编辑请求体。 */
public record PresetDTO(
        @NotBlank(message = "请输入预设名称")
        @Size(max = 50, message = "预设名称不能超过 50 个字")
        String presetName,

        String category,

        /** 入参声明：[{key,type,required}]，图内以 $param.x 引用 */
        List<Map<String, Object>> inputsSchema,

        /** 出参声明：[{key,type}]，需与 graph 的 exports 一致 */
        List<Map<String, Object>> outputsSchema,

        /** 子图：{nodes,edges,exports} */
        Map<String, Object> graphJson,

        Integer status,

        @Size(max = 200, message = "备注不能超过 200 个字")
        String remark) {
}
