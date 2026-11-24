/**
 * 运行时主题上下文（页面品牌色 + 站点深浅色 + xAI 门户设计令牌派生）。
 *
 * 规则来自 docs/PAGE_SCHEMA.md §3 / docs/UI_DESIGN.md §5：
 * - 页面 theme 决定品牌色（primary / accent / radius / 字体）；
 * - 站点设置 theme_mode（DARK / LIGHT / SYSTEM）+ 访客本地偏好决定明暗；
 * - light 模式覆盖底色 / 表面 / 文字 / 次要文字，品牌色保持不变；
 * - 在 --gcc-* 变量之上派生一套「门户外观令牌」（surface 层级 / hairline 边框 /
 *   mono 字体 / 卡片圆角 / hover 等），让公开端可以做出 near-black 科技感外观，
 *   同时所有颜色仍然来自站点主题（不做管理端 --x-* 硬编码）。
 *
 * PublicSiteLayout 用 useSiteTheme 计算最终模式并通过 provide 传给 CommunityRuntime；
 * 没有布局包裹的场景（如编辑器画布、草稿预览）由运行时自行兜底解析。
 */
import { ref, type ComputedRef, type InjectionKey } from 'vue'
import { DEFAULT_THEME, type ThemeConfig } from '@/types/page'
import { fontFamilyValue, themeToCssVars } from '@/utils/pageConfig'
import { prefersDark } from '@/utils/theme'

export type EffectiveTheme = 'dark' | 'light'

/** 布局 → 运行时传递「最终生效明暗模式」的注入键 */
export const RUNTIME_THEME_KEY: InjectionKey<ComputedRef<EffectiveTheme>> =
  Symbol('gcc-runtime-theme')

/** light 模式固定调色板（覆盖页面 theme 的底色/表面/文字/次要文字） */
export const LIGHT_PALETTE = {
  backgroundColor: '#f4f6fb',
  surfaceColor: '#ffffff',
  textColor: '#1c2438',
  mutedTextColor: '#5b6478',
} as const

const LIGHT_BORDER = 'rgba(28, 36, 56, 0.12)'
const LIGHT_BORDER_STRONG = 'rgba(28, 36, 56, 0.26)'
const LIGHT_SHADOW = '0 18px 44px rgba(31, 45, 84, 0.12)'
const MONO_STACK =
  "'JetBrains Mono', 'Geist Mono', 'Cascadia Code', 'SFMono-Regular', Consolas, 'Liberation Mono', monospace"

function parseHex(hex: string): [number, number, number] | null {
  const match = /^#([0-9a-fA-F]{6})$/.exec(String(hex ?? '').trim())
  if (!match) return null
  const value = Number.parseInt(match[1], 16)
  return [(value >> 16) & 255, (value >> 8) & 255, value & 255]
}

function toHex(rgb: [number, number, number]): string {
  return `#${rgb
    .map((channel) => Math.min(255, Math.max(0, Math.round(channel))).toString(16).padStart(2, '0'))
    .join('')}`
}

/** #RRGGBB → rgba(r,g,b,alpha)；非法颜色回退主题默认主色 */
export function hexToRgba(hex: string, alpha: number): string {
  const rgb = parseHex(hex) ?? parseHex(DEFAULT_THEME.primaryColor) ?? [91, 140, 255]
  return `rgba(${rgb[0]}, ${rgb[1]}, ${rgb[2]}, ${alpha})`
}

/** 两个 #RRGGBB 之间线性混合（ratio=0 取 source，1 取 target）；非法值原样返回 */
export function mixHex(hex: string, target: string, ratio: number): string {
  const source = parseHex(hex)
  const goal = parseHex(target)
  if (!source || !goal) return hex
  const clamped = Math.min(1, Math.max(0, ratio))
  return toHex([
    source[0] + (goal[0] - source[0]) * clamped,
    source[1] + (goal[1] - source[1]) * clamped,
    source[2] + (goal[2] - source[2]) * clamped,
  ])
}

function safeSurface(theme: ThemeConfig): string {
  return parseHex(theme.surfaceColor) ? theme.surfaceColor : DEFAULT_THEME.surfaceColor
}

function safeBackground(theme: ThemeConfig): string {
  return parseHex(theme.backgroundColor) ? theme.backgroundColor : DEFAULT_THEME.backgroundColor
}

/**
 * 由「页面主题 + 最终明暗模式」生成 CSS 变量与根样式。
 * 返回对象可直接绑定到 :style —— 运行时根节点与公开布局共用同一套口径。
 */
