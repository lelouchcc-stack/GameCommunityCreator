/**
 * 运行时组件读取配置属性 / 数据的纯函数工具。
 *
 * 页面配置 JSON 来自后端（可能被编辑器正在编辑中），这里对所有取值做防御性收窄：
 * 类型不对、越界、空字符串都回退到契约里的默认值，保证运行时永远不因坏配置崩溃。
 */
import type { Announcement, GameInfo } from '@/types/api'
import type {
  ChangelogView,
  DocView,
  FriendLinkView,
  IssueProgressItem,
  TeamMemberView,
} from '@/types/content'
import type { PageComponent } from '@/types/page'
import type { RuntimeComponentProps } from '@/types/runtime'

export type RuntimeData = RuntimeComponentProps['data']

/** 读取字符串属性（类型不对时回退） */
export function readString(component: PageComponent, key: string, fallback = ''): string {
  const value = component.props?.[key]
  return typeof value === 'string' ? value : fallback
}

/** 读取字符串属性并去掉首尾空白 */
export function readTrimmed(component: PageComponent, key: string, fallback = ''): string {
  const value = readString(component, key, fallback).trim()
  return value || fallback
}

/** 读取布尔属性 */
export function readBoolean(component: PageComponent, key: string, fallback = false): boolean {
  const value = component.props?.[key]
  return typeof value === 'boolean' ? value : fallback
}

/** 读取受限整数属性（越界时夹到 [min, max]，非法值用 fallback） */
export function readBoundedNumber(
  component: PageComponent,
  key: string,
  fallback: number,
  min: number,
  max: number,
): number {
  const raw = component.props?.[key]
  const numeric = typeof raw === 'number' ? raw : typeof raw === 'string' ? Number(raw) : Number.NaN
  const base = Number.isFinite(numeric) ? Math.trunc(numeric) : fallback
  return Math.min(max, Math.max(min, base))
}

/** 枚举型字符串属性（不在允许集合内回退） */
export function readEnum<T extends string>(
  component: PageComponent,
  key: string,
  allowed: readonly T[],
  fallback: T,
): T {
  const raw = component.props?.[key]
  return typeof raw === 'string' && (allowed as readonly string[]).includes(raw)
    ? (raw as T)
    : fallback
}

/** 列表摘要：折叠空白并截断，避免把整段正文塞进列表卡片 */
export function summarize(text: string | null | undefined, maxLength = 120): string {
  const normalized = String(text ?? '')
    .replace(/\s+/g, ' ')
    .trim()
  if (normalized.length <= maxLength) return normalized
  return `${normalized.slice(0, maxLength)}…`
}

/** 仅接受 http(s) 图片/链接地址 */
export function readHttpUrl(value: string | null | undefined): string {
  const raw = String(value ?? '').trim()
  return /^https?:\/\//i.test(raw) ? raw : ''
}

/* ------------------------------ 数据收窄 ------------------------------ */

/** game_info 数据收窄（ready 状态下的 GameInfo） */
export function asGameInfo(data: RuntimeData): GameInfo | null {
  if (!data || Array.isArray(data)) return null
  const info = data as GameInfo
  return typeof info.name === 'string' ? info : null
}

function asArray<T>(data: RuntimeData): T[] {
  return Array.isArray(data) ? (data as T[]) : []
}

/** announcements 数据收窄 */
export function asAnnouncementList(data: RuntimeData): Announcement[] {
  return asArray<Announcement>(data)
}

/** changelogs 数据收窄 */
export function asChangelogList(data: RuntimeData): ChangelogView[] {
  return asArray<ChangelogView>(data)
}

/** docs 数据收窄 */
export function asDocList(data: RuntimeData): DocView[] {
  return asArray<DocView>(data)
}

/** team 数据收窄 */
export function asTeamList(data: RuntimeData): TeamMemberView[] {
  return asArray<TeamMemberView>(data)
}

/** links 数据收窄 */
export function asLinkList(data: RuntimeData): FriendLinkView[] {
  return asArray<FriendLinkView>(data)
}

/** issues_progress 数据收窄（v3.2：已解决且允许公开的问题处理进展） */
export function asIssueProgressList(data: RuntimeData): IssueProgressItem[] {
  return asArray<IssueProgressItem>(data)
}
