package com.gcc.community.dto;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotNull;

/** 保存页面草稿的请求参数。 */
public record SavePageRequest(
        @NotNull(message = "config 不能为空")
        JsonNode config,

        @NotNull(message = "expectedVersion 不能为空")
        Integer expectedVersion) {
}
