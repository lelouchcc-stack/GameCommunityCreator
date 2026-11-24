/**
 * 运行时数据加载器（契约 docs/AGENT_CONTRACT.md §3.6 / docs/PAGE_SCHEMA.md §5）。
 *
 * - 草稿预览 / 编辑器画布：createDraftLoader(projectId) → 管理端接口（真实数据，不要求全部已发布）
 * - 公开页：createPublicLoader(projectSlug) → 公开只读接口（只读已发布快照）
 *
 * 两个 loader 都只负责「发请求 + 取 Paged.items」，失败原样抛出，
 * 由 CommunityRuntime 展示错误态并提供单源重试。
 */
import {
  getGameInfo,
  listAnnouncements,
  listChangelogs,
  listDocs,
  listIssues,
  listLinks,
  listTeamMembers,
} from '@/api/content'
import {
  getPublicGameInfo,
  getPublicIssueProgress,
  listPublicAnnouncements,
  listPublicChangelogs,
  listPublicDocs,
  listPublicLinks,
  listPublicTeam,
} from '@/api/public'
import type { Announcement, GameInfo, Paged } from '@/types/api'
import type {
  ChangelogView,
  DocView,
  FriendLinkView,
  IssueProgressItem,
  TeamMemberView,
} from '@/types/content'
import type { RuntimeDataLoader } from '@/types/runtime'

/** 契约 pageSize 允许 1–100；组件的 limit 白名单是 1–10，这里再兜一层 */
export function normalizePageSize(limit: number): number {
  const numeric = Number(limit)
  if (!Number.isFinite(numeric)) return 10
  return Math.min(100, Math.max(1, Math.trunc(numeric)))
}

function extractItems<T>(paged: Paged<T> | null | undefined): T[] {
  if (!paged || !Array.isArray(paged.items)) return []
  return paged.items
}

function normalizeCategory(category: string): string {
  return typeof category === 'string' ? category.trim() : ''
}

/**
 * 草稿预览 loader：读管理端真实数据。
 * 预览展示的口径与公开端一致 —— 更新日志/文档只取 PUBLISHED，友情链接只取启用项，
 * 避免「预览里能看到、发布后消失」的错觉。
 */
export function createDraftLoader(projectId: number): RuntimeDataLoader {
  return {
    loadGameInfo(): Promise<GameInfo> {
      return getGameInfo(projectId)
    },
    async loadAnnouncements(limit: number): Promise<Announcement[]> {
      const paged = await listAnnouncements(projectId, {
        pageNum: 1,
        pageSize: normalizePageSize(limit),
      })
      return extractItems(paged)
    },
    async loadChangelogs(limit: number): Promise<ChangelogView[]> {
      const paged = await listChangelogs(projectId, {
        pageNum: 1,
        pageSize: normalizePageSize(limit),
        status: 'PUBLISHED',
      })
      return extractItems(paged).filter((item) => item.status === 'PUBLISHED')
    },
    async loadDocs(limit: number, category: string): Promise<DocView[]> {
      const categoryFilter = normalizeCategory(category)
      // 管理端列表接口不支持 category 过滤：带分类时多取一些再本地过滤，保证条数够用
      const pageSize = normalizePageSize(categoryFilter ? 100 : limit)
      const paged = await listDocs(projectId, {
        pageNum: 1,
        pageSize,
        status: 'PUBLISHED',
      })
      const items = extractItems(paged).filter((item) => item.status === 'PUBLISHED')
      const filtered = categoryFilter
        ? items.filter((item) => item.category === categoryFilter)
        : items
      return filtered.slice(0, normalizePageSize(limit))
    },
    async loadTeam(): Promise<TeamMemberView[]> {
      const items = await listTeamMembers(projectId)
      return Array.isArray(items) ? items : []
    },
    async loadLinks(): Promise<FriendLinkView[]> {
      const items = await listLinks(projectId)
      const list = Array.isArray(items) ? items : []
      return list.filter((item) => item.enabled !== false)
    },
    /**
     * 预览口径与公开端一致：只展示「已解决 + 允许公开」的问题；
     * 管理端列表接口按 status 过滤，isPublic 由前端再过滤（多取一些避免过滤后不够）。
     */
    async loadIssueProgress(limit: number): Promise<IssueProgressItem[]> {
      const pageSize = normalizePageSize(limit * 4)
      const paged = await listIssues(projectId, {
        pageNum: 1,
        pageSize,
        status: 'RESOLVED',
      })
      return extractItems(paged)
        .filter((item) => item.isPublic === true)
        .slice(0, normalizePageSize(limit))
        .map((item) => ({
          id: item.id,
          title: item.title,
          category: item.category,
          description: item.description,
          resolution: item.resolution ?? '',
          updatedAt: item.updatedAt ?? item.createdAt,
        }))
    },
  }
}

/** 公开页 loader：读已发布快照对应的公开只读接口 */
export function createPublicLoader(projectSlug: string): RuntimeDataLoader {
  const slug = String(projectSlug ?? '')
  return {
    loadGameInfo(): Promise<GameInfo> {
      return getPublicGameInfo(slug)
    },
    async loadAnnouncements(limit: number): Promise<Announcement[]> {
      const paged = await listPublicAnnouncements(slug, {
        pageNum: 1,
        pageSize: normalizePageSize(limit),
      })
      return extractItems(paged)
    },
    async loadChangelogs(limit: number): Promise<ChangelogView[]> {
      const paged = await listPublicChangelogs(slug, {
        pageNum: 1,
        pageSize: normalizePageSize(limit),
      })
      return extractItems(paged)
    },
    async loadDocs(limit: number, category: string): Promise<DocView[]> {
      const categoryFilter = normalizeCategory(category)
      const paged = await listPublicDocs(slug, {
        pageNum: 1,
        pageSize: normalizePageSize(limit),
        category: categoryFilter || undefined,
      })
      return extractItems(paged)
    },
    async loadTeam(): Promise<TeamMemberView[]> {
      const items = await listPublicTeam(slug)
      return Array.isArray(items) ? items : []
    },
    async loadLinks(): Promise<FriendLinkView[]> {
      const items = await listPublicLinks(slug)
      return Array.isArray(items) ? items : []
    },
    async loadIssueProgress(limit: number): Promise<IssueProgressItem[]> {
      const items = await getPublicIssueProgress(slug)
      const list = Array.isArray(items) ? items : []
      return list.slice(0, normalizePageSize(limit))
    },
  }
}
