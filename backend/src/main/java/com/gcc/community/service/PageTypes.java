package com.gcc.community.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gcc.community.domain.SitePage;

/** 页面类型和一些固定的常量。 */
public final class PageTypes {

    public static final String HOME = "home";
    public static final String CUSTOM = "custom";
    public static final String CHANGELOG = "changelog";
    public static final String DOCS = "docs";
    public static final String TEAM = "team";
    public static final String ISSUE_REPORT = "issue-report";
    public static final String LINKS = "links";

    public static final List<String> PAGE_TYPES =
            List.of(HOME, CUSTOM, CHANGELOG, DOCS, TEAM, ISSUE_REPORT, LINKS);

    // 模块页每种最多一个
    public static final List<String> MODULE_PAGE_TYPES =
            List.of(CHANGELOG, DOCS, TEAM, ISSUE_REPORT, LINKS);

    // 页面类型对应的固定 slug（custom 不在里面）
    public static final Map<String, String> FIXED_SLUG_BY_TYPE = Map.of(
            HOME, "home",
            CHANGELOG, "changelog",
            DOCS, "docs",
            TEAM, "team",
            ISSUE_REPORT, "report",
            LINKS, "links");

    // 页面类型对应的 id 前缀（page_前缀_projectId）
    public static final Map<String, String> ID_PREFIX_BY_TYPE = Map.of(
            HOME, "home",
            CHANGELOG, "changelog",
            DOCS, "docs",
            TEAM, "team",
            ISSUE_REPORT, "report",
            LINKS, "links");

    // 页面类型对应的默认模板
    public static final Map<String, String> DEFAULT_TEMPLATE_BY_TYPE = Map.of(
            HOME, "default-home",
            CUSTOM, "blank",
            CHANGELOG, "default-changelog",
            DOCS, "default-docs",
            TEAM, "default-team",
            ISSUE_REPORT, "default-issue-report",
            LINKS, "default-links");

    // 允许的模板名，防止路径乱传
    public static final Set<String> TEMPLATE_KEYS = Set.of(
            "default-home", "default-changelog", "default-docs", "default-team",
            "default-issue-report", "default-links", "blank");

    // 自定义页面不能用的 slug
    public static final Set<String> RESERVED_CUSTOM_SLUGS = Set.of(
            "home", "p", "changelog", "docs", "team", "report", "links", "api", "admin");

    public static final List<String> PAGE_STATUSES = List.of("DRAFT", "PUBLISHED", "DISABLED");

    private PageTypes() {
    }

    public static boolean isValidPageType(String pageType) {
        return pageType != null && PAGE_TYPES.contains(pageType);
    }

    public static boolean isModuleType(String pageType) {
        return pageType != null && MODULE_PAGE_TYPES.contains(pageType);
    }

    public static String modulePageId(String pageType, long projectId) {
        return "page_" + ID_PREFIX_BY_TYPE.get(pageType) + "_" + projectId;
    }

    public static String homePageId(long projectId) {
        return "page_home_" + projectId;
    }

    // 默认页面标题，home 用「游戏名 官方社区」
    public static String defaultPageTitle(String pageType, String gameName, String pageName) {
        if (HOME.equals(pageType)) {
            return (gameName == null || gameName.isBlank() ? "游戏" : gameName) + " 官方社区";
        }
        return pageName;
    }

    // 公开访问的路径
    public static String publicPath(String projectSlug, SitePage page) {
        String base = "/community/" + projectSlug;
        return switch (page.pageType()) {
            case HOME -> base;
            case CUSTOM -> base + "/p/" + page.slug();
            case CHANGELOG -> base + "/changelog";
            case DOCS -> base + "/docs";
            case TEAM -> base + "/team";
            case ISSUE_REPORT -> base + "/report";
            case LINKS -> base + "/links";
            default -> base;
        };
    }
}
