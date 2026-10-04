import type { UiNode } from "./uiBuilder";

/** 与后端 SiteConfigVO 对应 */
export interface SiteConfig {
    siteName: string
    siteIntro: string
    logo: string
    copyright: string
    primaryColor: string
    darkMode: boolean
}

/** 与后端 PageTreeVO 对应：页面信息 + 组件节点树 */
export interface PageTree {
    pageKey: string
    pageName: string
    routePath: string | null
    nodes: UiNode[]
}

/** 与后端 SiteBundleVO 对应：用户端首屏一次拿全主题与首页组件树 */
export interface SiteBundle {
    theme: SiteConfig
    homePage: PageTree
}
