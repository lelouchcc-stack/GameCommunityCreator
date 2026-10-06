/** 一条站点导航（页头与页脚共用），与后端 SiteConfigVO.NavItemVO 对应 */
export interface SiteNavItem {
    label: string
    url: string
}

/** 与后端 SiteConfigVO 对应 */
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

/** 与后端 HomeModuleVO 对应 */
export interface HomeModule {
    id: number
    moduleType: string
    moduleName: string
    sort: number
    /** 1启用 0停用 */
    enabled: number
    configJson: string | null
}
