/**
 * v3.2 缺陷修复回归：运行时 loader 的新数据源 issues_progress。
 *
 * 背景：默认首页新增「问题处理进展」区块，草稿预览与公开页都要能取到
 * 仅「已解决 + 允许公开」的问题。本文件通过 mock API 模块验证：
 * - 公开 loader：直接读公开接口并截断 limit；
 * - 草稿 loader：走管理端列表（status=RESOLVED）后前端再过滤 isPublic，
 *   并做字段映射（resolution / updatedAt 兜底）。
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/public', () => ({
  getPublicGameInfo: vi.fn(),
  listPublicAnnouncements: vi.fn(),
  listPublicChangelogs: vi.fn(),
  listPublicDocs: vi.fn(),
  listPublicTeam: vi.fn(),
  listPublicLinks: vi.fn(),
  getPublicIssueProgress: vi.fn(),
}))

vi.mock('@/api/content', () => ({
  getGameInfo: vi.fn(),
  listAnnouncements: vi.fn(),
  listChangelogs: vi.fn(),
  listDocs: vi.fn(),
  listTeamMembers: vi.fn(),
  listLinks: vi.fn(),
  listIssues: vi.fn(),
}))

import { listIssues } from '@/api/content'
import { getPublicIssueProgress } from '@/api/public'
import { createDraftLoader, createPublicLoader } from '@/runtime/loaders'
import type { IssueProgressItem } from '@/types/content'

const PROGRESS: IssueProgressItem[] = [
  {
    id: 1,
    title: '存档偶发丢失',
    category: 'Bug 反馈',
    description: '退出后进度回退',
    resolution: '已在 v0.9.3 修复',
    updatedAt: '2026-10-20T10:00:00',
  },
  {
    id: 2,
    title: '手柄震动无效',
    category: 'Bug 反馈',
    description: 'PS 手柄无震动',
    resolution: '已在 v0.9.3 修复',
    updatedAt: '2026-10-21T10:00:00',
  },
  {
    id: 3,
    title: '建议增加拍照模式',
    category: '体验建议',
    description: '希望有自由视角',
    resolution: '已排期',
    updatedAt: '2026-10-22T10:00:00',
  },
]

const mockedListIssues = vi.mocked(listIssues)
const mockedPublicProgress = vi.mocked(getPublicIssueProgress)

beforeEach(() => {
  vi.clearAllMocks()
})

describe('公开 loader：loadIssueProgress', () => {
  it('调用公开接口 getPublicIssueProgress(slug)，并按 limit 截断', async () => {
    mockedPublicProgress.mockResolvedValue(PROGRESS)
    const loader = createPublicLoader('stellar-drift')

    const result = await loader.loadIssueProgress(2)

    expect(mockedPublicProgress).toHaveBeenCalledTimes(1)
    expect(mockedPublicProgress).toHaveBeenCalledWith('stellar-drift')
    expect(result.map((item) => item.id)).toEqual([1, 2])
  })

  it('接口返回非数组时安全降级为空数组（不崩溃）', async () => {
    mockedPublicProgress.mockResolvedValue(undefined as unknown as IssueProgressItem[])
    const loader = createPublicLoader('stellar-drift')
    await expect(loader.loadIssueProgress(5)).resolves.toEqual([])
  })
})

describe('草稿 loader：loadIssueProgress（预览口径与公开端一致）', () => {
  it('只保留 isPublic=true 的已解决问题，并映射 resolution / updatedAt', async () => {
    mockedListIssues.mockResolvedValue({
      items: [
        {
          id: 1,
          title: '存档偶发丢失',
          category: 'Bug 反馈',
          description: '退出后进度回退',
          resolution: '已在 v0.9.3 修复',
          updatedAt: '2026-10-20T10:00:00',
          createdAt: '2026-10-19T10:00:00',
          isPublic: true,
        },
        {
          id: 2,
          title: '内部待确认问题',
          category: 'Bug 反馈',
          description: '不公开',
          resolution: null,
          updatedAt: null,
          createdAt: '2026-10-19T11:00:00',
          isPublic: false,
        },
        {
          id: 3,
          title: '建议增加拍照模式',
          category: '体验建议',
          description: '希望有自由视角',
          resolution: null,
          updatedAt: null,
          createdAt: '2026-10-19T12:00:00',
          isPublic: true,
        },
      ],
      total: 3,
      pageNum: 1,
      pageSize: 20,
    } as never)

    const loader = createDraftLoader(7)
    const result = await loader.loadIssueProgress(5)

    // 过滤掉未公开项
    expect(result.map((item) => item.id)).toEqual([1, 3])
    // 字段映射：resolution 为 null → ''；updatedAt 为 null → createdAt 兜底
    expect(result[0].resolution).toBe('已在 v0.9.3 修复')
    expect(result[1].resolution).toBe('')
    expect(result[1].updatedAt).toBe('2026-10-19T12:00:00')
    // 管理端查询参数：只查 RESOLVED，pageSize = limit * 4（多取避免过滤后不够）
    expect(mockedListIssues).toHaveBeenCalledWith(7, {
      pageNum: 1,
      pageSize: 20,
      status: 'RESOLVED',
    })
  })
})
