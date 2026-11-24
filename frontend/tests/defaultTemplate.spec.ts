/**
 * 前后端一致性测试（v2）：直接读取后端默认页面模板资源（不复制内容），
 * 用前端注册表与校验规则校验它们 —— 防止「后端模板改了、前端渲染不认识」。
 *
 * 权威副本：backend/src/main/resources/templates/pages/{templateKey}.json（PAGE_SCHEMA §6，共 7 个模板）。
 */
import { readdirSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

import { COMPONENT_DEFS } from '@/components/registry'
import {
  COMPONENT_TYPES,
  DEFAULT_THEME,
  type ComponentType,
  type PageConfig,
} from '@/types/page'
import {
  COLOR_PATTERN,
  COMPONENT_ID_PATTERN,
  SLUG_PATTERN,
  validateDraftConfig,
} from '@/utils/pageConfig'

// npm run test 的 cwd 是 frontend/，资源在后端 resources 下
const TEMPLATE_DIR = resolve(process.cwd(), '../backend/src/main/resources/templates/pages')

/** PAGE_SCHEMA §6：7 个模板及其组件顺序（v3.2：default-home 改为 7 组件） */
const EXPECTED_TEMPLATES: Record<string, ComponentType[]> = {
  'default-home': [
    'hero',
    'about',
    'notice_list',
    'issue_progress',
    'changelog_list',
    'docs_list',
    'footer',
  ],
  'default-changelog': ['changelog_list', 'footer'],
  'default-docs': ['docs_list', 'footer'],
  'default-team': ['team_grid', 'footer'],
  'default-issue-report': ['issue_form', 'footer'],
  'default-links': ['links_grid', 'footer'],
  blank: ['footer'],
}

/** PAGE_SCHEMA §6：模板允许的唯一占位符 */
const ALLOWED_PLACEHOLDERS = new Set(['pageId', 'gameName', 'slug', 'pageTitle'])
const PLACEHOLDER_PATTERN = /\{\{\s*([A-Za-z0-9_]+)\s*\}\}/g

const EXAMPLE = {
  pageId: 'page_test_1',
  gameName: '星汐边境',
  slug: 'stellar-drift',
  pageTitle: '测试页面',
}

const TEMPLATE_KEYS = Object.keys(EXPECTED_TEMPLATES)

function readRaw(key: string): string {
  return readFileSync(resolve(TEMPLATE_DIR, `${key}.json`), 'utf8')
}

function loadTemplate(key: string): PageConfig {
  return JSON.parse(readRaw(key)) as PageConfig
}

/** 替换全部占位符，得到一份可通过前端校验的配置 */
function materialize(key: string): PageConfig {
  const replaced = readRaw(key)
    .replace(/\{\{pageId\}\}/g, EXAMPLE.pageId)
    .replace(/\{\{gameName\}\}/g, EXAMPLE.gameName)
    .replace(/\{\{slug\}\}/g, EXAMPLE.slug)
    .replace(/\{\{pageTitle\}\}/g, EXAMPLE.pageTitle)
  return JSON.parse(replaced) as PageConfig
}

function validationCtx(pageId = EXAMPLE.pageId) {
  return {
    projectSlug: EXAMPLE.slug,
    pageId,
    targetPages: [] as Array<{
      pageId: string
      name: string
      pageType: 'home'
      status: 'PUBLISHED'
    }>,
  }
}

// ---------------------------------------------------------------------------
// 模板文件集合
// ---------------------------------------------------------------------------

describe('默认模板文件集合（PAGE_SCHEMA §6）', () => {
  it('templates/pages 下恰好 7 个模板，与契约 templateKey 一一对应', () => {
    const files = readdirSync(TEMPLATE_DIR)
      .filter((file) => file.endsWith('.json'))
      .map((file) => file.replace(/\.json$/, ''))
      .sort()
    expect(files).toEqual([...TEMPLATE_KEYS].sort())
    expect(files).toHaveLength(7)
  })

  it.each(TEMPLATE_KEYS)('%s.json 是合法 JSON', (key) => {
    expect(() => loadTemplate(key)).not.toThrow()
  })

  it.each(TEMPLATE_KEYS)('%s 只包含约定的 4 个占位符', (key) => {
    const raw = readRaw(key)
    const placeholders = [...raw.matchAll(PLACEHOLDER_PATTERN)].map((match) => match[1])
    expect(placeholders.length).toBeGreaterThan(0)
    for (const name of placeholders) {
      expect(ALLOWED_PLACEHOLDERS.has(name), `出现未约定的占位符 {{${name}}}`).toBe(true)
    }
  })
})

// ---------------------------------------------------------------------------
// 顶层字段
// ---------------------------------------------------------------------------

describe('默认模板顶层字段（PAGE_SCHEMA §1）', () => {
  it.each(TEMPLATE_KEYS)('%s schemaVersion 固定为 2（新建只写 v2）', (key) => {
    expect(loadTemplate(key).schemaVersion).toBe(2)
  })

  it.each(TEMPLATE_KEYS)('%s pageId / pageTitle / projectSlug 均为占位符且替换后合法', (key) => {
    const config = loadTemplate(key)
    expect(config.pageId).toBe('{{pageId}}')
    expect(config.pageTitle).toBe('{{pageTitle}}')
    expect(config.projectSlug).toBe('{{slug}}')

    const materialized = materialize(key)
    expect(materialized.pageId).toBe(EXAMPLE.pageId)
    expect(materialized.pageTitle.trim().length).toBeGreaterThanOrEqual(1)
    expect(materialized.pageTitle.trim().length).toBeLessThanOrEqual(60)
    expect(SLUG_PATTERN.test(materialized.projectSlug)).toBe(true)
  })

  it.each(TEMPLATE_KEYS)('%s theme 与前端 DEFAULT_THEME 完全一致（星夜默认主题）', (key) => {
    expect(loadTemplate(key).theme).toEqual(DEFAULT_THEME)
  })

  it.each(TEMPLATE_KEYS)('%s theme 颜色/字体/圆角合法（PAGE_SCHEMA §3）', (key) => {
    const { theme } = loadTemplate(key)
    for (const colorKey of [
      'primaryColor',
      'accentColor',
      'backgroundColor',
      'surfaceColor',
      'textColor',
      'mutedTextColor',
    ] as const) {
      expect(COLOR_PATTERN.test(theme[colorKey]), `${colorKey}=${String(theme[colorKey])}`).toBe(true)
    }
    expect(['system', 'serif', 'mono']).toContain(theme.fontFamily)
    expect(Number.isInteger(theme.borderRadius)).toBe(true)
    expect(theme.borderRadius).toBeGreaterThanOrEqual(0)
    expect(theme.borderRadius).toBeLessThanOrEqual(24)
  })
})

// ---------------------------------------------------------------------------
// 组件结构
// ---------------------------------------------------------------------------

describe('默认模板组件结构（PAGE_SCHEMA §2 / §6）', () => {
  it.each(TEMPLATE_KEYS)('%s 组件顺序与 PAGE_SCHEMA §6 一致', (key) => {
    const config = loadTemplate(key)
    expect(config.components.map((component) => component.type)).toEqual(EXPECTED_TEMPLATES[key])
  })

  it.each(TEMPLATE_KEYS)('%s 组件 id 唯一且符合 ID 规则', (key) => {
    const config = loadTemplate(key)
    const ids = config.components.map((component) => component.id)
    expect(new Set(ids).size).toBe(ids.length)
    for (const id of ids) expect(COMPONENT_ID_PATTERN.test(id)).toBe(true)
  })

  it.each(TEMPLATE_KEYS)('%s 组件 type 都在前端注册表中', (key) => {
    for (const component of loadTemplate(key).components) {
      expect(COMPONENT_TYPES).toContain(component.type)
      expect(COMPONENT_DEFS[component.type]).toBeDefined()
    }
  })

  it.each(TEMPLATE_KEYS)('%s dataSource 与注册表固定数据源一致（footer/button/issue_form 为 null）', (key) => {
    for (const component of loadTemplate(key).components) {
      expect(component.dataSource).toBe(COMPONENT_DEFS[component.type].dataSource)
    }
  })

  it.each(TEMPLATE_KEYS)('%s visible/title 合法（1-60 字符）', (key) => {
    for (const component of loadTemplate(key).components) {
      expect(typeof component.visible).toBe('boolean')
      expect(component.title.trim().length).toBeGreaterThanOrEqual(1)
      expect(component.title.trim().length).toBeLessThanOrEqual(60)
    }
  })

  it.each(TEMPLATE_KEYS)('%s props 键全部在 propDefs 白名单内', (key) => {
    for (const component of loadTemplate(key).components) {
      const allowed = new Set(COMPONENT_DEFS[component.type].propDefs.map((prop) => prop.key))
      for (const propKey of Object.keys(component.props)) {
        expect(allowed.has(propKey), `${component.type}.${propKey} 不在白名单`).toBe(true)
      }
    }
  })

  it.each(TEMPLATE_KEYS)('%s props 值与 propDefs 声明一致（类型/范围/枚举）', (key) => {
    for (const component of loadTemplate(key).components) {
      for (const prop of COMPONENT_DEFS[component.type].propDefs) {
        const value = component.props[prop.key]
        if (value === undefined) continue
        if (prop.type === 'number') {
          expect(typeof value).toBe('number')
          expect(value as number).toBeGreaterThanOrEqual(prop.min)
          expect(value as number).toBeLessThanOrEqual(prop.max)
        } else if (prop.type === 'boolean') {
          expect(typeof value).toBe('boolean')
        } else if (prop.type === 'select') {
          expect(prop.options.some((option) => option.value === value)).toBe(true)
        } else {
          expect(typeof value).toBe('string')
          expect((value as string).length).toBeLessThanOrEqual(prop.maxLength)
        }
      }
    }
  })

  it.each(TEMPLATE_KEYS)('%s props 值均为标量（禁止嵌套对象/数组）', (key) => {
    for (const component of loadTemplate(key).components) {
      for (const value of Object.values(component.props)) {
        expect(['string', 'number', 'boolean']).toContain(typeof value)
      }
    }
  })

  it.each(TEMPLATE_KEYS)('%s 默认模板没有 button（动作按钮需用户自行添加）', (key) => {
    expect(loadTemplate(key).components.some((component) => component.type === 'button')).toBe(false)
  })
})

// ---------------------------------------------------------------------------
// v3.2 答辩前修复：默认首页 7 组件（问题处理进展 / 更新日志 / 公开文档）
// ---------------------------------------------------------------------------

describe('default-home（v3.2）：7 组件首页与新增模块', () => {
  const home = loadTemplate('default-home')
  const byType = (type: ComponentType) => home.components.find((component) => component.type === type)

  it('默认首页恰好 7 个组件，顺序为 hero → about → notice_list → issue_progress → changelog_list → docs_list → footer', () => {
    expect(home.components).toHaveLength(7)
    expect(home.components.map((component) => component.type)).toEqual([
      'hero',
      'about',
      'notice_list',
      'issue_progress',
      'changelog_list',
      'docs_list',
      'footer',
    ])
  })

  it('issue_progress 组件：dataSource=issues_progress，props 含 limit 与 emptyText', () => {
    const issueProgress = byType('issue_progress')
    expect(issueProgress).toBeDefined()
    expect(issueProgress?.dataSource).toBe('issues_progress')
    expect(Object.keys(issueProgress?.props ?? {}).sort()).toEqual(['emptyText', 'limit'])
    expect(typeof issueProgress?.props.limit).toBe('number')
    expect(issueProgress?.props.limit as number).toBeGreaterThanOrEqual(1)
    expect(issueProgress?.props.limit as number).toBeLessThanOrEqual(20)
    expect(typeof issueProgress?.props.emptyText).toBe('string')
    expect((issueProgress?.props.emptyText as string).length).toBeGreaterThan(0)
    expect((issueProgress?.props.emptyText as string).length).toBeLessThanOrEqual(60)
  })

  it('changelog_list 与 docs_list 组件存在且数据源正确（首页卡片跳转入口）', () => {
    expect(byType('changelog_list')?.dataSource).toBe('changelogs')
    expect(byType('docs_list')?.dataSource).toBe('docs')
  })

  it('默认首页只包含 4 类内容入口（公告/问题进展/更新日志/文档），不含已下线的玩家帖子列表', () => {
    const types = home.components.map((component) => component.type)
    expect(types).toEqual(
      expect.arrayContaining(['notice_list', 'issue_progress', 'changelog_list', 'docs_list']),
    )
    expect(types).not.toContain('post_list' as ComponentType)
  })
})

// ---------------------------------------------------------------------------
// 前端校验器接受
// ---------------------------------------------------------------------------

describe('默认模板可被前端校验器接受', () => {
  it.each(TEMPLATE_KEYS)('%s 占位符替换后 validateDraftConfig 通过', (key) => {
    const config = materialize(key)
    const result = validateDraftConfig(config, validationCtx())
    expect(result.errors).toEqual([])
    expect(result.valid).toBe(true)
  })

  it.each(TEMPLATE_KEYS)('%s pageId 与当前页面不一致时校验失败（防串页）', (key) => {
    const config = materialize(key)
    const result = validateDraftConfig(config, validationCtx('page_other_9'))
    expect(result.valid).toBe(false)
    expect(result.errors.join('\n')).toContain('pageId')
  })
})
