/**
 * 管理端内容 API：游戏信息、公告 + P1 内容模块（v2 新增）。
 * 契约见 docs/API.md v2 §4（游戏信息）、§5（公告）、§6（内容模块）。
 */
import { http } from './client'
import type {
  Announcement,
  GameInfo,
  Paged,
  PageQuery,
  UpdateGameInfoInput,
} from '@/types/api'
import type {
  ChangelogInput,
  ChangelogView,
  ContentStatus,
  DocInput,
  DocView,
  FriendLinkInput,
  FriendLinkView,
  IssueReportView,
  IssueStatus,
  TeamMemberInput,
  TeamMemberView,
} from '@/types/content'

/* ------------------------------ 游戏信息 ------------------------------ */

export function getGameInfo(projectId: number): Promise<GameInfo> {
  return http.get<GameInfo>(`/projects/${projectId}/game-info`)
}

export function updateGameInfo(projectId: number, input: UpdateGameInfoInput): Promise<GameInfo> {
  return http.put<GameInfo>(`/projects/${projectId}/game-info`, input)
}

/* ------------------------------ 公告 ------------------------------ */

export function listAnnouncements(
  projectId: number,
  params: PageQuery = {},
): Promise<Paged<Announcement>> {
  return http.get<Paged<Announcement>>(`/projects/${projectId}/announcements`, {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  })
}

export interface AnnouncementInput {
  title: string
  bodyMarkdown: string
  /** 兼容字段（可省略，后端与 bodyMarkdown 同步） */
  content?: string
  pinned?: boolean
}

export function createAnnouncement(
  projectId: number,
  input: AnnouncementInput,
): Promise<Announcement> {
  return http.post<Announcement>(`/projects/${projectId}/announcements`, input)
}

/** v3 新增：编辑已有公告（标题/正文/置顶） */
export function updateAnnouncement(
  announcementId: number,
  input: Partial<AnnouncementInput>,
): Promise<Announcement> {
  return http.put<Announcement>(`/announcements/${announcementId}`, input)
}

export function deleteAnnouncement(announcementId: number): Promise<void> {
  return http.delete(`/announcements/${announcementId}`)
}

/* ------------------------------ 更新日志 ------------------------------ */

export function listChangelogs(
  projectId: number,
  params: PageQuery & { status?: ContentStatus | '' } = {},
): Promise<Paged<ChangelogView>> {
  return http.get<Paged<ChangelogView>>(`/projects/${projectId}/changelogs`, {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    status: params.status,
  })
}

export function createChangelog(projectId: number, input: ChangelogInput): Promise<ChangelogView> {
  return http.post<ChangelogView>(`/projects/${projectId}/changelogs`, input)
}

export function updateChangelog(
  entryId: number,
  input: Partial<ChangelogInput>,
): Promise<ChangelogView> {
  return http.put<ChangelogView>(`/changelogs/${entryId}`, input)
}

export function deleteChangelog(entryId: number): Promise<void> {
  return http.delete(`/changelogs/${entryId}`)
}

/* ------------------------------ 公开文档 ------------------------------ */

export function listDocs(
  projectId: number,
  params: PageQuery & { status?: ContentStatus | '' } = {},
): Promise<Paged<DocView>> {
  return http.get<Paged<DocView>>(`/projects/${projectId}/docs`, {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    status: params.status,
  })
}

export function createDoc(projectId: number, input: DocInput): Promise<DocView> {
  return http.post<DocView>(`/projects/${projectId}/docs`, input)
}

export function updateDoc(docId: number, input: Partial<DocInput>): Promise<DocView> {
  return http.put<DocView>(`/docs/${docId}`, input)
}

export function deleteDoc(docId: number): Promise<void> {
  return http.delete(`/docs/${docId}`)
}

/* ------------------------------ 制作组成员 ------------------------------ */

export function listTeamMembers(projectId: number): Promise<TeamMemberView[]> {
  return http.get<TeamMemberView[]>(`/projects/${projectId}/team-members`)
}

export function createTeamMember(
  projectId: number,
  input: TeamMemberInput,
): Promise<TeamMemberView> {
  return http.post<TeamMemberView>(`/projects/${projectId}/team-members`, input)
}

export function updateTeamMember(
  memberId: number,
  input: Partial<TeamMemberInput>,
): Promise<TeamMemberView> {
  return http.put<TeamMemberView>(`/team-members/${memberId}`, input)
}

export function deleteTeamMember(memberId: number): Promise<void> {
  return http.delete(`/team-members/${memberId}`)
}

/* ------------------------------ 问题反馈 ------------------------------ */

export function listIssues(
  projectId: number,
  params: PageQuery & { status?: IssueStatus | '' } = {},
): Promise<Paged<IssueReportView>> {
  return http.get<Paged<IssueReportView>>(`/projects/${projectId}/issues`, {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    status: params.status,
  })
}

export interface UpdateIssueInput {
  status?: IssueStatus
  resolution?: string
  isPublic?: boolean
}

export function updateIssue(issueId: number, input: UpdateIssueInput): Promise<IssueReportView> {
  return http.patch<IssueReportView>(`/issues/${issueId}`, input)
}

export function deleteIssue(issueId: number): Promise<void> {
  return http.delete(`/issues/${issueId}`)
}

/* ------------------------------ 友情链接 ------------------------------ */

export function listLinks(projectId: number): Promise<FriendLinkView[]> {
  return http.get<FriendLinkView[]>(`/projects/${projectId}/links`)
}

export function createLink(projectId: number, input: FriendLinkInput): Promise<FriendLinkView> {
  return http.post<FriendLinkView>(`/projects/${projectId}/links`, input)
}

export function updateLink(
  linkId: number,
  input: Partial<FriendLinkInput>,
): Promise<FriendLinkView> {
  return http.put<FriendLinkView>(`/links/${linkId}`, input)
}

export function deleteLink(linkId: number): Promise<void> {
  return http.delete(`/links/${linkId}`)
}
