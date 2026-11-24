#!/usr/bin/env node
/**
 * GameCommunityCreator-MVP（v3）—— 真实浏览器端到端验收脚本
 *
 * 覆盖本次修复任务的四项验收（FIX-1..FIX-4）与 v2 核心回归：
 *   未登录跳转登录 → 注册开发者 A（自动登录）→ 我的游戏（空）→ 创建站点(6 页面)
 *   → 三栏编辑器默认模板/按钮动作/保存/刷新恢复 → 管理端主题切换+刷新持久化+门户主题不受影响
 *   → 站点发布 → 公开首页页脚唯一/导航/主题 → 模块页/文档详情页脚唯一
 *   → Markdown 文档创建（编辑/预览）→ 保存 → 发布 → 公开详情 Markdown 排版 + XSS 清洗
 *   → 退出登录 → 注册开发者 B（我的游戏为空）→ B 直接访问 A 的项目被拒(403)
 *   → 匿名可访问 A 的公开站点并提交问题反馈
 *   → JWT 失效跳登录提示 → dev 账号登录后 seed 旧项目可编辑/保存/发布/公开访问
 *   → 清理测试站点 → 控制台无异常
 *
 * 前置：
 *   1) MySQL 已初始化（database/schema.sql + seed.sql，迁移 003）；
 *   2) 后端 8080 以 `APP_BOOTSTRAP_USERNAME=dev`、`APP_BOOTSTRAP_PASSWORD=DevPass2026!` 启动；
 *   3) 前端生产构建 + vite preview 在 4173（或 dev 3000）。
 *
 * 用法：
 *   node scripts/e2e-browser.mjs
 *   E2E_BASE_URL=http://localhost:4173 node scripts/e2e-browser.mjs
 *   E2E_BOOTSTRAP_USERNAME=dev E2E_BOOTSTRAP_PASSWORD=DevPass2026! node scripts/e2e-browser.mjs
 *
 * 产物：.e2e-artifacts/ 下截图与 summary.json；退出码 0=全部通过。
 */
import { createRequire } from 'node:module'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join, resolve } from 'node:path'

const requireFromFrontend = createRequire(new URL('../frontend/package.json', import.meta.url))
const { chromium } = requireFromFrontend('playwright-core')

const BASE_URL = (process.env.E2E_BASE_URL ?? 'http://localhost:4173').replace(/\/+$/, '')
const API_URL = (process.env.E2E_API_URL ?? 'http://127.0.0.1:8080/api').replace(/\/+$/, '')
const CHROME_PATH =
  process.env.CHROME_PATH ?? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const BOOTSTRAP_USERNAME = process.env.E2E_BOOTSTRAP_USERNAME ?? 'dev'
const BOOTSTRAP_PASSWORD = process.env.E2E_BOOTSTRAP_PASSWORD ?? 'DevPass2026!'
const ARTIFACTS = resolve('.e2e-artifacts')
mkdirSync(ARTIFACTS, { recursive: true })

const RUN_ID = Date.now().toString(36)
const SITE_NAME = `浏览器验收站点 ${RUN_ID}`
const SLUG = `e2e-${RUN_ID}`
const GAME_NAME = `浏览器验收游戏 ${RUN_ID}`
const DESCRIPTION = '由 scripts/e2e-browser.mjs 自动创建，验收结束会删除'
const USER_A = { username: `e2ea${RUN_ID}`, displayName: '验收开发者 A', password: 'E2ePass123' }
const USER_B = { username: `e2eb${RUN_ID}`, displayName: '验收开发者 B', password: 'E2ePass123' }
const DEFAULT_PAGE_TITLES = ['首页', '更新日志', '公开文档', '制作团队', '问题反馈', '友情链接']
const BUTTON_LABEL = '查看更新日志（E2E）'
const PNG_1X1_BASE64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='
const DIALOG_TITLE = `P0 弹窗稳定性 ${RUN_ID}`
const DIALOG_SLUG = `dialog-e2e-${RUN_ID}`
const UPLOAD_DOC_TITLE = `图片上传验收 ${RUN_ID}`
const UPLOAD_DOC_SLUG = `upload-e2e-${RUN_ID}`
const DOC_TITLE = `Markdown 验收文档 ${RUN_ID}`
const DOC_SLUG = `md-e2e-${RUN_ID}`
const LEGACY_COMPONENT_TITLE = `社区公告（E2E 旧项目保存验证 ${RUN_ID}）`
const DOC_MARKDOWN = [
  '## 快速开始',
  '',
  '这是一段 **Markdown** 正文，包含 `行内代码`、[安全链接](https://example.com) 和 ~~删除线~~。',
  '',
  '- 列表项一',
  '- 列表项二',
  '',
  '> 引用：正文使用 Markdown 渲染，公开端经严格安全清洗。',
  '',
  '```js',
  'console.log("hello markdown")',
  '```',
  '',
  '| 名称 | 说明 |',
  '| --- | --- |',
  '| 页面 | 配置驱动 |',
  '',
  '<script>window.__mdXss = 1</script>',
  '',
  '[危险链接](javascript:alert(1))',
].join('\n')

let passed = 0
let failed = 0
const failures = []
const evidence = []
let currentStep = 0
let page
const consoleErrors = []

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

async function screenshot(name) {
  try {
    const file = join(ARTIFACTS, `${String(currentStep).padStart(2, '0')}-${name}.png`)
    await page.screenshot({ path: file, fullPage: true })
    return file
  } catch {
    return null
  }
}

