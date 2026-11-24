/**
 * 按钮动作协议的前端纯校验/解析工具（编辑器与运行时共用）。
 *
 * 唯一事实源：docs/PAGE_SCHEMA.md §4。**不执行任何用户代码**。
 */
import type { ActionType, PageAction } from '@/types/page'
import { ACTION_TYPES } from '@/types/page'
import type { PageStatus, PageType } from '@/types/site'

export const PAGE_ID_PATTERN = /^page_[a-z0-9_]{2,32}$/
export const ACTION_URL_MAX_LENGTH = 300

export interface ActionTargetPage {
  pageId: string
  name: string
  pageType: PageType
  status?: PageStatus
  slug?: string
}

export interface ActionValidationContext {
  targetPages: ActionTargetPage[]
}

export interface ActionValidationResult {
  /** 字段路径（如 targetPageId / url）→ 中文错误 */
  errors: Record<string, string>
  /** 可保存但需提示的情况（如目标页面尚未发布） */
  warnings: Record<string, string>
}

export function createDefaultAction(): PageAction {
  return { type: 'none' }
}

/** 是否是可保存/可执行的动作类型（白名单） */
export function isAllowedActionType(value: unknown): value is ActionType {
  return typeof value === 'string' && (ACTION_TYPES as readonly string[]).includes(value)
}

/** 仅接受 http/https 绝对 URL（禁止 javascript:/data:/file:/相对地址） */
export function isValidHttpUrl(url: unknown): boolean {
  if (typeof url !== 'string') return false
  const trimmed = url.trim()
  if (!trimmed || trimmed.length > ACTION_URL_MAX_LENGTH) return false
  // 明确拒绝协议相对地址（//host）与包含空白的地址
  if (trimmed.startsWith('//') || /\s/.test(trimmed)) return false
  let parsed: URL
  try {
    parsed = new URL(trimmed)
  } catch {
    return false
  }
  return (parsed.protocol === 'http:' || parsed.protocol === 'https:') && parsed.hostname.length > 0
}

/**
 * 校验动作声明，返回中文错误/警告。
 * 与后端 PageConfigValidator 的规则保持一致（前端先提示，后端权威校验）。
 */
export function validateAction(
  action: PageAction | undefined | null,
  ctx: ActionValidationContext,
): ActionValidationResult {
  const errors: Record<string, string> = {}
  const warnings: Record<string, string> = {}

  if (!action || typeof action !== 'object' || !isAllowedActionType((action as { type?: unknown }).type)) {
    errors.type = '请选择点击动作（站内跳转 / 外部链接 / 无动作）'
    return { errors, warnings }
  }

  if (action.type === 'none') return { errors, warnings }

  if (action.type === 'navigate') {
    const targetPageId = (action as { targetPageId?: unknown }).targetPageId
    if (typeof targetPageId !== 'string' || !PAGE_ID_PATTERN.test(targetPageId)) {
      errors.targetPageId = '请选择要跳转的站内页面'
      return { errors, warnings }
    }
    const target = ctx.targetPages.find((page) => page.pageId === targetPageId)
    if (!target) {
      errors.targetPageId = '目标页面不存在（可能已被删除）'
    } else if (target.status === 'DISABLED') {
      errors.targetPageId = `目标页面「${target.name}」已停用，请重新选择`
    } else if (target.status === 'DRAFT') {
      warnings.targetPageId = `目标页面「${target.name}」尚未发布，公开页点击时会提示未发布`
    }
    return { errors, warnings }
  }

  // externalLink
  const url = (action as { url?: unknown }).url
  if (!isValidHttpUrl(url)) {
    errors.url = '请输入合法的 http/https 外部链接'
  }
  const openInNewTab = (action as { openInNewTab?: unknown }).openInNewTab
  if (openInNewTab !== undefined && typeof openInNewTab !== 'boolean') {
    errors.openInNewTab = '新标签打开选项必须为布尔值'
  }
  return { errors, warnings }
}

/** 把动作转成一行人类可读描述（编辑器/预览提示用） */
export function describeAction(
  action: PageAction | undefined | null,
  pages: ActionTargetPage[] = [],
): string {
  if (!action) return '未配置动作'
  if (action.type === 'none') return '无动作（运行时不跳转）'
  if (action.type === 'navigate') {
    const target = pages.find((page) => page.pageId === action.targetPageId)
    return target ? `跳转到「${target.name}」` : `跳转到页面 ${action.targetPageId}（目标不存在）`
  }
  return `打开外部链接 ${action.url}${action.openInNewTab ? '（新标签）' : ''}`
}

/**
 * 页面 → 公开路径（唯一实现，PAGE_SCHEMA §4.3）。
 * 仅用于已发布页面注册表中的页面。
 */
export function pathForPage(
  page: { pageType: PageType; slug: string },
  projectSlug: string,
): string {
  const base = `/community/${projectSlug}`
  switch (page.pageType) {
    case 'home':
      return base
    case 'custom':
      return `${base}/p/${page.slug}`
    case 'changelog':
      return `${base}/changelog`
    case 'docs':
      return `${base}/docs`
    case 'team':
      return `${base}/team`
    case 'issue-report':
      return `${base}/report`
    case 'links':
      return `${base}/links`
    default:
      return base
  }
}

/** 页面 → 管理端草稿预览路径 */
export function previewPathForPage(projectId: number, pageId: string): string {
  return `/admin/projects/${projectId}/pages/${pageId}/preview`
}
