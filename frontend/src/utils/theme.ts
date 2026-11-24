/**
 * 站点深浅色主题工具（纯函数 + localStorage，便于单元测试）。
 * 站点设置 themeMode 为默认值；访客可在公开页覆盖（本地记忆）；SYSTEM 跟随系统。
 */
import type { ThemeMode } from '@/types/site'
import { THEME_MODE_LABELS } from '@/types/site'

export { THEME_MODE_LABELS }

export type EffectiveTheme = 'dark' | 'light'

export const THEME_MODE_ORDER: readonly ThemeMode[] = ['DARK', 'LIGHT', 'SYSTEM']

const STORAGE_PREFIX = 'gcc.theme.'

export function themeStorageKey(projectSlug: string): string {
  return `${STORAGE_PREFIX}${projectSlug}`
}

/** 公开页访客偏好的本地存储 key（项目隔离） */
export function readStoredThemeMode(projectSlug: string): ThemeMode | null {
  try {
    const value = window.localStorage.getItem(themeStorageKey(projectSlug))
    return value === 'DARK' || value === 'LIGHT' || value === 'SYSTEM' ? value : null
  } catch {
    return null
  }
}

export function storeThemeMode(projectSlug: string, mode: ThemeMode | null): void {
  try {
    if (mode) window.localStorage.setItem(themeStorageKey(projectSlug), mode)
    else window.localStorage.removeItem(themeStorageKey(projectSlug))
  } catch {
    /* localStorage 不可用时静默降级 */
  }
}

/**
 * 解析最终明暗模式：
 * - 访客本地偏好优先（LIGHT/DARK/SYSTEM）；
 * - 否则使用站点默认；
 * - SYSTEM 时跟随系统 prefers-color-scheme。
 */
export function resolveThemeMode(
  siteMode: ThemeMode,
  userMode: ThemeMode | null = null,
  systemPrefersDark = false,
): EffectiveTheme {
  const mode = userMode ?? siteMode
  if (mode === 'DARK') return 'dark'
  if (mode === 'LIGHT') return 'light'
  return systemPrefersDark ? 'dark' : 'light'
}

/** 切换顺序：深色 → 浅色 → 跟随系统 → 深色 */
export function nextThemeMode(current: ThemeMode): ThemeMode {
  const index = THEME_MODE_ORDER.indexOf(current)
  return THEME_MODE_ORDER[(index + 1) % THEME_MODE_ORDER.length] ?? 'DARK'
}

/** 是否在浏览器环境（SSR/测试保护） */
export function prefersDark(): boolean {
  try {
    return typeof window !== 'undefined' && typeof window.matchMedia === 'function'
      ? window.matchMedia('(prefers-color-scheme: dark)').matches
      : false
  } catch {
    return false
  }
}