async function step(name, fn) {
  currentStep += 1
  const started = Date.now()
  try {
    await fn()
    passed += 1
    console.log(`  PASS — ${name} (${Date.now() - started}ms)`)
    evidence.push({ step: currentStep, name, result: 'PASS' })
  } catch (error) {
    failed += 1
    const file = await screenshot('FAILED')
    console.log(`  FAIL — ${name}`)
    console.log(`         原因：${error.message}`)
    if (file) console.log(`         截图：${file}`)
    failures.push({ step: currentStep, name, error: error.message, screenshot: file })
    evidence.push({ step: currentStep, name, result: 'FAIL', error: error.message })
    throw error
  }
}

/* --------------------------------- API ---------------------------------- */

async function api(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    method: options.method ?? 'GET',
    headers: options.body === undefined ? {} : { 'Content-Type': 'application/json' },
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })
  if (response.status === 204) return { status: 204, data: null }
  let envelope = null
  try {
    envelope = await response.json()
  } catch {
    envelope = null
  }
  return { status: response.status, data: envelope?.data ?? null, envelope }
}

async function apiOk(path, options = {}) {
  const result = await api(path, options)
  if (result.status >= 400) {
    throw new Error(`API ${path} -> ${result.status}: ${result.envelope?.message ?? ''}`)
  }
  return result.data
}

/* -------------------------------- helpers -------------------------------- */


/** 以浏览器当前登录态（localStorage 中的 JWT，v4）调用 API；Node 侧 fetch 没有浏览器登录态 */
async function pageApi(path) {
  const result = await page.evaluate(async (apiPath) => {
    let token = null
    try {
      token = window.localStorage.getItem('gcc.auth.token')
    } catch {
      /* ignore */
    }
    const headers = token ? { Authorization: `Bearer ${token}` } : {}
    const response = await fetch(`/api${apiPath}`, { headers })
    let body = null
    try {
      body = await response.json()
    } catch {
      body = null
    }
    return { status: response.status, data: body?.data ?? null, message: body?.message ?? '' }
  }, path)
  if (result.status >= 400) {
    throw new Error(`API ${path} -> ${result.status}: ${result.message}`)
  }
  return result.data
}

async function bodyText() {
  return page.locator('body').innerText()
}

async function waitToast(text) {
  await page.locator('.toast-item', { hasText: text }).first().waitFor({ timeout: 15000 })
}

async function clearToasts() {
  const closes = await page.locator('.toast-close').all()
  for (const button of closes) {
    await button.click({ timeout: 1000 }).catch(() => {})
  }
  await page
    .waitForFunction(() => document.querySelectorAll('.toast-item').length === 0, undefined, {
      timeout: 5000,
    })
    .catch(() => {})
}

async function goto(path) {
  await page.goto(`${BASE_URL}${path}`, { waitUntil: 'domcontentloaded' })
}

async function login(username, password) {
  await goto('/login')
  await page.locator('#login-username').waitFor({ timeout: 20000 })
  await page.fill('#login-username', username)
  await page.fill('#login-password', password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL(/\/admin\/projects/, { timeout: 20000 })
  await page.locator('.admin-user-menu__user').waitFor({ timeout: 20000 })
}

async function logout() {
  if (!page.url().includes('/admin/')) await goto('/admin/projects')
  await page.locator('.admin-user-menu__logout').waitFor({ timeout: 20000 })
  await clearToasts()
  await page.locator('.admin-user-menu__logout').click()
  await page.waitForURL(/\/login/, { timeout: 20000 })
}

async function register(user) {
  await goto('/register')
  await page.locator('#register-username').waitFor({ timeout: 20000 })
  await page.fill('#register-username', user.username)
  await page.fill('#register-display-name', user.displayName)
  await page.fill('#register-password', user.password)
  await page.fill('#register-confirm', user.password)
  await page.getByRole('button', { name: '注册并登录', exact: true }).click()
  await page.waitForURL(/\/admin\/projects/, { timeout: 20000 })
  await page.locator('.admin-user-menu__user').waitFor({ timeout: 20000 })
}

async function waitStructure() {
  await page.locator('.structure-item').first().waitFor({ timeout: 20000 })
}

async function structureTitles() {
  return page.locator('.structure-item .item-title').allInnerTexts()
}

async function clickStructure(title, options = {}) {
  await page.evaluate(
    ({ text, last }) => {
      const nodes = [...document.querySelectorAll('.structure-item')].filter((node) =>
        (node.textContent ?? '').includes(text),
      )
      const target = last ? nodes[nodes.length - 1] : nodes[0]
      if (!target) throw new Error(`未找到页面结构项：${text}`)
      target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }))
    },
    { text: title, last: Boolean(options.last) },
  )
  await page.waitForTimeout(150)
}

async function addButtonComponent() {
  await page.evaluate(() => {
    const nodes = [...document.querySelectorAll('.palette-item')]
    const target = nodes.find((node) => (node.textContent ?? '').includes('按钮'))
    if (!target) throw new Error('未找到组件库「按钮」')
    target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }))
  })
  await page.locator('#action-type-select').waitFor({ timeout: 10000 })
}

async function saveDraft(expectedVersion) {
  await clearToasts()
  await page.locator('.topbar-actions button.gcc-btn-primary', { hasText: '保存' }).click()
  await waitToast(`保存成功，当前草稿 v${expectedVersion}`)
}

async function openPageFromList(pageName) {
  const row = page.locator('.table-row', { hasText: pageName }).first()
  await row.waitFor({ timeout: 20000 })
  await row.getByRole('button', { name: '编辑器' }).click()
  await waitStructure()
  await page.waitForFunction(
    () => document.querySelectorAll('.canvas-frame [data-component-type]').length >= 1,
    undefined,
    { timeout: 20000 },
  )
}

