/**
 * 社区运行时（runtime/CommunityRuntime.vue）契约测试（v2）。
 *
 * 依据：
 * - docs/AGENT_CONTRACT.md §3.6：props { config, loader, mode, site }
 * - docs/PAGE_SCHEMA.md §4/§5：按数组顺序渲染、11 种组件、7 个数据源去重、
 *   四态、visible=false 不渲染不请求、按钮动作经统一 executeAction 执行
 *
 * 全部使用假 loader（mock 数据，不访问网络、不依赖后端），并注入内存路由，
 * 因此同时充当「取数只发生在 CommunityRuntime 内」「动作只经统一入口」的回归保护。
 *
 * 依赖的、无副作用的 DOM 测试钩子（不改变对外契约）：
 * - 根容器 data-testid="community-runtime"，主题 CSS 变量与 data-theme 挂根容器
 * - 每个组件 wrapper：data-component-id / data-component-type / data-status
 * - loading 文案含「加载」、empty 文案含「暂无」、error 有「重试」按钮
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { COMPONENT_DEFS } from '@/components/registry'
import CommunityRuntime from '@/runtime/CommunityRuntime.vue'
import { resetToasts } from '@/runtime/toast'
import {
  DEFAULT_THEME,
  type ComponentType,
  type PageComponent,
  type PageConfig,
  type ThemeConfig,
} from '@/types/page'
import type { Announcement, GameInfo } from '@/types/api'
import type { ChangelogView, DocView, FriendLinkView, TeamMemberView } from '@/types/content'
import type { RuntimeDataLoader } from '@/types/runtime'
import type { SiteRuntimeContext } from '@/types/site'

// ---------------------------------------------------------------------------
// fixtures
// ---------------------------------------------------------------------------

const GAME_INFO: GameInfo = {
  projectId: 1,
  name: '星汐边境',
  tagline: '在星海尽头，建造属于你的边境站',
  description: '《星汐边境》是一款以星际拓荒为背景的模拟经营游戏。',
  coverUrl: '',
  platforms: 'PC / Switch',
  releaseStatus: 'EA',
}

const ANNOUNCEMENTS: Announcement[] = [
  {
    id: 1,
    title: '社区开张公告',
    content: '欢迎来到星汐边境玩家社区！',
    pinned: true,
    publishedAt: '2025-04-20T09:00:00',
  },
  {
    id: 2,
    title: '版本更新说明',
    content: '新增边境站建造玩法。',
    pinned: false,
    publishedAt: '2025-04-22T10:00:00',
  },
]

const CHANGELOGS: ChangelogView[] = [
  {
    id: 1,
    version: 'v0.9.2',
    title: '潮汐回响版本更新',
    content: '本次更新内容。',
    releaseDate: '2025-05-06',
    status: 'PUBLISHED',
    createdAt: '2025-05-06T10:00:00',
    updatedAt: '2025-05-06T10:00:00',
  },
]

const DOCS: DocView[] = [
  {
    id: 1,
    title: '入门指南',
    category: '上手',
    content: '从零开始。',
    slug: 'getting-started',
    status: 'PUBLISHED',
    createdAt: '2025-05-01T10:00:00',
    updatedAt: '2025-05-01T10:00:00',
  },
]

const TEAM: TeamMemberView[] = [
  { id: 1, name: '林澈', avatarUrl: '', role: '制作人', bio: '负责整体设计', sortOrder: 0 },
]

const LINKS: FriendLinkView[] = [
  { id: 1, name: 'IndieDB', url: 'https://www.indiedb.com', description: '独立游戏社区', sortOrder: 0, enabled: true },
]

/** 测试用主题：与默认主题不同，便于断言 CSS 变量真的来自配置 */
const TEST_THEME: ThemeConfig = {
  ...DEFAULT_THEME,
  primaryColor: '#13579B',
  accentColor: '#2468AC',
  borderRadius: 8,
}

/** 站点运行时上下文：按钮动作解析用（含已发布页面注册表） */
const SITE: SiteRuntimeContext = {
  projectId: 1,
  projectSlug: 'stellar-drift',
  pages: [
    { pageId: 'page_home_1', name: '首页', slug: 'home', pageType: 'home' },
    { pageId: 'page_changelog_1', name: '更新日志', slug: 'changelog', pageType: 'changelog' },
    { pageId: 'page_docs_1', name: '公开文档', slug: 'docs', pageType: 'docs' },
  ],
  themeMode: 'DARK',
}

