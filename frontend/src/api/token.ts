/**
 * JWT 令牌存储（v4）。
 *
 * 登录/注册成功后把后端签发的 token 持久化到 `localStorage`（key：`gcc.auth.token`），
 * 统一 API 客户端每次请求自动读取并放入 `Authorization: Bearer <token>` 头。
 *
 * 说明：localStorage 在浏览器隐私模式/存储被禁用时可能不可用，此时退化为内存存储
 * （仅当前页面会话有效，刷新后需重新登录），不阻塞应用运行。
 */

export const AUTH_TOKEN_STORAGE_KEY = 'gcc.auth.token'

/** localStorage 不可用时的内存兜底（仅当前页面生命周期）。 */
let memoryToken: string | null = null

function storage(): Storage | null {
  try {
    return typeof window !== 'undefined' && window.localStorage ? window.localStorage : null
  } catch {
    return null
  }
}

/** 读取当前 JWT；未登录返回 null。 */
export function getAuthToken(): string | null {
  const store = storage()
  if (!store) return memoryToken
  try {
    return store.getItem(AUTH_TOKEN_STORAGE_KEY)
  } catch {
    return memoryToken
  }
}

/** 保存 JWT（传 null 等价于清除）。 */
export function setAuthToken(token: string | null): void {
  const value = typeof token === 'string' && token.trim() ? token.trim() : null
  memoryToken = value
  const store = storage()
  if (!store) return
  try {
    if (value) store.setItem(AUTH_TOKEN_STORAGE_KEY, value)
    else store.removeItem(AUTH_TOKEN_STORAGE_KEY)
  } catch {
    // 存储不可写时保留内存兜底，不打断登录流程
  }
}

/** 清除 JWT（退出登录 / token 失效）。 */
export function clearAuthToken(): void {
  setAuthToken(null)
}
