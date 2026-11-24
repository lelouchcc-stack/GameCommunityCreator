/**
 * CommunityRuntime 向子组件暴露的上下文（mode / 页脚站点导航）。
 *
 * 冻结契约要求运行时组件统一 props 为
 * `{ component, status, data, errorMessage, retry, site }`，
 * 因此「编辑器 / 预览 / 公开」模式与站点页脚导航都通过 provide/inject 传递，
 * 不额外增加 props（`SiteRuntimeContext` 不含 navigation，由布局统一下发已过滤的结果）。
 */
import { computed, inject, type ComputedRef, type InjectionKey } from 'vue'
import type { RuntimeMode } from './types'

export const RUNTIME_MODE_KEY: InjectionKey<ComputedRef<RuntimeMode>> =
  Symbol('gcc-runtime-mode')

const FALLBACK_MODE: ComputedRef<RuntimeMode> = computed(() => 'public')

export function injectRuntimeMode(): ComputedRef<RuntimeMode> {
  return inject(RUNTIME_MODE_KEY, FALLBACK_MODE)
}

/** 更新日志 / 文档卡片内部跳转的公开详情路径（PAGE_SCHEMA §4.3） */
export function changelogDetailPath(projectSlug: string, entryId: number | string): string {
  return `/community/${projectSlug}/changelog/${entryId}`
}

export function docDetailPath(projectSlug: string, docSlug: string): string {
  return `/community/${projectSlug}/docs/${encodeURIComponent(docSlug)}`
}

/* ------------------------------ 站点页脚导航 ------------------------------ */

/**
 * 已解析的站点导航条目（由 PublicSiteLayout 过滤 + pathForPage 解析后下发）。
 * FooterComponent 用它渲染页脚导航，保证「由页面配置 footer 组件渲染」时导航仍存在。
 */
export interface RuntimeNavItem {
  key: string
  title: string
  path: string
  external: boolean
  current: boolean
}

export const RUNTIME_FOOTER_NAV_KEY: InjectionKey<ComputedRef<RuntimeNavItem[]>> =
  Symbol('gcc-runtime-footer-nav')

const EMPTY_FOOTER_NAV: ComputedRef<RuntimeNavItem[]> = computed(() => [])

export function injectFooterNav(): ComputedRef<RuntimeNavItem[]> {
  return inject(RUNTIME_FOOTER_NAV_KEY, EMPTY_FOOTER_NAV)
}
