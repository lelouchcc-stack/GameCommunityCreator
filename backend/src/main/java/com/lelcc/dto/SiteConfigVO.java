package com.lelcc.dto;

/**
 * 站点信息/主题。管理端读配置与用户端主题共用同一份形状，
 * 用户端把它全体注入 CSS 变量（site_name/logo 走 DOM，其余走 --site-* 变量）。
 */
public record SiteConfigVO(
        String siteName,
        String siteIntro,
        String logo,
        String copyright,
        String primaryColor,
        Boolean darkMode) {
}
