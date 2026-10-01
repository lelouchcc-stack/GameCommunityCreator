package com.lelcc.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 举报处理请求体。 */
public record ReportHandleDTO(
        /** 1已处理 2已驳回 */
        @NotNull(message = "请选择处理结果")
        Integer status,

        /** 处理/驳回说明；驳回时必填，同时下架帖子时也必填 */
        @Size(max = 500, message = "说明不能超过 500 个字")
        String remark,

        /** 是否同时下架被举报的帖子 */
        Boolean offlinePost) {
}
