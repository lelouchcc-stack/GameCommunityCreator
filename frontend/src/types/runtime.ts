/**
 * 运行时数据加载契约（v2：新增更新日志/文档/团队/友情链接数据源）。
 *
 * 预览（草稿）与公开页（发布快照）使用不同 loader 实现，
 * 但 CommunityRuntime.vue 与全部运行时组件只依赖本接口 —— 便于测试注入假数据。
 */
import type { Announcement, GameInfo } from './api'
import type {
  ChangelogView,
  DocView,
  FriendLinkView,
  IssueProgressItem,
  TeamMemberView,
} from './content'
import type { PageComponent } from './page'
import type { SiteRuntimeContext } from './site'

export type ComponentStatus = 'loading' | 'ready' | 'empty' | 'error'

export type RuntimeData =
  | GameInfo
  | Announcement[]
  | ChangelogView[]
  | DocView[]
  | TeamMemberView[]
  | FriendLinkView[]
  | IssueProgressItem[]
  | null

export interface RuntimeDataLoader {
  loadGameInfo(): Promise<GameInfo>
  loadAnnouncements(limit: number): Promise<Announcement[]>
  loadChangelogs(limit: number): Promise<ChangelogView[]>
  loadDocs(limit: number, category: string): Promise<DocView[]>
  loadTeam(): Promise<TeamMemberView[]>
  loadLinks(): Promise<FriendLinkView[]>
  /** v3.2：公开「问题处理进展」（仅已解决且允许公开的问题） */
  loadIssueProgress(limit: number): Promise<IssueProgressItem[]>
}

/** 运行时组件统一 props（由 CommunityRuntime.vue 传入） */
export interface RuntimeComponentProps {
  component: PageComponent
  status: ComponentStatus
  data: RuntimeData
  errorMessage: string
  retry: () => void
  /** 站点上下文：按钮动作解析、内部跳转、主题 */
  site: SiteRuntimeContext
}
