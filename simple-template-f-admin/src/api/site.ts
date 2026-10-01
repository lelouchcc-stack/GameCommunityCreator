import http from "../utils/request";
import type { HomeModule, SiteConfig } from "../types/site";

export function getSiteConfig() {
    return http.get<SiteConfig, SiteConfig>("/admin/site/config");
}

/** 返回保存后的最新配置，调用方拿它调 applySiteTheme 就地预览 */
export function updateSiteConfig(data: SiteConfig) {
    return http.put<SiteConfig, SiteConfig>("/admin/site/config", data);
}

export function listHomeModules() {
    return http.get<HomeModule[], HomeModule[]>("/admin/site/home-modules");
}

/** 整表批量保存：启用状态与顺序一次提交 */
export function saveHomeModules(data: HomeModule[]) {
    return http.put<HomeModule[], HomeModule[]>("/admin/site/home-modules", data);
}
