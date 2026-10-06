package com.lelcc.dto;

import java.util.List;

/**
 * 应用模板的结果报告。管理端据此告诉管理员"这次建了什么、跳过了什么、还要做什么"，
 * 而不是只回一句"成功"——模板初始化动的是整站数据，必须能交代清楚。
 */
public record TemplateApplyVO(
        String code,
        String name,
        /** 应用后的站点配置，管理端可直接用于即时预览 */
        SiteConfigVO theme,
        /** 本次新建的页面 */
        List<String> createdPages,
        /** 本次按模板重建了内容（节点树被覆盖）的页面 */
        List<String> rebuiltPages,
        /** 因「不覆盖已有页面」而跳过的页面 */
        List<String> keptPages,
        List<String> createdComponents,
        List<String> updatedComponents,
        /** 本次写入的初始内容（板块/版本/阶段/标签/公告） */
        List<String> seeded,
        /** 因已有数据而跳过的初始内容 */
        List<String> skippedSeed,
        /** 需要管理员留意的事项 */
        List<String> warnings) {
}
