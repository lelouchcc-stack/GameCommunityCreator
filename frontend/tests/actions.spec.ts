/**
 * 按钮动作协议测试（v2，PAGE_SCHEMA §4 / PAGE_SCHEMA §4.3）。
 *
 * 覆盖两层：
 * 1) `utils/actions.ts` —— 纯校验 / 路径解析（编辑器与运行时共用）
 * 2) `runtime/actions.ts` —— 唯一动作执行入口 executeAction（跳转 / 新标签 / 提示）
 *
 * 约束：配置中禁止任何函数/表达式/脚本；本测试也同时验证「未知动作类型被拒绝」。
 */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { Router } from 'vue-router'

import type { PageAction } from '@/types/page'
import type { SiteRuntimeContext } from '@/types/site'
import {
  ACTION_URL_MAX_LENGTH,
  PAGE_ID_PATTERN,
  createDefaultAction,
  describeAction,
  isAllowedActionType,
  isValidHttpUrl,
  pathForPage,
  previewPathForPage,
  validateAction,
  type ActionTargetPage,
} from '@/utils/actions'
import { executeAction, type ActionContext } from '@/runtime/actions'
import { resetToasts, toastState } from '@/runtime/toast'

// ---------------------------------------------------------------------------
// fixtures
// ---------------------------------------------------------------------------

const TARGETS: ActionTargetPage[] = [
  { pageId: 'page_home_1', name: '首页', pageType: 'home', status: 'PUBLISHED', slug: 'home' },
  { pageId: 'page_changelog_1', name: '更新日志', pageType: 'changelog', status: 'PUBLISHED', slug: 'changelog' },
  { pageId: 'page_docs_1', name: '公开文档', pageType: 'docs', status: 'DRAFT', slug: 'docs' },
  { pageId: 'page_team_1', name: '制作组', pageType: 'team', status: 'DISABLED', slug: 'team' },
  { pageId: 'page_guide_1', name: '玩法指南', pageType: 'custom', status: 'PUBLISHED', slug: 'guide-2025' },
]

function makeRouter(): { router: Router; push: ReturnType<typeof vi.fn> } {
  const push = vi.fn(() => Promise.resolve())
  return { router: { push } as unknown as Router, push }
}

function makeSite(patch: Partial<SiteRuntimeContext> = {}): SiteRuntimeContext {
  return {
    projectId: 1,
    projectSlug: 'stellar-drift',
    pages: TARGETS.map((target) => ({
      pageId: target.pageId,
      name: target.name,
      slug: target.slug ?? '',
      pageType: target.pageType,
      status: target.status,
    })),
    themeMode: 'DARK',
    ...patch,
  }
}

function lastToastText(): string {
  const items = toastState().items
  return items.length > 0 ? items[items.length - 1].message : ''
}

beforeEach(() => {
  resetToasts()
})

afterEach(() => {
  vi.restoreAllMocks()
})

// ---------------------------------------------------------------------------
// isValidHttpUrl
// ---------------------------------------------------------------------------

describe('isValidHttpUrl：只接受 http/https 绝对 URL（PAGE_SCHEMA §4.1）', () => {
  it.each([
    'https://example.com',
    'https://example.com/path?query=1#hash',
    'http://example.com:8080/a',
    'https://sub.example.com/friend',
  ])('合法：%s', (url) => {
    expect(isValidHttpUrl(url)).toBe(true)
  })

  it.each([
    ['javascript:', 'javascript:alert(1)'],
    ['data:', 'data:text/html,<script>alert(1)</script>'],
    ['file:', 'file:///c:/windows/win.ini'],
    ['mailto:', 'mailto:dev@example.com'],
    ['协议相对地址', '//example.com/a'],
    ['相对地址', '/relative/path'],
    ['普通文本', 'example.com'],
    ['空字符串', ''],
    ['仅空白', '   '],
    ['含空白', 'https://exa mple.com'],
    ['缺少 host', 'http:'],
  ])('拒绝 %s：%s', (_label, url) => {
    expect(isValidHttpUrl(url)).toBe(false)
  })

  it('拒绝非字符串值', () => {
    expect(isValidHttpUrl(undefined)).toBe(false)
    expect(isValidHttpUrl(null)).toBe(false)
    expect(isValidHttpUrl(123)).toBe(false)
    expect(isValidHttpUrl({ url: 'https://example.com' })).toBe(false)
  })

  it(`URL 长度上限为 ${ACTION_URL_MAX_LENGTH}（超长拒绝）`, () => {
    const tooLong = `https://example.com/${'a'.repeat(ACTION_URL_MAX_LENGTH)}`
    expect(tooLong.length).toBeGreaterThan(ACTION_URL_MAX_LENGTH)
    expect(isValidHttpUrl(tooLong)).toBe(false)
  })
})