function makeComponent(type: ComponentType, patch: Partial<PageComponent> = {}): PageComponent {
  const def = COMPONENT_DEFS[type]
  const component: PageComponent = {
    id: `cmp_test_${type}`,
    type,
    title: def.defaultTitle,
    visible: true,
    dataSource: def.dataSource,
    props: { ...def.defaultProps },
    ...patch,
  }
  if (def.supportsAction && component.action === undefined) {
    component.action = { type: 'navigate', targetPageId: 'page_changelog_1' }
  }
  return component
}

/** 默认 5 组件配置：hero → about → notice_list → changelog_list → footer */
function makeConfig(patch: Partial<PageConfig> = {}): PageConfig {
  return {
    schemaVersion: 2,
    pageId: 'page_home_1',
    pageTitle: '星汐边境 官方社区',
    projectSlug: 'stellar-drift',
    theme: { ...TEST_THEME },
    components: [
      makeComponent('hero', {
        props: { ...COMPONENT_DEFS.hero.defaultProps, subheadline: '欢迎来到星汐边境玩家社区' },
      }),
      makeComponent('about'),
      makeComponent('notice_list'),
      makeComponent('changelog_list'),
      makeComponent('footer', {
        props: { copyright: '© 2025 星汐边境 社区', note: '由 GameCommunityCreator 搭建' },
      }),
    ],
    ...patch,
  }
}

interface FakeLoader extends RuntimeDataLoader {
  loadGameInfo: ReturnType<typeof vi.fn>
  loadAnnouncements: ReturnType<typeof vi.fn>
  loadChangelogs: ReturnType<typeof vi.fn>
  loadDocs: ReturnType<typeof vi.fn>
  loadTeam: ReturnType<typeof vi.fn>
  loadLinks: ReturnType<typeof vi.fn>
  loadIssueProgress: ReturnType<typeof vi.fn>
}

function createLoader(
  options: {
    gameInfo?: GameInfo
    announcements?: Announcement[]
    changelogs?: ChangelogView[]
    docs?: DocView[]
    team?: TeamMemberView[]
    links?: FriendLinkView[]
    failAnnouncements?: boolean
  } = {},
): FakeLoader {
  return {
    loadGameInfo: vi.fn(async () => options.gameInfo ?? GAME_INFO),
    loadAnnouncements: vi.fn(async () => {
      if (options.failAnnouncements) throw new Error('公告接口暂时不可用')
      return options.announcements ?? ANNOUNCEMENTS
    }),
    loadChangelogs: vi.fn(async () => options.changelogs ?? CHANGELOGS),
    loadDocs: vi.fn(async () => options.docs ?? DOCS),
    loadTeam: vi.fn(async () => options.team ?? TEAM),
    loadLinks: vi.fn(async () => options.links ?? LINKS),
    loadIssueProgress: vi.fn(async () => []),
  }
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

type Mounted = { wrapper: ReturnType<typeof mount>; router: ReturnType<typeof createRouter> }

async function mountRuntime(
  config: PageConfig,
  loader: RuntimeDataLoader,
  options: { mode?: 'editor' | 'preview' | 'public'; site?: SiteRuntimeContext } = {},
): Promise<Mounted> {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }],
  })
  await router.push('/')
  await router.isReady()
  const wrapper = mount(CommunityRuntime, {
    props: {
      config,
      loader,
      mode: options.mode ?? 'public',
      site: options.site ?? SITE,
    },
    global: { plugins: [router] },
  })
  return { wrapper, router }
}

// ---------------------------------------------------------------------------
// DOM 查询辅助
// ---------------------------------------------------------------------------

function root(wrapper: Mounted['wrapper']) {
  const node = wrapper.find('[data-testid="community-runtime"]')
  expect(node.exists(), '运行时根容器应带 data-testid="community-runtime"').toBe(true)
  return node
}

function componentTypesInOrder(wrapper: Mounted['wrapper']): string[] {
  return wrapper.findAll('[data-component-type]').map((node) => node.attributes('data-component-type') ?? '')
}

