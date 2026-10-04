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

/** 一段逻辑图：nodes + edges + （预设才有）exports */
export interface LogicGraph {
    nodes: Array<Record<string, any>>
    edges: Array<Record<string, any>>
    exports?: Record<string, string>
}

/**
 * 与后端 SiteBundleVO 对应：用户端首屏一次拿全主题、首页组件树、首页逻辑图、
 * 以及图里引用到的预设定义（键为 "presetCode@version"，供执行器展开预设节点）。
 */
export interface SiteBundle {
    theme: SiteConfig
    homePage: PageTree
    flows: Record<string, LogicGraph>
    presets: Record<string, LogicGraph>
}