// ---------------------------------------------------------------------------
// 动作类型白名单
// ---------------------------------------------------------------------------

describe('动作类型白名单', () => {
  it('只允许 navigate / externalLink / none', () => {
    expect(isAllowedActionType('navigate')).toBe(true)
    expect(isAllowedActionType('externalLink')).toBe(true)
    expect(isAllowedActionType('none')).toBe(true)
    expect(isAllowedActionType('popup')).toBe(false)
    expect(isAllowedActionType('script')).toBe(false)
    expect(isAllowedActionType(undefined)).toBe(false)
    expect(isAllowedActionType(123)).toBe(false)
  })

  it('createDefaultAction 返回 none', () => {
    expect(createDefaultAction()).toEqual({ type: 'none' })
  })
})

// ---------------------------------------------------------------------------
// validateAction
// ---------------------------------------------------------------------------

describe('validateAction：navigate', () => {
  const ctx = { targetPages: TARGETS }

  it('目标页面已发布 → 无错误无警告', () => {
    const result = validateAction({ type: 'navigate', targetPageId: 'page_changelog_1' }, ctx)
    expect(result.errors).toEqual({})
    expect(result.warnings).toEqual({})
  })

  it('缺少 targetPageId → 报错', () => {
    const result = validateAction({ type: 'navigate' } as PageAction, ctx)
    expect(result.errors.targetPageId).toContain('请选择')
  })

  it('targetPageId 格式非法 → 报错', () => {
    const result = validateAction({ type: 'navigate', targetPageId: 'not-a-page-id' }, ctx)
    expect(result.errors.targetPageId).toContain('请选择')
  })

  it('目标页面不存在 → 报错', () => {
    const result = validateAction({ type: 'navigate', targetPageId: 'page_notexist_1' }, ctx)
    expect(result.errors.targetPageId).toContain('目标页面不存在')
  })

  it('目标页面 DISABLED → 报错', () => {
    const result = validateAction({ type: 'navigate', targetPageId: 'page_team_1' }, ctx)
    expect(result.errors.targetPageId).toContain('已停用')
  })

  it('目标页面 DRAFT → 允许保存但给出 warning（公开页会提示未发布）', () => {
    const result = validateAction({ type: 'navigate', targetPageId: 'page_docs_1' }, ctx)
    expect(result.errors).toEqual({})
    expect(result.warnings.targetPageId).toContain('尚未发布')
  })
})

describe('validateAction：externalLink', () => {
  const ctx = { targetPages: [] }

  it('合法 https URL → 无错误', () => {
    const result = validateAction(
      { type: 'externalLink', url: 'https://example.com', openInNewTab: true },
      ctx,
    )
    expect(result.errors).toEqual({})
  })

  it('非法 URL → 报错', () => {
    for (const url of ['javascript:alert(1)', 'data:text/html,x', 'file:///x', '/relative']) {
      const result = validateAction({ type: 'externalLink', url, openInNewTab: true }, ctx)
      expect(result.errors.url, url).toBeTruthy()
    }
  })

  it('openInNewTab 非布尔值 → 报错；未提供则视为默认 true 不报错', () => {
    const bad = validateAction(
      { type: 'externalLink', url: 'https://example.com', openInNewTab: 'yes' as unknown as boolean },
      ctx,
    )
    expect(bad.errors.openInNewTab).toBeTruthy()

    const ok = validateAction({ type: 'externalLink', url: 'https://example.com' } as PageAction, ctx)
    expect(ok.errors).toEqual({})
  })
})