async function publishSiteFromList(siteName) {
  await goto('/admin/projects')
  const card = page.locator('.project-card', { hasText: siteName }).first()
  await card.waitFor({ timeout: 20000 })
  await clearToasts()
  await card.getByRole('button', { name: /站点发布/ }).click()
  await waitToast('发布完成')
}

async function footerCount() {
  return page.evaluate(() => document.querySelectorAll('footer').length)
}

async function adminTheme() {
  return page.evaluate(() => {
    const scope = document.querySelector('.admin-theme-scope')
    return {
      scopeTheme: scope?.getAttribute('data-admin-theme') ?? null,
      stored: window.localStorage.getItem('gcc.admin.theme'),
    }
  })
}

/* --------------------------------- main --------------------------------- */

async function main() {
  console.log('==============================================================')
  console.log(' GameCommunityCreator-MVP v3 · 真实浏览器 E2E（修复验收）')
  console.log(` 前端: ${BASE_URL}   后端: ${API_URL}`)
  console.log(` 测试站点: ${SITE_NAME} (${SLUG})`)
  console.log('==============================================================\n')

  const health = await apiOk('/health')
  assert(health.status === 'UP' && health.database === 'UP', '后端健康检查未通过')
  console.log('  预检通过：后端与数据库 UP\n')

  const browser = await chromium.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-gpu', '--hide-scrollbars'],
  })
  const context = await browser.newContext({
    viewport: { width: 1600, height: 1000 },
    locale: 'zh-CN',
  })
  page = await context.newPage()
  page.on('dialog', (dialog) => dialog.accept().catch(() => {}))
  page.on('pageerror', (error) => consoleErrors.push(`PAGEERROR: ${error.message}`))
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })

  let projectId = null
  let homePageId = null
  let pages = []

  try {
    await step('1. 未登录访问管理端 → 跳转登录页（FIX-3）', async () => {
      await goto('/admin/projects')
      await page.waitForURL(/\/login/, { timeout: 20000 })
      await page.locator('#login-username').waitFor({ timeout: 20000 })
      assert(page.url().includes('redirect='), '登录跳转应带 redirect 参数')
      assert((await bodyText()).includes('登录'), '登录页未渲染')
      await screenshot('login-page')
    })

    await step('2. 注册开发者 A（自动登录）→ 我的游戏为空（FIX-3）', async () => {
      await goto('/register')
      await page.locator('#register-username').waitFor({ timeout: 20000 })
      await screenshot('register-page')
      await register(USER_A)
      await page.waitForFunction(() => document.body.innerText.includes('我的游戏'), undefined, {
        timeout: 20000,
      })
      const text = await bodyText()
      assert(text.includes(USER_A.displayName), '顶栏未显示当前开发者')
      assert(
        text.includes('新建项目') || text.includes('新建站点'),
        '我的游戏缺少新建项目入口',
      )
      const projects = await pageApi('/projects')
      assert(Array.isArray(projects) && projects.length === 0, '新开发者不应看到任何项目')
      await screenshot('my-games-empty')
    })

    await step('2b. 新控制台导航与页面（UI 重构验收）', async () => {
      await goto('/admin/overview')
      await page.locator('.admin-shell').waitFor({ timeout: 20000 })
      await page.locator('.admin-shell__nav-item').first().waitFor({ timeout: 10000 })
      const navText = (await page.locator('.admin-shell__nav-item').allInnerTexts()).join(' ')
      for (const label of ['总览', '项目', '模板', '组件', '设置']) {
        assert(navText.includes(label), `控制台侧边栏缺少导航：${label}`)
      }
      assert((await bodyText()).includes('BUILD · CONFIGURE'), '总览 Hero 标题未渲染')
      const pagesToCheck = [
        ['/admin/templates', /模板/],
        ['/admin/components', /组件/],
        ['/admin/settings', /设置/],
      ]
      for (const [path, pattern] of pagesToCheck) {
        await goto(path)
        await page.locator('.admin-shell').waitFor({ timeout: 20000 })
        await page.waitForTimeout(300)
        const text = await bodyText()
        assert(pattern.test(text), `页面 ${path} 内容未渲染`)
      }
      await goto('/admin/projects')
      await page.locator('.projects-page').first().waitFor({ timeout: 20000 })
      await page.waitForFunction(() => document.body.innerText.includes('我的游戏'), undefined, {
        timeout: 20000,
      })
    })

    await step('3. 创建游戏站点 → 自动生成 6 个默认页面（回归 U1/U2）', async () => {
      await page.getByRole('link', { name: /新建项目|新建站点/ }).first().click()
      await page.locator('#create-name').waitFor({ timeout: 20000 })
      await page.fill('#create-name', SITE_NAME)
      await page.fill('#create-slug', SLUG)
      await page.fill('#create-game-name', GAME_NAME)
      await page.fill('#create-description', DESCRIPTION)
      await page.getByRole('button', { name: /创建站点并进入页面管理/ }).click()
      await page.waitForURL(/\/admin\/projects\/\d+\/pages/, { timeout: 20000 })
      projectId = Number(page.url().match(/projects\/(\d+)\/pages/)[1])
      await page.locator('.table-row').first().waitFor({ timeout: 20000 })
      const text = await bodyText()
      for (const title of DEFAULT_PAGE_TITLES) assert(text.includes(title), `缺少默认页面：${title}`)
      pages = await pageApi(`/projects/${projectId}/pages`)
      homePageId = pages.find((item) => item.isHome).pageId
    })

    await step('4. 编辑器默认模板 + 按钮动作 + 保存/刷新恢复（回归 U2–U5）', async () => {
      await openPageFromList('首页')
      assert((await structureTitles()).length === 7, '默认首页应有 7 个组件（hero/about/notice/issue_progress/changelog/docs/footer）')
      const changelogPage = pages.find((item) => item.pageType === 'changelog')
      await addButtonComponent()
      await page.locator('#prop-label').fill(BUTTON_LABEL)
      await page.locator('#action-type-select').selectOption('navigate')
      await page.locator('#action-target-select').selectOption(changelogPage.pageId)
      await saveDraft(2)
      const draft = await pageApi(`/projects/${projectId}/pages/${homePageId}/page`)
      const button = draft.config.components.find((component) => component.type === 'button')
      assert(button?.action?.type === 'navigate', '按钮动作未保存')
      assert(button.action.targetPageId === changelogPage.pageId, '按钮目标页面不正确')
      await page.reload({ waitUntil: 'domcontentloaded' })
      await waitStructure()
      assert((await structureTitles()).length === 8, '刷新后组件数应为 8')
    })

    await step('5. 管理端主题切换 + 刷新持久化 + 门户主题不受影响（FIX-2）', async () => {
      const before = await adminTheme()
      assert(before.scopeTheme === 'dark', `默认管理端主题应为 dark，实际 ${before.scopeTheme}`)
      await page.locator('.admin-user-menu__theme').click()
      await page.waitForFunction(
        () => document.querySelector('.admin-theme-scope')?.getAttribute('data-admin-theme') === 'light',
        undefined,
        { timeout: 10000 },
      )
      const stored = await page.evaluate(() => window.localStorage.getItem('gcc.admin.theme'))
      assert(stored === 'LIGHT', `管理端主题偏好应持久化为 LIGHT，实际 ${stored}`)
      await page.reload({ waitUntil: 'domcontentloaded' })
      await waitStructure()
      const after = await adminTheme()
      assert(after.scopeTheme === 'light', '刷新后管理端浅色主题未恢复')
      const canvasBg = await page
        .locator('.canvas-frame .gcc-runtime')
        .evaluate((el) => getComputedStyle(el).getPropertyValue('--gcc-bg').trim())
      assert(canvasBg.toLowerCase() === '#0b1020', `画布门户主题不应受管理端主题影响：${canvasBg}`)
      await screenshot('admin-light-theme')
    })

    await step('6. 站点发布 → 公开首页页脚唯一/导航/主题（FIX-1/U11）', async () => {
      await publishSiteFromList(SITE_NAME)
      const project = await pageApi(`/projects/${projectId}`)
      assert(project.status === 'PUBLISHED', '站点发布后状态应为 PUBLISHED')
      await goto(`/community/${SLUG}`)
      await page.locator('[data-testid="community-runtime"]').waitFor({ timeout: 20000 })
      const text = await bodyText()
      assert(text.includes(GAME_NAME), '公开首页缺少游戏名')
      const footers = await footerCount()
      assert(footers === 1, `公开首页 footer 数量应为 1，实际 ${footers}`)
      const siteTheme = await page.locator('.site-layout').getAttribute('data-theme')
      assert(siteTheme === 'dark', `门户主题应为 dark（不受管理端浅色影响），实际 ${siteTheme}`)
      const navText = await page.locator('.site-nav').innerText()
      assert(navText.includes('更新日志'), '公开导航缺少更新日志')
      await screenshot('public-home-footer-once')
    })

    await step('7. 模块页/详情页页脚唯一 + seed Markdown 展示（FIX-1/FIX-4）', async () => {
      await goto(`/community/${SLUG}/changelog`)
      await page.locator('[data-testid="community-runtime"]').waitFor({ timeout: 20000 })
      const moduleFooters = await footerCount()
      assert(moduleFooters === 1, `模块页 footer 应为 1，实际 ${moduleFooters}`)
      await goto('/community/stellar-drift/docs/getting-started')
      await page.locator('.markdown-body').first().waitFor({ timeout: 20000 })
      const detailFooters = await footerCount()
      assert(detailFooters === 1, `文档详情页 footer 应为 1，实际 ${detailFooters}`)
      const markdownText = await page.locator('.markdown-body').first().innerText()
      assert(markdownText.length > 20, 'seed 文档详情 Markdown 正文为空')
    })

    await step('7b. 公开首页：问题处理进展 + 公告/更新日志/文档卡片可点击（P1）', async () => {
      await goto('/community/stellar-drift')
      await page.locator('[data-testid="community-runtime"]').waitFor({ timeout: 20000 })
      // 首页内容模块数据为异步加载：等待关键模块渲染完成后再断言
      await page.locator('.issue-progress-item').first().waitFor({ timeout: 20000 })
      await page.locator('.notice-item').first().waitFor({ timeout: 20000 })
      await page.locator('.changelog-row').first().waitFor({ timeout: 20000 })
      await page.locator('.docs-card').first().waitFor({ timeout: 20000 })
      const homeText = await bodyText()
      assert(homeText.includes('问题处理进展'), '公开首页缺少「问题处理进展」模块')
      assert(!homeText.includes('玩家讨论'), '公开首页仍存在「玩家讨论」（应替换为问题处理进展）')
      assert(homeText.includes('登录界面偶发闪退'), '公开首页未展示已解决且公开的问题')
      assert(!homeText.includes('文本错别字'), '未公开的问题不应出现在公开首页')
      assert(!homeText.includes('手柄震动异常'), '未解决的问题不应出现在公开首页')
      assert((await footerCount()) === 1, '公开首页 footer 数量应为 1')

      // 公告卡片点击 → 公告详情
      await page.locator('.notice-item').first().click()
      await page.waitForURL(/\/community\/stellar-drift\/news\/\d+/, { timeout: 15000 })
      await page.locator('.news-detail').waitFor({ timeout: 15000 })
      await page.locator('.news-detail .markdown-body').first().waitFor({ timeout: 15000 })
      const newsText = await page.locator('.news-detail').innerText()
      assert(newsText.length > 10, '公告详情内容为空')
      assert((await footerCount()) === 1, '公告详情页 footer 数量应为 1')

      // 更新日志卡片点击 → 更新日志详情
      await goto('/community/stellar-drift')
      await page.locator('[data-testid="community-runtime"]').waitFor({ timeout: 20000 })
      await page.locator('.changelog-row').first().click()
      await page.waitForURL(/\/community\/stellar-drift\/changelog\/\d+/, { timeout: 15000 })
      await page.locator('.markdown-body').first().waitFor({ timeout: 15000 })

      // 公开文档卡片点击 → 文档详情
      await goto('/community/stellar-drift')
      await page.locator('[data-testid="community-runtime"]').waitFor({ timeout: 20000 })
      await page.locator('.docs-card').first().click()
      await page.waitForURL(/\/community\/stellar-drift\/docs\/[a-z0-9-]+$/, { timeout: 15000 })
      await page.locator('.markdown-body').first().waitFor({ timeout: 15000 })

      // 问题处理进展卡片点击 → 进展列表并定位锚点
      await goto('/community/stellar-drift')
      await page.locator('[data-testid="community-runtime"]').waitFor({ timeout: 20000 })
      const progressItem = page.locator('.issue-progress-item').first()
      await progressItem.waitFor({ timeout: 15000 })
      const issueId = await progressItem.getAttribute('data-issue-id')
      assert(Boolean(issueId), '问题处理进展条目缺少 data-issue-id')
      await progressItem.click()
      await page.waitForURL(new RegExp(`/community/stellar-drift/progress#issue-${issueId}$`), {
        timeout: 15000,
      })
      await page.locator(`.progress-item#issue-${issueId}`).waitFor({ timeout: 15000 })
      const progressText = await page.locator('.progress-item').first().innerText()
      assert(progressText.includes('处理结果'), '进展列表条目缺少处理结果字段')
      assert((await footerCount()) === 1, '问题处理进展页 footer 数量应为 1')

      // 进展列表只展示已解决且公开的问题
      await goto('/community/stellar-drift/progress')
      await page.locator('.progress-item').first().waitFor({ timeout: 20000 })
      const progressBody = await bodyText()
      assert(progressBody.includes('登录界面偶发闪退'), '进展列表缺少公开已解决问题')
      assert(!progressBody.includes('文本错别字'), '进展列表不应包含未公开问题')
      assert(!progressBody.includes('手柄震动异常'), '进展列表不应包含未解决问题')
    })

    await step('8. Markdown 文档：编辑器预览 + 保存 + 公开渲染 + XSS 清洗（FIX-4）', async () => {
      await goto(`/admin/projects/${projectId}/content/docs`)
      await page.locator('.module-tab').first().waitFor({ timeout: 20000 })
      await page.getByRole('button', { name: /新增公开文档/ }).click()
      await page.locator('#field-title').fill(DOC_TITLE)
      await page.locator('#field-category').fill('验收')
      await page.locator('#field-slug').fill(DOC_SLUG)
      // 文档内容为 PUBLISHED 才在公开端可见（内容发布不走页面快照）
      await page.locator('#field-status').selectOption('PUBLISHED')
      const markdownInput = page.locator('.markdown-editor__textarea').first()
      await markdownInput.waitFor({ timeout: 10000 })
      await markdownInput.fill(DOC_MARKDOWN)
      await page.locator('.markdown-editor__mode', { hasText: '预览' }).click()
      await page
        .locator('.markdown-editor__preview .markdown-body h2')
        .first()
        .waitFor({ timeout: 10000 })
      assert(
        (await page.locator('.markdown-editor__preview .markdown-body table').count()) > 0,
        '编辑器预览未渲染表格',
      )
      assert(
        (await page.locator('.markdown-editor__preview .markdown-body pre').count()) > 0,
        '编辑器预览未渲染代码块',
      )
      await screenshot('markdown-editor-preview')
      await clearToasts()
      await page.locator('.dialog').getByRole('button', { name: '保存', exact: true }).click()
      await page.locator('.notice-banner', { hasText: '创建成功' }).first().waitFor({ timeout: 15000 })
      const docs = await pageApi(`/projects/${projectId}/docs?pageNum=1&pageSize=10`)
      const created = docs.items.find((item) => item.slug === DOC_SLUG)
      assert(created, '后端未保存 Markdown 文档')
      assert(created.bodyMarkdown.includes('## 快速开始'), 'bodyMarkdown 未正确保存')
      assert(created.status === 'PUBLISHED', '文档状态应为 PUBLISHED')
      await goto(`/community/${SLUG}/docs`)
      await page.locator('[data-testid="community-runtime"]').waitFor({ timeout: 20000 })
      await page.locator('.docs-card', { hasText: DOC_TITLE }).first().click()
      await page.waitForURL(new RegExp(`/community/${SLUG}/docs/${DOC_SLUG}$`), { timeout: 15000 })
      const article = page.locator('.markdown-body').first()
      await article.waitFor({ timeout: 20000 })
      assert((await article.locator('h2').count()) > 0, '公开文档未渲染标题')
      assert((await article.locator('table').count()) > 0, '公开文档未渲染表格')
      assert((await article.locator('pre code').count()) > 0, '公开文档未渲染代码块')
      assert((await article.locator('blockquote').count()) > 0, '公开文档未渲染引用')
      const xssFlag = await page.evaluate(() => window.__mdXss ?? 0)
      assert(xssFlag === 0, 'Markdown 中的脚本被执行（XSS 失败）')
      assert((await article.locator('script').count()) === 0, '渲染结果中不应存在 script 节点')
      const dangerousLinks = await article
        .locator('a')
        .evaluateAll((links) => links.map((link) => link.getAttribute('href') ?? ''))
      assert(
        !dangerousLinks.some((href) => href.toLowerCase().startsWith('javascript:')),
        '危险 javascript: 链接未被清洗',
      )
      await screenshot('public-markdown-doc')
    })

    await step('8b. Markdown 编辑器：弹窗稳定 / 未保存保护 / 图片上传闭环（P0）', async () => {
      await goto(`/admin/projects/${projectId}/content/docs`)
      await page.locator('.module-tab').first().waitFor({ timeout: 20000 })
      await page.getByRole('button', { name: /新增公开文档/ }).click()
      await page.locator('.dialog').waitFor({ timeout: 10000 })
      await page.locator('#field-title').fill(DIALOG_TITLE)
      await page.locator('#field-category').fill('验收')
      await page.locator('#field-slug').fill(DIALOG_SLUG)
      await page.locator('#field-status').selectOption('PUBLISHED')
      const markdownInput = page.locator('.markdown-editor__textarea').first()
      await markdownInput.waitFor({ timeout: 10000 })
      const longMarkdown = `# P0 稳定性验收\n\n${'这是一段用于验证弹窗不会异常关闭的较长 Markdown 内容。'.repeat(8)}\n\n- 列表一\n- 列表二\n\n[安全链接](https://example.com)\n`
      await markdownInput.fill(longMarkdown)

      // 点击工具栏 / 模式控件：弹窗必须保持打开且内容不丢失
      for (const title of ['标题', '粗体', '表格']) {
        await page.locator(`.markdown-editor__tool[title="${title}"]`).click()
        await page.waitForTimeout(100)
        assert((await page.locator('.dialog').count()) === 1, `点击工具栏「${title}」后弹窗不应关闭`)
      }
      for (const label of ['预览', '分栏', '编辑']) {
        await page.locator('.markdown-editor__mode', { hasText: label }).click()
        await page.waitForTimeout(100)
        assert((await page.locator('.dialog').count()) === 1, `切换编辑模式「${label}」后弹窗不应关闭`)
      }
      // 预览中的链接点击不应离开编辑页
      await page.locator('.markdown-editor__mode', { hasText: '预览' }).click()
      await page.locator('.markdown-editor__preview .markdown-body a').first().click()
      await page.waitForTimeout(300)
      assert((await page.locator('.dialog').count()) === 1, '点击预览链接后弹窗不应关闭')
      assert(page.url().includes('/content/docs'), '点击预览链接不应跳转离开编辑页')
      await page.locator('.markdown-editor__mode', { hasText: '编辑' }).click()
      assert((await markdownInput.inputValue()).includes('P0 稳定性验收'), '编辑内容不应丢失')

      // 未保存保护：取消 → 提示 → 继续编辑（内容仍在）→ 再取消 → 放弃修改（关闭）
      await page.locator('.dialog').getByRole('button', { name: '取消' }).click()
      await page.locator('.confirm-dialog').waitFor({ timeout: 10000 })
      assert((await bodyText()).includes('尚未保存'), '未保存确认提示文案缺失')
      await page.locator('.confirm-dialog').getByRole('button', { name: '继续编辑' }).click()
      assert((await page.locator('.dialog').count()) === 1, '继续编辑后弹窗应保持打开')
      assert((await markdownInput.inputValue()).includes('P0 稳定性验收'), '继续编辑后内容应保留')
      await page.locator('.dialog').getByRole('button', { name: '取消' }).click()
      await page.locator('.confirm-dialog').waitFor({ timeout: 10000 })
      await page.locator('.confirm-dialog').getByRole('button', { name: '放弃修改' }).click()
      await page.waitForTimeout(300)
      assert((await page.locator('.dialog').count()) === 0, '放弃修改后弹窗应关闭')
      assert((await page.locator('.confirm-dialog').count()) === 0, '放弃修改后确认框应关闭')

      // 图片上传：真实上传接口 → 光标处插入 Markdown → 保存 → 发布 → 公开可见
      await page.getByRole('button', { name: /新增公开文档/ }).click()
      await page.locator('.dialog').waitFor({ timeout: 10000 })
      await page.locator('#field-title').fill(UPLOAD_DOC_TITLE)
      await page.locator('#field-category').fill('验收')
      await page.locator('#field-slug').fill(UPLOAD_DOC_SLUG)
      await page.locator('#field-status').selectOption('PUBLISHED')
      const uploadInput = page.locator('.markdown-editor__file').first()
      await uploadInput.waitFor({ state: 'attached', timeout: 10000 })
      const [chooser] = await Promise.all([
        page.waitForEvent('filechooser', { timeout: 10000 }),
        page.locator('.markdown-editor__upload').first().click(),
      ])
      await chooser.setFiles({
        name: 'e2e-upload.png',
        mimeType: 'image/png',
        buffer: Buffer.from(PNG_1X1_BASE64, 'base64'),
      })
      await page.waitForFunction(
        () => {
          const input = document.querySelector('.markdown-editor__textarea')
          return Boolean(input && input.value.includes('/api/public/uploads/'))
        },
        undefined,
        { timeout: 20000 },
      )
      assert((await page.locator('.dialog').count()) === 1, '上传图片后弹窗不应关闭')
      const uploadMarkdown = await page.locator('.markdown-editor__textarea').first().inputValue()
      const uploadedUrlMatch = uploadMarkdown.match(/\(([^)]*\/api\/public\/uploads\/[^)]+)\)/)
      assert(Boolean(uploadedUrlMatch), 'Markdown 中未插入上传图片语法')
      const uploadedUrl = uploadedUrlMatch[1]
      await screenshot('markdown-image-upload')
      await clearToasts()
      await page.locator('.dialog').getByRole('button', { name: '保存', exact: true }).click()
      await page.locator('.notice-banner', { hasText: '创建成功' }).first().waitFor({ timeout: 15000 })
      const uploadDocs = await pageApi(`/projects/${projectId}/docs?pageNum=1&pageSize=20`)
      const uploadDoc = uploadDocs.items.find((item) => item.slug === UPLOAD_DOC_SLUG)
      assert(uploadDoc, '图片上传文档未保存')
      assert(uploadDoc.bodyMarkdown.includes('/api/public/uploads/'), 'bodyMarkdown 未保存图片地址')
      // 文档内容为 PUBLISHED 后直接公开展示（内容发布不走页面快照，无需重复站点发布）
      await goto(`/community/${SLUG}/docs`)
      await page.locator('[data-testid="community-runtime"]').waitFor({ timeout: 20000 })
      await page.locator('.docs-card', { hasText: UPLOAD_DOC_TITLE }).first().click()
      await page.waitForURL(new RegExp(`/community/${SLUG}/docs/${UPLOAD_DOC_SLUG}$`), { timeout: 15000 })
      const img = page.locator('.markdown-body img').first()
      await img.waitFor({ timeout: 20000 })
      const imageOk = await img.evaluate(
        (element) => element.complete && element.naturalWidth > 0,
      )
      assert(imageOk, '公开文档中的上传图片未正常加载')
      const imageResponseStatus = await page.evaluate(async (url) => {
        const response = await fetch(url)
        return response.status
      }, uploadedUrl)
      assert(imageResponseStatus === 200, `上传图片匿名访问应 200，实际 ${imageResponseStatus}`)
    })

    await step('9. 退出登录 + 注册开发者 B → 我的游戏为空（FIX-3）', async () => {
      await logout()
      await register(USER_B)
      const text = await bodyText()
      assert(text.includes('我的游戏'), '开发者 B 未进入我的游戏')
      assert(!text.includes(SITE_NAME), '开发者 B 不应看到开发者 A 的站点')
      const projects = await pageApi('/projects')
      assert(projects.length === 0, '开发者 B 项目列表应为空')
      await screenshot('my-games-user-b')
    })

    await step('10. 开发者 B 直接访问 A 的项目/页面接口被拒（FIX-3 所有权隔离）', async () => {
      const pageListStatus = await page.evaluate(async (id) => {
        let token = null
        try {
          token = window.localStorage.getItem('gcc.auth.token')
        } catch {
          /* ignore */
        }
        const headers = token ? { Authorization: `Bearer ${token}` } : {}
        const response = await fetch(`/api/projects/${id}/pages`, { headers })
        return response.status
      }, projectId)
      assert(pageListStatus === 403, `B 访问 A 页面接口应 403，实际 ${pageListStatus}`)
      await goto(`/admin/projects/${projectId}/pages`)
      await page.waitForTimeout(1000)
      const text = await bodyText()
      assert(
        text.includes('无权访问') || text.includes('403') || text.includes('加载失败'),
        `B 直接打开 A 的页面管理应显示无权/失败提示，实际：${text.slice(0, 160)}`,
      )
    })

    await step('11. 匿名访客仍可访问公开站点并提交问题反馈（FIX-3 公开端）', async () => {
      await page.evaluate(() => {
        try {
          window.localStorage.clear()
        } catch {
          /* ignore */
        }
      })
      await goto(`/community/${SLUG}`)
      await page.locator('[data-testid="community-runtime"]').waitFor({ timeout: 20000 })
      await goto(`/community/${SLUG}/report`)
      await page.locator('[data-testid="community-runtime"]').waitFor({ timeout: 20000 })
      const titleInput = page.locator('[id^="issue-title-"]').first()
      await titleInput.waitFor({ timeout: 15000 })
      await titleInput.fill('匿名访客反馈（E2E）')
      const categorySelect = page.locator('[id^="issue-category-"]').first()
      const categoryOptions = await categorySelect
        .locator('option')
        .evaluateAll((options) => options.map((option) => option.value).filter((value) => value !== ''))
      if (categoryOptions.length > 0) await categorySelect.selectOption(categoryOptions[0])
      await page.locator('[id^="issue-desc-"]').first().fill('匿名访客提交的问题描述')
      await page.locator('.issue-form button[type="submit"]').first().click()
      await page.waitForFunction(() => document.body.innerText.includes('感谢反馈'), undefined, {
        timeout: 15000,
      })
    })

    await step('12. JWT 失效 → 跳登录并提示重新登录（FIX-3）', async () => {
      await login(USER_A.username, USER_A.password)
      await page.locator('.project-card', { hasText: SITE_NAME }).first().waitFor({ timeout: 20000 })
      // 模拟 token 失效/被篡改：保留登录态内存，但让后续请求携带非法 token
      await page.evaluate(() => {
        try {
          window.localStorage.setItem('gcc.auth.token', 'invalid.jwt.token')
        } catch {
          /* ignore */
        }
      })
      await page
        .locator('.project-card', { hasText: SITE_NAME })
        .first()
        .getByRole('button', { name: '页面管理' })
        .click()
      await page.waitForURL(/\/login.*expired=1/, { timeout: 20000 })
      const text = await bodyText()
      assert(text.includes('登录状态已过期'), 'JWT 失效后应提示登录状态已过期')
      await screenshot('jwt-expired')
    })

    await step('13. 旧演示项目经 bootstrap 认领：dev 可编辑/保存（FIX-3 兼容）', async () => {
      await login(BOOTSTRAP_USERNAME, BOOTSTRAP_PASSWORD)
      await page.waitForFunction(
        () => document.body.innerText.includes('星汐边境官方社区'),
        undefined,
        { timeout: 20000 },
      )
      const text = await bodyText()
      assert(text.includes('星汐边境官方社区'), 'dev 登录后应看到 seed 站点 stellar-drift')
      assert(text.includes('回响花园'), 'dev 登录后应看到 seed 站点 echo-garden')
      const seedCard = page.locator('.project-card', { hasText: '星汐边境官方社区' }).first()
      await seedCard.getByRole('button', { name: '页面管理' }).click()
      await page.locator('.table-row').first().waitFor({ timeout: 20000 })
      await openPageFromList('首页')
      await clickStructure('社区公告')
      const titleInput = page.locator('#component-title-input')
      await titleInput.fill(LEGACY_COMPONENT_TITLE)
      const seedPages = await pageApi('/projects/1/pages')
      const seedHomeId = seedPages.find((item) => item.isHome).pageId
      const before = await pageApi(`/projects/1/pages/${seedHomeId}/page`)
      await saveDraft(before.configVersion + 1)
      const saved = await pageApi(`/projects/1/pages/${seedHomeId}/page`)
      assert(
        saved.config.components.some((component) => component.title === LEGACY_COMPONENT_TITLE),
        '旧项目页面组件标题保存失败',
      )
      await screenshot('legacy-project-editor')
    })

    await step('14. 旧项目发布后公开访问（FIX-3/U11 兼容）', async () => {
      await publishSiteFromList('星汐边境官方社区')
      await goto('/community/stellar-drift')
      await page.locator('[data-testid="community-runtime"]').waitFor({ timeout: 20000 })
      const text = await bodyText()
      assert(text.includes(LEGACY_COMPONENT_TITLE), '旧项目发布后公开页未显示新内容')
      const footers = await footerCount()
      assert(footers === 1, `旧项目公开首页 footer 应为 1，实际 ${footers}`)
    })

    await step('15. 浏览器控制台无未处理异常（无白屏/JS 崩溃）', async () => {
      const critical = consoleErrors.filter(
        (message) =>
          !/favicon/i.test(message) &&
          !/Failed to load resource/i.test(message) &&
          !/WebSocket/i.test(message) &&
          !/\[vite\]/i.test(message) &&
          !/401 \(Unauthorized\)/i.test(message) &&
          !/403 \(Forbidden\)/i.test(message),
      )
      assert(critical.length === 0, `浏览器控制台存在异常：\n${critical.slice(0, 5).join('\n')}`)
    })

    await step('16. 清理：开发者 A 删除测试站点（公开端 404）', async () => {
      await logout()
      await login(USER_A.username, USER_A.password)
      await goto('/admin/projects')
      const card = page.locator('.project-card', { hasText: SITE_NAME }).first()
      await card.waitFor({ timeout: 20000 })
      await clearToasts()
      await card.getByRole('button', { name: /删除站点/ }).click()
      await waitToast('已删除站点')
      const response = await fetch(`${API_URL}/public/communities/${SLUG}`)
      assert(response.status === 404, `删除后公开端应 404，实际 ${response.status}`)
    })

    console.log('\n==============================================================')
    console.log(` 浏览器 E2E 汇总：PASS ${passed}，FAIL ${failed}`)
    console.log(' 提示：本脚本修改了 seed 站点 stellar-drift 的组件标题并发布；')
    console.log('       请执行 `bash scripts/init-db.sh` 并重启后端恢复演示数据。')
    console.log(` 产物目录：${ARTIFACTS}`)
    console.log('==============================================================')
    writeFileSync(
      join(ARTIFACTS, 'summary.json'),
      JSON.stringify(
        {
          baseUrl: BASE_URL,
          apiUrl: API_URL,
          runId: RUN_ID,
          projectId,
          slug: SLUG,
          passed,
          failed,
          failures,
          evidence,
          consoleErrors,
          finishedAt: new Date().toISOString(),
        },
        null,
        2,
      ),
    )
    if (failed > 0) process.exitCode = 1
  } catch (error) {
    console.log('\n==============================================================')
    console.log(` 浏览器 E2E 中断：PASS ${passed}，FAIL ${failed}`)
    console.log(` 首个失败步骤：${failures[0]?.name ?? '未知'}`)
    console.log('==============================================================')
    writeFileSync(
      join(ARTIFACTS, 'summary.json'),
      JSON.stringify(
        {
          baseUrl: BASE_URL,
          apiUrl: API_URL,
          runId: RUN_ID,
          projectId,
          slug: SLUG,
          passed,
          failed,
          failures,
          evidence,
          consoleErrors,
          aborted: true,
          finishedAt: new Date().toISOString(),
        },
        null,
        2,
      ),
    )
    process.exitCode = 1
  } finally {
    await context.close().catch(() => {})
    await browser.close().catch(() => {})
  }
}

main().catch((error) => {
  console.error('浏览器 E2E 执行异常：', error)
  process.exitCode = 1
})
