/**
 * 页面配置 v2 校验测试（PAGE_SCHEMA §1–§4，前端轻校验 validateDraftConfig）。
 *
 * 与后端 PageConfigValidator v2 对齐：schemaVersion=2、pageId、主题、11 种组件、
 * 属性白名单/类型/范围、按钮动作规则（navigate / externalLink / none）。
 */
import { describe, expect, it } from 'vitest'

import { COMPONENT_DEFS } from '@/components/registry'
import {
  DEFAULT_THEME,
  MAX_COMPONENTS,
  PAGE_CONFIG_MAX_BYTES,
  PAGE_SCHEMA_VERSION,
  type ComponentType,
  type PageComponent,
  type PageConfig,
  type ThemeConfig,
} from '@/types/page'
import {
  COLOR_PATTERN,
  COMPONENT_ID_PATTERN,
  PAGE_TITLE_MAX_LENGTH,
  SLUG_PATTERN,
  cloneConfig,
  createComponent,
  createComponentId,
  findComponent,
  validateDraftConfig,
  type PageValidationContext,
} from '@/utils/pageConfig'

// ---------------------------------------------------------------------------
// fixtures
// ---------------------------------------------------------------------------

const CTX: PageValidationContext = {
  projectSlug: 'stellar-drift',
  pageId: 'page_home_1',
  targetPages: [
    { pageId: 'page_home_1', name: '首页', pageType: 'home', status: 'PUBLISHED', slug: 'home' },
    { pageId: 'page_changelog_1', name: '更新日志', pageType: 'changelog', status: 'PUBLISHED', slug: 'changelog' },
    { pageId: 'page_docs_1', name: '公开文档', pageType: 'docs', status: 'DRAFT', slug: 'docs' },
    { pageId: 'page_team_1', name: '制作组', pageType: 'team', status: 'DISABLED', slug: 'team' },
  ],
}

const THEME: ThemeConfig = { ...DEFAULT_THEME }

function component(type: ComponentType, patch: Partial<PageComponent> = {}): PageComponent {
  const def = COMPONENT_DEFS[type]
  return {
    id: `cmp_${type.toLowerCase()}_1`,
    type,
    title: def.defaultTitle,
    visible: true,
    dataSource: def.dataSource,
    props: { ...def.defaultProps },
    ...patch,
  }
}

function validConfig(patch: Partial<PageConfig> = {}): PageConfig {
  return {
    schemaVersion: PAGE_SCHEMA_VERSION,
    pageId: 'page_home_1',
    pageTitle: '星汐边境 官方社区',
    projectSlug: 'stellar-drift',
    theme: { ...THEME },
    components: [
      component('hero'),
      component('notice_list'),
      component('footer', { id: 'cmp_footer_1' }),
      component('button', {
        id: 'cmp_button_1',
        action: { type: 'navigate', targetPageId: 'page_changelog_1' },
      }),
    ],
    ...patch,
  }
}

function errorsOf(config: PageConfig, ctx: PageValidationContext = CTX): string[] {
  return validateDraftConfig(config, ctx).errors
}

function expectInvalid(config: PageConfig, expected: RegExp, ctx: PageValidationContext = CTX): void {
  const result = validateDraftConfig(config, ctx)
  expect(result.valid).toBe(false)
  expect(result.errors.join('\n')).toMatch(expected)
}

// ---------------------------------------------------------------------------
// 合法配置 & schemaVersion
// ---------------------------------------------------------------------------

describe('合法配置', () => {
  it('完整 v2 配置通过校验（无 error / warning）', () => {
    const result = validateDraftConfig(validConfig(), CTX)
    expect(result.errors).toEqual([])
    expect(result.warnings).toEqual([])
    expect(result.valid).toBe(true)
  })

  it('schemaVersion 必须是 2（v1 历史配置拒绝保存）', () => {
    expectInvalid(validConfig({ schemaVersion: 1 }), /schemaVersion|格式版本/)
    expectInvalid(validConfig({ schemaVersion: 3 }), /schemaVersion|格式版本/)
  })

  it('空配置 / 无组件无法通过', () => {
    expectInvalid(validConfig({ components: [] }), /组件数量/)
  })
})

// ---------------------------------------------------------------------------
// pageId / pageTitle / projectSlug
// ---------------------------------------------------------------------------

