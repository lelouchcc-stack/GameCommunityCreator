/**
 * P1 内容模块 DTO（管理端 + 公开端），与 docs/API.md v2 §6/§7 对应。
 */

export type ContentStatus = 'DRAFT' | 'PUBLISHED'
export const CONTENT_STATUSES: readonly ContentStatus[] = ['DRAFT', 'PUBLISHED']

export type IssueStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED'
export const ISSUE_STATUSES: readonly IssueStatus[] = ['PENDING', 'PROCESSING', 'RESOLVED']

export const ISSUE_STATUS_LABELS: Record<IssueStatus, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
}

export const CONTENT_STATUS_LABELS: Record<ContentStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
}

export interface ChangelogView {
  id: number
  version: string
  title: string
  /** Markdown 正文（v3）；content 为兼容旧客户端的同值别名 */
  bodyMarkdown: string
  content: string
  releaseDate: string
  status: ContentStatus
  createdAt: string
  updatedAt: string
}

export interface ChangelogInput {
  version: string
  title: string
  bodyMarkdown: string
  /** 兼容字段（可省略，后端与 bodyMarkdown 同步） */
  content?: string
  releaseDate: string
  status?: ContentStatus
}

export interface DocView {
  id: number
  title: string
  category: string
  /** Markdown 正文（v3）；content 为兼容旧客户端的同值别名 */
  bodyMarkdown: string
  content: string
  slug: string
  status: ContentStatus
  createdAt: string
  updatedAt: string
}

export interface DocInput {
  title: string
  category: string
  bodyMarkdown: string
  /** 兼容字段（可省略，后端与 bodyMarkdown 同步） */
  content?: string
  slug: string
  status?: ContentStatus
}

export interface TeamMemberView {
  id: number
  name: string
  avatarUrl: string
  role: string
  bio: string
  sortOrder: number
}

export interface TeamMemberInput {
  name: string
  avatarUrl?: string
  role?: string
  bio?: string
  sortOrder?: number
}

export interface IssueReportView {
  id: number
  title: string
  category: string
  description: string
  gameVersion: string
  steps: string
  status: IssueStatus
  /** 处理结果（管理员填写，v3.2 新增） */
  resolution: string
  /** 是否允许公开展示（仅 RESOLVED + true 会出现在公开进展） */
  isPublic: boolean
  createdAt: string
  updatedAt?: string
}

/** 公开「问题处理进展」条目（后端 /public/.../issues/progress 返回） */
export interface IssueProgressItem {
  id: number
  title: string
  category: string
  description: string
  resolution: string
  updatedAt: string
}

export interface IssueReportInput {
  title: string
  category: string
  description: string
  gameVersion: string
  steps: string
}

export interface IssueSubmitResult {
  id: number
  status: IssueStatus
  createdAt: string
}

export interface FriendLinkView {
  id: number
  name: string
  url: string
  description: string
  sortOrder: number
  enabled: boolean
}

export interface FriendLinkInput {
  name: string
  url: string
  description?: string
  sortOrder?: number
  enabled?: boolean
}
