/**
 * 图片上传 API（答辩前缺陷修复 P0-C）。
 *
 * 复用后端真实上传接口：POST /api/uploads/images（需要登录，v4 JWT Bearer 认证）。
 * 返回的 url 为 `/api/public/uploads/{storedName}`，公开端匿名可访问。
 */
import { getAuthToken } from './token'

export interface UploadedImage {
  url: string
  name: string
  size: number
  contentType: string
}

const UPLOAD_ENDPOINT = '/api/uploads/images'
const MAX_IMAGE_BYTES = 5 * 1024 * 1024
const ALLOWED_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']

export class UploadError extends Error {}

function readErrorMessage(payload: unknown, status: number): string {
  if (payload && typeof payload === 'object') {
    const message = (payload as { message?: unknown }).message
    if (typeof message === 'string' && message.trim()) return message
  }
  return `图片上传失败（HTTP ${status}）`
}

/** 上传本地图片，返回可插入 Markdown 的 URL；失败抛 UploadError（中文提示） */
export async function uploadImage(file: File): Promise<UploadedImage> {
  if (!ALLOWED_TYPES.includes(file.type)) {
    throw new UploadError('仅支持 PNG / JPEG / WebP / GIF 格式图片')
  }
  if (file.size > MAX_IMAGE_BYTES) {
    throw new UploadError('图片大小不能超过 5MB')
  }
  const form = new FormData()
  form.append('file', file)
  const token = getAuthToken()
  const headers: Record<string, string> = {}
  if (token) headers.Authorization = `Bearer ${token}`
  let response: Response
  try {
    response = await fetch(UPLOAD_ENDPOINT, {
      method: 'POST',
      body: form,
      headers,
    })
  } catch {
    throw new UploadError('无法连接上传接口，请确认后端服务已启动')
  }
  let payload: unknown = null
  try {
    payload = await response.json()
  } catch {
    payload = null
  }
  const envelope = payload as { code?: number; data?: UploadedImage; message?: string } | null
  if (!response.ok || (envelope?.code ?? 200) >= 400 || !envelope?.data?.url) {
    throw new UploadError(readErrorMessage(envelope, response.status))
  }
  return envelope.data
}
