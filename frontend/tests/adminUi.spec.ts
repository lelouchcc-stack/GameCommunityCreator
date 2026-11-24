/**
 * xAI 风格管理端 UI 重构回归测试（v3）。
 *
 * 背景：v3 把管理端重构为「近黑控制台 + AdminShell 侧边栏」，新增
 * Overview / Templates / Components / Settings 四个控制台页面与路由，
 * 重写 ProjectListView / ProjectCard 与设计系统（XIcon / PageHeader）。
 * 本文件覆盖重构后**新 UI 的关键契约**，确保 E2E 依赖的文案与导航结构稳定：
 *
 * 1. 路由表：新控制台路由（admin-overview/admin-templates/admin-components/
 *    admin-settings）可达，且 `/admin` 重定向到 overview；
 *    未登录访问新控制台路由仍会被守卫拦到登录页。
 * 2. AdminShell：侧边栏 5 个中文导航项（总览/项目/模板/组件/设置）
 *    存在且 href 正确；active 判定（Overview 只在自己路由高亮，项目子页面归属 Projects）。
 * 3. 设计系统原子组件：XIcon 渲染（stroke 线框 SVG）与 PageHeader 渲染（kicker/title/描述/操作插槽）。
 * 4. 项目卡片 ProjectCard：按钮文案「页面管理」「站点发布」「删除站点」与状态展示（E2E 流程依赖）。
 */
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter, type RouteRecordRaw } from 'vue-router'

import { installMemoryStorage } from './helpers/webStorage'

// 路由守卫依赖登录态：这里用可变 mock 控制「已登录 / 未登录」两种场景
// Pinia setup store 的 ref 会解包：直接读 auth.isAuthenticated / auth.user
const authState = vi.hoisted(() => ({ authenticated: true }))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    init: async () => {},
    get isAuthenticated() {
      return authState.authenticated
    },
    get user() {
      return authState.authenticated
        ? { id: 1, username: 'dev', displayName: '独立开发者' }
        : null
    },
  }),
}))

import AdminShell from '@/components/admin/AdminShell.vue'
import PageHeader from '@/components/admin/PageHeader.vue'
import ProjectCard from '@/components/admin/ProjectCard.vue'
import XIcon from '@/components/admin/XIcon.vue'
import type { ProjectView } from '@/types/api'

// ---------------------------------------------------------------------------
// 路由表（新控制台）回归
// ---------------------------------------------------------------------------

/** 新控制台路由：路径 → 路由名 → 页面标题 */
const NEW_ADMIN_ROUTES: ReadonlyArray<[path: string, name: string]> = [
  ['/admin/overview', 'admin-overview'],
  ['/admin/templates', 'admin-templates'],
  ['/admin/components', 'admin-components'],
  ['/admin/settings', 'admin-settings'],
]

async function freshSourceRouter() {
  vi.resetModules()
  const { default: router } = await import('@/router/index')
  return router
}

beforeEach(() => {
  authState.authenticated = true
  installMemoryStorage()
  // jsdom 未实现 window.scrollTo（vue-router scrollBehavior 会调用），补空实现
  window.scrollTo = (() => {}) as typeof window.scrollTo
})

describe('路由表：xAI 控制台新页面', () => {
  it('新控制台路由全部注册（Overview/Templates/Components/Settings）', async () => {
    const router = await freshSourceRouter()
    const routeNames = router.getRoutes().map((route) => route.name)
    for (const [, name] of NEW_ADMIN_ROUTES) {
      expect(routeNames, `缺少路由 ${name}`).toContain(name)
    }
  })

  it('/admin 重定向到 /admin/overview（控制台默认首页）', async () => {
    authState.authenticated = true
    const router = await freshSourceRouter()
    await router.push('/admin')
    expect(router.currentRoute.value.name).toBe('admin-overview')
    expect(router.currentRoute.value.path).toBe('/admin/overview')
  })

  it('新控制台路由逐个可达', async () => {
    authState.authenticated = true
    const router = await freshSourceRouter()
    for (const [path, name] of NEW_ADMIN_ROUTES) {
      await router.push(path)
      expect(router.currentRoute.value.name, `${path} 未解析到 ${name}`).toBe(name)
      expect(router.currentRoute.value.path).toBe(path)
    }
  })

  it('未登录访问新控制台路由 → 守卫重定向到登录页并带 redirect', async () => {
    authState.authenticated = false
    const router = await freshSourceRouter()
    await router.push('/admin/templates')
    expect(router.currentRoute.value.name).toBe('auth-login')
    expect(router.currentRoute.value.query.redirect).toBe('/admin/templates')
  })
})

// ---------------------------------------------------------------------------
// AdminShell 导航 / active 判定
// ---------------------------------------------------------------------------

const ShellRouteStub = { template: '<div class="shell-route-stub" />' }

