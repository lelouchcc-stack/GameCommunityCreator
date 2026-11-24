/**
 * 主题系统测试（v2，PAGE_SCHEMA §3：页面品牌色 + 站点深浅色）。
 *
 * 覆盖：
 * 1) `utils/theme.ts` —— 模式解析 / 顺序循环 / localStorage 记忆（访客偏好）
 * 2) `runtime/themeContext.ts` —— 页面主题 + 明暗模式 → CSS 变量（品牌色保留）
 */
import { beforeEach, describe, expect, it } from 'vitest'

import { DEFAULT_THEME, THEME_PRESETS, type ThemeConfig } from '@/types/page'
import { THEME_MODE_LABELS, THEME_MODES, type ThemeMode } from '@/types/site'
import {
  THEME_MODE_ORDER,
  nextThemeMode,
  prefersDark,
  readStoredThemeMode,
  resolveThemeMode,
  storeThemeMode,
  themeStorageKey,
} from '@/utils/theme'
import {
  LIGHT_PALETTE,
  buildThemeStyle,
  hexToRgba,
  mixHex,
  systemPrefersDark,
} from '@/runtime/themeContext'
import { fontFamilyValue, matchThemePreset, themeToCssVars } from '@/utils/pageConfig'
import { installMemoryStorage } from './helpers/webStorage'

const TEST_THEME: ThemeConfig = {
  ...DEFAULT_THEME,
  primaryColor: '#13579B',
  accentColor: '#2468AC',
  borderRadius: 8,
  fontFamily: 'serif',
}

beforeEach(() => {
  // Node 25 + jsdom 下全局 localStorage 可能不可用，统一替换为内存实现（见 helper 注释）
  installMemoryStorage()
})

// ---------------------------------------------------------------------------
// 主题模式枚举与顺序
// ---------------------------------------------------------------------------

describe('主题模式枚举', () => {
  it('只允许 DARK / LIGHT / SYSTEM，且都有中文标签', () => {
    expect(THEME_MODES).toEqual(['DARK', 'LIGHT', 'SYSTEM'])
    for (const mode of THEME_MODES) {
      expect(THEME_MODE_LABELS[mode].length).toBeGreaterThan(0)
    }
  })

  it('切换顺序：深色 → 浅色 → 跟随系统 → 深色', () => {
    expect(THEME_MODE_ORDER).toEqual(['DARK', 'LIGHT', 'SYSTEM'])
    expect(nextThemeMode('DARK')).toBe('LIGHT')
    expect(nextThemeMode('LIGHT')).toBe('SYSTEM')
    expect(nextThemeMode('SYSTEM')).toBe('DARK')
    // 循环 3 次回到原点
    let mode: ThemeMode = 'DARK'
    for (let index = 0; index < 3; index += 1) mode = nextThemeMode(mode)
    expect(mode).toBe('DARK')
  })
})

// ---------------------------------------------------------------------------
// localStorage 记忆（按站点隔离）
// ---------------------------------------------------------------------------

describe('访客主题偏好（localStorage，按 projectSlug 隔离）', () => {
  it('存储 key 带项目 slug，不同站点互不影响', () => {
    expect(themeStorageKey('stellar-drift')).toBe('gcc.theme.stellar-drift')
    expect(themeStorageKey('echo-garden')).toBe('gcc.theme.echo-garden')
    expect(themeStorageKey('stellar-drift')).not.toBe(themeStorageKey('echo-garden'))
  })

  it('未设置时返回 null', () => {
    expect(readStoredThemeMode('stellar-drift')).toBeNull()
  })

  it('三种模式均可存取；传 null 时清除', () => {
    for (const mode of THEME_MODES) {
      storeThemeMode('stellar-drift', mode)
      expect(readStoredThemeMode('stellar-drift')).toBe(mode)
    }
    storeThemeMode('stellar-drift', null)
    expect(readStoredThemeMode('stellar-drift')).toBeNull()
  })

  it('localStorage 中的非法值被忽略（不当作有效偏好）', () => {
    window.localStorage.setItem(themeStorageKey('stellar-drift'), 'BLUE')
    expect(readStoredThemeMode('stellar-drift')).toBeNull()
  })
})

// ---------------------------------------------------------------------------
// resolveThemeMode：站点默认 + 访客偏好 + 系统偏好
// ---------------------------------------------------------------------------

describe('resolveThemeMode：最终明暗解析', () => {
  it('站点 DARK / LIGHT 直接生效', () => {
    expect(resolveThemeMode('DARK', null, false)).toBe('dark')
    expect(resolveThemeMode('DARK', null, true)).toBe('dark')
    expect(resolveThemeMode('LIGHT', null, true)).toBe('light')
  })

  it('访客本地偏好优先于站点默认', () => {
    expect(resolveThemeMode('DARK', 'LIGHT', false)).toBe('light')
    expect(resolveThemeMode('LIGHT', 'DARK', false)).toBe('dark')
  })

  it('SYSTEM 跟随系统 prefers-color-scheme（站点默认与访客覆盖同理）', () => {
    expect(resolveThemeMode('SYSTEM', null, true)).toBe('dark')
    expect(resolveThemeMode('SYSTEM', null, false)).toBe('light')
    expect(resolveThemeMode('DARK', 'SYSTEM', true)).toBe('dark')
    expect(resolveThemeMode('DARK', 'SYSTEM', false)).toBe('light')
  })

  it('prefersDark 在 jsdom 环境返回布尔值（无 matchMedia 时降级 false）', () => {
    expect(typeof prefersDark()).toBe('boolean')
    expect(typeof systemPrefersDark.value).toBe('boolean')
  })
})

