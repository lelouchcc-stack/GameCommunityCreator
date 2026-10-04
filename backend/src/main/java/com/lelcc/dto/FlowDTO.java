package com.lelcc.dto;

import jakarta.validation.constraints.Size;

import java.util.Map;

/** 页面逻辑图保存请求体。flowKey 走路径参数，所以不在这里。 */
public record FlowDTO(
        @Size(max = 50, message = "图名称不能超过 50 个字")
        String flowName,

        /** 逻辑图：{nodes,edges,exports} */
        Map<String, Object> graphJson,

        Integer status,

        @Size(max = 200, message = "备注不能超过 200 个字")
        String remark) {
}