describe('页面身份字段', () => {
  it('缺少 pageId → 失败', () => {
    expectInvalid(validConfig({ pageId: undefined }), /pageId/)
  })

  it('pageId 格式非法 → 失败', () => {
    for (const pageId of ['home', 'page_1', 'page_ABC', 'page-abc']) {
      expectInvalid(validConfig({ pageId }), /pageId/)
    }
  })

  it('pageId 与当前页面不一致 → 失败（防复制串页）', () => {
    expectInvalid(validConfig({ pageId: 'page_other_9' }), /pageId.*不一致|不一致.*pageId/)
  })

  it(`pageTitle 为空 / 超过 ${PAGE_TITLE_MAX_LENGTH} 字符 → 失败`, () => {
    expectInvalid(validConfig({ pageTitle: '' }), /页面标题/)
    expectInvalid(validConfig({ pageTitle: '   ' }), /页面标题/)
    expectInvalid(validConfig({ pageTitle: '标'.repeat(PAGE_TITLE_MAX_LENGTH + 1) }), /页面标题/)
  })

  it(`pageTitle 恰好 ${PAGE_TITLE_MAX_LENGTH} 字符 → 通过`, () => {
    const result = validateDraftConfig(
      validConfig({ pageTitle: '标'.repeat(PAGE_TITLE_MAX_LENGTH) }),
      CTX,
    )
    expect(result.errors).toEqual([])
  })

  it('projectSlug 格式非法 / 与站点不一致 → 失败', () => {
    expectInvalid(validConfig({ projectSlug: 'Bad Slug' }), /项目标识/)
    expectInvalid(validConfig({ projectSlug: 'another-site' }), /项目标识与当前站点不一致/)
  })
})

// ---------------------------------------------------------------------------
// 主题（PAGE_SCHEMA §3）
// ---------------------------------------------------------------------------

describe('主题配置', () => {
  it('6 个颜色必须是 #RRGGBB', () => {
    const colorKeys = [
      'primaryColor',
      'accentColor',
      'backgroundColor',
      'surfaceColor',
      'textColor',
      'mutedTextColor',
    ] as const
    for (const key of colorKeys) {
      expectInvalid(validConfig({ theme: { ...THEME, [key]: 'red' } }), /主题颜色/)
    }
  })

  it('fontFamily 只允许 system / serif / mono', () => {
    expectInvalid(validConfig({ theme: { ...THEME, fontFamily: 'comic' as never } }), /主题字体/)
  })

  it('borderRadius 必须是 0–24 整数', () => {
    expectInvalid(validConfig({ theme: { ...THEME, borderRadius: -1 } }), /主题圆角/)
    expectInvalid(validConfig({ theme: { ...THEME, borderRadius: 25 } }), /主题圆角/)
    expectInvalid(validConfig({ theme: { ...THEME, borderRadius: 8.5 } }), /主题圆角/)
  })
})

// ---------------------------------------------------------------------------
// 组件列表与类型
// ---------------------------------------------------------------------------

describe('组件列表规则', () => {
  it(`组件数量 1–${MAX_COMPONENTS} 个`, () => {
    const many = Array.from({ length: MAX_COMPONENTS + 1 }, (_, index) =>
      component('footer', { id: `cmp_footer_${index}` }),
    )
    expectInvalid(validConfig({ components: many }), /组件数量/)
  })

  it('组件 id 必须唯一且符合格式', () => {
    const duplicate = [component('footer', { id: 'cmp_same' }), component('footer', { id: 'cmp_same' })]
    expectInvalid(validConfig({ components: duplicate }), /ID 重复/)
    expectInvalid(
      validConfig({ components: [component('footer', { id: 'BAD-ID' })] }),
      /ID 格式不正确/,
    )
    expectInvalid(validConfig({ components: [component('footer', { id: 'ab' })] }), /ID 格式不正确/)
  })

  it('未知组件类型 → 失败', () => {
    const unknown = { ...component('footer'), type: 'iframe' as ComponentType }
    expectInvalid(validConfig({ components: [unknown] }), /类型不支持/)
  })

  it('11 种注册类型全部可被接受', () => {
    for (const type of Object.keys(COMPONENT_DEFS) as ComponentType[]) {
      const config = validConfig({
        components: [
          component(type, {
            id: 'cmp_known_1',
            ...(type === 'button' ? { action: { type: 'none' as const } } : {}),
          }),
        ],
      })
      const result = validateDraftConfig(config, CTX)
      expect(result.errors, `组件类型 ${type} 应被接受`).toEqual([])
    }
  })

  it('dataSource 必须等于该类型固定数据源', () => {
    expectInvalid(
      validConfig({ components: [component('hero', { dataSource: 'changelogs' })] }),
      /数据源与组件类型不匹配/,
    )
    expectInvalid(
      validConfig({ components: [component('footer', { dataSource: 'game_info' })] }),
      /数据源与组件类型不匹配/,
    )
  })

  it('组件标题 1–60 字符，visible 必须是布尔值', () => {
    expectInvalid(validConfig({ components: [component('footer', { title: '' })] }), /标题/)
    expectInvalid(
      validConfig({ components: [component('footer', { title: '标'.repeat(61) })] }),
      /标题/,
    )
    expectInvalid(
      validConfig({ components: [component('footer', { visible: 'yes' as unknown as boolean })] }),
      /显示状态/,
    )
  })
})

