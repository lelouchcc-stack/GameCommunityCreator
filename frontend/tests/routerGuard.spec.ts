/**
 * 前端路由守卫（v3 / FIX-3）测试。
 *
 * 覆盖：
 * - 未登录访问 /admin/** → 重定向 /login 并带 redirect 查询参数；
 * - 已登录访问 /admin/** → 放行；
 * - 已登录访问 guestOnly 的 /login、/register → 重定向「我的游戏」；
 * - 未登录访问 /login、公开端 /community/** → 正常放行。
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { installMemoryStorage } from './helpers/webStorage'

const authState = vi.hoisted(() => ({ authenticated: false }))

// Pinia setup store 的 ref 会解包：路由守卫直接读 auth.isAuthenticated（不再 .value）
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    init: async () => {},
    get isAuthenticated() {
      return authState.authenticated
    },
    get user() {
      return authState.authenticated ? { id: 1, username: 'dev', displayName: '独立开发者' } : null
    },
  }),
}))

async function freshRouter() {
  vi.resetModules()
  const { default: router } = await import('@/router/index')
  return router
}

beforeEach(() => {
  authState.authenticated = false
  // Node 25 + jsdom 下全局 localStorage 可能不可用，统一替换为内存实现
  installMemoryStorage()
  // jsdom 未实现 window.scrollTo（vue-router scrollBehavior 会调用），用空实现避免噪音
  window.scrollTo = (() => {}) as typeof window.scrollTo
})

describe('路由守卫：未登录', () => {
  it('访问 /admin/projects → 跳转 /login 且带 redirect=原路径', async () => {
    const router = await freshRouter()
    await router.push('/admin/projects')
    expect(router.currentRoute.value.name).toBe('auth-login')
    expect(router.currentRoute.value.query.redirect).toBe('/admin/projects')
  })

  it('访问 /login、/register、/community/demo-site → 放行', async () => {
    const router = await freshRouter()
    await router.push('/login')
    expect(router.currentRoute.value.name).toBe('auth-login')
    await router.push('/register')
    expect(router.currentRoute.value.name).toBe('auth-register')
    await router.push('/community/demo-site')
    expect(router.currentRoute.value.name).toBe('community-home')
  })
})

describe('路由守卫：已登录', () => {
  it('访问 /admin/projects → 放行到 project-list', async () => {
    authState.authenticated = true
    const router = await freshRouter()
    await router.push('/admin/projects')
    expect(router.currentRoute.value.name).toBe('project-list')
  })

  it('访问 guestOnly 的 /login、/register → 重定向到「我的游戏」', async () => {
    authState.authenticated = true
    const router = await freshRouter()
    await router.push('/login')
    expect(router.currentRoute.value.name).toBe('project-list')
    await router.push('/register')
    expect(router.currentRoute.value.name).toBe('project-list')
  })
})
