/** 展示格式化工具（运行时与列表页共用） */

/** ISO 本地时间 → "2025-05-01 10:30"；解析失败时原样返回 */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return ''
  const normalized = iso.includes('T') ? iso : iso.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return iso
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`
}

/** 数字展示：12345 → 1.2万 */
export function formatCount(value: number | null | undefined): string {
  const count = Number(value ?? 0)
  if (!Number.isFinite(count) || count < 0) return '0'
  if (count < 10000) return String(count)
  return `${(count / 10000).toFixed(1).replace(/\.0$/, '')}万`
}

export function formatDate(iso: string | null | undefined): string {
  return formatDateTime(iso).slice(0, 10)
}
