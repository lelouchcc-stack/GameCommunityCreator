/**
 * 页面模板目录（constants/pageTemplates.ts）一致性测试。
 *
 * 目录是「总览 · 推荐模板」与「模板中心」的唯一数据源；
 * 这里锁定它必须与后端 PageTypes.TEMPLATE_KEYS 的 7 个页面模板一一对应，
 * 避免再次出现「推荐了不存在的模板」这种前后端不一致。
 */
import { describe, expect, it } from 'vitest'
import {
  PAGE_TEMPLATES,
  RECOMMENDED_PAGE_TEMPLATES,
} from '@/constants/pageTemplates'

/** 后端 PageTypes.TEMPLATE_KEYS（镜像，顺序与目录一致） */
const BACKEND_TEMPLATE_KEYS = [
  'default-home',
  'default-changelog',
  'default-docs',
  'default-team',
  'default-issue-report',
  'default-links',
  'blank',
]

describe('页面模板目录', () => {
  it('key 与后端 PageTypes.TEMPLATE_KEYS 一一对应', () => {
    expect(PAGE_TEMPLATES.map((template) => template.key)).toEqual(BACKEND_TEMPLATE_KEYS)
  })

  it('key 唯一，且每项都声明了名称、说明与组件', () => {
    const keys = PAGE_TEMPLATES.map((template) => template.key)
    expect(new Set(keys).size).toBe(keys.length)
    for (const template of PAGE_TEMPLATES) {
      expect(template.name.trim()).not.toBe('')
      expect(template.mono.trim()).not.toBe('')
      expect(template.description.trim()).not.toBe('')
      expect(template.components.length).toBeGreaterThan(0)
    }
  })

  it('推荐模板是目录子集，且正好 4 个', () => {
    expect(RECOMMENDED_PAGE_TEMPLATES).toHaveLength(4)
    for (const template of RECOMMENDED_PAGE_TEMPLATES) {
      expect(PAGE_TEMPLATES).toContain(template)
      expect(template.recommended).toBe(true)
    }
  })
})
