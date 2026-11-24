/**
 * 页脚唯一性（v3 / FIX-1）测试。
 *
 * 通过 mock 公开页数据（usePublicPageData）挂载 PublicPageShell，断言：
 * - 页面配置存在可见 footer 组件 → 由配置 Footer 渲染，布局页脚关闭；全页 footer 数量 = 1；
 * - 页面配置没有 footer 组件 → 布局页脚兜底；数量 = 1；
 * - footer 组件 visible=false → 布局页脚兜底；数量 = 1；
 * - 固定详情页（默认插槽）→ 布局页脚；数量 = 1。
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it, vi } from 'vitest'

interface MockPublicState {
  config: { value: unknown }
  detail: { value: unknown }
  loading: { value: boolean }
  notFound: { value: boolean }
  errorMessage: { value: string }
  missingText: { value: string }
  theme: { value: unknown }
  loader: { value: Record<string, unknown> }
  site: { value: unknown }
  reload: () => void
}

const holder = vi.hoisted(() => ({ state: null as MockPublicState | null }))

vi.mock('@/runtime/usePublicPageData', async () => {
  const { ref } = await import('vue')
  const state: MockPublicState = {
    config: ref(null),
    detail: ref(null),
    loading: ref(false),
    notFound: ref(false),
    errorMessage: ref(''),
    missingText: ref('该页面不存在或尚未发布'),
    theme: ref(null),
    loader: ref({}),
    site: ref({ projectId: 1, projectSlug: 'demo-site', pages: [], themeMode: 'DARK' }),
    reload: () => {},
  }
  holder.state = state
  return { usePublicPageData: () => state }
})

import PublicPageShell from '@/runtime/PublicPageShell.vue'

const THEME = {
  primaryColor: '#5B8CFF',
  accentColor: '#9B5CFF',
  backgroundColor: '#0B1020',
  surfaceColor: '#151B33',
  textColor: '#E8ECFF',
  mutedTextColor: '#9AA6CC',
  fontFamily: 'system',
  borderRadius: 16,
}

function footerComponent(visible = true) {
  return {
    id: 'cmp_footer_test',
    type: 'footer',
    title: '页脚',
    visible,
    dataSource: null,
    props: { copyright: '© 冒烟测试社区', note: '页脚唯一性测试' },
  }
}

function pageConfig(components: unknown[]) {
  return {
    schemaVersion: 2,
    pageId: 'page_home_1',
    pageTitle: '冒烟测试门户',
    projectSlug: 'demo-site',
    theme: THEME,
    components,
  }
}

function publicDetail(config: unknown) {
  return {
    project: { id: 1, name: '冒烟测试站点', slug: 'demo-site', gameName: '冒烟游戏', description: '描述' },
    page: {
      pageId: 'page_home_1',
      name: '首页',
      slug: 'home',
      pageType: 'home',
      configVersion: 1,
      publishedAt: '2025-05-01T10:00:00',
      config,
    },
    pages: [],
    navigation: [],
    settings: { themeMode: 'DARK' },
  }
}

function makeLoader() {
  const fn = vi.fn(async () => [])
  return {
    loadGameInfo: vi.fn(async () => ({
      name: '冒烟游戏',
      tagline: '',
      description: '',
      coverUrl: '',
      platforms: '',
      releaseStatus: '',
    })),
    loadAnnouncements: fn,
    loadChangelogs: fn,
    loadDocs: fn,
    loadTeam: fn,
    loadLinks: fn,
    loadIssueProgress: fn,
  }
}

async function mountShell(config: unknown, options: { slot?: boolean } = {}) {
  const state = holder.state
  if (!state) throw new Error('usePublicPageData mock 未初始化')
  state.config.value = config
  state.detail.value = publicDetail(config)
  state.loading.value = false
  state.notFound.value = false
  state.errorMessage.value = ''
  state.theme.value = THEME
  state.loader.value = makeLoader()

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }],
  })
  await router.push('/community/demo-site')
  await router.isReady()

  const slots = options.slot ? { default: '<div class="custom-content">固定详情页内容</div>' } : {}
  const wrapper = mount(PublicPageShell, {
    props: { slug: 'demo-site' },
    slots,
    global: { plugins: [router] },
  })
  await flushPromises()
  return wrapper
}

function footerCount(wrapper: ReturnType<typeof mount>): number {
  return wrapper.findAll('footer').length
}

describe('页脚唯一性（FIX-1）', () => {
  it('页面配置含可见 footer 组件 → 只渲染配置页脚（布局页脚关闭），数量 = 1', async () => {
    const wrapper = await mountShell(pageConfig([footerComponent(true)]))
    expect(footerCount(wrapper)).toBe(1)
    expect(wrapper.text()).toContain('© 冒烟测试社区')
  })

  it('页面配置没有 footer 组件 → 布局页脚兜底，数量 = 1', async () => {
    const wrapper = await mountShell(
      pageConfig([
        { id: 'cmp_about', type: 'about', title: '简介', visible: true, dataSource: 'game_info', props: {} },
      ]),
    )
    expect(footerCount(wrapper)).toBe(1)
  })

  it('footer 组件 visible=false → 视为没有页脚组件，布局兜底，数量 = 1', async () => {
    const wrapper = await mountShell(pageConfig([footerComponent(false)]))
    expect(footerCount(wrapper)).toBe(1)
  })

  it('固定详情页（默认插槽）→ 使用布局页脚，配置 footer 不参与渲染，数量 = 1', async () => {
    const wrapper = await mountShell(pageConfig([footerComponent(true)]), { slot: true })
    expect(wrapper.find('.custom-content').exists()).toBe(true)
    expect(footerCount(wrapper)).toBe(1)
  })
})
