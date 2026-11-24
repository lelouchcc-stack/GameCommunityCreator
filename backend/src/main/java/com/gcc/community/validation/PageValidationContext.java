package com.gcc.community.validation;

import java.util.Map;

// 校验页面配置时要用到的上下文信息。
// 包含站点 slug、当前页面 id 和所有页面的状态。
public record PageValidationContext(
        String projectSlug,
        String pageId,
        Map<String, String> targetPageStatuses) {

    public PageValidationContext {
        targetPageStatuses = targetPageStatuses == null ? Map.of() : Map.copyOf(targetPageStatuses);
    }

    // 只校验 slug 和 pageId，不查目标页面在不在
    public static PageValidationContext of(String projectSlug, String pageId) {
        return new PageValidationContext(projectSlug, pageId, Map.of());
    }
}
