import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * 全局路由（Lead 维护，v2 多页面）。
 * 组件路径与 docs/AGENT_CONTRACT.md §3.5 一致，由 B/C 两个 Agent 实现。
 */
const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/admin/overview' },

  // ---------------- 开发者认证（v3，公开） ----------------
  {
    path: '/login',
    name: 'auth-login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '开发者登录', guestOnly: true },
  },
  {
    path: '/register',
    name: 'auth-register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '开发者注册', guestOnly: true },
  },

  // ---------------- 管理端：控制台 ----------------
  {
    path: '/admin',
    redirect: '/admin/overview',
  },
  {
    path: '/admin/overview',
    name: 'admin-overview',
    component: () => import('@/views/admin/OverviewView.vue'),
    meta: { title: '总览' },
  },
  {
    path: '/admin/templates',
    name: 'admin-templates',
    component: () => import('@/views/admin/TemplatesView.vue'),
    meta: { title: '模板中心' },
  },
  {
    path: '/admin/components',
    name: 'admin-components',
    component: () => import('@/views/admin/ComponentsView.vue'),
    meta: { title: '组件库' },
  },
  {
    path: '/admin/settings',
    name: 'admin-settings',
    component: () => import('@/views/admin/AdminSettingsView.vue'),
    meta: { title: '设置' },
  },

  // ---------------- 管理端：站点与页面 ----------------
  {
    path: '/admin/projects',
    name: 'project-list',
    component: () => import('@/views/admin/ProjectListView.vue'),
    meta: { title: '游戏站点' },
  },
  {
    path: '/admin/projects/new',
    name: 'project-create',
    component: () => import('@/views/admin/ProjectCreateView.vue'),
    meta: { title: '创建游戏站点' },
  },
  {
    path: '/admin/projects/:id/pages',
    name: 'page-list',
    component: () => import('@/views/admin/PageListView.vue'),
    meta: { title: '页面管理' },
  },
  {
    path: '/admin/projects/:id/pages/:pageId/editor',
    name: 'page-editor',
    component: () => import('@/views/admin/EditorView.vue'),
    meta: { title: '可视化编辑器', workspace: true },
  },
  {
    path: '/admin/projects/:id/pages/:pageId/preview',
    name: 'page-preview',
    component: () => import('@/views/admin/PreviewView.vue'),
    meta: { title: '草稿预览', workspace: true },
  },
  {
    path: '/admin/projects/:id/settings',
    name: 'site-settings',
    component: () => import('@/views/admin/SiteSettingsView.vue'),
    meta: { title: '导航与主题' },
  },
  {
    path: '/admin/projects/:id/content/:module',
    name: 'content-manager',
    component: () => import('@/views/admin/ContentManagerView.vue'),
    props: true,
    meta: { title: '内容管理' },
  },
  // v1 旧路由重定向（兼容旧链接）
  {
    path: '/admin/projects/:id/editor',
    redirect: (to) => ({ name: 'page-list', params: { id: to.params.id } }),
  },
  {
    path: '/admin/projects/:id/preview',
    redirect: (to) => ({ name: 'page-list', params: { id: to.params.id } }),
  },

  // ---------------- 公开端：游戏门户 ----------------
  {
    path: '/community',
    name: 'community-gallery',
    component: () => import('@/views/public/CommunityGalleryView.vue'),
    meta: { title: '游戏门户广场' },
  },
  {
    path: '/community/:slug',
    name: 'community-home',
    component: () => import('@/views/public/CommunityPageView.vue'),
    meta: { title: '游戏门户' },
  },
  {
    path: '/community/:slug/p/:pageSlug',
    name: 'community-page',
    component: () => import('@/views/public/PublicPageView.vue'),
    meta: { title: '游戏门户' },
  },
  {
    path: '/community/:slug/changelog',
    name: 'community-changelog',
    component: () => import('@/views/public/ChangelogListView.vue'),
    meta: { title: '更新日志' },
  },
  {
    path: '/community/:slug/changelog/:entryId',
    name: 'community-changelog-detail',
    component: () => import('@/views/public/ChangelogDetailView.vue'),
    meta: { title: '更新日志详情' },
  },
  {
    path: '/community/:slug/docs',
    name: 'community-docs',
    component: () => import('@/views/public/DocsListView.vue'),
    meta: { title: '公开文档' },
  },
  {
    path: '/community/:slug/docs/:docSlug',
    name: 'community-doc-detail',
    component: () => import('@/views/public/DocDetailView.vue'),
    meta: { title: '文档详情' },
  },
  {
    path: '/community/:slug/progress',
    name: 'page-progress',
    component: () => import('@/views/public/ProgressListView.vue'),
    meta: { title: '问题处理进展' },
  },
  {
    path: '/community/:slug/news/:announcementId',
    name: 'page-news-detail',
    component: () => import('@/views/public/AnnouncementDetailView.vue'),
    meta: { title: '公告详情' },
  },
  {
    path: '/community/:slug/team',
    name: 'community-team',
    component: () => import('@/views/public/TeamView.vue'),
    meta: { title: '制作团队' },
  },
  {
    path: '/community/:slug/report',
    name: 'community-report',
    component: () => import('@/views/public/IssueReportView.vue'),
    meta: { title: '问题汇报' },
  },
  {
    path: '/community/:slug/links',
    name: 'community-links',
    component: () => import('@/views/public/LinksView.vue'),
    meta: { title: '友情链接' },
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    if (to.path !== from.path) return { top: 0 }
    return {}
  },
})

/**
 * v3 登录守卫：/admin/** 需要登录；已登录用户访问登录/注册页时回到管理端。
 * 首次导航前 await auth.init()，避免刷新后误判未登录（Pinia store，读到的是解包后的状态值）。
 */
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.init()
  const requiresAuth = to.path.startsWith('/admin')
  if (requiresAuth && !auth.isAuthenticated) {
    return { name: 'auth-login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'project-list' }
  }
  return true
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : ''
  document.title = title ? `${title} · GameCommunityCreator` : 'GameCommunityCreator'
})

export default router
