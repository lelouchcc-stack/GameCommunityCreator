/**
 * 站点级类型：页面管理、导航、站点设置、公开端站点上下文。
 * 与 docs/API.md v2 §3/§4/§5/§7 对应。
 */
import type { PageConfig } from './page'

export const PAGE_TYPES = [
  'home',
  'custom',
  'changelog',
  'docs',
  'team',
  'issue-report',
  'links',
] as const

export type PageType = (typeof PAGE_TYPES)[number]

export const MODULE_PAGE_TYPES = [
  'changelog',
  'docs',
  'team',
  'issue-report',
  'links',
] as const

export type ModulePageType = (typeof MODULE_PAGE_TYPES)[number]

export const PAGE_TYPE_LABELS: Record<PageType, string> = {
  home: '首页',
  custom: '自定义页面',
  changelog: '更新日志',
  docs: '公开文档',
  team: '制作组',
  'issue-report': '问题反馈',
  links: '友情链接',
}

/** 页面类型 → 默认模板 key（与 PAGE_SCHEMA §6 一致） */
export const DEFAULT_TEMPLATE_BY_PAGE_TYPE: Record<PageType, string> = {
  home: 'default-home',
  custom: 'blank',
  changelog: 'default-changelog',
  docs: 'default-docs',
  team: 'default-team',
  'issue-report': 'default-issue-report',
  links: 'default-links',
}

/** 模块页/首页固定 slug（与 API.md §3.3 一致） */
export const FIXED_SLUG_BY_PAGE_TYPE: Record<Exclude<PageType, 'custom'>, string> = {
  home: 'home',
  changelog: 'changelog',
  docs: 'docs',
  team: 'team',
  'issue-report': 'report',
  links: 'links',
}

/** 自定义页面 slug 保留字（避免与系统路由混淆） */
export const RESERVED_CUSTOM_SLUGS: readonly string[] = [
  'home',
  'p',
  'changelog',
  'docs',
  'team',
  'report',
  'links',
  'api',
  'admin',
]

export const PAGE_STATUSES = ['DRAFT', 'PUBLISHED', 'DISABLED'] as const
export type PageStatus = (typeof PAGE_STATUSES)[number]

export const PAGE_STATUS_LABELS: Record<PageStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  DISABLED: '已停用',
}

export const THEME_MODES = ['DARK', 'LIGHT', 'SYSTEM'] as const
export type ThemeMode = (typeof THEME_MODES)[number]

export const THEME_MODE_LABELS: Record<ThemeMode, string> = {
  DARK: '深色',
  LIGHT: '浅色',
  SYSTEM: '跟随系统',
}

export interface SitePageView {
  pageId: string
  projectId: number
  name: string
  slug: string
  pageType: PageType
  templateKey: string
  status: PageStatus
  draftVersion: number
  publishedVersion: number | null
  isHome: boolean
  sortOrder: number
  updatedAt: string
}

/** PageEnvelope v2：单页草稿 */
export interface SitePageDraftEnvelope {
  pageId: string
  projectId: number
  pageSlug: string
  pageType: PageType
  configVersion: number
  updatedAt: string
  config: PageConfig
}

export interface CreatePageInput {
  name: string
  pageType: PageType
  slug?: string
  templateKey?: string
}

export interface UpdatePageInput {
  name?: string
  slug?: string
  status?: PageStatus
  templateKey?: string
}

export interface SiteSettings {
  themeMode: ThemeMode
}

export type NavigationLocation = 'header' | 'footer'
export type NavigationTargetType = 'page' | 'external'

export interface NavigationItemView {
  id: number
  location: NavigationLocation
  title: string
  targetType: NavigationTargetType
  targetPageId: string | null
  url: string
  sortOrder: number
  visible: boolean
}

export interface NavigationItemInput {
  location: NavigationLocation
  title: string
  targetType: NavigationTargetType
  targetPageId: string | null
  url: string
  sortOrder: number
  visible: boolean
}

export interface PublicProject {
  id: number
  name: string
  slug: string
  gameName: string
  description: string
}

export interface PublicPageView {
  pageId: string
  name: string
  slug: string
  pageType: PageType
  configVersion: number
  publishedAt: string
  config: PageConfig
}

export interface PublicPageSummary {
  pageId: string
  name: string
  slug: string
  pageType: PageType
}

export interface PublicSiteDetail {
  project: PublicProject
  page: PublicPageView
  pages: PublicPageSummary[]
  navigation: NavigationItemView[]
  settings: SiteSettings
}

export interface PublishPageResult {
  pageId: string
  pageSlug: string
  publicationId: number
  version: number
  publishedAt: string
  publicPath: string
}

export interface SitePublishResult {
  publishedPages: PublishPageResult[]
  skippedPages: Array<{ pageId: string; reason: string }>
}

export interface PublicationView {
  publicationId: number
  version: number
  publishedAt: string
}

/** 运行时需要的站点上下文（按钮动作解析、主题、页面注册表） */
export interface SiteRuntimeContext {
  projectId: number
  projectSlug: string
  /** 已发布页面注册表（公开端 = PublicPageSummary[]；预览 = SitePageView[]） */
  pages: Array<
    Pick<PublicPageSummary, 'pageId' | 'name' | 'slug' | 'pageType'> & {
      status?: PageStatus
    }
  >
  themeMode: ThemeMode
}
