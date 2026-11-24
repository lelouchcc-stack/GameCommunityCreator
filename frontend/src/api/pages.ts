/** 页面管理 API（管理端），契约见 docs/API.md v2 §3 + §9（v1 home 别名） */
import { http } from './client'
import type { LegacyPageEnvelope, LegacyPublishResult } from '@/types/api'
import type { PageConfig } from '@/types/page'
import type {
  CreatePageInput,
  PublicationView,
  PublishPageResult,
  SitePageDraftEnvelope,
  SitePageView,
  SitePublishResult,
  UpdatePageInput,
} from '@/types/site'

export function listPages(projectId: number): Promise<SitePageView[]> {
  return http.get<SitePageView[]>(`/projects/${projectId}/pages`)
}

export function createPage(projectId: number, input: CreatePageInput): Promise<SitePageView> {
  return http.post<SitePageView>(`/projects/${projectId}/pages`, input)
}

export function getPage(projectId: number, pageId: string): Promise<SitePageView> {
  return http.get<SitePageView>(`/projects/${projectId}/pages/${pageId}`)
}

export function updatePage(
  projectId: number,
  pageId: string,
  input: UpdatePageInput,
): Promise<SitePageView> {
  return http.put<SitePageView>(`/projects/${projectId}/pages/${pageId}`, input)
}

export function deletePage(projectId: number, pageId: string): Promise<void> {
  return http.delete(`/projects/${projectId}/pages/${pageId}`)
}

export function copyPage(projectId: number, pageId: string): Promise<SitePageView> {
  return http.post<SitePageView>(`/projects/${projectId}/pages/${pageId}/copy`)
}

/* --------------------------- 单页草稿 / 发布 --------------------------- */

export function getPageDraft(projectId: number, pageId: string): Promise<SitePageDraftEnvelope> {
  return http.get<SitePageDraftEnvelope>(`/projects/${projectId}/pages/${pageId}/page`)
}

export function savePageDraft(
  projectId: number,
  pageId: string,
  config: PageConfig,
  expectedVersion: number,
): Promise<SitePageDraftEnvelope> {
  return http.put<SitePageDraftEnvelope>(`/projects/${projectId}/pages/${pageId}/page`, {
    config,
    expectedVersion,
  })
}

export function resetPageDraft(
  projectId: number,
  pageId: string,
): Promise<SitePageDraftEnvelope> {
  return http.post<SitePageDraftEnvelope>(`/projects/${projectId}/pages/${pageId}/page/reset-default`)
}

export function publishPage(projectId: number, pageId: string): Promise<PublishPageResult> {
  return http.post<PublishPageResult>(`/projects/${projectId}/pages/${pageId}/publish`)
}

export function listPagePublications(
  projectId: number,
  pageId: string,
): Promise<PublicationView[]> {
  return http.get<PublicationView[]>(`/projects/${projectId}/pages/${pageId}/publications`)
}

/** 站点发布：发布所有有更新的页面（独立路径，避免与 v1 home 发布别名冲突） */
export function publishSite(projectId: number): Promise<SitePublishResult> {
  return http.post<SitePublishResult>(`/projects/${projectId}/publish-site`)
}

/* ----------------------- v1 单页接口（home 页面别名） ----------------------- */

export function getDraftPage(projectId: number): Promise<LegacyPageEnvelope> {
  return http.get<LegacyPageEnvelope>(`/projects/${projectId}/page`)
}

export function saveDraftPage(
  projectId: number,
  config: PageConfig,
  expectedVersion: number,
): Promise<LegacyPageEnvelope> {
  return http.put<LegacyPageEnvelope>(`/projects/${projectId}/page`, { config, expectedVersion })
}

export function resetDraftPage(projectId: number): Promise<LegacyPageEnvelope> {
  return http.post<LegacyPageEnvelope>(`/projects/${projectId}/page/reset-default`)
}

export function publishProject(projectId: number): Promise<LegacyPublishResult> {
  return http.post<LegacyPublishResult>(`/projects/${projectId}/publish`)
}

export function listPublications(projectId: number): Promise<PublicationView[]> {
  return http.get<PublicationView[]>(`/projects/${projectId}/publications`)
}