function componentNode(wrapper: Mounted['wrapper'], type: ComponentType) {
  return wrapper.find(`[data-component-type="${type}"]`)
}

function statusOf(wrapper: Mounted['wrapper'], type: ComponentType): string | undefined {
  const node = componentNode(wrapper, type)
  expect(node.exists(), `应渲染组件类型 ${type}`).toBe(true)
  return node.attributes('data-status')
}

function bodyHasText(text: string): boolean {
  return document.body.textContent?.includes(text) ?? false
}

beforeEach(() => {
  resetToasts()
})

afterEach(() => {
  vi.restoreAllMocks()
  resetToasts()
})

// ---------------------------------------------------------------------------
// 渲染顺序 / 主题 / 真实数据
// ---------------------------------------------------------------------------

describe('运行时：渲染顺序、主题与数据', () => {
  it('按 config.components 数组顺序渲染全部 5 个默认模板组件', async () => {
    const { wrapper } = await mountRuntime(makeConfig(), createLoader())
    await flushPromises()

    expect(componentTypesInOrder(wrapper)).toEqual([
      'hero',
      'about',
      'notice_list',
      'changelog_list',
      'footer',
    ])
  })

  it('组件标题按契约渲染（footer 不展示标题），文本顺序与配置一致', async () => {
    const { wrapper } = await mountRuntime(makeConfig(), createLoader())
    await flushPromises()
    const text = wrapper.text()

    expect(text).toContain(COMPONENT_DEFS.about.defaultTitle)
    expect(text).toContain(COMPONENT_DEFS.notice_list.defaultTitle)
    expect(text).toContain(COMPONENT_DEFS.changelog_list.defaultTitle)
    expect(text).not.toContain(COMPONENT_DEFS.footer.defaultTitle)

    const order = [
      '欢迎来到星汐边境玩家社区',
      COMPONENT_DEFS.about.defaultTitle,
      COMPONENT_DEFS.notice_list.defaultTitle,
      COMPONENT_DEFS.changelog_list.defaultTitle,
      '© 2025 星汐边境 社区',
    ].map((marker) => text.indexOf(marker))
    expect(order.every((index) => index >= 0), `缺少文本标记：${JSON.stringify(order)}`).toBe(true)
    expect(order).toEqual([...order].sort((a, b) => a - b))
  })

  it('主题配置 → 根容器 CSS 变量（primary/accent/radius 来自 config.theme）', async () => {
    const { wrapper } = await mountRuntime(makeConfig(), createLoader())
    await flushPromises()

    const rootNode = root(wrapper)
    const style = rootNode.attributes('style') ?? ''
    expect(style).toContain(`--gcc-primary: ${TEST_THEME.primaryColor}`)
    expect(style).toContain(`--gcc-accent: ${TEST_THEME.accentColor}`)
    expect(style).toContain('--gcc-radius: 8px')
  })

  it('站点 themeMode=LIGHT 时根容器 data-theme=light，品牌色仍保留（U9）', async () => {
    const { wrapper } = await mountRuntime(makeConfig(), createLoader(), {
      site: { ...SITE, themeMode: 'LIGHT' },
    })
    await flushPromises()

    const rootNode = root(wrapper)
    expect(rootNode.attributes('data-theme')).toBe('light')
    const style = rootNode.attributes('style') ?? ''
    expect(style).toContain(`--gcc-primary: ${TEST_THEME.primaryColor}`)
    expect(style).toContain('--gcc-bg: #f4f6fb')
  })

  it('渲染真实数据：游戏信息、公告、更新日志（来自 loader，非写死）', async () => {
    const { wrapper } = await mountRuntime(makeConfig(), createLoader())
    await flushPromises()
    const text = wrapper.text()

    expect(text).toContain(GAME_INFO.name)
    expect(text).toContain(GAME_INFO.tagline)
    expect(text).toContain(GAME_INFO.description)
    for (const announcement of ANNOUNCEMENTS) expect(text).toContain(announcement.title)
    for (const entry of CHANGELOGS) {
      expect(text).toContain(entry.title)
      expect(text).toContain(entry.version)
    }
  })
})

// ---------------------------------------------------------------------------
// 11 种组件 / 7 个数据源（v2）
// ---------------------------------------------------------------------------

