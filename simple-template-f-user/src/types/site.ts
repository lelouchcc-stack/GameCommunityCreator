/** 与后端 SiteConfigVO 对应 */
export interface SiteConfig {
    siteName: string
    siteIntro: string
    logo: string
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

/** 与后端 SiteBundleVO 对应：用户端首屏一次拿全主题与首页模块 */
export interface SiteBundle {
    theme: SiteConfig
    homeModules: HomeModule[]
}
