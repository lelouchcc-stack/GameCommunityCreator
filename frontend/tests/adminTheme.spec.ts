/**
 * 管理端独立主题（v3 / FIX-2）测试。
 *
 * 覆盖：模式解析 / 切换循环 / localStorage 持久化 / SYSTEM 跟随系统 / 不调用后端接口。
 * 与门户主题（useSiteTheme / 站点 themeMode）严格分离。
 */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { installMemoryStorage } from './helpers/webStorage'

const STORAGE_KEY = 'gcc.admin.theme'
let moduleVersion = 0

async function loadAdminTheme() {
  vi.resetModules()
  moduleVersion += 1
  return import(`@/composables/useAdminTheme?case=${moduleVersion}`)
}

beforeEach(() => {
  // Node 25 + jsdom 下全局 localStorage 可能不可用，统一替换为内存实现
  installMemoryStorage()
  delete (document.documentElement.dataset as DOMStringMap).adminTheme
})

afterEach(() => {
  vi.restoreAllMocks()
  delete (document.documentElement.dataset as DOMStringMap).adminTheme
})

describe('resolveAdminTheme：模式解析', () => {
  it('DARK / LIGHT 直接生效；SYSTEM 跟随系统偏好', async () => {
    const { resolveAdminTheme } = await loadAdminTheme()
    expect(resolveAdminTheme('DARK', false)).toBe('dark')
    expect(resolveAdminTheme('DARK', true)).toBe('dark')
    expect(resolveAdminTheme('LIGHT', true)).toBe('light')
    expect(resolveAdminTheme('SYSTEM', true)).toBe('dark')
    expect(resolveAdminTheme('SYSTEM', false)).toBe('light')
  })
})

describe('initAdminTheme：初始化与持久化', () => {
  it('默认 DARK，并写入 document.documentElement[data-admin-theme]', async () => {
    const { initAdminTheme, useAdminTheme } = await loadAdminTheme()
    initAdminTheme()
    const theme = useAdminTheme()
    expect(theme.mode.value).toBe('DARK')
    expect(theme.effectiveMode.value).toBe('dark')
  })

  it('初始化时读取 localStorage 中已有偏好（刷新恢复）', async () => {
    window.localStorage.setItem(STORAGE_KEY, 'LIGHT')
    const { initAdminTheme, useAdminTheme } = await loadAdminTheme()
    initAdminTheme()
    expect(useAdminTheme().mode.value).toBe('LIGHT')
    expect(useAdminTheme().effectiveMode.value).toBe('light')
  })

  it('localStorage 中的非法值回退 DARK', async () => {
    window.localStorage.setItem(STORAGE_KEY, 'BLUE')
    const { initAdminTheme, useAdminTheme } = await loadAdminTheme()
    initAdminTheme()
    expect(useAdminTheme().mode.value).toBe('DARK')
  })
})

describe('setMode / cycleMode：切换与持久化', () => {
  it('setMode 更新状态、写入 localStorage 与 document 属性', async () => {
    const { initAdminTheme, useAdminTheme } = await loadAdminTheme()
    initAdminTheme()
    const theme = useAdminTheme()
    theme.setMode('LIGHT')
    expect(theme.mode.value).toBe('LIGHT')
    expect(theme.effectiveMode.value).toBe('light')
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe('LIGHT')
  })

  it('cycleMode 顺序：深色 → 浅色 → 跟随系统 → 深色', async () => {
    const { initAdminTheme, useAdminTheme } = await loadAdminTheme()
    initAdminTheme()
    const theme = useAdminTheme()
    expect(theme.mode.value).toBe('DARK')
    theme.cycleMode()
    expect(theme.mode.value).toBe('LIGHT')
    theme.cycleMode()
    expect(theme.mode.value).toBe('SYSTEM')
    theme.cycleMode()
    expect(theme.mode.value).toBe('DARK')
  })

  it('ADMIN_THEME_LABELS 提供中文文案，label 随模式变化', async () => {
    const { initAdminTheme, useAdminTheme, ADMIN_THEME_LABELS } = await loadAdminTheme()
    initAdminTheme()
    const theme = useAdminTheme()
    expect(ADMIN_THEME_LABELS.DARK).toBe('深色')
    expect(ADMIN_THEME_LABELS.LIGHT).toBe('浅色')
    expect(ADMIN_THEME_LABELS.SYSTEM).toBe('跟随系统')
    expect(theme.label.value).toBe('深色')
  })
})

describe('主题分离（FIX-2 核心约束）', () => {
  it('切换管理端主题不调用任何后端接口（不修改站点 themeMode）', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    const { initAdminTheme, useAdminTheme } = await loadAdminTheme()
    initAdminTheme()
    useAdminTheme().setMode('LIGHT')
    useAdminTheme().cycleMode()
    expect(fetchSpy).not.toHaveBeenCalled()
  })
})
