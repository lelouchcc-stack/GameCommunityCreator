/**
 * v3.2 缺陷修复回归：MarkdownEditor（图片上传 / 表单提交安全 / 预览链接拦截）。
 *
 * 背景（答辩前 P0 缺陷）：
 * 1) Markdown 弹窗内缺少图片上传能力 → 新增真实上传；
 * 2) 工具栏 / 模式按钮若为 submit 会提交父表单导致「未保存就被关闭」→ 必须全部 type="button"；
 * 3) 预览区链接点击会整页跳转、丢失未保存内容 → 拦截并提示。
 *
 * 全部通过 mock `@/api/uploads` 实现，不发起真实网络请求。
 */
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, nextTick, ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const uploadState = vi.hoisted(() => {
  class MockUploadError extends Error {}
  return {
    uploadImage: vi.fn(),
    UploadError: MockUploadError,
  }
})

vi.mock('@/api/uploads', () => ({
  uploadImage: uploadState.uploadImage,
  UploadError: uploadState.UploadError,
}))

import MarkdownEditor from '@/components/markdown/MarkdownEditor.vue'
import { uploadImage, UploadError } from '@/api/uploads'

const IMAGE = new File([new Uint8Array([1, 2, 3])], 'shot.png', { type: 'image/png' })

function mountEditor(props: Record<string, unknown> = {}) {
  return mount(MarkdownEditor, {
    props: { modelValue: '', ...props },
  })
}

/** 在 form 中挂载编辑器，用于验证内部按钮不会触发父级表单提交 */
function mountInForm(initialValue = '') {
  const onSubmit = vi.fn((event: Event) => event.preventDefault())
  const Host = defineComponent({
    setup() {
      const text = ref(initialValue)
      return () =>
        h('form', { onSubmit }, [
          h(MarkdownEditor, {
            modelValue: text.value,
            'onUpdate:modelValue': (value: string) => {
              text.value = value
            },
          }),
        ])
    },
  })
  return { wrapper: mount(Host), onSubmit }
}

async function setSelection(
  wrapper: ReturnType<typeof mountEditor>,
  start: number,
  end = start,
): Promise<void> {
  const textarea = wrapper.find('textarea').element as HTMLTextAreaElement
  textarea.focus()
  textarea.setSelectionRange(start, end)
  await nextTick()
}

async function chooseFile(
  wrapper: ReturnType<typeof mountEditor>,
  file: File = IMAGE,
): Promise<void> {
  const input = wrapper.find('input[type="file"]')
  expect(input.exists(), '应存在图片文件选择框').toBe(true)
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')
  await flushPromises()
}

function lastEmittedValue(wrapper: ReturnType<typeof mountEditor>): string | undefined {
  const events = wrapper.emitted('update:modelValue')
  if (!events || events.length === 0) return undefined
  return events[events.length - 1][0] as string
}

beforeEach(() => {
  vi.clearAllMocks()
  uploadState.uploadImage.mockResolvedValue({
    url: 'https://cdn.example.com/shot.png',
    name: 'shot.png',
    size: 3,
    contentType: 'image/png',
  })
})

describe('MarkdownEditor：表单提交安全（未保存保护配合项）', () => {
  it('工具栏 / 模式 / 上传按钮全部 type="button"，在 form 内点击不会提交父表单', async () => {
    const { wrapper, onSubmit } = mountInForm('已有正文')

    const buttons = wrapper.findAll('.markdown-editor button')
    expect(buttons.length).toBeGreaterThan(10)
    for (const button of buttons) {
      expect(button.attributes('type'), `${button.text()} 应为 type=button`).toBe('button')
    }

    await wrapper.find('.markdown-editor__tool').trigger('click')
    await wrapper.findAll('.markdown-editor__mode')[1].trigger('click')
    await wrapper.find('.markdown-editor__upload').trigger('click')
    await flushPromises()

    expect(onSubmit).not.toHaveBeenCalled()
  })
})

