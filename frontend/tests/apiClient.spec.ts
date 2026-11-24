/**
 * 统一 API 客户端（src/api/client.ts）测试。
 *
 * 全部 mock global.fetch，不依赖运行中的后端：
 * - 成功响应解包 { code, message, data } 并返回 data
 * - 400 业务错误转换为 ApiError（message / fieldErrors / status 可读）
 * - 204 无响应体返回 undefined
 * - 网络失败 / 超时 转换为友好中文错误
 * - 请求方法、URL、JSON 请求体符合 API.md §0
 * - v4：已登录时自动携带 `Authorization: Bearer <token>`（token 由 api/token.ts 持久化）；
 *       非认证接口 401 触发全局登录失效回调（认证接口不触发）
 */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError, http, isApiError, setUnauthorizedHandler } from '@/api/client'
import { AUTH_TOKEN_STORAGE_KEY, clearAuthToken, setAuthToken } from '@/api/token'
import { installMemoryStorage } from './helpers/webStorage'

type FetchMock = ReturnType<typeof vi.fn>

function envelope(code: number, message: string, data: unknown, fieldErrors?: Record<string, string>) {
  return { code, message, data, ...(fieldErrors ? { fieldErrors } : {}) }
}

function jsonResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as unknown as Response
}

function noContentResponse(): Response {
  return {
    ok: true,
    status: 204,
    json: async () => {
      throw new Error('204 不应解析 JSON')
    },
  } as unknown as Response
}

