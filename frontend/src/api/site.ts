/** 站点导航与设置 API（管理端），契约见 docs/API.md v2 §4–§5 */
import { http } from './client'
import type { NavigationItemInput, NavigationItemView, SiteSettings } from '@/types/site'

export function getNavigation(projectId: number): Promise<{ items: NavigationItemView[] }> {
  return http.get<{ items: NavigationItemView[] }>(`/projects/${projectId}/navigation`)
}

export function saveNavigation(
  projectId: number,
  items: NavigationItemInput[],
): Promise<{ items: NavigationItemView[] }> {
  return http.put<{ items: NavigationItemView[] }>(`/projects/${projectId}/navigation`, { items })
}

export function getSiteSettings(projectId: number): Promise<SiteSettings> {
  return http.get<SiteSettings>(`/projects/${projectId}/settings`)
}

export function saveSiteSettings(projectId: number, settings: SiteSettings): Promise<SiteSettings> {
  return http.put<SiteSettings>(`/projects/${projectId}/settings`, settings)
}
