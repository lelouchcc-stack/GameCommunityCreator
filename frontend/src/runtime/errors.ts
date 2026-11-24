/**
 * 运行时错误文案工具。
 *
 * 数据加载失败时 loader 会原样抛出（通常是 api/client.ts 的 ApiError），
 * 运行时只负责把「面向用户的 message」展示出来，不吞错误、不拼接堆栈。
 */
import { isApiError } from '@/api/client'

export const DEFAULT_LOAD_ERROR = '数据加载失败，请稍后重试'

/** 从任意异常中提取可展示的中文错误消息，永不返回空串 */
export function toErrorMessage(error: unknown, fallback: string = DEFAULT_LOAD_ERROR): string {
  if (isApiError(error)) {
    return (error.message ?? '').trim() || fallback
  }
  if (error instanceof Error) {
    return (error.message ?? '').trim() || fallback
  }
  if (typeof error === 'string' && error.trim()) {
    return error.trim()
  }
  return fallback
}

/** 是否为「资源不存在」类错误（公开社区未发布 / 项目已被删除） */
export function isNotFoundError(error: unknown): boolean {
  return isApiError(error) && error.status === 404
}