describe('MarkdownEditor：图片上传按钮', () => {
  it('默认显示「上传图片」按钮与文件选择框（accept 限定图片类型）', () => {
    const wrapper = mountEditor()
    const uploadButton = wrapper.find('.markdown-editor__upload')
    expect(uploadButton.exists()).toBe(true)
    expect(uploadButton.text()).toBe('上传图片')
    const input = wrapper.find('input[type="file"]')
    expect(input.exists()).toBe(true)
    expect(input.attributes('accept')).toBe('image/png,image/jpeg,image/webp,image/gif')
  })

  it('allowImageUpload=false（如无上传接口的场景）时不渲染上传按钮与文件框', () => {
    const wrapper = mountEditor({ allowImageUpload: false })
    expect(wrapper.find('.markdown-editor__upload').exists()).toBe(false)
    expect(wrapper.find('input[type="file"]').exists()).toBe(false)
  })

  it('选择文件后调用 uploadImage(选中文件)，并在 v-model 中插入 ![alt](url) 且保留原内容', async () => {
    const wrapper = mountEditor({ modelValue: '第一段内容' })
    await setSelection(wrapper, '第一段内容'.length)

    await chooseFile(wrapper)

    expect(uploadImage).toHaveBeenCalledTimes(1)
    expect(uploadImage).toHaveBeenCalledWith(IMAGE)
    expect(lastEmittedValue(wrapper)).toBe('第一段内容![shot](https://cdn.example.com/shot.png)')
    expect(wrapper.find('.markdown-editor__error').exists()).toBe(false)
  })

  it('光标在中间时插入到光标位置（不覆盖原有内容）', async () => {
    const wrapper = mountEditor({ modelValue: 'AB' })
    await setSelection(wrapper, 1)

    await chooseFile(wrapper)

    expect(lastEmittedValue(wrapper)).toBe('A![shot](https://cdn.example.com/shot.png)B')
  })

  it('上传失败：编辑器内显示中文错误，且不修改正文', async () => {
    uploadState.uploadImage.mockRejectedValue(new UploadError('图片大小不能超过 5MB'))
    const wrapper = mountEditor({ modelValue: '正文' })

    await chooseFile(wrapper)

    const errorNode = wrapper.find('.markdown-editor__error')
    expect(errorNode.exists()).toBe(true)
    expect(errorNode.text()).toContain('图片大小不能超过 5MB')
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('上传中：按钮文案变为「上传中…」且禁用，完成后恢复', async () => {
    let resolveUpload!: (value: { url: string; name: string; size: number; contentType: string }) => void
    uploadState.uploadImage.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveUpload = resolve
        }),
    )
    const wrapper = mountEditor({ modelValue: '' })
    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', { value: [IMAGE], configurable: true })

    await input.trigger('change')
    await nextTick()

    const busyButton = wrapper.find('.markdown-editor__upload')
    expect(busyButton.text()).toBe('上传中…')
    expect(busyButton.attributes('disabled')).toBeDefined()

    resolveUpload({ url: 'https://cdn.example.com/shot.png', name: 'shot.png', size: 3, contentType: 'image/png' })
    await flushPromises()
    expect(wrapper.find('.markdown-editor__upload').text()).toBe('上传图片')
  })
})

describe('MarkdownEditor：预览区链接拦截（防止未保存内容丢失）', () => {
  it('预览中点击链接会 preventDefault，并显示「不会跳转」提示', async () => {
    const wrapper = mountEditor({ modelValue: '[官网](https://example.com/docs)' })

    const modeButtons = wrapper.findAll('.markdown-editor__mode')
    await modeButtons[1].trigger('click') // 预览
    await nextTick()

    const anchor = wrapper.find('.markdown-editor__preview a')
    expect(anchor.exists(), '预览区应渲染出链接').toBe(true)

    const event = new MouseEvent('click', { bubbles: true, cancelable: true })
    anchor.element.dispatchEvent(event)
    await nextTick()

    expect(event.defaultPrevented).toBe(true)
    const hint = wrapper.find('.markdown-editor__hint')
    expect(hint.exists()).toBe(true)
    expect(hint.text()).toContain('不会跳转')
    // 模式切换不应因点击链接而离开预览
    expect(wrapper.find('.markdown-editor__body').classes()).toContain('is-preview')
  })
})

describe('MarkdownEditor：编辑模式与工具栏', () => {
  it('三种模式切换只影响预览布局类名（编辑 / 预览 / 分栏）', async () => {
    const wrapper = mountEditor({ modelValue: '## 标题' })
    const modes = wrapper.findAll('.markdown-editor__mode')
    await modes[1].trigger('click')
    expect(wrapper.find('.markdown-editor__body').classes()).toContain('is-preview')
    await modes[2].trigger('click')
    expect(wrapper.find('.markdown-editor__body').classes()).toContain('is-split')
    await modes[0].trigger('click')
    expect(wrapper.find('.markdown-editor__body').classes()).toContain('is-edit')
  })

  it('工具栏插入 Markdown（粗体）并通过 v-model 回传', async () => {
    const wrapper = mountEditor({ modelValue: '' })
    const boldButton = wrapper.findAll('.markdown-editor__tool').find(
      (button) => button.attributes('title') === '粗体',
    )
    expect(boldButton).toBeDefined()
    await boldButton?.trigger('click')
    expect(lastEmittedValue(wrapper)).toBe('**粗体文字**')
  })
})
