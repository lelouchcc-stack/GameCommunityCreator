/**
 * Markdown 渲染工具（v3）—— 成熟渲染器 + 严格安全清洗。
 *
 * 安全策略：
 * 1. marked 渲染 GFM；**原始 HTML 不作为 HTML 渲染**（按纯文本转义，默认禁用原始 HTML）；
 * 2. 渲染结果经 DOMPurify 严格清洗：禁止 script/style/iframe/form 等标签与事件属性；
 * 3. 链接仅允许 http/https/mailto/相对路径（禁止 javascript:/data:/file:）；外链自动加
 *    target="_blank" rel="noopener noreferrer"；
 * 4. 图片仅允许 http/https 绝对地址；
 * 5. 不执行任何配置中的代码（无 eval/new Function）。
 */
import { Marked } from 'marked'
import DOMPurify from 'dompurify'

const ALLOWED_TAGS = [
  'p', 'br', 'hr', 'strong', 'b', 'em', 'i', 'del', 's', 'code', 'pre',
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'ul', 'ol', 'li', 'blockquote',
  'a', 'img',
  'table', 'thead', 'tbody', 'tr', 'th', 'td',
  'span', 'div',
]

const ALLOWED_ATTR = ['href', 'title', 'target', 'rel', 'src', 'alt', 'class', 'align']

function escapeHtml(text: string): string {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function isSafeHref(href: string): boolean {
  const value = href.trim()
  if (!value) return false
  if (value.startsWith('#') || value.startsWith('/')) return true
  try {
    const url = new URL(value, 'https://placeholder.invalid')
    return url.protocol === 'http:' || url.protocol === 'https:' || url.protocol === 'mailto:'
  } catch {
    return false
  }
}

function isSafeImageSrc(src: string): boolean {
  try {
    const url = new URL(src.trim(), 'https://placeholder.invalid')
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

/** 原始 HTML 按纯文本显示（禁用原始 HTML 渲染） */
const rawHtmlRenderer = {
  html(token: { text?: string }) {
    return escapeHtml(token.text ?? '')
  },
  link(token: { href?: string; title?: string | null; text?: string }) {
    const href = (token.href ?? '').trim()
    const label = token.text ?? ''
    if (!isSafeHref(href)) return label
    const title = token.title ? ` title="${escapeHtml(token.title)}"` : ''
    const external = /^https?:/i.test(href)
    const rel = external ? ' target="_blank" rel="noopener noreferrer"' : ''
    return `<a href="${escapeHtml(href)}"${title}${rel}>${label}</a>`
  },
}

const marked = new Marked({ gfm: true, breaks: false })
// eslint-disable-next-line @typescript-eslint/no-explicit-any
;(marked as any).use({ renderer: rawHtmlRenderer })

/* --------------------------- DOMPurify 清洗器 --------------------------- */

interface PurifierLike {
  sanitize: (html: string, config?: Record<string, unknown>) => string
  addHook: (name: string, hook: (node: Element) => void) => void
}

let purifier: PurifierLike | null = null
let hooksRegistered = false

function getPurifier(): PurifierLike | null {
  if (purifier) return purifier
  const candidate = DOMPurify as unknown
  if (typeof candidate === 'function') {
    // 非浏览器环境（如无 window 的 Node 测试）返回工厂函数，缺少 window 时无法清洗
    if (typeof window === 'undefined') return null
    purifier = (candidate as (win: Window) => PurifierLike)(window)
  } else {
    purifier = candidate as PurifierLike
  }
  if (purifier && !hooksRegistered) {
    purifier.addHook('afterSanitizeAttributes', (node: Element) => {
      if (node.tagName === 'A') {
        const href = node.getAttribute('href') ?? ''
        if (!isSafeHref(href)) {
          node.removeAttribute('href')
        } else if (/^https?:/i.test(href)) {
          node.setAttribute('target', '_blank')
          node.setAttribute('rel', 'noopener noreferrer')
        }
      }
      if (node.tagName === 'IMG') {
        const src = node.getAttribute('src') ?? ''
        if (!isSafeImageSrc(src)) node.removeAttribute('src')
      }
    })
    hooksRegistered = true
  }
  return purifier
}

/** Markdown → 安全 HTML（用于受控 v-html，输出已严格清洗） */
export function renderMarkdown(markdown: string | null | undefined): string {
  const source = (markdown ?? '').trim()
  if (!source) return ''
  const rawHtml = marked.parse(source) as string
  const sanitizer = getPurifier()
  if (!sanitizer) {
    // 无 DOM 环境（极少数测试）降级为纯文本，绝不直接输出未清洗 HTML
    return `<p>${escapeHtml(source)}</p>`
  }
  return sanitizer.sanitize(rawHtml, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOW_DATA_ATTR: false,
    FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed', 'form', 'input', 'button', 'textarea', 'select', 'link', 'meta'],
    FORBID_ATTR: ['style', 'onerror', 'onload', 'onclick', 'onmouseover', 'onfocus', 'srcdoc'],
  })
}

/** 提取纯文本摘要（列表卡片/公告摘要用），去除 Markdown 语法标签 */
export function markdownToPlainText(markdown: string | null | undefined, maxLength = 0): string {
  const source = (markdown ?? '').trim()
  if (!source) return ''
  let text = source
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`([^`]*)`/g, '$1')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/^\s*[-*+]\s+/gm, '')
    .replace(/^\s*\d+\.\s+/gm, '')
    .replace(/^>\s?/gm, '')
    .replace(/[*_~]{1,3}/g, '')
    .replace(/\|/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  if (maxLength > 0 && text.length > maxLength) {
    text = `${text.slice(0, maxLength)}…`
  }
  return text
}