/** 给 AdminShell 用的轻量 router（与真实路由解耦，只验证外壳行为） */
function createShellRouter() {
  const routes: RouteRecordRaw[] = [
    { path: '/admin', redirect: '/admin/overview' },
    { path: '/admin/overview', name: 'admin-overview', component: ShellRouteStub },
    { path: '/admin/projects', name: 'project-list', component: ShellRouteStub },
    { path: '/admin/projects/:id/pages', name: 'page-list', component: ShellRouteStub },
    { path: '/admin/templates', name: 'admin-templates', component: ShellRouteStub },
    { path: '/admin/components', name: 'admin-components', component: ShellRouteStub },
    { path: '/admin/settings', name: 'admin-settings', component: ShellRouteStub },
    { path: '/community', name: 'community-gallery', component: ShellRouteStub },
  ]
  return createRouter({ history: createMemoryHistory(), routes })
}

async function mountShell(path: string) {
  const router = createShellRouter()
  await router.push(path)
  await router.isReady()
  const wrapper = mount(AdminShell, {
    global: {
      plugins: [router],
      // 用户菜单会拉 auth store / 主题逻辑，外壳测试中用占位替换，聚焦导航结构
      stubs: { AdminUserMenu: { template: '<div class="admin-user-menu-stub" />' } },
    },
  })
  await flushPromises()
  return { wrapper, router }
}

function navItems(wrapper: Awaited<ReturnType<typeof mountShell>>['wrapper']) {
  return wrapper.findAll('.admin-shell__nav-item')
}

function activeNavLabels(wrapper: Awaited<ReturnType<typeof mountShell>>['wrapper']): string[] {
  return wrapper
    .findAll('.admin-shell__nav-item.is-active')
    .map((item) => (item.find('.admin-shell__nav-text').text() || '').trim())
}

describe('AdminShell：侧边栏导航', () => {
  it('包含 5 个中文导航项（总览/项目/模板/组件/设置）', async () => {
    const { wrapper } = await mountShell('/admin/overview')
    const labels = navItems(wrapper).map((item) => ({ label: (item.find('.admin-shell__nav-text').text() || '').trim(), href: item.attributes('href') ?? '' }))
    expect(labels.map((item) => item.label)).toEqual([
      '总览',
      '项目',
      '模板',
      '组件',
      '设置',
    ])
    expect(labels.map((item) => item.href)).toEqual([
      '/admin/overview',
      '/admin/projects',
      '/admin/templates',
      '/admin/components',
      '/admin/settings',
    ])
  })

  it('导航按 工作区 / 构建 / 系统 分组展示', async () => {
    const { wrapper } = await mountShell('/admin/overview')
    const groups = wrapper.findAll('.admin-shell__nav-label').map((label) => label.text().trim())
    expect(groups).toEqual(['工作区', '构建', '系统'])
  })

  it('/admin/overview 下仅「总览」高亮', async () => {
    const { wrapper } = await mountShell('/admin/overview')
    expect(activeNavLabels(wrapper)).toEqual(['总览'])
  })

  it('/admin/projects 下仅「项目」高亮', async () => {
    const { wrapper } = await mountShell('/admin/projects')
    expect(activeNavLabels(wrapper)).toEqual(['项目'])
  })

  it('项目子页面（页面管理）仍归属「项目」高亮', async () => {
    const { wrapper } = await mountShell('/admin/projects/1/pages')
    expect(activeNavLabels(wrapper)).toEqual(['项目'])
  })

  it('/admin/settings 下仅「设置」高亮', async () => {
    const { wrapper } = await mountShell('/admin/settings')
    expect(activeNavLabels(wrapper)).toEqual(['设置'])
  })

  it('顶栏包含搜索框、当前页面上下文与用户菜单位置', async () => {
    const { wrapper } = await mountShell('/admin/overview')
    expect(wrapper.find('input[aria-label="搜索项目"]').exists()).toBe(true)
    expect(wrapper.find('.admin-shell__context').exists()).toBe(true)
    expect(wrapper.find('.admin-user-menu-stub').exists()).toBe(true)
    // 品牌区链接指向控制台首页
    expect(wrapper.find('.admin-shell__brand').attributes('href')).toBe('/admin/overview')
  })
})

// ---------------------------------------------------------------------------
// 设计系统原子组件
// ---------------------------------------------------------------------------

describe('XIcon：线性图标渲染', () => {
  it('渲染 SVG 线框图标（1.5 stroke、无填充、尺寸可控）', () => {
    const wrapper = mount(XIcon, { props: { name: 'home', size: 20 } })
    const svg = wrapper.find('svg.x-icon')
    expect(svg.exists()).toBe(true)
    expect(svg.attributes('width')).toBe('20')
    expect(svg.attributes('height')).toBe('20')
    expect(svg.attributes('fill')).toBe('none')
    expect(svg.attributes('stroke')).toBe('currentColor')
    expect(svg.attributes('stroke-width')).toBe('1.5')
    expect(svg.attributes('aria-hidden')).toBe('true')
    expect(wrapper.findAll('path').length).toBeGreaterThan(0)
  })

  it('默认尺寸为 16，且不同图标 path 数量不同', () => {
    const menu = mount(XIcon, { props: { name: 'menu' } })
    const grid = mount(XIcon, { props: { name: 'grid' } })
    expect(menu.find('svg').attributes('width')).toBe('16')
    expect(menu.findAll('path').length).toBe(3) // menu：三条横线
    expect(grid.findAll('path').length).toBe(4) // grid：2×2 方格
    expect(grid.findAll('path').length).toBeGreaterThan(menu.findAll('path').length)
  })
})