// ---------------------------------------------------------------------------
// 页面主题 → CSS 变量（品牌色保留，明暗只覆盖底色/表面/文字）
// ---------------------------------------------------------------------------

describe('buildThemeStyle：页面品牌色 + 明暗模式', () => {
  it('深色模式使用页面 theme 的底色/文字，品牌色原样输出', () => {
    const style = buildThemeStyle(TEST_THEME, 'dark')
    expect(style['--gcc-primary']).toBe(TEST_THEME.primaryColor)
    expect(style['--gcc-accent']).toBe(TEST_THEME.accentColor)
    expect(style['--gcc-radius']).toBe('8px')
    expect(style['--gcc-bg']).toBe(TEST_THEME.backgroundColor)
    expect(style['--gcc-text']).toBe(TEST_THEME.textColor)
    expect(style.colorScheme).toBe('dark')
  })

  it('浅色模式覆盖底色/表面/文字/弱文字，但品牌色与圆角保留', () => {
    const style = buildThemeStyle(TEST_THEME, 'light')
    expect(style['--gcc-primary']).toBe(TEST_THEME.primaryColor)
    expect(style['--gcc-accent']).toBe(TEST_THEME.accentColor)
    expect(style['--gcc-radius']).toBe('8px')
    expect(style['--gcc-bg']).toBe(LIGHT_PALETTE.backgroundColor)
    expect(style['--gcc-surface']).toBe(LIGHT_PALETTE.surfaceColor)
    expect(style['--gcc-text']).toBe(LIGHT_PALETTE.textColor)
    expect(style['--gcc-muted']).toBe(LIGHT_PALETTE.mutedTextColor)
    expect(style.colorScheme).toBe('light')
  })

  it('theme 缺省/非法颜色时回退默认主题，不产生非法 CSS 值', () => {
    const fallback = buildThemeStyle(null, 'dark')
    expect(fallback['--gcc-primary']).toBe(DEFAULT_THEME.primaryColor)
    const invalid = buildThemeStyle({ ...TEST_THEME, backgroundColor: 'not-a-color' }, 'dark')
    expect(invalid['--gcc-bg']).toBe(DEFAULT_THEME.backgroundColor)
  })

  it('主题字体族映射到 --gcc-font', () => {
    expect(buildThemeStyle(TEST_THEME, 'dark')['--gcc-font']).toContain('Georgia')
    expect(fontFamilyValue('system')).toContain('Microsoft YaHei')
    expect(fontFamilyValue('mono')).toContain('monospace')
    expect(fontFamilyValue(undefined)).toContain('sans-serif')
  })
})

// ---------------------------------------------------------------------------
// 颜色工具与主题变量
// ---------------------------------------------------------------------------

describe('主题颜色工具', () => {
  it('hexToRgba 输出 rgba；非法颜色回退默认主色', () => {
    expect(hexToRgba('#5B8CFF', 0.5)).toBe('rgba(91, 140, 255, 0.5)')
    expect(hexToRgba('bad', 0.5)).toContain('rgba(')
  })

  it('mixHex 线性混合；非法输入原样返回', () => {
    expect(mixHex('#000000', '#ffffff', 0.5)).toBe('#808080')
    expect(mixHex('#000000', '#ffffff', 0)).toBe('#000000')
    expect(mixHex('#000000', '#ffffff', 1)).toBe('#ffffff')
    expect(mixHex('bad', '#ffffff', 0.5)).toBe('bad')
  })

  it('themeToCssVars 输出 7 个与 PAGE_SCHEMA §3 对应的变量', () => {
    const vars = themeToCssVars(TEST_THEME)
    expect(vars).toMatchObject({
      '--gcc-primary': '#13579B',
      '--gcc-accent': '#2468AC',
      '--gcc-bg': TEST_THEME.backgroundColor,
      '--gcc-surface': TEST_THEME.surfaceColor,
      '--gcc-text': TEST_THEME.textColor,
      '--gcc-muted': TEST_THEME.mutedTextColor,
      '--gcc-radius': '8px',
    })
    expect(Object.keys(vars)).toHaveLength(7)
  })

  it('matchThemePreset 命中星夜预设（主色+强调色相同）', () => {
    expect(matchThemePreset(DEFAULT_THEME)).toBe('night')
    expect(THEME_PRESETS.map((preset) => preset.id)).toEqual(['night', 'crimson', 'forest'])
    expect(matchThemePreset(TEST_THEME)).toBeNull()
  })
})