export function buildThemeStyle(
  theme: ThemeConfig | null | undefined,
  effective: EffectiveTheme,
): Record<string, string> {
  const source: ThemeConfig = theme ?? DEFAULT_THEME
  const light = effective === 'light'
  const background = light ? LIGHT_PALETTE.backgroundColor : safeBackground(source)
  const surface = light ? LIGHT_PALETTE.surfaceColor : safeSurface(source)
  const text = light ? LIGHT_PALETTE.textColor : source.textColor
  const muted = light ? LIGHT_PALETTE.mutedTextColor : source.mutedTextColor

  /* ---- 派生门户外观令牌 ---- */
  const surface2 = light ? '#f2f2f3' : mixHex(surface, '#ffffff', 0.05)
  const surface3 = light ? '#e9e9ec' : mixHex(surface, '#ffffff', 0.09)
  const textSecondary = light ? '#3f4147' : hexToRgba(text, 0.82)
  const border = light ? LIGHT_BORDER : hexToRgba(text, 0.14)
  const borderStrong = light ? LIGHT_BORDER_STRONG : hexToRgba(text, 0.28)
  const headerBg = light ? 'rgba(255, 255, 255, 0.88)' : hexToRgba(background, 0.86)
  const inputBg = light ? '#ffffff' : mixHex(background, '#000000', 0.24)
  const cardBg = light ? '#ffffff' : mixHex(surface, '#ffffff', 0.02)
  const hover = light ? 'rgba(15, 23, 42, 0.05)' : 'rgba(255, 255, 255, 0.06)'
  const hoverStrong = light ? 'rgba(15, 23, 42, 0.09)' : 'rgba(255, 255, 255, 0.1)'

  const backgroundImage = light
    ? `radial-gradient(1100px 560px at 10% -8%, ${hexToRgba(source.primaryColor, 0.1)}, transparent 62%), radial-gradient(900px 480px at 100% 0%, ${hexToRgba(
        source.accentColor,
        0.08,
      )}, transparent 55%), ${background}`
    : `radial-gradient(1200px 600px at 12% -8%, ${hexToRgba(source.primaryColor, 0.16)}, transparent 60%), radial-gradient(900px 500px at 100% 0%, ${hexToRgba(
        source.accentColor,
        0.12,
      )}, transparent 55%), ${background}`

  const font = fontFamilyValue(source.fontFamily)

  return {
    ...themeToCssVars(source),
    // 明暗模式覆盖底色/表面/文字/次要文字；primary/accent/radius 来自页面主题保持不变
    '--gcc-bg': background,
    '--gcc-surface': surface,
    '--gcc-surface-2': surface2,
    '--gcc-surface-3': surface3,
    '--gcc-text': text,
    '--gcc-text-secondary': textSecondary,
    '--gcc-muted': muted,
    '--gcc-border': border,
    '--gcc-border-strong': borderStrong,
    '--gcc-shadow': light ? LIGHT_SHADOW : 'none',
    '--gcc-card-bg': cardBg,
    '--gcc-panel': cardBg,
    '--gcc-input-bg': inputBg,
    '--gcc-nav-bg': headerBg,
    '--gcc-header-bg': headerBg,
    '--gcc-hover': hover,
    '--gcc-hover-strong': hoverStrong,
    '--gcc-badge-bg': light ? 'rgba(28, 36, 56, 0.05)' : 'rgba(255, 255, 255, 0.05)',
    '--gcc-accent-soft': hexToRgba(source.accentColor, light ? 0.1 : 0.16),
    '--gcc-primary-soft': hexToRgba(source.primaryColor, light ? 0.1 : 0.16),
    // xAI 门户外观：卡片圆角不超过 8px、pill 按钮、mono 字体、hairline 边框
    '--gcc-card-radius': 'min(var(--gcc-radius), 8px)',
    '--gcc-pill-radius': '9999px',
    '--gcc-mono': MONO_STACK,
    '--gcc-font': font,
    fontFamily: font,
    background: backgroundImage,
    color: text,
    colorScheme: light ? 'light' : 'dark',
  }
}

/* ------------------------------ 系统深色偏好 ------------------------------ */

export const systemPrefersDark = ref<boolean>(prefersDark())

let systemListenerInstalled = false

/** 懒注册 prefers-color-scheme 监听（只注册一次，SSR / 测试环境安全降级） */
export function ensureSystemThemeListener(): void {
  if (systemListenerInstalled) return
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return
  systemListenerInstalled = true
  try {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    systemPrefersDark.value = mediaQuery.matches
    mediaQuery.addEventListener?.('change', (event) => {
      systemPrefersDark.value = event.matches
    })
  } catch {
    /* matchMedia 不可用时保持当前值 */
  }
}