describe('PageHeader：页面标题区渲染', () => {
  it('渲染 kicker / 标题 / 描述，并通过 actions 插槽承载操作按钮', () => {
    const wrapper = mount(PageHeader, {
      props: {
        kicker: 'WORKSPACE / OVERVIEW',
        title: '总览',
        description: '开发者站点与内容活动概览',
      },
      slots: { actions: '<button type="button" class="page-header-action">新建站点</button>' },
    })
    expect(wrapper.find('.page-header__kicker').text()).toBe('WORKSPACE / OVERVIEW')
    expect(wrapper.find('.page-header__title').text()).toBe('总览')
    expect(wrapper.find('.page-header__desc').text()).toBe('开发者站点与内容活动概览')
    expect(wrapper.find('.page-header__actions .page-header-action').text()).toBe('新建站点')
  })

  it('未传 kicker/description/actions 时不渲染对应节点', () => {
    const wrapper = mount(PageHeader, { props: { title: '模板中心' } })
    expect(wrapper.find('.page-header__title').text()).toBe('模板中心')
    expect(wrapper.find('.page-header__kicker').exists()).toBe(false)
    expect(wrapper.find('.page-header__desc').exists()).toBe(false)
    expect(wrapper.find('.page-header__actions').exists()).toBe(false)
  })
})

// ---------------------------------------------------------------------------
// 项目卡片（ProjectCard）：E2E 依赖的操作文案
// ---------------------------------------------------------------------------

const PROJECT: ProjectView = {
  id: 7,
  name: '星汐边境官方社区',
  slug: 'stellar-drift',
  gameName: '星汐边境',
  description: '面向独立游戏《星汐边境》的玩家社区',
  status: 'PUBLISHED',
  homePageId: 'page_home',
  draftVersion: 3,
  publishedVersion: 2,
  pageCount: 5,
  publishedPageCount: 4,
  createdAt: '2026-10-01T10:00:00',
  updatedAt: '2026-10-26T17:02:12',
}

describe('ProjectCard：站点卡片', () => {
  it('渲染站点名称/游戏名/发布状态与页面进度', () => {
    const wrapper = mount(ProjectCard, { props: { project: PROJECT } })
    expect(wrapper.find('.project-card__title').text()).toBe('星汐边境官方社区')
    expect(wrapper.find('.project-card__game').text()).toContain('星汐边境')
    expect(wrapper.find('.project-card__status').text()).toBe('Published')
    // PAGES 显示 已发布页数/总页数
    expect(wrapper.text()).toContain('4/5')
    expect(wrapper.text()).toContain('/stellar-drift')
  })

  it('操作按钮文案保持「页面管理」「站点发布」「删除站点」（E2E 依赖）', () => {
    const wrapper = mount(ProjectCard, { props: { project: PROJECT } })
    const buttonTexts = wrapper.findAll('button').map((button) => button.text().trim())
    expect(buttonTexts).toContain('页面管理')
    expect(buttonTexts).toContain('站点发布')
    expect(buttonTexts).toContain('删除站点')
    expect(buttonTexts).toContain('设置')
    // 图标按钮用 aria-label 暴露语义
    expect(wrapper.find('button[aria-label="访问公开站点"]').exists()).toBe(true)
    expect(wrapper.find('button[aria-label="编辑站点信息"]').exists()).toBe(true)
  })

  it('点击操作按钮抛出对应事件（open / publish / delete / settings）', async () => {
    const wrapper = mount(ProjectCard, { props: { project: PROJECT } })
    const buttonByText = (text: string) =>
      wrapper.findAll('button').find((button) => button.text().trim() === text)
    await buttonByText('页面管理')?.trigger('click')
    await buttonByText('站点发布')?.trigger('click')
    await buttonByText('设置')?.trigger('click')
    await buttonByText('删除站点')?.trigger('click')
    expect(wrapper.emitted('open')).toHaveLength(1)
    expect(wrapper.emitted('publish')).toHaveLength(1)
    expect(wrapper.emitted('settings')).toHaveLength(1)
    expect(wrapper.emitted('delete')).toHaveLength(1)
  })

  it('草稿站点展示 Draft 状态', () => {
    const draft = mount(ProjectCard, {
      props: { project: { ...PROJECT, status: 'DRAFT', publishedVersion: null, publishedPageCount: 0 } },
    })
    expect(draft.find('.project-card__status').text()).toBe('Draft')
  })
})
