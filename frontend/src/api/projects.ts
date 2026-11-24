/** 游戏站点（原社区项目）API（管理端），契约见 docs/API.md v2 §2 */
import { http } from './client'
import type {
  CreateProjectInput,
  ProjectView,
  UpdateProjectInput,
} from '@/types/api'

export function listProjects(): Promise<ProjectView[]> {
  return http.get<ProjectView[]>('/projects')
}

export function createProject(input: CreateProjectInput): Promise<ProjectView> {
  return http.post<ProjectView>('/projects', input)
}

export function getProject(projectId: number): Promise<ProjectView> {
  return http.get<ProjectView>(`/projects/${projectId}`)
}

export function updateProject(projectId: number, input: UpdateProjectInput): Promise<ProjectView> {
  return http.put<ProjectView>(`/projects/${projectId}`, input)
}

export function deleteProject(projectId: number): Promise<void> {
  return http.delete(`/projects/${projectId}`)
}

// v1 单页接口（home 页面别名）保留导出，兼容旧调用点；新代码请使用 api/pages.ts
export {
  getDraftPage,
  saveDraftPage,
  resetDraftPage,
  publishProject,
  listPublications,
} from './pages'
