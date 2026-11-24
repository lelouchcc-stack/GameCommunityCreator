/**
 * 页面配置类型定义（v2：多页面 + 按钮动作）。
 *
 * 与 docs/PAGE_SCHEMA.md v2 逐字对应，是编辑器、后端校验、运行时三方的共享契约。
 * 修改本文件前必须同步修改 docs/PAGE_SCHEMA.md 并通知所有 Agent。
 */

/** 新建/保存的配置格式版本；v1 仅用于历史快照只读兼容 */
export const PAGE_SCHEMA_VERSION = 2
export const SUPPORTED_SCHEMA_VERSIONS: readonly number[] = [1, 2]
/** 页面配置序列化后的最大字节数（与后端 MAX_CONFIG_BYTES 一致） */
export const PAGE_CONFIG_MAX_BYTES = 204800
export const MAX_COMPONENTS = 20
export const MIN_COMPONENTS = 1

export type ComponentType =
  | 'hero'
  | 'about'
  | 'notice_list'
  | 'footer'
  | 'button'
  | 'changelog_list'
  | 'docs_list'
  | 'team_grid'
  | 'issue_form'
  | 'links_grid'
  | 'issue_progress'

export const COMPONENT_TYPES: readonly ComponentType[] = [
  'hero',
  'about',
  'notice_list',
  'footer',
  'button',
  'changelog_list',
  'docs_list',
  'team_grid',
  'issue_form',
  'links_grid',
  'issue_progress',
]

export type DataSourceId =
  | 'game_info'
  | 'announcements'
  | 'changelogs'
  | 'docs'
  | 'team'
  | 'links'
  | 'issues_progress'

export const DATA_SOURCES: readonly DataSourceId[] = [
  'game_info',
  'announcements',
  'changelogs',
  'docs',
  'team',
  'links',
  'issues_progress',
]

export type FontFamily = 'system' | 'serif' | 'mono'

export const FONT_FAMILIES: readonly FontFamily[] = ['system', 'serif', 'mono']

export interface ThemeConfig {
  primaryColor: string
  accentColor: string
  backgroundColor: string
  surfaceColor: string
  textColor: string
  mutedTextColor: string
  fontFamily: FontFamily
  borderRadius: number
}

export type ComponentPropValue = string | number | boolean

/** 组件属性：v2 只允许标量值，禁止嵌套对象/数组 */
export type ComponentProps = Record<string, ComponentPropValue>

/* ------------------------------ 按钮动作协议 ------------------------------ */

export type ActionType = 'navigate' | 'externalLink' | 'none'

export interface NavigateAction {
  type: 'navigate'
  targetPageId: string
}

export interface ExternalLinkAction {
  type: 'externalLink'
  url: string
  openInNewTab: boolean
}

export interface NoneAction {
  type: 'none'
}

export type PageAction = NavigateAction | ExternalLinkAction | NoneAction

export const ACTION_TYPES: readonly ActionType[] = ['navigate', 'externalLink', 'none']

export const ACTION_TYPE_LABELS: Record<ActionType, string> = {
  navigate: '站内跳转',
  externalLink: '外部链接',
  none: '无动作',
}

/* ------------------------------- 页面/组件 ------------------------------- */

export interface PageComponent {
  id: string
  type: ComponentType
  title: string
  visible: boolean
  dataSource: DataSourceId | null
  props: ComponentProps
  /** 仅 button 组件允许且必填（v2） */
  action?: PageAction
}

export interface PageConfig {
  schemaVersion: number
  /** v2 必填：所属站内页面稳定 ID；v1 历史配置缺省 */
  pageId?: string
  pageTitle: string
  projectSlug: string
  theme: ThemeConfig
  components: PageComponent[]
}

export const DEFAULT_THEME: ThemeConfig = {
  primaryColor: '#5B8CFF',
  accentColor: '#9B5CFF',
  backgroundColor: '#0B1020',
  surfaceColor: '#151B33',
  textColor: '#E8ECFF',
  mutedTextColor: '#9AA6CC',
  fontFamily: 'system',
  borderRadius: 16,
}

export interface ThemePreset {
  id: string
  label: string
  theme: ThemeConfig
}

/** 编辑器主题预设（星夜为默认） */
export const THEME_PRESETS: readonly ThemePreset[] = [
  { id: 'night', label: '星夜', theme: { ...DEFAULT_THEME } },
  {
    id: 'crimson',
    label: '赤霄',
    theme: {
      primaryColor: '#F25F5C',
      accentColor: '#FF9F1C',
      backgroundColor: '#160B0D',
      surfaceColor: '#241417',
      textColor: '#FFF1EC',
      mutedTextColor: '#C9A8A5',
      fontFamily: 'system',
      borderRadius: 12,
    },
  },
  {
    id: 'forest',
    label: '幽林',
    theme: {
      primaryColor: '#3DDC97',
      accentColor: '#8AE9C1',
      backgroundColor: '#07130F',
      surfaceColor: '#0F231C',
      textColor: '#E7FFF6',
      mutedTextColor: '#9FC9B8',
      fontFamily: 'system',
      borderRadius: 20,
    },
  },
]

/** 编辑器属性面板的字段描述 */
export type PropDef =
  | {
      key: string
      label: string
      type: 'text' | 'textarea'
      maxLength: number
      placeholder?: string
    }
  | {
      key: string
      label: string
      type: 'number'
      min: number
      max: number
      step?: number
    }
  | { key: string; label: string; type: 'boolean' }
  | {
      key: string
      label: string
      type: 'select'
      options: ReadonlyArray<{ label: string; value: string }>
    }

export interface ComponentDefinition {
  type: ComponentType
  label: string
  description: string
  /** 组件面板图标（emoji） */
  icon: string
  defaultTitle: string
  /** 该类型固定的数据源，无数据源为 null */
  dataSource: DataSourceId | null
  defaultProps: ComponentProps
  propDefs: PropDef[]
  /** 是否为支持 action 配置的通用按钮（仅 button 为 true） */
  supportsAction: boolean
}
