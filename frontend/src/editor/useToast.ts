/**
 * 轻量全局提示（不引入任何 UI 库）。
 *
 * 用法：
 *   const toast = useToast()
 *   toast.success('保存成功')
 *   toast.error('发布失败：……')
 *
 * 页面里放一个 <ToastHost /> 即可渲染，提示 3.6s 后自动消失（error 停留更久）。
 */
import { reactive } from 'vue'

export type ToastType = 'success' | 'error' | 'info'

export interface ToastItem {
  id: number
  type: ToastType
  message: string
}

interface ToastState {
  items: ToastItem[]
}

const state = reactive<ToastState>({ items: [] })

let seed = 0
const AUTO_DISMISS_MS: Record<ToastType, number> = {
  success: 3600,
  info: 3600,
  error: 5200,
}

export function dismissToast(id: number): void {
  const index = state.items.findIndex((item) => item.id === id)
  if (index >= 0) state.items.splice(index, 1)
}

function push(type: ToastType, message: string): number {
  const id = (seed += 1)
  state.items.push({ id, type, message })
  // 同一时间最多保留 4 条，避免刷屏
  while (state.items.length > 4) {
    const oldest = state.items[0]
    if (oldest) dismissToast(oldest.id)
    else break
  }
  if (typeof window !== 'undefined') {
    window.setTimeout(() => dismissToast(id), AUTO_DISMISS_MS[type])
  }
  return id
}

export function useToast() {
  return {
    items: state.items,
    show: (message: string, type: ToastType = 'success') => push(type, message),
    success: (message: string) => push('success', message),
    info: (message: string) => push('info', message),
    error: (message: string) => push('error', message),
    dismiss: dismissToast,
  }
}
