/**
 * 页面配置的纯函数工具（编辑器与运行时共用，不依赖 DOM）。
 * 校验规则与后端 PageConfigValidator v2 对齐；后端为权威校验，前端用于即时提示。
 */
import {
  COMPONENT_TYPES,
  MAX_COMPONENTS,
  MIN_COMPONENTS,
  PAGE_SCHEMA_VERSION,
  THEME_PRESETS,
  type ComponentType,
  type PageComponent,
  type PageConfig,
  type ThemeConfig,
} from '@/types/page'
import { getComponentDef, isComponentType } from '@/components/registry'
import { PAGE_ID_PATTERN, validateAction, type ActionTargetPage } from './actions'

const ID_ALPHABET = 'abcdefghijklmnopqrstuvwxyz0123456789'

/** 生成组件唯一 ID：cmp_ + 8 位小写字母数字，满足 PAGE_SCHEMA 的 ^[a-z0-9_]{3,40}$ */
export function createComponentId(): string {
  let suffix = ''
  for (let i = 0; i < 8; i += 1) {
    suffix += ID_ALPHABET[Math.floor(Math.random() * ID_ALPHABET.length)]
  }
  return `cmp_${suffix}`
}

/** 依据组件注册表创建默认配置的新组件（button 自动带 none 动作） */
export function createComponent(type: ComponentType): PageComponent {
  const def = getComponentDef(type)
  const component: PageComponent = {
    id: createComponentId(),
    type: def.type,
    title: def.defaultTitle,
    visible: true,
    dataSource: def.dataSource,
    props: { ...def.defaultProps },
  }
  if (def.supportsAction) {
    component.action = { type: 'none' }
  }
  return component
}

/** 深拷贝配置（编辑器本地状态与原始配置隔离） */
export function cloneConfig<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

export function findComponent(config: PageConfig, id: string): PageComponent | undefined {
  return config.components.find((item) => item.id === id)
}

export const PAGE_TITLE_MAX_LENGTH = 60
export const SLUG_PATTERN = /^[a-z0-9][a-z0-9-]{1,62}$/
export const COMPONENT_ID_PATTERN = /^[a-z0-9_]{3,40}$/
export const COLOR_PATTERN = /^#[0-9A-Fa-f]{6}$/

/** 前端校验上下文：页面身份 + 可跳转页面注册表（用于按钮 action 校验） */
export interface PageValidationContext {
  projectSlug: string
  pageId: string
  targetPages: ActionTargetPage[]
}

export interface ClientValidationResult {
  valid: boolean
  errors: string[]
  warnings: string[]
}

/**
 * 前端轻量校验：保存前尽早提示明显错误；后端仍会做权威校验。
 * 只接受 schemaVersion=2（编辑/保存的是 v2 配置）。
 */
