/**
 * 公开端统一内容跳转（v3.2）。
 *
 * 首页内容卡片（公告 / 更新日志 / 公开文档 / 问题处理进展）与「查看全部」入口
 * 都通过这里构造 **命名路由** 目标并跳转，统一携带当前社区 slug（`site.projectSlug`），
 * 禁止在各组件里散落硬编码路径。
 */
import type { RouteLocationRaw, Router } from 'vue-router'
import type { SiteRuntimeContext } from '@/types/site'

function slugOf(site: SiteRuntimeContext): string {
  return site?.projectSlug ?? ''
}

/* ------------------------------ 路由目标 ------------------------------ */

/** 公告详情：/community/:slug/news/:announcementId */
export function announcementDetailTarget(
  site: SiteRuntimeContext,
  announcementId: number | string,
): RouteLocationRaw {
  return {
    name: 'page-news-detail',
    params: { slug: slugOf(site), announcementId },
  }
}

/** 更新日志详情：/community/:slug/changelog/:entryId */
export function changelogDetailTarget(
  site: SiteRuntimeContext,
  entryId: number | string,
): RouteLocationRaw {
  return {
    name: 'community-changelog-detail',
    params: { slug: slugOf(site), entryId },
  }
}

/** 更新日志列表：/community/:slug/changelog */
export function changelogListTarget(site: SiteRuntimeContext): RouteLocationRaw {
  return { name: 'community-changelog', params: { slug: slugOf(site) } }
}

/** 文档详情：/community/:slug/docs/:docSlug */
export function docDetailTarget(site: SiteRuntimeContext, docSlug: string): RouteLocationRaw {
  return {
    name: 'community-doc-detail',
    params: { slug: slugOf(site), docSlug },
  }
}

/** 公开文档列表：/community/:slug/docs */
export function docsListTarget(site: SiteRuntimeContext): RouteLocationRaw {
  return { name: 'community-docs', params: { slug: slugOf(site) } }
}

/**
 * 问题处理进展：/community/:slug/progress[#issue-{id}]
 * 带 issueId 时落到对应条目锚点（首页「问题处理进展」条目点击定位）。
 */
export function issueProgressTarget(
  site: SiteRuntimeContext,
  issueId?: number | string | null,
): RouteLocationRaw {
  const base: Record<string, unknown> = {
    name: 'page-progress',
    params: { slug: slugOf(site) },
  }
  if (issueId !== undefined && issueId !== null && issueId !== '') {
    base.hash = `#issue-${issueId}`
  }
  return base as RouteLocationRaw
}

/* ------------------------------ 跳转执行 ------------------------------ */

function push(router: Router | undefined, to: RouteLocationRaw): void {
  if (!router) return
  void router.push(to).catch(() => {
    /* 路由重复 / 导航被取消：不提示，避免误报 */
  })
}

export function navigateToAnnouncement(
  router: Router,
  site: SiteRuntimeContext,
  announcementId: number | string,
): void {
  push(router, announcementDetailTarget(site, announcementId))
}

export function navigateToChangelog(
  router: Router,
  site: SiteRuntimeContext,
  entryId: number | string,
): void {
  push(router, changelogDetailTarget(site, entryId))
}

export function navigateToDoc(
  router: Router,
  site: SiteRuntimeContext,
  docSlug: string,
): void {
  push(router, docDetailTarget(site, docSlug))
}

export function navigateToIssueProgress(
  router: Router,
  site: SiteRuntimeContext,
  issueId?: number | string | null,
): void {
  push(router, issueProgressTarget(site, issueId))
}
