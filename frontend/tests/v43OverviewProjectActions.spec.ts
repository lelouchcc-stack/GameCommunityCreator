/**
 * v4.3 缺陷回归：管理端「总览（首页）」站点卡片的按钮必须真实生效。
 *
 * 缺陷现象：/admin/overview 上的 ProjectCard 渲染了「站点发布」「编辑站点信息」
 * 「删除站点」，但点击后毫无反应；同样的按钮在「我的游戏」（/admin/projects）
 * 上却是正常的。根因是 OverviewView 只监听了 open / settings / visit 三个事件，
 * 未监听 publish / edit / delete。
 *
 * 本文件通过挂载 OverviewView + mock API，直接断言「点击 → 调用接口 → 刷新列表」，
 * 而不是只断言按钮文案，避免同类「有按钮没功能」问题再次漏测。
 */
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({
  listProjects: vi.fn(),
  updateProject: vi.fn(),
  deleteProject: vi.fn(),
  publishSite: vi.fn(),
}))

vi.mock('@/api/projects', () => ({
  listProjects: api.listProjects,
  updateProject: api.updateProject,
  deleteProject: api.deleteProject,
}))

vi.mock('@/api/pages', () => ({
  publishSite: api.publishSite,
}))

import OverviewView from '@/views/admin/OverviewView.vue'
import type { ProjectView } from '@/types/api'

const PROJECT: ProjectView = {
  id: 7,
  name: '星汐边境官方社区',
  slug: 'stellar-drift',
  gameName: '星汐边境',
  description: '一款关于星海航行的独立游戏。',
  status: 'PUBLISHED',
  homePageId: 'home',
  draftVersion: 3,
  publishedVersion: 3,
  pageCount: 6,
  publishedPageCount: 6,
  createdAt: '2026-10-20T10:00:00',
  updatedAt: '2026-10-26T17:02:12',
}

/** 挂载总览页并等待首次 listProjects 结束 */
async function mountOverview() {
  const wrapper = mount(OverviewView, {
    global: {
      stubs: {
        // 只关心按钮行为，RouterLink 用最简桩替换，避免引入真实路由
        RouterLink: { template: '<a><slot /></a>' },
      },
    },
  })
  await flushPromises()
  return wrapper
}

function buttonByText(wrapper: Awaited<ReturnType<typeof mountOverview>>, text: string) {
  const button = wrapper.findAll('button').find((item) => item.text().trim() === text)
  expect(button, `未找到按钮：${text}`).toBeTruthy()
  return button!
}

beforeEach(() => {
  vi.clearAllMocks()
  api.listProjects.mockResolvedValue([PROJECT])
  api.updateProject.mockResolvedValue({ ...PROJECT, name: '星汐边境 2.0' })
  api.deleteProject.mockResolvedValue(undefined)
  api.publishSite.mockResolvedValue({
    publishedPages: [{ pageId: 'home', pageSlug: 'home', publicationId: 1, version: 4, publishedAt: '', publicPath: '/' }],
    skippedPages: [],
  })
  // 删除有两次 window.confirm 二次确认
  vi.spyOn(window, 'confirm').mockReturnValue(true)
})

describe('管理端总览：站点卡片操作真实生效（v4.3 修复）', () => {
  it('首页卡片仍渲染「站点发布」「编辑站点信息」「删除站点」', async () => {
    const wrapper = await mountOverview()
    const texts = wrapper.findAll('button').map((item) => item.text().trim())
    expect(texts).toContain('站点发布')
    expect(texts).toContain('删除站点')
    expect(wrapper.find('button[aria-label="编辑站点信息"]').exists()).toBe(true)
  })

  it('点击「删除站点」→ 二次确认 → 调用删除接口并刷新列表', async () => {
    const wrapper = await mountOverview()
    expect(api.listProjects).toHaveBeenCalledTimes(1)

    await buttonByText(wrapper, '删除站点').trigger('click')
    await flushPromises()

    expect(window.confirm).toHaveBeenCalledTimes(2)
    expect(api.deleteProject).toHaveBeenCalledWith(PROJECT.id)
    // 删除成功后重新拉取列表（统计分栏与「我的项目」随之更新）
    expect(api.listProjects).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('已删除站点「星汐边境官方社区」')
  })

  it('确认弹窗被取消时不删除任何站点', async () => {
    vi.mocked(window.confirm).mockReturnValue(false)
    const wrapper = await mountOverview()

    await buttonByText(wrapper, '删除站点').trigger('click')
    await flushPromises()

    expect(api.deleteProject).not.toHaveBeenCalled()
    expect(api.listProjects).toHaveBeenCalledTimes(1)
  })

  it('点击「编辑站点信息」→ 打开弹窗 → 保存后调用更新接口并刷新列表', async () => {
    const wrapper = await mountOverview()

    await wrapper.find('button[aria-label="编辑站点信息"]').trigger('click')
    await flushPromises()

    const nameInput = wrapper.find('#edit-project-name')
    expect(nameInput.exists(), '编辑弹窗未打开').toBe(true)
    expect((nameInput.element as HTMLInputElement).value).toBe(PROJECT.name)

    await nameInput.setValue('星汐边境 2.0')
    await wrapper.find('form.dialog__body').trigger('submit')
    await flushPromises()

    expect(api.updateProject).toHaveBeenCalledWith(PROJECT.id, {
      name: '星汐边境 2.0',
      gameName: PROJECT.gameName,
      description: PROJECT.description,
    })
    // 保存成功后关闭弹窗并刷新列表
    expect(wrapper.find('#edit-project-name').exists()).toBe(false)
    expect(api.listProjects).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('站点「星汐边境 2.0」信息已更新')
  })

  it('编辑弹窗必填校验：清空站点名称不会提交', async () => {
    const wrapper = await mountOverview()

    await wrapper.find('button[aria-label="编辑站点信息"]').trigger('click')
    await flushPromises()
    await wrapper.find('#edit-project-name').setValue('   ')
    await wrapper.find('form.dialog__body').trigger('submit')
    await flushPromises()

    expect(api.updateProject).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请填写站点名称')
  })

  it('点击「站点发布」→ 调用站点发布接口并刷新列表', async () => {
    const wrapper = await mountOverview()

    await buttonByText(wrapper, '站点发布').trigger('click')
    await flushPromises()

    expect(api.publishSite).toHaveBeenCalledWith(PROJECT.id)
    expect(api.listProjects).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('发布完成')
  })
})