describe('validateAction：none / 非法动作', () => {
  it('none → 无错误', () => {
    expect(validateAction({ type: 'none' }, { targetPages: TARGETS }).errors).toEqual({})
  })

  it('undefined / null / 未知类型 → 要求选择动作', () => {
    for (const action of [undefined, null, { type: 'popup' } as unknown as PageAction]) {
      const result = validateAction(action, { targetPages: TARGETS })
      expect(result.errors.type).toBeTruthy()
    }
  })
})

// ---------------------------------------------------------------------------
// describeAction / pathForPage / previewPathForPage
// ---------------------------------------------------------------------------

describe('describeAction', () => {
  it('各种动作的中文描述', () => {
    expect(describeAction(undefined)).toContain('未配置')
    expect(describeAction({ type: 'none' })).toContain('无动作')
    expect(describeAction({ type: 'navigate', targetPageId: 'page_changelog_1' }, TARGETS)).toContain(
      '更新日志',
    )
    expect(describeAction({ type: 'navigate', targetPageId: 'page_notexist_1' }, TARGETS)).toContain(
      '目标不存在',
    )
    expect(
      describeAction(
        { type: 'externalLink', url: 'https://example.com', openInNewTab: true },
        TARGETS,
      ),
    ).toContain('新标签')
  })
})

describe('pathForPage：7 种页面类型的公开路径（PAGE_SCHEMA §4.3）', () => {
  const slug = 'stellar-drift'

  it('home → /community/{slug}', () => {
    expect(pathForPage({ pageType: 'home', slug: 'home' }, slug)).toBe(`/community/${slug}`)
  })

  it('custom → /community/{slug}/p/{pageSlug}', () => {
    expect(pathForPage({ pageType: 'custom', slug: 'guide-2025' }, slug)).toBe(
      `/community/${slug}/p/guide-2025`,
    )
  })

  it.each([
    ['changelog', 'changelog'],
    ['docs', 'docs'],
    ['team', 'team'],
    ['issue-report', 'report'],
    ['links', 'links'],
  ] as const)('%s → /community/{slug}/%s', (pageType, path) => {
    expect(pathForPage({ pageType, slug: path }, slug)).toBe(`/community/${slug}/${path}`)
  })

  it('未知类型回退到站点首页路径', () => {
    expect(pathForPage({ pageType: 'unknown' as never, slug: 'x' }, slug)).toBe(`/community/${slug}`)
  })

  it('previewPathForPage → 管理端预览路由', () => {
    expect(previewPathForPage(3, 'page_home_3')).toBe('/admin/projects/3/pages/page_home_3/preview')
  })
})

// ---------------------------------------------------------------------------
// executeAction（runtime/actions.ts，唯一执行入口）
// ---------------------------------------------------------------------------

describe('executeAction：navigate（公开端 / 预览 / 编辑模式）', () => {
  it('公开模式：目标已发布 → router.push 到公开路径', () => {
    const { router, push } = makeRouter()
    const ctx: ActionContext = { router, site: makeSite(), mode: 'public' }
    const handled = executeAction({ type: 'navigate', targetPageId: 'page_changelog_1' }, ctx)
    expect(handled).toBe(true)
    expect(push).toHaveBeenCalledWith('/community/stellar-drift/changelog')
  })

  it('公开模式：目标不存在 → 提示且不跳转', () => {
    const { router, push } = makeRouter()
    const ctx: ActionContext = { router, site: makeSite(), mode: 'public' }
    expect(executeAction({ type: 'navigate', targetPageId: 'page_notexist_1' }, ctx)).toBe(false)
    expect(push).not.toHaveBeenCalled()
    expect(lastToastText()).toContain('目标页面不存在或尚未发布')
  })

  it('公开模式：注册表页面带 status 且非 PUBLISHED → 拒绝跳转（防草稿泄漏）', () => {
    const { router, push } = makeRouter()
    const site = makeSite({
      pages: [
        {
          pageId: 'page_docs_1',
          name: '公开文档',
          slug: 'docs',
          pageType: 'docs',
          status: 'DRAFT',
        },
      ],
    })
    const ctx: ActionContext = { router, site, mode: 'public' }
    expect(executeAction({ type: 'navigate', targetPageId: 'page_docs_1' }, ctx)).toBe(false)
    expect(push).not.toHaveBeenCalled()
  })

  it('编辑模式：不跳转并提示', () => {
    const { router, push } = makeRouter()
    const ctx: ActionContext = { router, site: makeSite(), mode: 'editor' }
    expect(executeAction({ type: 'navigate', targetPageId: 'page_changelog_1' }, ctx)).toBe(false)
    expect(push).not.toHaveBeenCalled()
    expect(lastToastText()).toContain('编辑模式')
  })

  it('预览模式：跳管理端预览路由', () => {
    const { router, push } = makeRouter()
    const ctx: ActionContext = { router, site: makeSite(), mode: 'preview' }
    expect(executeAction({ type: 'navigate', targetPageId: 'page_changelog_1' }, ctx)).toBe(true)
    expect(push).toHaveBeenCalledWith('/admin/projects/1/pages/page_changelog_1/preview')
  })

  it('targetPageId 格式非法 → 提示且不跳转', () => {
    const { router, push } = makeRouter()
    const ctx: ActionContext = { router, site: makeSite(), mode: 'public' }
    expect(executeAction({ type: 'navigate', targetPageId: 'bad-id' }, ctx)).toBe(false)
    expect(push).not.toHaveBeenCalled()
    expect(lastToastText()).toContain('缺少有效的目标页面')
  })
})

