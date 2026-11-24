/** 开发者认证 API（v4 JWT，契约见 docs/API.md §11） */
import { http } from './client'
import type { AuthResult, LoginInput, RegisterInput, UserView } from '@/types/auth'

export function register(input: RegisterInput): Promise<AuthResult> {
  return http.post<AuthResult>('/auth/register', input)
}

export function login(input: LoginInput): Promise<AuthResult> {
  return http.post<AuthResult>('/auth/login', input)
}

export function logout(): Promise<void> {
  return http.post<void>('/auth/logout')
}

export function getMe(): Promise<UserView> {
  return http.get<UserView>('/auth/me')
}
