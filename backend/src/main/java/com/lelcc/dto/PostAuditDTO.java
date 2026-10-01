package com.lelcc.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 帖子审核请求体。 */
public record PostAuditDTO(
        /** 1通过 2下架 */
        @NotNull(message = "请选择审核结果")
        Integer status,

        /** 下架原因，下架时必填 */
        @Size(max = 200, message = "下架原因不能超过 200 个字")
        String reason) {
}
