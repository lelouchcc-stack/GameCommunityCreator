/**
 * 运行时轻量提示（toast）：动作执行失败 / 未配置动作 / 预览模式提示都走这里。
 *
 * 不引入 UI 框架：一个响应式消息队列 + RuntimeToast.vue 渲染。
 */
import { reactive } from 'vue'

export type ToastKind = 'info' | 'success' | 'error'

export interface RuntimeToastItem {
  id: number
  message: string
  kind: ToastKind
}

export const TOAST_DURATION_MS = 2600

const state = reactive<{ items: RuntimeToastItem[] }>({ items: [] })
const timers = new Map<number, ReturnType<typeof setTimeout>>()
let seq = 0

export function toastState(): { items: RuntimeToastItem[] } {
  return state
}

export function dismissToast(id: number): void {
  const timer = timers.get(id)
  if (timer) {
    clearTimeout(timer)
    timers.delete(id)
  }
  const index = state.items.findIndex((item) => item.id === id)
  if (index >= 0) state.items.splice(index, 1)
}

export function pushToast(
  message: string,
  kind: ToastKind = 'info',
  duration: number = TOAST_DURATION_MS,
): void {
  const text = String(message ?? '').trim()
  if (!text) return
  seq += 1
  const item: RuntimeToastItem = { id: seq, message: text, kind }
  state.items.push(item)
  if (typeof setTimeout === 'function' && duration > 0) {
    const timer = setTimeout(() => dismissToast(item.id), duration)
    timers.set(item.id, timer)
  }
}

/** 清空全部提示（单测 / 路由切换用） */
export function resetToasts(): void {
  for (const timer of timers.values()) clearTimeout(timer)
  timers.clear()
  state.items.splice(0, state.items.length)
}
