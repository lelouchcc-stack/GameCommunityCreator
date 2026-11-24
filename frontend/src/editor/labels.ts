/**
 * 编辑器用到的中文展示标签（数据源、页面类型/状态别名），集中在一处避免各面板各写一套。
 * 页面类型/状态的中文标签以 `@/types/site` 为准，这里只做 re-export 与补充。
 */
import type { DataSourceId } from '@/types/page'

export { PAGE_TYPE_LABELS, PAGE_STATUS_LABELS, THEME_MODE_LABELS } from '@/types/site'

/** 数据源中文名 + 对应接口（只读信息展示用） */
export const DATA_SOURCE_LABELS: Record<DataSourceId, string> = {
  game_info: '游戏信息',
  announcements: '公告列表',
  changelogs: '更新日志',
  docs: '公开文档',
  team: '制作组成员',
  links: '友情链接',
  issues_progress: '问题处理进展（已解决且公开）',
}

export const DATA_SOURCE_APIS: Record<DataSourceId, string> = {
  game_info: 'GET /api/projects/{id}/game-info',
  announcements: 'GET /api/projects/{id}/announcements',
  changelogs: 'GET /api/projects/{id}/changelogs',
  docs: 'GET /api/projects/{id}/docs',
  team: 'GET /api/projects/{id}/team-members',
  links: 'GET /api/projects/{id}/links',
  issues_progress: 'GET /api/public/communities/{slug}/issues/progress',
}

export function dataSourceLabel(source: DataSourceId | null): string {
  if (source === null) return '无（静态组件，不请求接口）'
  return DATA_SOURCE_LABELS[source] ?? source
}

export function dataSourceApi(source: DataSourceId | null): string {
  if (source === null) return '不发起请求'
  return DATA_SOURCE_APIS[source] ?? ''
}
