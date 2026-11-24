package com.gcc.community.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 页面配置，校验通过后转成 JSON 存库。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageConfig(
        int schemaVersion,      // 现在只接受 2
        String pageId,          // 页面稳定 ID
        String pageTitle,
        String projectSlug,
        ThemeConfig theme,
        List<PageComponentConfig> components) {
}