describe('executeAction：externalLink', () => {
  it('合法 URL → window.open(url, "_blank", "noopener,noreferrer")', () => {
    const open = vi.spyOn(window, 'open').mockImplementation(() => null)
    const { router } = makeRouter()
    const ctx: ActionContext = { router, site: makeSite(), mode: 'public' }
    const handled = executeAction(
      { type: 'externalLink', url: 'https://example.com/join', openInNewTab: true },
      ctx,
    )
    expect(handled).toBe(true)
    expect(open).toHaveBeenCalledWith('https://example.com/join', '_blank', 'noopener,noreferrer')
  })

  it('非法 URL → 不打开并提示', () => {
    const open = vi.spyOn(window, 'open').mockImplementation(() => null)
    const { router } = makeRouter()
    const ctx: ActionContext = { router, site: makeSite(), mode: 'public' }
    expect(executeAction({ type: 'externalLink', url: 'javascript:alert(1)', openInNewTab: true }, ctx)).toBe(
      false,
    )
    expect(open).not.toHaveBeenCalled()
    expect(lastToastText()).toContain('外部链接不合法')
  })
})

describe('executeAction：none / 未知类型 / 空动作', () => {
  it('none → 提示「该按钮尚未配置动作」，不跳转', () => {
    const { router, push } = makeRouter()
    const ctx: ActionContext = { router, site: makeSite(), mode: 'public' }
    expect(executeAction({ type: 'none' }, ctx)).toBe(false)
    expect(push).not.toHaveBeenCalled()
    expect(lastToastText()).toContain('尚未配置动作')
  })

  it('未知类型 → 拒绝执行并提示', () => {
    const { router, push } = makeRouter()
    const ctx: ActionContext = { router, site: makeSite(), mode: 'public' }
    expect(executeAction({ type: 'popup' } as unknown as PageAction, ctx)).toBe(false)
    expect(push).not.toHaveBeenCalled()
    expect(lastToastText()).toContain('暂不支持的动作类型')
  })

  it('空动作 → 提示未配置', () => {
    const { router } = makeRouter()
    const ctx: ActionContext = { router, site: makeSite(), mode: 'public' }
    expect(executeAction(undefined, ctx)).toBe(false)
    expect(lastToastText()).toContain('尚未配置动作')
  })
})

describe('页面稳定 ID 规则', () => {
  it('PAGE_ID_PATTERN 与 PAGE_SCHEMA §0 一致', () => {
    expect(PAGE_ID_PATTERN.test('page_home_1')).toBe(true)
    expect(PAGE_ID_PATTERN.test('page_abcd1234')).toBe(true)
    expect(PAGE_ID_PATTERN.test('page_1')).toBe(false) // 后缀至少 2 位
    expect(PAGE_ID_PATTERN.test('home_1')).toBe(false)
    expect(PAGE_ID_PATTERN.test('page_ABC')).toBe(false)
  })
})
