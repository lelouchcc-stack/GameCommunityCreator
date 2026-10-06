package com.lelcc.dto;

import java.util.List;

/**
 * 站点信息/主题。管理端读配置与用户端主题共用同一份形状，
 * 用户端把它全体注入 CSS 变量（site_name/logo 走 DOM，其余走 --site-* 变量）。
 *
 * <p>navItems 是唯一的结构化项：页头/页脚的导航菜单，站点配置表里以 JSON 存放，
 * 这里转成对象下发，前端不必再解析一次字符串。</p>
 */
public record SiteConfigVO(
        String siteName,
        String gameName,
        String siteIntro,
        String logo,
        List<NavItemVO> navItems,
        String copyright,
        String primaryColor,
        Boolean darkMode) {

    /** 一条导航：label 显示文案，url 是 / 开头的站内路径或 http(s) 外链 */
    public record NavItemVO(String label, String url) {
    }
}
