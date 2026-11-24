/**
 * Markdown 渲染安全（v3 / FIX-4）测试。
 *
 * 覆盖：GFM 基础排版、原始 HTML 转义、XSS 清洗（script/onerror/javascript:/data:）、
 * 链接与图片协议白名单、外链安全属性、纯文本摘要（列表/公告摘要用）。
 */
import { describe, expect, it } from 'vitest'

import { markdownToPlainText, renderMarkdown } from '@/utils/markdown'

describe('renderMarkdown：GFM 基础排版', () => {
  it('标题 / 列表 / 链接 / 引用 / 代码块渲染为 HTML', () => {
    const html = renderMarkdown(
      '## 标题\n\n- 一\n- 二\n\n> 引用\n\n[链接](https://example.com)\n\n```js\nconst a = 1\n```',
    )
    expect(html).toContain('<h2>标题</h2>')
    expect(html).toContain('<ul>')
    expect(html).toContain('<blockquote>')
    expect(html).toContain('<a href="https://example.com"')
    expect(html).toContain('<pre>')
  })

  it('表格渲染（GFM）', () => {
    const html = renderMarkdown('| A | B |\n|---|---|\n| 1 | 2 |')
    expect(html).toContain('<table>')
    expect(html).toContain('<td>1</td>')
  })

  it('空 Markdown 返回空字符串', () => {
    expect(renderMarkdown('')).toBe('')
    expect(renderMarkdown(null)).toBe('')
    expect(renderMarkdown(undefined)).toBe('')
  })
})

describe('renderMarkdown：XSS 与危险内容清洗', () => {
  it('<script> 不进入输出（原始 HTML 按纯文本转义 + DOMPurify 兜底）', () => {
    const html = renderMarkdown('<script>alert(1)</script>\n\n正常文字')
    expect(html).not.toContain('<script')
    expect(html).toContain('&lt;script&gt;')
    expect(html).toContain('正常文字')
  })

  it('<img onerror=...> 事件属性被移除', () => {
    const html = renderMarkdown('<img src="x" onerror="alert(1)">')
    expect(html).not.toContain('<img')
    expect(html).toContain('&lt;img')
  })

  it('javascript: 链接被移除，不产生可点击危险链接', () => {
    const html = renderMarkdown('[点我](javascript:alert(1))')
    expect(html).not.toContain('javascript:')
    expect(html).not.toContain('href="javascript')
  })

  it('data: 链接被移除', () => {
    const html = renderMarkdown('[点我](data:text/html,<script>alert(1)</script>)')
    expect(html).not.toContain('data:text/html')
  })

  it('file:// 链接被移除', () => {
    const html = renderMarkdown('[本地](file:///c:/windows/win.ini)')
    expect(html).not.toContain('file:///')
  })

  it('非 http(s) 图片地址被移除', () => {
    const html = renderMarkdown('![x](javascript:alert(1))')
    expect(html.toLowerCase()).not.toContain('javascript:')
    const html2 = renderMarkdown('![x](file:///etc/passwd)')
    expect(html2).not.toContain('file:///')
  })

  it('style/iframe 等危险标签被移除', () => {
    const html = renderMarkdown('<style>body{display:none}</style><iframe src="https://evil.example"></iframe>')
    expect(html).not.toContain('<style')
    expect(html).not.toContain('<iframe')
  })
})

describe('renderMarkdown：链接与图片安全策略', () => {
  it('http(s) 外链带 target=_blank 与 rel=noopener noreferrer', () => {
    const html = renderMarkdown('[官网](https://example.com)')
    expect(html).toContain('target="_blank"')
    expect(html).toContain('rel="noopener noreferrer"')
  })

  it('mailto 链接保留；http(s) 图片保留', () => {
    const html = renderMarkdown('[邮件](mailto:dev@example.com)\n\n![图](https://example.com/a.png)')
    expect(html).toContain('mailto:dev@example.com')
    expect(html).toContain('https://example.com/a.png')
  })

  it('原始 HTML 标签按纯文本显示（不允许任意 HTML 注入）', () => {
    const html = renderMarkdown('<b>加粗</b>')
    expect(html).toContain('&lt;b&gt;')
    expect(html).not.toContain('<b>加粗</b>')
  })
})

describe('markdownToPlainText：列表/公告摘要', () => {
  it('去除标题/列表/链接/图片/代码等 Markdown 标记', () => {
    const text = markdownToPlainText(
      '## 更新\n\n- 新增 **建造** 系统\n- 修复 [问题](https://example.com)\n\n![图](https://example.com/a.png)\n\n`code`',
    )
    expect(text).toContain('更新')
    expect(text).toContain('新增 建造 系统')
    expect(text).not.toContain('##')
    expect(text).not.toContain('**')
    expect(text).not.toContain('https://example.com')
    expect(text).not.toContain('```')
  })

  it('maxLength 截断并追加省略号', () => {
    const text = markdownToPlainText('一二三四五六七八九十', 5)
    expect(text).toBe('一二三四五…')
  })

  it('空值返回空字符串', () => {
    expect(markdownToPlainText('')).toBe('')
    expect(markdownToPlainText(null)).toBe('')
  })
})
