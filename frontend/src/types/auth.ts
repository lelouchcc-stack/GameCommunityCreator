/** 开发者账号与认证类型（v4 JWT，契约见 docs/API.md §11） */

export interface UserView {
  id: number
  username: string
  displayName: string
}

export interface LoginInput {
  username: string
  password: string
}

export interface RegisterInput {
  username: string
  password: string
  displayName: string
}

/** 登录/注册成功返回的认证结果：JWT + 有效期 + 用户信息 */
export interface AuthResult {
  token: string
  tokenType: string
  expiresIn: number
  user: UserView
}

export const USERNAME_PATTERN = /^[A-Za-z0-9_-]{3,30}$/
export const PASSWORD_MIN_LENGTH = 8

/** 前端密码规则与后端一致：≥8 位且同时包含字母与数字 */
export function validatePasswordRules(password: string): string[] {
  const errors: string[] = []
  if (password.length < PASSWORD_MIN_LENGTH) errors.push(`密码至少 ${PASSWORD_MIN_LENGTH} 位`)
  if (!/[A-Za-z]/.test(password)) errors.push('密码需包含字母')
  if (!/[0-9]/.test(password)) errors.push('密码需包含数字')
  return errors
}

export function validateUsernameRules(username: string): string[] {
  return USERNAME_PATTERN.test(username.trim())
    ? []
    : ['用户名需为 3-30 位字母、数字、下划线或中划线']
}
