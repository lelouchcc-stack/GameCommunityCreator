package com.gcc.community.dto;

import java.time.LocalDateTime;

import com.gcc.community.domain.PageConfig;

/** 单个页面草稿的返回结果。 */
public record PageEnvelopeV2(
        // 页面 ID
        String pageId,
        long projectId,
        // 页面 slug，前端注册路由时要用
        String pageSlug,
        // 页面类型，比如 home、custom、docs
        String pageType,
        // 草稿版本号，修改时用来防冲突
        int configVersion,
        LocalDateTime updatedAt,
        // 页面配置
        PageConfig config) {
}