function mockFetch(impl: (url: string, init?: RequestInit) => Promise<Response>): FetchMock {
  const fetchMock = vi.fn(impl)
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** 取出 ApiError（让断言失败信息更直白） */
async function captureError(task: Promise<unknown>): Promise<ApiError> {
  try {
    await task
  } catch (error) {
    expect(error).toBeInstanceOf(ApiError)
    return error as ApiError
  }
  throw new Error('期望请求抛出 ApiError，但请求成功了')
}

function headersOf(fetchMock: FetchMock, index = 0): Record<string, string> {
  const init = fetchMock.mock.calls[index][1] as RequestInit
  return (init.headers as Record<string, string> | undefined) ?? {}
}

beforeEach(() => {
  // Node 25 + jsdom 下全局 localStorage 可能不可用，统一替换为内存实现后再写 token
  installMemoryStorage()
  clearAuthToken()
  vi.stubGlobal('fetch', vi.fn())
  setUnauthorizedHandler(null)
})

afterEach(() => {
  clearAuthToken()
  setUnauthorizedHandler(null)
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('http 成功路径', () => {
  it('GET 解包 envelope 并返回 data，URL 带 /api 前缀', async () => {
    const fetchMock = mockFetch(async () =>
      jsonResponse(envelope(200, 'OK', [{ id: 1, name: '星汐边境官方社区' }])),
    )

    const data = await http.get<Array<{ id: number; name: string }>>('/projects')

    expect(data).toEqual([{ id: 1, name: '星汐边境官方社区' }])
    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('/api/projects')
    expect(init.method).toBe('GET')
    expect(init.body).toBeUndefined()
  })

  it('查询参数拼接为 query string，并跳过 undefined / null / 空串', async () => {
    const fetchMock = mockFetch(async () => jsonResponse(envelope(200, 'OK', { items: [] })))

    await http.get('/projects/1/announcements', {
      pageNum: 2,
      pageSize: 10,
      empty: '',
      missing: undefined,
      nullable: null,
    })

    const url = (fetchMock.mock.calls[0] as [string])[0]
    expect(url).toBe('/api/projects/1/announcements?pageNum=2&pageSize=10')
  })

  it('POST 发送 JSON 请求体并包含 Content-Type', async () => {
    const fetchMock = mockFetch(async () => jsonResponse(envelope(201, 'Created', { id: 9 })))

    const created = await http.post<{ id: number }>('/projects', { name: '社区', slug: 'demo-1' })

    expect(created).toEqual({ id: 9 })
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('/api/projects')
    expect(init.method).toBe('POST')
    expect(init.body).toBe(JSON.stringify({ name: '社区', slug: 'demo-1' }))
    expect(headersOf(fetchMock)['Content-Type']).toBe('application/json')
  })

  it('DELETE 遇到 204 返回 undefined（不尝试解析响应体）', async () => {
    mockFetch(async () => noContentResponse())

    const result = await http.delete('/projects/3')

    expect(result).toBeUndefined()
  })
})

describe('v4：JWT Bearer 与 401 登录失效', () => {
  it('已登录（有 token）时 GET 请求自动携带 Authorization: Bearer', async () => {
    setAuthToken('test-jwt-token')
    const fetchMock = mockFetch(async () => jsonResponse(envelope(200, 'OK', [])))

    await http.get('/projects')

    expect(headersOf(fetchMock).Authorization).toBe('Bearer test-jwt-token')
  })

  it('未登录（无 token）时不带 Authorization 头', async () => {
    const fetchMock = mockFetch(async () => jsonResponse(envelope(200, 'OK', [])))

    await http.get('/projects')

    expect(headersOf(fetchMock).Authorization).toBeUndefined()
  })

  it('非 GET 请求同样携带 Bearer（不再有 CSRF 头）', async () => {
    setAuthToken('post-token')
    const fetchMock = mockFetch(async () => jsonResponse(envelope(201, 'Created', { id: 1 })))

    await http.post('/projects', { name: 'x' })

    const headers = headersOf(fetchMock)
    expect(headers.Authorization).toBe('Bearer post-token')
    expect(headers['X-XSRF-TOKEN']).toBeUndefined()
  })

  it('token 持久化到 localStorage（key: gcc.auth.token），clearAuthToken 清除', () => {
    setAuthToken('persisted-jwt')
    expect(localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe('persisted-jwt')

    clearAuthToken()
    expect(localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBeNull()
  })

  it('非认证接口 401 触发全局登录失效回调', async () => {
    mockFetch(async () =>
      jsonResponse(envelope(401, '登录已过期或未登录，请重新登录', null), 401),
    )
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    const error = await captureError(http.get('/projects'))
    expect(error.status).toBe(401)
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('认证接口 401（登录失败）不触发全局登录失效回调', async () => {
    mockFetch(async () => jsonResponse(envelope(401, '用户名或密码错误', null), 401))
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    const error = await captureError(http.post('/auth/login', { username: 'x', password: 'y' }))
    expect(error.status).toBe(401)
    expect(handler).not.toHaveBeenCalled()
  })
})

describe('http 错误路径', () => {
  it('400 校验错误转换为 ApiError，保留 message 与 fieldErrors', async () => {
    mockFetch(async () =>
      jsonResponse(envelope(400, '参数校验失败', null, { slug: '项目标识已被占用' }), 400),
    )

    const error = await captureError(http.post('/projects', { slug: 'taken' }))

    expect(error.status).toBe(400)
    expect(error.message).toBe('参数校验失败')
    expect(error.fieldErrors).toEqual({ slug: '项目标识已被占用' })
    expect(isApiError(error)).toBe(true)
  })

  it('409 版本冲突转换为 ApiError，页面可直接展示中文提示', async () => {
    mockFetch(async () =>
      jsonResponse(envelope(409, '页面配置已被其他操作修改，请刷新后重试', null), 409),
    )

    const error = await captureError(http.put('/projects/1/page', { expectedVersion: 1 }))

    expect(error.status).toBe(409)
    expect(error.message).toContain('请刷新后重试')
  })

  it('HTTP 非 2xx 且 envelope code 也非成功时，优先使用 envelope.code/message', async () => {
    mockFetch(async () => jsonResponse(envelope(404, '项目不存在', null), 404))

    const error = await captureError(http.get('/projects/999'))

    expect(error.status).toBe(404)
    expect(error.message).toBe('项目不存在')
  })

  it('HTTP 200 但业务 code >= 400 时同样抛 ApiError（防后端异常响应被当成成功）', async () => {
    mockFetch(async () => jsonResponse(envelope(500, '服务器内部错误', null), 200))

    const error = await captureError(http.get('/health'))

    expect(error.status).toBe(500)
    expect(error.message).toBe('服务器内部错误')
  })

  it('非 JSON 错误响应返回带 HTTP 状态码的兜底中文消息', async () => {
    mockFetch(
      async () =>
        ({
          ok: false,
          status: 502,
          json: async () => {
            throw new SyntaxError('Unexpected token < in JSON')
          },
        }) as unknown as Response,
    )

    const error = await captureError(http.get('/health'))

    expect(error.status).toBe(502)
    expect(error.message).toBe('请求失败（HTTP 502）')
  })

  it('网络不可达时抛出友好中文错误（status=0）', async () => {
    mockFetch(async () => {
      throw new TypeError('Failed to fetch')
    })

    const error = await captureError(http.get('/projects'))

    expect(error.status).toBe(0)
    expect(error.message).toContain('无法连接后端服务')
  })

  it('请求超时（AbortError）时抛出中文超时提示', async () => {
    mockFetch(async () => {
      throw new DOMException('The operation was aborted.', 'AbortError')
    })

    const error = await captureError(http.get('/projects'))

    expect(error.status).toBe(0)
    expect(error.message).toContain('请求超时')
  })
})

describe('ApiError 类型', () => {
  it('fieldErrors 缺省为空对象，isApiError 只识别 ApiError', () => {
    const error = new ApiError(400, '参数错误')
    expect(error.fieldErrors).toEqual({})
    expect(error.name).toBe('ApiError')
    expect(isApiError(error)).toBe(true)
    expect(isApiError(new Error('普通错误'))).toBe(false)
  })
})
