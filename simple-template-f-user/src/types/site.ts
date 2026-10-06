/** 一段逻辑图：直接复用逻辑层的类型，避免同一个形状在两处各写一份后走样 */
import type { LogicGraph } from "../logic/types";
export type { LogicGraph };

import type { UiNode } from "./uiBuilder";

/** 一条站点导航（页头与页脚共用） */
export interface SiteNavItem {
    label: string
    url: string
}

/**
 * 与后端 SiteConfigVO 对应。
 * 这些值就是「改站名即可运行」的全部输入：模板里不写死社区名与游戏名。
 */
export interface SiteConfig {
    siteName: string
    gameName: string
    siteIntro: string
    logo: string
    navItems: SiteNavItem[]
    copyright: string
    primaryColor: string
    darkMode: boolean
}

/** 与后端 StorageConfigVO 对应：图片存在哪个存储、对外基址是什么 */
export interface StorageConfig {
    provider: string
    publicBaseUrl: string
}

/**
 * 与后端 PageBundleVO 对应：一个页面 + 它的可见节点树 + 它自己的逻辑图。
 * 图按页挂是因为 flowKey（init/submit...）只在页内唯一，平铺会键冲撞。
 */
export interface PageEntry {
    pageKey: string
    pageName: string
    routePath: string | null
    isHome: number
    nodes: UiNode[]
    flows: Record<string, LogicGraph>
}

/**
 * 与后端 SiteBundleVO 对应（v2 多页）：用户端首屏一次拿全主题、所有启用页面、
 * 以及这些页的图引用到的预设定义（键为 "presetCode@version"，供执行器展开预设节点）。
 * storage 用于把页面里存的 objectKey 现拼成图片地址。
 */
export interface SiteBundle {
    theme: SiteConfig
    storage: StorageConfig
    presets: Record<string, LogicGraph>
    pages: PageEntry[]
}