describe('运行时 v2：11 种组件与 7 个数据源', () => {
  const ALL_TYPES: ComponentType[] = [
    'hero',
    'about',
    'notice_list',
    'issue_progress',
    'footer',
    'button',
    'changelog_list',
    'docs_list',
    'team_grid',
    'issue_form',
    'links_grid',
  ]

  it('11 种组件全部可渲染且顺序与配置一致', async () => {
    const config = makeConfig({ components: ALL_TYPES.map((type) => makeComponent(type)) })
    const { wrapper } = await mountRuntime(config, createLoader())
    await flushPromises()

    expect(componentTypesInOrder(wrapper)).toEqual(ALL_TYPES)
    // 数据组件处于 ready；无数据源组件（footer/button/issue_form）状态为 ready
    for (const type of ['changelog_list', 'docs_list', 'team_grid', 'links_grid'] as ComponentType[]) {
      expect(statusOf(wrapper, type)).toBe('ready')
    }
  })

  it('7 个数据源各请求一次；同源多组件去重（hero+about 共用 game_info）', async () => {
    const loader = createLoader()
    const config = makeConfig({
      components: [
        makeComponent('hero'),
        makeComponent('about'),
        makeComponent('notice_list'),
        makeComponent('issue_progress'),
        makeComponent('changelog_list'),
        makeComponent('docs_list'),
        makeComponent('team_grid'),
        makeComponent('links_grid'),
        makeComponent('footer'),
      ],
    })
    const { wrapper } = await mountRuntime(config, loader)
    await flushPromises()

    expect(loader.loadGameInfo).toHaveBeenCalledTimes(1)
    expect(loader.loadAnnouncements).toHaveBeenCalledTimes(1)
    expect(loader.loadChangelogs).toHaveBeenCalledTimes(1)
    expect(loader.loadDocs).toHaveBeenCalledTimes(1)
    expect(loader.loadTeam).toHaveBeenCalledTimes(1)
    expect(loader.loadLinks).toHaveBeenCalledTimes(1)
    expect(loader.loadIssueProgress).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain(CHANGELOGS[0].title)
    expect(wrapper.text()).toContain(DOCS[0].title)
    expect(wrapper.text()).toContain(TEAM[0].name)
    expect(wrapper.text()).toContain(LINKS[0].name)
  })

  it('docs 按 category 分组去重：同分类只请求一次，不同分类各自一次', async () => {
    const loader = createLoader()
    const config = makeConfig({
      components: [
        makeComponent('docs_list', {
          id: 'cmp_docs_all_1',
          props: { ...COMPONENT_DEFS.docs_list.defaultProps, category: '' },
        }),
        makeComponent('docs_list', {
          id: 'cmp_docs_all_2',
          props: { ...COMPONENT_DEFS.docs_list.defaultProps, category: '' },
        }),
        makeComponent('docs_list', {
          id: 'cmp_docs_guide_1',
          props: { ...COMPONENT_DEFS.docs_list.defaultProps, category: '上手' },
        }),
      ],
    })
    const { wrapper } = await mountRuntime(config, loader)
    await flushPromises()

    expect(loader.loadDocs).toHaveBeenCalledTimes(2)
    const categories = loader.loadDocs.mock.calls.map((call) => call[1]).sort()
    expect(categories).toEqual(['', '上手'])
    expect(wrapper.findAll('[data-component-type="docs_list"]')).toHaveLength(3)
  })

  it('footer / button / issue_form 不声明数据源：渲染但零请求', async () => {
    const loader = createLoader()
    const config = makeConfig({
      components: [makeComponent('footer'), makeComponent('button'), makeComponent('issue_form')],
    })
    const { wrapper } = await mountRuntime(config, loader)
    await flushPromises()

    expect(componentTypesInOrder(wrapper)).toEqual(['footer', 'button', 'issue_form'])
    for (const key of [
      'loadGameInfo',
      'loadAnnouncements',
      'loadChangelogs',
      'loadDocs',
      'loadTeam',
      'loadLinks',
      'loadIssueProgress',
    ] as const) {
      expect(loader[key], key).not.toHaveBeenCalled()
    }
  })

  it('未知组件类型渲染兜底提示而不是整页崩溃', async () => {
    const config = makeConfig({
      components: [{ ...makeComponent('footer'), type: 'iframe' as ComponentType }],
    })
    const { wrapper } = await mountRuntime(config, createLoader())
    await flushPromises()
    expect(wrapper.text()).toContain('不支持的组件类型')
  })
})