// ---------------------------------------------------------------------------
// 属性白名单 / 类型 / 范围
// ---------------------------------------------------------------------------

describe('组件属性（props）校验', () => {
  it('未知属性 → 失败（白名单之外一律拒绝）', () => {
    expectInvalid(
      validConfig({
        components: [component('footer', { props: { copyright: '', note: '', danger: '<script>' } })],
      }),
      /未开放的属性：danger/,
    )
  })

  it('number 属性超出范围 / 类型错误 → 失败', () => {
    expectInvalid(
      validConfig({ components: [component('notice_list', { props: { limit: 99, showPinned: true } })] }),
      /展示条数/,
    )
    expectInvalid(
      validConfig({ components: [component('docs_list', { props: { limit: 21, category: '' } })] }),
      /展示条数/,
    )
    expectInvalid(
      validConfig({ components: [component('team_grid', { props: { columns: 0, showRole: true } })] }),
      /每行卡片数/,
    )
    expectInvalid(
      validConfig({
        components: [component('notice_list', { props: { limit: '3' as unknown as number, showPinned: true } })],
      }),
      /展示条数/,
    )
  })

  it('boolean 属性类型错误 → 失败', () => {
    expectInvalid(
      validConfig({
        components: [component('about', { props: { body: '', showTagline: 'yes' as unknown as boolean } })],
      }),
      /显示一句话简介/,
    )
    expectInvalid(
      validConfig({
        components: [component('notice_list', { props: { limit: 3, showPinned: 1 as unknown as boolean } })],
      }),
      /置顶公告优先/,
    )
  })

  it('select 属性只允许枚举值', () => {
    expectInvalid(
      validConfig({
        components: [component('hero', { props: { ...COMPONENT_DEFS.hero.defaultProps, align: 'justify' } })],
      }),
      /内容对齐/,
    )
    expectInvalid(
      validConfig({
        components: [
          component('button', {
            props: { ...COMPONENT_DEFS.button.defaultProps, variant: 'danger' },
            action: { type: 'none' },
          }),
        ],
      }),
      /按钮样式/,
    )
  })

  it('text 属性必须是字符串且不超过 maxLength', () => {
    expectInvalid(
      validConfig({
        components: [component('about', { props: { body: 123 as unknown as string, showTagline: true } })],
      }),
      /简介正文/,
    )
    expectInvalid(
      validConfig({
        components: [component('about', { props: { body: '正'.repeat(1001), showTagline: true } })],
      }),
      /简介正文/,
    )
  })

  it('嵌套对象/数组作为属性值 → 失败（v2 只允许标量）', () => {
    expectInvalid(
      validConfig({
        components: [
          component('hero', {
            props: { ...COMPONENT_DEFS.hero.defaultProps, headline: { html: '<b>x</b>' } as never },
          }),
        ],
      }),
      /横幅主标题/,
    )
  })
})

// ---------------------------------------------------------------------------
// 按钮动作（PAGE_SCHEMA §4）
// ---------------------------------------------------------------------------

