import http from "../utils/request";
import type { SiteBundle } from "../types/site";

/** 站点外观配置与首页模块：匿名可读（后端已对会员拦截器放行该路径） */
export function getSiteBundle() {
    return http.get<SiteBundle, SiteBundle>("/app/site/config");
}
