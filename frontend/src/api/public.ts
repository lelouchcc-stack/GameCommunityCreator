/**
 * 公开端 API（只读发布快照 + 模块内容 + 问题提交），契约见 docs/API.md v2 §7。
 */
import { http } from './client'
import type {
  Announcement,
  GameInfo,
  Paged,
  PageQuery,
  PublicCommunitySummary,
} from '@/types/api'
import type {
  ChangelogView,
  DocView,
  FriendLinkView,
  IssueProgressItem,
  IssueReportInput,
  IssueSubmitResult,
  TeamMemberView,
} from '@/types/content'
import type { PublicSiteDetail } from '@/types/site'

const communityPath = (slug: string) => `/public/communities/${encodeURIComponent(slug)}`

export function listPublicCommunities(): Promise<PublicCommunitySummary[]> {
  return http.get<PublicCommunitySummary[]>('/public/communities')
}

/** 公开站点上下文（首页）：project + home page + pages + navigation + settings */
export function getPublicCommunity(slug: string): Promise<PublicSiteDetail> {
  return http.get<PublicSiteDetail>(communityPath(slug))
}

/** 公开站点上下文（指定页面）：pageSlug 为页面的 slug */
export function getPublicPage(slug: string, pageSlug: string): Promise<PublicSiteDetail> {
  return http.get<PublicSiteDetail>(`${communityPath(slug)}/pages/${encodeURIComponent(pageSlug)}`)
}

export function getPublicGameInfo(slug: string): Promise<GameInfo> {
  return http.get<GameInfo>(`${communityPath(slug)}/game-info`)
}

export function listPublicAnnouncements(
  slug: string,
  params: PageQuery = {},
): Promise<Paged<Announcement>> {
  return http.get<Paged<Announcement>>(`${communityPath(slug)}/announcements`, {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  })
}

/* ------------------------------ 更新日志 ------------------------------ */

export function listPublicChangelogs(
  slug: string,
  params: PageQuery = {},
): Promise<Paged<ChangelogView>> {
  return http.get<Paged<ChangelogView>>(`${communityPath(slug)}/changelogs`, {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  })
}

export function getPublicChangelog(slug: string, entryId: number): Promise<ChangelogView> {
  return http.get<ChangelogView>(`${communityPath(slug)}/changelogs/${entryId}`)
}

/* ------------------------------ 公开文档 ------------------------------ */

export function listPublicDocs(
  slug: string,
  params: PageQuery & { category?: string } = {},
): Promise<Paged<DocView>> {
  return http.get<Paged<DocView>>(`${communityPath(slug)}/docs`, {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    category: params.category,
  })
}

export function getPublicDoc(slug: string, docSlug: string): Promise<DocView> {
  return http.get<DocView>(`${communityPath(slug)}/docs/${encodeURIComponent(docSlug)}`)
}

/* ------------------------------ 制作组 / 友情链接 ------------------------------ */

export function listPublicTeam(slug: string): Promise<TeamMemberView[]> {
  return http.get<TeamMemberView[]>(`${communityPath(slug)}/team`)
}

export function listPublicLinks(slug: string): Promise<FriendLinkView[]> {
  return http.get<FriendLinkView[]>(`${communityPath(slug)}/links`)
}

/** 公开「问题处理进展」：仅已解决且允许公开的问题（匿名可读） */
export function getPublicIssueProgress(slug: string): Promise<IssueProgressItem[]> {
  return http.get<IssueProgressItem[]>(`${communityPath(slug)}/issues/progress`)
}

/** 公开公告详情（首页公告卡片点击进入） */
export function getPublicAnnouncement(slug: string, announcementId: number): Promise<Announcement> {
  return http.get<Announcement>(`${communityPath(slug)}/news/${announcementId}`)
}

/* ------------------------------ 问题提交 ------------------------------ */

export function submitIssue(slug: string, input: IssueReportInput): Promise<IssueSubmitResult> {
  return http.post<IssueSubmitResult>(`${communityPath(slug)}/issues`, input)
}
