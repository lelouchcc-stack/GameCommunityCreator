/**
 * v3.2 缺陷修复回归：内容管理弹窗「未保存修改」保护（P0-B）。
 *
 * ContentManagerView 依赖较多 API 与弹窗内部状态，组件级挂载成本高；
 * 按 task 要求采用「静态契约断言」：直接读取 SFC 源码，锁定
 * - 未保存确认弹层的文案与可访问性结构；
 * - 关闭入口（× / 取消 / 点击遮罩）统一走 requestClose，脏数据时先弹确认；
 * - 「继续编辑」只关确认层、不关闭编辑弹窗；「放弃修改」才真正关闭；
 * - 正文 MarkdownEditor 嵌在 form 内且上传能力开启（配合上传按钮不提交表单）。
 */
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  resolve(process.cwd(), 'src/views/admin/ContentManagerView.vue'),
  'utf8',
)

/** 取某个函数的源码片段（到下一个行首 `}` 为止，足够判定块内语句） */
function functionBody(name: string): string {
  const start = SOURCE.indexOf(`function ${name}(`)
  expect(start, `缺少函数 ${name}`).toBeGreaterThanOrEqual(0)
  const end = SOURCE.indexOf('\n}', start)
  return SOURCE.slice(start, end === -1 ? undefined : end)
}

describe('ContentManagerView：未保存确认弹层（P0-B）', () => {
  it('存在未保存确认弹层文案与可访问性结构', () => {
    expect(SOURCE).toContain('当前编辑内容尚未保存，确认放弃修改吗？')
    expect(SOURCE).toContain('继续编辑')
    expect(SOURCE).toContain('放弃修改')
    expect(SOURCE).toContain('Unsaved changes')
    expect(SOURCE).toContain('role="alertdialog"')
    expect(SOURCE).toContain('aria-modal="true"')
    expect(SOURCE).toContain('class="dialog confirm-dialog"')
  })

  it('关闭入口（× / 取消 / 点击遮罩）统一调用 requestClose', () => {
    // 右上角 × 与底部「取消」按钮直接调用；点击遮罩走 @click.self（不会误伤弹窗内容点击）
    const directCalls = SOURCE.split('@click="requestClose"').length - 1
    expect(directCalls).toBeGreaterThanOrEqual(2)
    expect(SOURCE).toContain('@click.self="requestClose"')
  })

  it('requestClose：有未保存修改时先弹确认（不直接关闭弹窗）', () => {
    const body = functionBody('requestClose')
    expect(body).toContain('isDirty.value')
    expect(body).toContain('discardConfirmOpen.value = true')
    expect(body).toContain('closeDialog()')
  })

  it('「继续编辑」只关闭确认层；「放弃修改」才调用 closeDialog', () => {
    const keepEditing = functionBody('keepEditing')
    expect(keepEditing).toContain('discardConfirmOpen.value = false')
    expect(keepEditing).not.toContain('closeDialog()')

    const discardAndClose = functionBody('discardAndClose')
    expect(discardAndClose).toContain('discardConfirmOpen.value = false')
    expect(discardAndClose).toContain('closeDialog()')
  })
})

describe('ContentManagerView：Markdown 正文与上传（P0-A/P0-C 配合项）', () => {
  it('正文使用 MarkdownEditor，且编辑弹窗是 form + submit 处理（上传按钮不触发提交）', () => {
    expect(SOURCE).toContain('<MarkdownEditor')
    expect(SOURCE).toContain('@submit.prevent="submit"')
    // 默认 allowImageUpload=true（组件默认值），内容管理不应显式关闭上传
    expect(SOURCE).not.toContain(':allow-image-upload="false"')
    expect(SOURCE).not.toContain('allow-image-upload="false"')
  })
})
