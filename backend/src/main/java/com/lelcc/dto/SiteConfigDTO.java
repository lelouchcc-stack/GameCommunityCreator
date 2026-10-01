package com.lelcc.dto;

/** 站点信息/主题的保存请求体。取值校验统一由 SiteConfigRegistry 完成。 */
public record SiteConfigDTO(
        String siteName,
        String siteIntro,
        String logo,
        String copyright,
        String primaryColor,
        Boolean darkMode) {
}
