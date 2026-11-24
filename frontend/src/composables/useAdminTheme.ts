/**
 * 管理端主题（v3）：DARK / LIGHT / SYSTEM，localStorage 记忆。
 *
 * 严格与「游戏门户主题」（站点 themeMode + useSiteTheme）分离：
 * 这里不调用任何后端设置接口，只影响管理端 `.admin-theme-scope` 容器。
 */
import { computed, reactive } from 'vue'
import type { ThemeMode } from '@/types/site'
import { nextThemeMode, prefersDark } from '@/utils/theme'

const STORAGE_KEY = 'gcc.admin.theme'
export const ADMIN_THEME_LABELS: Record<ThemeMode, string> = {
  DARK: '深色',
  LIGHT: '浅色',
  SYSTEM: '跟随系统',
}

interface AdminThemeState {
  mode: ThemeMode
  systemPrefersDark: boolean
  initialized: boolean
}

const state = reactive<AdminThemeState>({
  mode: 'DARK',
  systemPrefersDark: false,
  initialized: false,
})

let mediaQuery: MediaQueryList | null = null
/** 只有管理端路由激活时才把主题属性挂到 <html>（Teleport 到 body 的 Toast 等需要） */
let attributeEnabled = false

function applyToDocument(): void {
  if (typeof document === 'undefined') return
  if (!attributeEnabled) {
    delete document.documentElement.dataset.adminTheme
    return
  }
  document.documentElement.dataset.adminTheme = resolveAdminTheme(
    state.mode,
    state.systemPrefersDark,
  )
}

/** 由 App.vue 根据当前路由调用：进入管理端启用，离开管理端移除，避免影响公开端 */
export function setAdminThemeAttributeEnabled(enabled: boolean): void {
  attributeEnabled = enabled
  applyToDocument()
}

export function resolveAdminTheme(mode: ThemeMode, systemPrefersDark: boolean): 'dark' | 'light' {
  if (mode === 'DARK') return 'dark'
  if (mode === 'LIGHT') return 'light'
  return systemPrefersDark ? 'dark' : 'light'
}

function readStoredMode(): ThemeMode {
  try {
    const value = window.localStorage.getItem(STORAGE_KEY)
    return value === 'DARK' || value === 'LIGHT' || value === 'SYSTEM' ? value : 'DARK'
  } catch {
    return 'DARK'
  }
}

function onChange(event: MediaQueryListEvent): void {
  state.systemPrefersDark = event.matches
  applyToDocument()
}

/** 应用启动时调用一次 */
export function initAdminTheme(): void {
  if (state.initialized) return
  state.mode = readStoredMode()
  state.systemPrefersDark = prefersDark()
  state.initialized = true
  applyToDocument()

  if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
    mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    state.systemPrefersDark = mediaQuery.matches
    mediaQuery.addEventListener?.('change', onChange)
    applyToDocument()
  }
}

function setMode(mode: ThemeMode): void {
  state.mode = mode
  try {
    window.localStorage.setItem(STORAGE_KEY, mode)
  } catch {
    /* 忽略存储异常 */
  }
  applyToDocument()
}

function cycleMode(): void {
  setMode(nextThemeMode(state.mode))
}

export function useAdminTheme() {
  return {
    state,
    mode: computed(() => state.mode),
    effectiveMode: computed(() => resolveAdminTheme(state.mode, state.systemPrefersDark)),
    label: computed(() => ADMIN_THEME_LABELS[state.mode]),
    setMode,
    cycleMode,
  }
}
