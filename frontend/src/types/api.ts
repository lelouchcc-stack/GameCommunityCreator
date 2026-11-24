/**
 * 后端 REST 接口的通用 DTO 类型（与 docs/API.md v2 逐字对应）。
 * 站点/页面/内容模块专用类型见 types/site.ts、types/content.ts。
 */
import type { PageConfig } from './page'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
  fieldErrors?: Record<string, string> | null
}

export type ProjectStatus = 'DRAFT' | 'PUBLISHED'

export interface ProjectView {
  id: number
  name: string
  slug: string
  gameName: string
  description: string
  status: ProjectStatus
  /** 首页稳定 ID（打开首页编辑器用） */
  homePageId: string
  /** 首页草稿版本（兼容旧字段语义） */
  draftVersion: number
  /** 首页发布版本（兼容旧字段语义） */
  publishedVersion: number | null
  pageCount: number
  publishedPageCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateProjectInput {
  name: string
  slug: string
  gameName: string
  description: string
}

export interface UpdateProjectInput {
  name: string
  gameName: string
  description: string
}

/** v1 单页接口（home 别名）的草稿响应结构，保持旧版兼容 */
export interface LegacyPageEnvelope {
  projectId: number
  configVersion: number
  updatedAt: string
  config: PageConfig
}

/** v1 单页接口的发布结果结构 */
export interface LegacyPublishResult {
  publicationId: number
  version: number
  publishedAt: string
  publicPath: string
}

export interface GameInfo {
  projectId: number
  name: string
  tagline: string
  description: string
  coverUrl: string
  platforms: string
  releaseStatus: string
}

export type UpdateGameInfoInput = Omit<GameInfo, 'projectId'>

export interface Announcement {
  id: number
  title: string
  /** Markdown 正文（v3）；content 为兼容旧客户端的同值别名 */
  bodyMarkdown: string
  content: string
  pinned: boolean
  publishedAt: string
}

export interface Paged<T> {
  items: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface PageQuery {
  pageNum?: number
  pageSize?: number
}

export interface PublicCommunitySummary {
  id: number
  name: string
  slug: string
  gameName: string
  description: string
  coverUrl: string
  publishedVersion: number
  publishedAt: string
}

export interface HealthData {
  status: string
  database: string
  time: string
}