describe('按钮动作校验', () => {
  function buttonConfig(action: PageComponent['action']): PageConfig {
    return validConfig({
      components: [component('button', { id: 'cmp_button_1', action })],
    })
  }

  it('button 必须带 action', () => {
    expectInvalid(buttonConfig(undefined), /请选择点击动作/)
  })

  it('button 动作 none → 允许保存（运行时提示未配置）', () => {
    const result = validateDraftConfig(buttonConfig({ type: 'none' }), CTX)
    expect(result.errors).toEqual([])
  })

  it('navigate 到已发布页面 → 通过', () => {
    const result = validateDraftConfig(
      buttonConfig({ type: 'navigate', targetPageId: 'page_changelog_1' }),
      CTX,
    )
    expect(result.errors).toEqual([])
  })

  it('navigate 目标不存在 / 已停用 → 失败', () => {
    expectInvalid(buttonConfig({ type: 'navigate', targetPageId: 'page_notexist_1' }), /目标页面不存在/)
    expectInvalid(buttonConfig({ type: 'navigate', targetPageId: 'page_team_1' }), /已停用/)
  })

  it('navigate 目标未发布（DRAFT）→ 通过但给出 warning', () => {
    const result = validateDraftConfig(buttonConfig({ type: 'navigate', targetPageId: 'page_docs_1' }), CTX)
    expect(result.errors).toEqual([])
    expect(result.warnings.join('\n')).toContain('尚未发布')
  })

  it('externalLink 合法 http/https → 通过', () => {
    const result = validateDraftConfig(
      buttonConfig({ type: 'externalLink', url: 'https://example.com/join', openInNewTab: true }),
      CTX,
    )
    expect(result.errors).toEqual([])
  })

  it.each([
    'javascript:alert(1)',
    'data:text/html,<script>alert(1)</script>',
    'file:///c:/windows/win.ini',
    '/relative/path',
    '//example.com',
    '',
  ])('externalLink 非法 URL 被拒绝：%s', (url) => {
    expectInvalid(buttonConfig({ type: 'externalLink', url, openInNewTab: true }), /http\/https/)
  })

  it('未知动作类型 → 失败', () => {
    expectInvalid(buttonConfig({ type: 'popup' } as never), /请选择点击动作/)
  })

  it('非 button 组件配置 action → 失败', () => {
    expectInvalid(validConfig({ components: [component('footer', { action: { type: 'none' } })] }), /不支持动作配置/)
  })

  it('多个组件的错误会全部累积（不在第一个错误处中断）', () => {
    const config = validConfig({
      components: [
        component('notice_list', { props: { limit: 99, showPinned: true } }),
        component('button', {
          id: 'cmp_button_bad',
          action: { type: 'navigate', targetPageId: 'page_notexist_1' },
        }),
      ],
    })
    const errors = errorsOf(config)
    expect(errors.length).toBeGreaterThanOrEqual(2)
    expect(errors.join('\n')).toMatch(/展示条数/)
    expect(errors.join('\n')).toMatch(/目标页面不存在/)
  })
})

// ---------------------------------------------------------------------------
// 纯函数工具
// ---------------------------------------------------------------------------

describe('配置工具函数', () => {
  it('createComponentId 生成 cmp_ 开头、8 位小写字母数字的唯一 id', () => {
    const ids = new Set(Array.from({ length: 50 }, () => createComponentId()))
    expect(ids.size).toBe(50)
    for (const id of ids) {
      expect(COMPONENT_ID_PATTERN.test(id)).toBe(true)
      expect(id).toMatch(/^cmp_[a-z0-9]{8}$/)
    }
  })

  it('createComponent 使用注册表默认值，button 自动带 none 动作', () => {
    for (const type of Object.keys(COMPONENT_DEFS) as ComponentType[]) {
      const created = createComponent(type)
      const def = COMPONENT_DEFS[type]
      expect(created.type).toBe(type)
      expect(created.title).toBe(def.defaultTitle)
      expect(created.visible).toBe(true)
      expect(created.dataSource).toBe(def.dataSource)
      expect(created.props).toEqual(def.defaultProps)
      if (def.supportsAction) expect(created.action).toEqual({ type: 'none' })
      else expect(created.action).toBeUndefined()
    }
  })

  it('cloneConfig 深拷贝：修改副本不影响原配置', () => {
    const config = validConfig()
    const copy = cloneConfig(config)
    copy.components[0].title = '被修改'
    copy.theme.primaryColor = '#000000'
    expect(config.components[0].title).not.toBe('被修改')
    expect(config.theme.primaryColor).toBe(THEME.primaryColor)
  })

  it('findComponent 按 id 查找', () => {
    const config = validConfig()
    expect(findComponent(config, 'cmp_button_1')?.type).toBe('button')
    expect(findComponent(config, 'not-exist')).toBeUndefined()
  })

  it('常量与 PAGE_SCHEMA 一致', () => {
    expect(PAGE_SCHEMA_VERSION).toBe(2)
    expect(PAGE_CONFIG_MAX_BYTES).toBe(204800)
    expect(PAGE_TITLE_MAX_LENGTH).toBe(60)
    expect(COLOR_PATTERN.test('#5B8CFF')).toBe(true)
    expect(COLOR_PATTERN.test('#5b8cff')).toBe(true)
    expect(COLOR_PATTERN.test('5B8CFF')).toBe(false)
    expect(SLUG_PATTERN.test('stellar-drift')).toBe(true)
    expect(SLUG_PATTERN.test('Stellar')).toBe(false)
  })
})