// ---------------------------------------------------------------------------
// 按钮动作（唯一执行入口 executeAction）
// ---------------------------------------------------------------------------

describe('运行时 v2：按钮动作执行', () => {
  function buttonConfig(action: PageComponent['action']): PageConfig {
    return makeConfig({ components: [makeComponent('button', { action })] })
  }

  it('navigate：公开模式点击后跳转到目标页面公开路由', async () => {
    const { wrapper, router } = await mountRuntime(
      buttonConfig({ type: 'navigate', targetPageId: 'page_changelog_1' }),
      createLoader(),
    )
    await flushPromises()

    await wrapper.find('.runtime-button button').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/community/stellar-drift/changelog')
  })

  it('navigate：目标不在已发布注册表 → 不跳转并提示', async () => {
    const { wrapper, router } = await mountRuntime(
      buttonConfig({ type: 'navigate', targetPageId: 'page_notexist_1' }),
      createLoader(),
    )
    await flushPromises()

    await wrapper.find('.runtime-button button').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/')
    expect(bodyHasText('目标页面不存在或尚未发布')).toBe(true)
  })

  it('externalLink：合法 URL → window.open 带 noopener,noreferrer', async () => {
    const open = vi.spyOn(window, 'open').mockImplementation(() => null)
    const { wrapper } = await mountRuntime(
      buttonConfig({ type: 'externalLink', url: 'https://example.com/join', openInNewTab: true }),
      createLoader(),
    )
    await flushPromises()

    await wrapper.find('.runtime-button button').trigger('click')
    expect(open).toHaveBeenCalledWith('https://example.com/join', '_blank', 'noopener,noreferrer')
  })

  it('externalLink：非法 URL → 阻止打开并提示', async () => {
    const open = vi.spyOn(window, 'open').mockImplementation(() => null)
    const { wrapper } = await mountRuntime(
      buttonConfig({ type: 'externalLink', url: 'javascript:alert(1)', openInNewTab: true }),
      createLoader(),
    )
    await flushPromises()

    await wrapper.find('.runtime-button button').trigger('click')
    expect(open).not.toHaveBeenCalled()
    expect(bodyHasText('外部链接不合法')).toBe(true)
  })

  it('none：按钮渲染为禁用并提示未配置动作', async () => {
    const { wrapper } = await mountRuntime(buttonConfig({ type: 'none' }), createLoader())
    await flushPromises()

    const button = wrapper.find('.runtime-button button')
    expect(button.attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('该按钮尚未配置动作')
  })

  it('编辑模式点击：不执行跳转并提示', async () => {
    const { wrapper, router } = await mountRuntime(
      buttonConfig({ type: 'navigate', targetPageId: 'page_changelog_1' }),
      createLoader(),
      { mode: 'editor' },
    )
    await flushPromises()

    await wrapper.find('.runtime-button button').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/')
    expect(bodyHasText('编辑模式')).toBe(true)
  })
})

// ---------------------------------------------------------------------------
// 四态：loading / ready / empty / error + 重试
// ---------------------------------------------------------------------------

describe('运行时：四种加载状态', () => {
  it('loading → ready：数据返回后组件状态切换且不再显示加载态', async () => {
    const gameInfoDeferred = deferred<GameInfo>()
    const announcementsDeferred = deferred<Announcement[]>()
    const changelogsDeferred = deferred<ChangelogView[]>()
    const loader: RuntimeDataLoader = {
      loadGameInfo: vi.fn(() => gameInfoDeferred.promise),
      loadAnnouncements: vi.fn(() => announcementsDeferred.promise),
      loadChangelogs: vi.fn(() => changelogsDeferred.promise),
      loadDocs: vi.fn(async () => DOCS),
      loadTeam: vi.fn(async () => TEAM),
      loadLinks: vi.fn(async () => LINKS),
      loadIssueProgress: vi.fn(async () => []),
    }

    const { wrapper } = await mountRuntime(makeConfig(), loader)
    expect(statusOf(wrapper, 'notice_list')).toBe('loading')
    expect(statusOf(wrapper, 'changelog_list')).toBe('loading')
    expect(wrapper.text()).toContain('加载')

    gameInfoDeferred.resolve(GAME_INFO)
    announcementsDeferred.resolve(ANNOUNCEMENTS)
    changelogsDeferred.resolve(CHANGELOGS)
    await flushPromises()

    expect(statusOf(wrapper, 'notice_list')).toBe('ready')
    expect(statusOf(wrapper, 'changelog_list')).toBe('ready')
    expect(wrapper.text()).toContain(ANNOUNCEMENTS[0].title)
    expect(wrapper.text()).toContain(CHANGELOGS[0].title)
  })

  it('empty：数据为空时显示空态文案（不是 error）', async () => {
    const loader = createLoader({ announcements: [], changelogs: [] })
    const { wrapper } = await mountRuntime(makeConfig(), loader)
    await flushPromises()

    expect(statusOf(wrapper, 'notice_list')).toBe('empty')
    expect(statusOf(wrapper, 'changelog_list')).toBe('empty')
    expect(wrapper.text()).toContain('暂无')
  })

  it('error：加载失败显示错误与「重试」按钮；点击重试后恢复 ready', async () => {
    const loader = createLoader({ failAnnouncements: true })
    const { wrapper } = await mountRuntime(makeConfig(), loader)
    await flushPromises()

    expect(statusOf(wrapper, 'notice_list')).toBe('error')
    const noticeNode = componentNode(wrapper, 'notice_list')
    const retryButton = noticeNode.find('button')
    expect(retryButton.exists(), 'error 态应渲染重试按钮').toBe(true)
    expect(retryButton.text()).toContain('重试')
    expect(statusOf(wrapper, 'changelog_list')).toBe('ready')

    loader.loadAnnouncements.mockResolvedValue(ANNOUNCEMENTS)
    await retryButton.trigger('click')
    await flushPromises()

    expect(statusOf(wrapper, 'notice_list')).toBe('ready')
    expect(wrapper.text()).toContain(ANNOUNCEMENTS[0].title)
  })
})

// ---------------------------------------------------------------------------
// 可见性 / 取数规则
// ---------------------------------------------------------------------------

describe('运行时：可见性与取数规则', () => {
  it('visible=false 的组件不渲染、也不触发其数据源请求', async () => {
    const loader = createLoader()
    const config = makeConfig()
    config.components[2] = { ...config.components[2], visible: false } // notice_list 隐藏

    const { wrapper } = await mountRuntime(config, loader)
    await flushPromises()

    expect(componentNode(wrapper, 'notice_list').exists()).toBe(false)
    expect(loader.loadAnnouncements).not.toHaveBeenCalled()
    expect(loader.loadGameInfo).toHaveBeenCalled()
    expect(loader.loadChangelogs).toHaveBeenCalled()
  })

  it('同一数据源只请求一次（hero + about 共用 game_info，两个公告组件共用 announcements）', async () => {
    const loader = createLoader()
    const config = makeConfig()
    config.components.push(
      makeComponent('notice_list', { id: 'cmp_test_notice_second', title: '公告列表（第二处）' }),
    )

    const { wrapper } = await mountRuntime(config, loader)
    await flushPromises()

    expect(loader.loadGameInfo).toHaveBeenCalledTimes(1)
    expect(loader.loadAnnouncements).toHaveBeenCalledTimes(1)
    expect(loader.loadChangelogs).toHaveBeenCalledTimes(1)
    expect(wrapper.findAll('[data-component-type="notice_list"]').length).toBe(2)
    expect(wrapper.text()).toContain(ANNOUNCEMENTS[0].title)
  })

  it('footer 不声明数据源：渲染但不发任何请求', async () => {
    const loader = createLoader()
    const config = makeConfig({ components: [makeComponent('footer')] })

    const { wrapper } = await mountRuntime(config, loader)
    await flushPromises()

    expect(componentTypesInOrder(wrapper)).toEqual(['footer'])
    expect(wrapper.text()).toContain('GameCommunityCreator')
    expect(loader.loadGameInfo).not.toHaveBeenCalled()
    expect(loader.loadAnnouncements).not.toHaveBeenCalled()
    expect(loader.loadChangelogs).not.toHaveBeenCalled()
  })
})
