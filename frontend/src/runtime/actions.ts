/**
 * 按钮动作执行器（唯一入口，禁止 eval / new Function，见 docs/PAGE_SCHEMA.md §4）。
 *
 * 只处理三种白名单动作：
 * - `navigate`     站内跳转：目标必须存在于「站点页面注册表」，按模式解析路由
 * - `externalLink` 外部链接：仅 http/https，安全新标签打开
 * - `none`         未配置动作：提示，不跳转
 *
 * 任何其他类型一律拒绝并提示；动作参数非法时不跳转、不请求。
 */
import type { Router } from 'vue-router'
import type { NavigateAction, PageAction } from '@/types/page'
import type { SiteRuntimeContext } from '@/types/site'
import {
  PAGE_ID_PATTERN,
  isAllowedActionType,
  isValidHttpUrl,
  pathForPage,
  previewPathForPage,
} from '@/utils/actions'
import type { RuntimeMode } from './types'
import { pushToast } from './toast'

export interface ActionContext {
  router: Router
  site: SiteRuntimeContext
  mode: RuntimeMode
}

const TARGET_MISSING_MESSAGE = '目标页面不存在或尚未发布'

function safePush(router: Router | undefined, path: string): void {
  if (!router) return
  void router.push(path).catch(() => {
    /* 路由重复 / 导航被取消：不提示，避免误报 */
  })
}

/**
 * 执行一个页面上配置的按钮动作。
 * 返回是否真的触发了「跳转 / 打开链接」，便于测试断言。
 */
export function executeAction(action: PageAction | undefined | null, ctx: ActionContext): boolean {
  const type = (action as { type?: unknown } | null | undefined)?.type

  if (!action || type === undefined) {
    pushToast('该按钮尚未配置动作')
    return false
  }

  if (!isAllowedActionType(type)) {
    pushToast('暂不支持的动作类型，已忽略', 'error')
    return false
  }

  if (type === 'none') {
    pushToast('该按钮尚未配置动作')
    return false
  }

  if (type === 'navigate') {
    return executeNavigate(action as NavigateAction, ctx)
  }

  return executeExternalLink(action as { url?: unknown })
}

function executeNavigate(action: NavigateAction, ctx: ActionContext): boolean {
  const targetPageId = action.targetPageId
  if (typeof targetPageId !== 'string' || !PAGE_ID_PATTERN.test(targetPageId)) {
    pushToast('按钮动作缺少有效的目标页面', 'error')
    return false
  }

  // 目标必须存在于站点页面注册表（公开端 = 已发布页面；预览端 = 全部页面）
  const target = ctx.site.pages.find((page) => page.pageId === targetPageId)
  if (!target) {
    pushToast(TARGET_MISSING_MESSAGE)
    return false
  }

  if (ctx.mode === 'editor') {
    pushToast('编辑模式下不执行跳转，可在预览中查看效果')
    return false
  }

  if (ctx.mode === 'preview') {
    if (!ctx.site.projectId) {
      pushToast('预览上下文未就绪，暂不跳转')
      return false
    }
    safePush(ctx.router, previewPathForPage(ctx.site.projectId, target.pageId))
    return true
  }

  // 公开端：注册表里只应有已发布页面；这里再做一次防御（预览注入的 site.pages 可能带 status）
  if (target.status && target.status !== 'PUBLISHED') {
    pushToast(TARGET_MISSING_MESSAGE)
    return false
  }
  if (!ctx.site.projectSlug) {
    pushToast('站点标识缺失，暂不跳转', 'error')
    return false
  }
  safePush(ctx.router, pathForPage(target, ctx.site.projectSlug))
  return true
}

function executeExternalLink(action: { url?: unknown }): boolean {
  const url = action.url
  if (!isValidHttpUrl(url)) {
    pushToast('外部链接不合法（仅支持 http/https），已阻止打开', 'error')
    return false
  }
  try {
    // noopener/noreferrer：新标签无法通过 window.opener 反向控制当前页
    window.open(String(url).trim(), '_blank', 'noopener,noreferrer')
  } catch {
    pushToast('无法打开外部链接，请稍后重试', 'error')
    return false
  }
  return true
}
