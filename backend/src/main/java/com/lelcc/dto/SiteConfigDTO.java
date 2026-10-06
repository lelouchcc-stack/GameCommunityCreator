package com.lelcc.dto;

import java.util.List;

/**
 * 站点信息/主题的保存请求体。取值校验统一由 SiteConfigRegistry 完成。
 * navItems 传 null 或空数组都视为「未配置」，读回时回落到内置的默认导航。
 */
public record SiteConfigDTO(
        String siteName,
        String gameName,
        String siteIntro,
        String logo,
        List<SiteConfigVO.NavItemVO> navItems,
        String copyright,
        String primaryColor,
        Boolean darkMode) {
}