export function validateDraftConfig(
  config: PageConfig,
  ctx: PageValidationContext,
): ClientValidationResult {
  const errors: string[] = []
  const warnings: string[] = []
  const push = (message: string) => errors.push(message)
  const warn = (message: string) => warnings.push(message)

  if (config.schemaVersion !== PAGE_SCHEMA_VERSION) {
    push(`不支持的配置格式版本：${String(config.schemaVersion)}（当前编辑器只保存 v${PAGE_SCHEMA_VERSION}）`)
  }
  const pageId = config.pageId ?? ''
  if (!PAGE_ID_PATTERN.test(pageId)) {
    push('页面配置缺少合法的 pageId')
  } else if (pageId !== ctx.pageId) {
    push('页面配置的 pageId 与当前页面不一致（可能复制串页），请刷新后重试')
  }
  const title = (config.pageTitle ?? '').trim()
  if (title.length < 1 || title.length > PAGE_TITLE_MAX_LENGTH) {
    push(`页面标题需为 1-${PAGE_TITLE_MAX_LENGTH} 个字符`)
  }
  if (!SLUG_PATTERN.test(config.projectSlug ?? '')) {
    push('项目标识格式不正确')
  } else if (config.projectSlug !== ctx.projectSlug) {
    push('配置中的项目标识与当前站点不一致，请刷新后重试')
  }

  const { theme } = config
  const colorKeys: Array<keyof ThemeConfig> = [
    'primaryColor',
    'accentColor',
    'backgroundColor',
    'surfaceColor',
    'textColor',
    'mutedTextColor',
  ]
  for (const key of colorKeys) {
    if (!COLOR_PATTERN.test(String(theme?.[key] ?? ''))) {
      push(`主题颜色 ${key} 格式不正确（应为 #RRGGBB）`)
    }
  }
  if (!['system', 'serif', 'mono'].includes(theme?.fontFamily)) {
    push('主题字体设置不正确')
  }
  if (!Number.isInteger(theme?.borderRadius) || theme.borderRadius < 0 || theme.borderRadius > 24) {
    push('主题圆角需为 0-24 的整数')
  }

  if (config.components.length < MIN_COMPONENTS || config.components.length > MAX_COMPONENTS) {
    push(`组件数量需为 ${MIN_COMPONENTS}-${MAX_COMPONENTS} 个`)
  }
  const ids = new Set<string>()
  config.components.forEach((component, index) => {
    const label = `组件 ${index + 1}`
    if (!COMPONENT_ID_PATTERN.test(component.id)) {
      push(`${label} 的 ID 格式不正确`)
    }
    if (ids.has(component.id)) {
      push(`${label} 的 ID 重复`)
    }
    ids.add(component.id)
    if (!COMPONENT_TYPES.includes(component.type) || !isComponentType(component.type)) {
      push(`${label} 的类型不支持：${component.type}`)
      return
    }
    const def = getComponentDef(component.type)
    if (component.dataSource !== def.dataSource) {
      push(`${label} 的数据源与组件类型不匹配`)
    }
    const componentTitle = (component.title ?? '').trim()
    if (componentTitle.length < 1 || componentTitle.length > 60) {
      push(`${label} 的标题需为 1-60 个字符`)
    }
    if (typeof component.visible !== 'boolean') {
      push(`${label} 的显示状态必须为布尔值`)
    }

    const allowed = new Set(def.propDefs.map((prop) => prop.key))
    Object.keys(component.props ?? {}).forEach((key) => {
      if (!allowed.has(key)) {
        push(`${label} 包含未开放的属性：${key}`)
      }
    })
    def.propDefs.forEach((prop) => {
      const value = component.props?.[prop.key]
      if (value === undefined) return
      if (prop.type === 'number') {
        if (typeof value !== 'number' || !Number.isFinite(value) || value < prop.min || value > prop.max) {
          push(`${label} 的「${prop.label}」需为 ${prop.min}-${prop.max} 的数字`)
        }
      } else if (prop.type === 'boolean') {
        if (typeof value !== 'boolean') {
          push(`${label} 的「${prop.label}」需为布尔值`)
        }
      } else if (prop.type === 'select') {
        if (typeof value !== 'string' || !prop.options.some((option) => option.value === value)) {
          push(`${label} 的「${prop.label}」取值不合法`)
        }
      } else {
        if (typeof value !== 'string') {
          push(`${label} 的「${prop.label}」需为文本`)
        } else if (value.length > prop.maxLength) {
          push(`${label} 的「${prop.label}」不能超过 ${prop.maxLength} 个字符`)
        }
      }
    })

    // 按钮动作校验
    if (def.supportsAction) {
      const result = validateAction(component.action, { targetPages: ctx.targetPages })
      Object.values(result.errors).forEach((message) => push(`${label}（${component.title}）：${message}`))
      Object.values(result.warnings).forEach((message) => warn(`${label}（${component.title}）：${message}`))
    } else if (component.action !== undefined) {
      push(`${label} 的类型不支持动作配置（action 仅允许 button 组件）`)
    }
  })

  return { valid: errors.length === 0, errors, warnings }
}

/** 主题 → CSS 变量对象（供 :style 绑定），与 PAGE_SCHEMA §3 的变量名一一对应 */
export function themeToCssVars(theme: ThemeConfig): Record<string, string> {
  return {
    '--gcc-primary': theme.primaryColor,
    '--gcc-accent': theme.accentColor,
    '--gcc-bg': theme.backgroundColor,
    '--gcc-surface': theme.surfaceColor,
    '--gcc-text': theme.textColor,
    '--gcc-muted': theme.mutedTextColor,
    '--gcc-radius': `${theme.borderRadius}px`,
  }
}

export function fontFamilyValue(family: ThemeConfig['fontFamily'] | undefined): string {
  if (family === 'serif') {
    return 'Georgia, "Songti SC", "Noto Serif SC", serif'
  }
  if (family === 'mono') {
    return '"JetBrains Mono", "Cascadia Code", Consolas, monospace'
  }
  return '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif'
}

/** 找到与当前主题最接近的预设 id（用于编辑器色板高亮） */
export function matchThemePreset(theme: ThemeConfig): string | null {
  const preset = THEME_PRESETS.find(
    (item) =>
      item.theme.primaryColor.toLowerCase() === theme.primaryColor.toLowerCase() &&
      item.theme.accentColor.toLowerCase() === theme.accentColor.toLowerCase(),
  )
  return preset?.id ?? null
}
