/**
 * v3.2 缺陷修复回归：组件注册表 / 运行时组件映射 / 数据源枚举。
 *
 * 背景：答辩前新增「问题处理进展」首页模块（issue_progress）与两个公开路由，
 * 默认首页模板改为 7 组件。本文件保证注册表、类型枚举与运行时映射三方一致：
 * - components/registry.ts（编辑器元数据）
 * - types/page.ts（COMPONENT_TYPES / DATA_SOURCES 契约）
 * - runtime/componentMap.ts（公开端渲染映射）
 */
import { describe, expect, it } from 'vitest'

import {
  COMPONENT_DEFS,
  COMPONENT_DEF_LIST,
  getComponentDef,
  isComponentType,
} from '@/components/registry'
import { getRuntimeComponent, RUNTIME_COMPONENT_MAP } from '@/runtime/componentMap'
import { COMPONENT_TYPES, DATA_SOURCES, type ComponentType } from '@/types/page'

describe('组件类型枚举（v3.2）', () => {
  it('COMPONENT_TYPES 共 11 种且包含 issue_progress', () => {
    expect(COMPONENT_TYPES).toHaveLength(11)
    expect(COMPONENT_TYPES).toContain('issue_progress')
    expect(COMPONENT_TYPES).toContain('changelog_list')
    expect(COMPONENT_TYPES).toContain('docs_list')
    expect(COMPONENT_TYPES).not.toContain('post_list')
  })

  it('COMPONENT_TYPES 与注册表键完全一致（顺序也一致）', () => {
    expect([...COMPONENT_TYPES]).toEqual(Object.keys(COMPONENT_DEFS))
    expect([...COMPONENT_TYPES]).toEqual(COMPONENT_DEF_LIST.map((definition) => definition.type))
  })

  it('DATA_SOURCES 共 7 种且包含 issues_progress（新数据源）', () => {
    expect(DATA_SOURCES).toHaveLength(7)
    expect(DATA_SOURCES).toContain('issues_progress')
    expect(DATA_SOURCES).toContain('changelogs')
    expect(DATA_SOURCES).toContain('docs')
    expect(DATA_SOURCES).toContain('team')
    expect(DATA_SOURCES).toContain('links')
    expect(DATA_SOURCES).not.toContain('posts')
  })

  it('注册表使用的数据源集合与 DATA_SOURCES 契约一致', () => {
    const used = new Set(
      Object.values(COMPONENT_DEFS)
        .map((definition) => definition.dataSource)
        .filter((dataSource): dataSource is NonNullable<typeof dataSource> => dataSource !== null),
    )
    expect([...used].sort()).toEqual([...DATA_SOURCES].sort())
  })
})

describe('issue_progress 组件定义（v3.2 新增）', () => {
  const definition = COMPONENT_DEFS.issue_progress

  it('注册表存在且 label / 默认标题 / dataSource 符合契约', () => {
    expect(getComponentDef('issue_progress')).toBe(definition)
    expect(definition.type).toBe('issue_progress')
    expect(definition.label).toBe('问题处理进展')
    expect(definition.defaultTitle).toBe('问题处理进展')
    expect(definition.dataSource).toBe('issues_progress')
    expect(definition.icon.length).toBeGreaterThan(0)
    expect(definition.description.length).toBeGreaterThan(0)
    expect(definition.supportsAction).toBe(false)
  })

  it('propDefs 恰好是 limit（数字 1–20）与 emptyText（文本）', () => {
    expect(definition.propDefs.map((prop) => prop.key)).toEqual(['limit', 'emptyText'])
    const limit = definition.propDefs.find((prop) => prop.key === 'limit')
    expect(limit?.type).toBe('number')
    if (limit?.type === 'number') {
      expect(limit.min).toBeGreaterThanOrEqual(1)
      expect(limit.max).toBeLessThanOrEqual(20)
    }
    const emptyText = definition.propDefs.find((prop) => prop.key === 'emptyText')
    expect(emptyText?.type).toBe('text')
    if (emptyText?.type === 'text') {
      expect(emptyText.maxLength).toBeGreaterThan(0)
    }
  })

  it('默认 props：limit=5 且 emptyText 非空（首页模板默认值来源）', () => {
    expect(definition.defaultProps.limit).toBe(5)
    expect(typeof definition.defaultProps.emptyText).toBe('string')
    expect(String(definition.defaultProps.emptyText).trim().length).toBeGreaterThan(0)
    // 默认值必须能在 propDefs 范围内通过校验
    const result = {
      limit: definition.defaultProps.limit as number,
      emptyText: definition.defaultProps.emptyText as string,
    }
    expect(result.limit).toBeGreaterThanOrEqual(1)
    expect(result.limit).toBeLessThanOrEqual(20)
  })
})

describe('注册表完整性', () => {
  it('11 种组件元数据齐全（label/defaultTitle/propDefs/defaultProps）', () => {
    for (const type of COMPONENT_TYPES) {
      const definition = COMPONENT_DEFS[type]
      expect(definition, `缺少 ${type} 定义`).toBeDefined()
      expect(definition.type).toBe(type)
      expect(definition.label.trim().length).toBeGreaterThan(0)
      expect(definition.defaultTitle.trim().length).toBeGreaterThan(0)
      expect(definition.propDefs.length).toBeGreaterThan(0)
      expect(typeof definition.defaultProps).toBe('object')
      expect(typeof definition.supportsAction).toBe('boolean')
      // 固定数据源：dataSource 只能为 DATA_SOURCES 中的值或 null
      if (definition.dataSource !== null) {
        expect(DATA_SOURCES).toContain(definition.dataSource)
      }
    }
    expect(isComponentType('issue_progress')).toBe(true)
    expect(isComponentType('not_a_component')).toBe(false)
  })
})

describe('运行时组件映射（runtime/componentMap）', () => {
  it('RUNTIME_COMPONENT_MAP 覆盖全部 11 种组件类型', () => {
    expect(Object.keys(RUNTIME_COMPONENT_MAP)).toHaveLength(COMPONENT_TYPES.length)
    for (const type of COMPONENT_TYPES) {
      expect(RUNTIME_COMPONENT_MAP[type as ComponentType], `缺少 ${type} 运行时组件`).toBeTruthy()
    }
  })

  it('getRuntimeComponent：issue_progress 可渲染，未知类型安全返回 null', () => {
    expect(getRuntimeComponent('issue_progress')).toBeTruthy()
    expect(getRuntimeComponent('changelog_list')).toBeTruthy()
    expect(getRuntimeComponent('docs_list')).toBeTruthy()
    expect(getRuntimeComponent('old_removed_component')).toBeNull()
  })
})
