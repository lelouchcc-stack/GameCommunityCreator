/**
 * v3.2 缺陷修复回归：首页内容卡片点击跳转（统一路由目标）与新公开路由。
 *
 * 背景：修复「首页公告/更新日志/文档/问题处理进展卡片点击不跳转」缺陷后，
 * 公开端统一走 runtime/contentTargets.ts 构造命名路由；本文件覆盖：
 * 1) contentTargets 纯函数产出的 RouteLocationRaw（name / params / hash）；
 * 2) 四个 navigate* 跳转函数确实把目标交给 router.push；
 * 3) router 路由表中 page-progress / page-news-detail 的路由名、路径与参数解析。
 */
import { describe, expect, it, vi } from 'vitest'
import type { Router } from 'vue-router'

import {
  announcementDetailTarget,
  changelogDetailTarget,
  changelogListTarget,
  docDetailTarget,
  docsListTarget,
  issueProgressTarget,
  navigateToAnnouncement,
  navigateToChangelog,
  navigateToDoc,
  navigateToIssueProgress,
} from '@/runtime/contentTargets'
import type { SiteRuntimeContext } from '@/types/site'

/** contentTargets 只读 site.projectSlug，其余站点字段与跳转无关 */
const SITE = { projectSlug: 'stellar-drift' } as unknown as SiteRuntimeContext

/** 最小 router 替身：只需要 push（navigate* 内部会 .catch 忽略导航异常） */
function fakeRouter() {
  const push = vi.fn(() => Promise.resolve())
  return { push } as unknown as Router & { push: ReturnType<typeof vi.fn> }
}

describe('contentTargets：公告（首页公告卡片）', () => {
  it('announcementDetailTarget → page-news-detail + slug + announcementId', () => {
    expect(announcementDetailTarget(SITE, 42)).toEqual({
      name: 'page-news-detail',
      params: { slug: 'stellar-drift', announcementId: 42 },
    })
  })

  it('公告 id 兼容字符串形式', () => {
    expect(announcementDetailTarget(SITE, '42')).toEqual({
      name: 'page-news-detail',
      params: { slug: 'stellar-drift', announcementId: '42' },
    })
  })

  it('navigateToAnnouncement 调用 router.push 且携带正确目标', () => {
    const router = fakeRouter()
    navigateToAnnouncement(router, SITE, 7)
    expect(router.push).toHaveBeenCalledTimes(1)
    expect(router.push).toHaveBeenCalledWith({
      name: 'page-news-detail',
      params: { slug: 'stellar-drift', announcementId: 7 },
    })
  })
})

describe('contentTargets：更新日志（首页更新日志卡片 / 查看全部）', () => {
  it('changelogDetailTarget → community-changelog-detail', () => {
    expect(changelogDetailTarget(SITE, 3)).toEqual({
      name: 'community-changelog-detail',
      params: { slug: 'stellar-drift', entryId: 3 },
    })
  })

  it('changelogListTarget → community-changelog（列表页）', () => {
    expect(changelogListTarget(SITE)).toEqual({
      name: 'community-changelog',
      params: { slug: 'stellar-drift' },
    })
  })

  it('navigateToChangelog 调用 router.push', () => {
    const router = fakeRouter()
    navigateToChangelog(router, SITE, 3)
    expect(router.push).toHaveBeenCalledTimes(1)
  })
})

describe('contentTargets：公开文档（首页文档卡片 / 查看全部）', () => {
  it('docDetailTarget → community-doc-detail + docSlug', () => {
    expect(docDetailTarget(SITE, 'getting-started')).toEqual({
      name: 'community-doc-detail',
      params: { slug: 'stellar-drift', docSlug: 'getting-started' },
    })
  })

  it('docsListTarget → community-docs（列表页）', () => {
    expect(docsListTarget(SITE)).toEqual({
      name: 'community-docs',
      params: { slug: 'stellar-drift' },
    })
  })

  it('navigateToDoc 调用 router.push', () => {
    const router = fakeRouter()
    navigateToDoc(router, SITE, 'getting-started')
    expect(router.push).toHaveBeenCalledTimes(1)
  })
})

describe('contentTargets：问题处理进展（v3.2 新增）', () => {
  it('issueProgressTarget 无 issueId → 只到 /progress 列表', () => {
    const target = issueProgressTarget(SITE) as {
      name: string
      params: Record<string, unknown>
      hash?: string
    }
    expect(target.name).toBe('page-progress')
    expect(target.params).toEqual({ slug: 'stellar-drift' })
    expect(target.hash).toBeUndefined()
  })

  it('issueProgressTarget 带 issueId → 定位到 #issue-{id} 锚点', () => {
    const target = issueProgressTarget(SITE, 12) as {
      name: string
      params: Record<string, unknown>
      hash?: string
    }
    expect(target.name).toBe('page-progress')
    expect(target.hash).toBe('#issue-12')
  })

  it('navigateToIssueProgress 调用 router.push（携带锚点）', () => {
    const router = fakeRouter()
    navigateToIssueProgress(router, SITE, 12)
    expect(router.push).toHaveBeenCalledTimes(1)
    expect(router.push).toHaveBeenCalledWith({
      name: 'page-progress',
      params: { slug: 'stellar-drift' },
      hash: '#issue-12',
    })
  })

  it('site.projectSlug 缺失时 slug 为空串（不抛异常，由路由兜底 404）', () => {
    const target = announcementDetailTarget({} as SiteRuntimeContext, 1) as {
      params: Record<string, unknown>
    }
    expect(target.params.slug).toBe('')
  })
})

describe('路由表：v3.2 新增公开路由', () => {
  it('注册 page-progress（/community/:slug/progress）与 page-news-detail（/community/:slug/news/:announcementId）', async () => {
    const { default: router } = await import('@/router/index')
    const routes = router.getRoutes()
    const progress = routes.find((route) => route.name === 'page-progress')
    const news = routes.find((route) => route.name === 'page-news-detail')
    expect(progress?.path).toBe('/community/:slug/progress')
    expect(news?.path).toBe('/community/:slug/news/:announcementId')
  })

  it('router.resolve 能解析问题处理进展页与公告详情页（含动态参数）', async () => {
    const { default: router } = await import('@/router/index')

    const progress = router.resolve('/community/stellar-drift/progress')
    expect(progress.name).toBe('page-progress')
    expect(progress.params.slug).toBe('stellar-drift')

    const news = router.resolve('/community/stellar-drift/news/42')
    expect(news.name).toBe('page-news-detail')
    expect(news.params.slug).toBe('stellar-drift')
    expect(news.params.announcementId).toBe('42')
    expect(news.href).toBe('/community/stellar-drift/news/42')
  })
})
