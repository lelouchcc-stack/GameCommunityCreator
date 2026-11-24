/**
 * v3.2 缺陷修复回归：公开 API 与图片上传 API 的请求契约（mock fetch）。
 *
 * 背景：答辩前新增两个公开接口（问题处理进展 / 公告详情）与 Markdown 图片上传接口。
 * 本文件只断言「HTTP 方法 + 路径 + Authorization 头 + FormData + 错误转换」，
 * 不依赖运行中的后端：
 * - GET /api/public/communities/{slug}/issues/progress
 * - GET /api/public/communities/{slug}/news/{announcementId}
 * - POST /api/uploads/images（Authorization: Bearer + multipart FormData）
 */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// 上传接口需要 JWT 登录态；测试中固定返回 token，避免依赖 localStorage 与真实登录
vi.mock('@/api/token', () => ({
  getAuthToken: vi.fn(() => 'test-jwt-token'),
}))

import { getPublicAnnouncement, getPublicIssueProgress } from '@/api/public'
import { UploadError, uploadImage } from '@/api/uploads'
import type { IssueProgressItem } from '@/types/content'

const ISSUE_PROGRESS: IssueProgressItem[] = [
  {
    id: 1,
    title: '存档偶发丢失',
    category: 'Bug 反馈',
    description: '退出游戏后进度回退',
    resolution: '已在 v0.9.3 修复',
    updatedAt: '2026-10-20T10:00:00',
  },
]

function jsonResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as unknown as Response
}

function envelope<T>(data: T, code = 200, message = 'OK') {
  return { code, message, data }
}

let fetchMock: ReturnType<typeof vi.fn>

function stubFetch(impl: () => Promise<Response>) {
  fetchMock = vi.fn(impl)
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function calledUrl(): string {
  return (fetchMock.mock.calls[0] as [string])[0]
}

function calledInit(): RequestInit {
  return (fetchMock.mock.calls[0] as [string, RequestInit])[1]
}

beforeEach(() => {
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('公开 API：问题处理进展（v3.2 新增）', () => {
  it('getPublicIssueProgress → GET /api/public/communities/{slug}/issues/progress，返回数组', async () => {
    stubFetch(async () => jsonResponse(envelope(ISSUE_PROGRESS)))
    const items = await getPublicIssueProgress('stellar-drift')
    expect(items).toEqual(ISSUE_PROGRESS)
    expect(calledUrl()).toBe('/api/public/communities/stellar-drift/issues/progress')
    expect(calledInit().method).toBe('GET')
  })

  it('slug 做 URL 编码（防止特殊字符拼坏路径）', async () => {
    stubFetch(async () => jsonResponse(envelope([])))
    await getPublicIssueProgress('a b/c')
    expect(calledUrl()).toBe('/api/public/communities/a%20b%2Fc/issues/progress')
  })
})

describe('公开 API：公告详情（v3.2 首页公告卡片跳转）', () => {
  it('getPublicAnnouncement → GET /api/public/communities/{slug}/news/{id}', async () => {
    const announcement = {
      id: 42,
      title: '版本更新公告',
      bodyMarkdown: '正文',
      content: '正文',
      pinned: true,
      publishedAt: '2026-10-01T09:00:00',
    }
    stubFetch(async () => jsonResponse(envelope(announcement)))
    const result = await getPublicAnnouncement('stellar-drift', 42)
    expect(result).toEqual(announcement)
    expect(calledUrl()).toBe('/api/public/communities/stellar-drift/news/42')
    expect(calledInit().method).toBe('GET')
  })
})

describe('图片上传 API：uploadImage（v3.2 新增）', () => {
  const IMAGE = new File([new Uint8Array([1, 2, 3])], 'shot.png', { type: 'image/png' })

  it('成功：POST /api/uploads/images，带 Bearer 头与 multipart FormData，返回 url', async () => {
    const uploaded = {
      url: '/api/public/uploads/shot-a1b2.png',
      name: 'shot.png',
      size: 3,
      contentType: 'image/png',
    }
    stubFetch(async () => jsonResponse(envelope(uploaded)))

    const result = await uploadImage(IMAGE)

    expect(result).toEqual(uploaded)
    expect(calledUrl()).toBe('/api/uploads/images')
    const init = calledInit()
    expect(init.method).toBe('POST')
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer test-jwt-token')
    expect(init.body).toBeInstanceOf(FormData)
    const file = (init.body as FormData).get('file')
    expect(file).toBeInstanceOf(File)
    expect((file as File).name).toBe('shot.png')
  })

  it('非法图片类型：不发请求，抛中文 UploadError', async () => {
    const bmp = new File([new Uint8Array([1])], 'shot.bmp', { type: 'image/bmp' })
    const error = await uploadImage(bmp).catch((reason: unknown) => reason)
    expect(error).toBeInstanceOf(UploadError)
    expect((error as UploadError).message).toContain('仅支持 PNG / JPEG / WebP / GIF')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('超过 5MB：不发请求，抛「图片大小不能超过 5MB」', async () => {
    const big = { name: 'big.png', type: 'image/png', size: 5 * 1024 * 1024 + 1 } as unknown as File
    const error = await uploadImage(big).catch((reason: unknown) => reason)
    expect(error).toBeInstanceOf(UploadError)
    expect((error as UploadError).message).toBe('图片大小不能超过 5MB')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('服务端 400：透传 envelope.message 为中文 UploadError', async () => {
    stubFetch(async () =>
      jsonResponse({ code: 400, message: '图片存储未配置，请联系管理员', data: null }, 400),
    )
    const error = await uploadImage(IMAGE).catch((reason: unknown) => reason)
    expect(error).toBeInstanceOf(UploadError)
    expect((error as UploadError).message).toBe('图片存储未配置，请联系管理员')
  })

  it('网络失败：抛带中文提示的 UploadError（不泄漏底层异常）', async () => {
    stubFetch(async () => {
      throw new TypeError('Failed to fetch')
    })
    const error = await uploadImage(IMAGE).catch((reason: unknown) => reason)
    expect(error).toBeInstanceOf(UploadError)
    expect((error as UploadError).message).toContain('无法连接上传接口')
  })
})
