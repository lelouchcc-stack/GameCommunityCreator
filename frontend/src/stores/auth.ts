/**
 * 开发者登录态（v4.2 Pinia + Sa-Token + JWT）。
 *
 * - Pinia setup store（`defineStore('auth', ...)`）：应用级单例，组件/路由守卫/入口文件都用
 *   `useAuthStore()` 访问；state 用 `ref` 定义，暴露到 store 后会自动解包（模板里写 `auth.user`，
 *   不再需要 `.value`）；`isAuthenticated` 为 `computed`。
 * - token 由 `api/token.ts` 持久化到 localStorage，API 客户端自动放入 Bearer 头；
 * - init()：应用启动/路由守卫前调用——有本地 token 时 `GET /auth/me` 恢复登录态，
 *   无 token 直接视为未登录；token 失效（401）时清除本地 token；`initPromise` 防止并发重复请求；
 * - login/register：调用后端认证接口拿到 `{ token, user }`，保存 token 并更新用户；
 * - logout：通知后端注销 Sa-Token 服务端会话（204），无论成败都清除本地 token/用户；
 * - 401 由 api client 的全局回调通知，清理登录态并跳转登录页（在 main.ts 中接线）。
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getMe, login as apiLogin, logout as apiLogout, register as apiRegister } from '@/api/auth'
import { isApiError } from '@/api/client'
import { clearAuthToken, getAuthToken, setAuthToken } from '@/api/token'
import type { LoginInput, RegisterInput, UserView } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  /* ------------------------------- state ------------------------------- */

  const user = ref<UserView | null>(null)
  const initialized = ref(false)
  const loading = ref(false)
  const isAuthenticated = computed(() => user.value !== null)

  /** 防止 App.vue onMounted 与路由守卫同时调用 init() 时重复请求 /auth/me */
  let initPromise: Promise<void> | null = null

  /* ------------------------------- actions ------------------------------ */

  /** 恢复登录态：有本地 token 才请求 /auth/me；无 token 直接未登录。 */
  async function init(force = false): Promise<void> {
    if (initialized.value && !force) return
    if (initPromise && !force) return initPromise
    initPromise = (async () => {
      try {
        if (!getAuthToken()) {
          user.value = null
          return
        }
        user.value = await getMe()
      } catch (error) {
        // 401 = token 失效（清掉本地 token）；网络错误不阻塞进入登录页
        user.value = null
        if (isApiError(error) && error.status === 401) {
          clearAuthToken()
        }
      } finally {
        initialized.value = true
        initPromise = null
      }
    })()
    return initPromise
  }

  async function login(input: LoginInput): Promise<UserView> {
    loading.value = true
    try {
      const result = await apiLogin(input)
      setAuthToken(result.token)
      user.value = result.user
      initialized.value = true
      return result.user
    } finally {
      loading.value = false
    }
  }

  async function register(input: RegisterInput): Promise<UserView> {
    loading.value = true
    try {
      const result = await apiRegister(input)
      setAuthToken(result.token)
      user.value = result.user
      initialized.value = true
      return result.user
    } finally {
      loading.value = false
    }
  }

  async function logout(): Promise<void> {
    try {
      await apiLogout()
    } catch {
      // token 已失效/网络异常时也继续清理本地状态
    } finally {
      clearAuthToken()
      user.value = null
      initialized.value = true
    }
  }

  /** 401 全局回调：清除本地 token 与用户（路由跳转由接线方完成） */
  function clearSession(): void {
    clearAuthToken()
    user.value = null
    initialized.value = true
  }

  return {
    user,
    initialized,
    loading,
    isAuthenticated,
    init,
    login,
    register,
    logout,
    clearSession,
  }
})
