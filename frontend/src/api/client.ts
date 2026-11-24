/**
 * 统一 API 客户端：解包 { code, message, data }，失败抛 ApiError。
 *
 * v4.1 Sa-Token + JWT 认证相关能力（Bearer Token）：
 * - 已登录时自动为请求携带 `Authorization: Bearer <token>`（token 由 `api/token.ts` 持久化）；
 * - 401（非认证接口）触发全局登录失效回调，由 auth store 清理登录态并跳转登录页；
 * - 认证接口（/auth/*）的 401 由调用方自行处理，不触发全局跳转。
 */
import type { ApiEnvelope } from '@/types/api'
import { getAuthToken } from './token'

const API_BASE = (import.meta.env.VITE_API_BASE as string | undefined) ?? '/api'
const REQUEST_TIMEOUT_MS = 15000

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors?: Record<string, string> | null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors ?? {}
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

/* ------------------------------ 登录失效回调 ----------------------------- */

let unauthorizedHandler: (() => void) | null = null

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler
}

/** 认证接口（登录失败/未登录探测）由调用方自行处理，不触发全局跳转 */
function isAuthEndpoint(path: string): boolean {
  return path.startsWith('/auth/')
}

/* --------------------------------- 请求 --------------------------------- */

function buildUrl(
  path: string,
  query?: Record<string, string | number | boolean | undefined | null>,
): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  if (!query) return `${API_BASE}${normalizedPath}`
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value))
    }
  }
  const queryString = search.toString()
  return queryString ? `${API_BASE}${normalizedPath}?${queryString}` : `${API_BASE}${normalizedPath}`
}

interface RequestOptions {
  body?: unknown
  query?: Record<string, string | number | boolean | undefined | null>
}

async function requestOnce<T>(
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE',
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)
  const headers: Record<string, string> = {}
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  const token = getAuthToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let response: Response
  try {
    response = await fetch(buildUrl(path, options.query), {
      method,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: controller.signal,
    })
  } catch (error) {
    window.clearTimeout(timeout)
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiError(0, '请求超时，请检查后端服务是否启动')
    }
    throw new ApiError(0, '无法连接后端服务，请确认后端已启动（默认 http://localhost:8080）')
  } finally {
    window.clearTimeout(timeout)
  }

  if (response.status === 204) {
    return undefined as T
  }

  let envelope: ApiEnvelope<T> | null = null
  try {
    envelope = (await response.json()) as ApiEnvelope<T>
  } catch {
    if (!response.ok) {
      throw new ApiError(response.status, `请求失败（HTTP ${response.status}）`)
    }
    throw new ApiError(response.status, '服务器返回了无法解析的数据')
  }

  if (response.status === 401 && !isAuthEndpoint(path)) {
    unauthorizedHandler?.()
  }

  if (!response.ok || envelope.code >= 400) {
    throw new ApiError(
      envelope.code || response.status,
      envelope.message || `请求失败（HTTP ${response.status}）`,
      envelope.fieldErrors,
    )
  }
  return envelope.data
}

export const http = {
  get<T>(
    path: string,
    query?: Record<string, string | number | boolean | undefined | null>,
  ): Promise<T> {
    return requestOnce<T>('GET', path, { query })
  },
  post<T>(path: string, body?: unknown): Promise<T> {
    return requestOnce<T>('POST', path, { body })
  },
  put<T>(path: string, body?: unknown): Promise<T> {
    return requestOnce<T>('PUT', path, { body })
  },
  patch<T>(path: string, body?: unknown): Promise<T> {
    return requestOnce<T>('PATCH', path, { body })
  },
  delete<T = void>(path: string): Promise<T> {
    return requestOnce<T>('DELETE', path)
  },
}
