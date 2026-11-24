/**
 * 开发者登录态 Pinia store（v4.2）测试。
 *
 * 覆盖：init 恢复登录态（有 token → /me 成功 / 401 清 token / 无 token 不请求）、
 * login/register 保存 token 并更新用户、logout 无论接口成败都清理本地 token/用户、
 * 401 全局回调 clearSession 清 token。
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/api/auth', () => ({
  getMe: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('@/api/token', () => ({
  getAuthToken: vi.fn(),
  setAuthToken: vi.fn(),
  clearAuthToken: vi.fn(),
}))

vi.mock('@/api/client', () => ({
  isApiError: (error: unknown) => Boolean(error && typeof error === 'object' && 'status' in error),
}))

import * as authApi from '@/api/auth'
import * as tokenApi from '@/api/token'
import { useAuthStore } from '@/stores/auth'

const mocked = {
  getMe: vi.mocked(authApi.getMe),
  login: vi.mocked(authApi.login),
  register: vi.mocked(authApi.register),
  logout: vi.mocked(authApi.logout),
  getAuthToken: vi.mocked(tokenApi.getAuthToken),
  setAuthToken: vi.mocked(tokenApi.setAuthToken),
  clearAuthToken: vi.mocked(tokenApi.clearAuthToken),
}

const USER = { id: 1, username: 'dev', displayName: '独立开发者' }
const AUTH_RESULT = { token: 'jwt-token-abc', tokenType: 'Bearer', expiresIn: 7200, user: USER }

beforeEach(() => {
  // 每个用例一个全新的 Pinia 实例，保证 store 状态互不影响
  setActivePinia(createPinia())
  vi.clearAllMocks()
  mocked.getAuthToken.mockReturnValue(null)
})

describe('init：恢复登录态', () => {
  it('有本地 token → GET /auth/me 恢复用户且 initialized=true', async () => {
    mocked.getAuthToken.mockReturnValue('stored-token')
    mocked.getMe.mockResolvedValue(USER)
    const store = useAuthStore()
    await store.init()
    expect(mocked.getMe).toHaveBeenCalledTimes(1)
    expect(store.user).toEqual(USER)
    expect(store.isAuthenticated).toBe(true)
    expect(store.initialized).toBe(true)
  })

  it('无本地 token → 不请求 /auth/me，直接未登录', async () => {
    const store = useAuthStore()
    await store.init()
    expect(mocked.getMe).not.toHaveBeenCalled()
    expect(store.user).toBeNull()
    expect(store.isAuthenticated).toBe(false)
    expect(store.initialized).toBe(true)
  })

  it('token 失效（401）→ 清 token、user=null，不抛异常', async () => {
    mocked.getAuthToken.mockReturnValue('expired-token')
    mocked.getMe.mockRejectedValue({ status: 401, message: '登录已过期' })
    const store = useAuthStore()
    await store.init()
    expect(mocked.clearAuthToken).toHaveBeenCalled()
    expect(store.user).toBeNull()
    expect(store.isAuthenticated).toBe(false)
    expect(store.initialized).toBe(true)
  })

  it('并发调用 init() 只请求一次 /auth/me（initPromise 去重）', async () => {
    mocked.getAuthToken.mockReturnValue('stored-token')
    mocked.getMe.mockResolvedValue(USER)
    const store = useAuthStore()
    await Promise.all([store.init(), store.init(), store.init()])
    expect(mocked.getMe).toHaveBeenCalledTimes(1)
  })
})

describe('login / register / logout', () => {
  it('login 成功保存 token 并写入 user', async () => {
    mocked.login.mockResolvedValue(AUTH_RESULT)
    const store = useAuthStore()
    const user = await store.login({ username: 'dev', password: 'x' })
    expect(user).toEqual(USER)
    expect(store.user).toEqual(USER)
    expect(store.isAuthenticated).toBe(true)
    expect(mocked.setAuthToken).toHaveBeenCalledWith('jwt-token-abc')
  })

  it('register 成功保存 token 并写入 user（注册即登录）', async () => {
    mocked.register.mockResolvedValue(AUTH_RESULT)
    const store = useAuthStore()
    const user = await store.register({
      username: 'dev',
      password: 'DevPass2026',
      displayName: '独立开发者',
    })
    expect(user).toEqual(USER)
    expect(store.user).toEqual(USER)
    expect(mocked.setAuthToken).toHaveBeenCalledWith('jwt-token-abc')
  })

  it('login 失败不保存 token', async () => {
    mocked.login.mockRejectedValue({ status: 401, message: '用户名或密码错误' })
    const store = useAuthStore()
    await expect(store.login({ username: 'dev', password: 'bad' })).rejects.toBeTruthy()
    expect(mocked.setAuthToken).not.toHaveBeenCalled()
    expect(store.user).toBeNull()
  })

  it('logout 成功 → 清 token、清 user', async () => {
    mocked.getAuthToken.mockReturnValue('stored-token')
    mocked.getMe.mockResolvedValue(USER)
    const store = useAuthStore()
    await store.init()
    mocked.logout.mockResolvedValue(undefined)
    await store.logout()
    expect(mocked.clearAuthToken).toHaveBeenCalled()
    expect(store.user).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })

  it('logout 接口失败（token 已失效）也清理本地状态', async () => {
    mocked.getAuthToken.mockReturnValue('stored-token')
    mocked.getMe.mockResolvedValue(USER)
    const store = useAuthStore()
    await store.init()
    mocked.logout.mockRejectedValue({ status: 401, message: '已过期' })
    await store.logout()
    expect(mocked.clearAuthToken).toHaveBeenCalled()
    expect(store.user).toBeNull()
    expect(store.initialized).toBe(true)
  })
})

describe('401 全局回调 clearSession', () => {
  it('clearSession 清除 token 与用户（路由跳转由接线方负责）', async () => {
    mocked.getAuthToken.mockReturnValue('stored-token')
    mocked.getMe.mockResolvedValue(USER)
    const store = useAuthStore()
    await store.init()
    expect(store.isAuthenticated).toBe(true)
    store.clearSession()
    expect(mocked.clearAuthToken).toHaveBeenCalled()
    expect(store.user).toBeNull()
    expect(store.initialized).toBe(true)
  })
})
