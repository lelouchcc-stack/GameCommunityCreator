package com.gcc.community.domain;

/**
 * 页面主题配置，前端用它生成 CSS 变量。
 */
public record ThemeConfig(
        String primaryColor,
        String accentColor,
        String backgroundColor,
        String surfaceColor,
        String textColor,
        String mutedTextColor,
        String fontFamily,
        int borderRadius) {
}
