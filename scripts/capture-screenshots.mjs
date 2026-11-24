#!/usr/bin/env node
/**
 * GameCommunityCreator-MVP v3.1（xAI 风格 UI 重构）—— 真实运行截图采集脚本
 *
 * 覆盖新 UI：
 *   登录 / 注册（独立近黑全屏布局）
 *   管理端 AdminShell 控制台：总览（Hero/统计/我的项目/推荐模板）、我的项目、创建项目、
 *   模板中心、组件库、设置（管理端深色 + 浅色）
 *   管理端内页：页面列表、三栏编辑器、组件属性/按钮动作面板、内容管理（Markdown 列表）、
 *   Markdown 编辑器（编辑/预览/分栏）、草稿预览
 *   公开端：社区首页（Hero/导航/内容）深色 + 浅色、游戏门户广场、Changelog Release Notes、
 *   Changelog 详情、公开文档列表/文档详情 Markdown、制作团队、友情链接、问题反馈
 *   开发者隔离：开发者 B 空「我的项目」/ 直接访问初始开发者项目被拒（403）
 *   另做 1440/1280/1024/900/768 响应式横向溢出检测并记录。
 *
 * 认证：环境变量提供初始开发者（不内置口令）：
 *   SHOT_ADMIN_USERNAME / SHOT_ADMIN_PASSWORD（或 APP_BOOTSTRAP_USERNAME / APP_BOOTSTRAP_PASSWORD）
 *
 * 用法：
 *   SHOT_ADMIN_USERNAME=dev SHOT_ADMIN_PASSWORD=**** \
 *   BASE_URL=http://localhost:4173 API_URL=http://localhost:18080/api \
 *     node scripts/capture-screenshots.mjs
 *
 * 输出：docs/screenshots/NN-name.png（≥24 张，1440×900）+ README.md + capture-log.json
 * 约定：公开站点页强制校验 `<footer>` 数量 = 1（≠1 判失败）；平台「游戏门户广场」不含站点壳页脚，
 *       记录 footerCount=0 并在 README/report 注明例外。绝不修改业务数据（编辑器加按钮为内存态、不保存）。
 */

import { createRequire } from 'node:module'
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = resolve(HERE, '..')

const BASE_URL = (process.env.BASE_URL || 'http://localhost:4173').replace(/\/+$/, '')
const API_URL = (process.env.API_URL || 'http://127.0.0.1:8080/api').replace(/\/+$/, '')
const SEED_SLUG = process.env.SEED_PROJECT_SLUG || 'stellar-drift'
const OUT_DIR = resolve(REPO, process.env.SHOT_OUT_DIR || 'docs/screenshots')
const VIEWPORT = { width: 1440, height: 900 }
const READY_TIMEOUT_MS = 20_000
const SETTLE_MS = 650

const ADMIN_USERNAME =
  process.env.SHOT_ADMIN_USERNAME || process.env.APP_BOOTSTRAP_USERNAME || ''
const ADMIN_PASSWORD =
  process.env.SHOT_ADMIN_PASSWORD || process.env.APP_BOOTSTRAP_PASSWORD || ''
const RUN_ID = Date.now().toString(36)
const DEV_B_USERNAME = `shotb${RUN_ID}`
const DEV_B_PASSWORD = `ShotB${RUN_ID}9x`

const CHROME_CANDIDATES = [
  process.env.CHROME_PATH,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
].filter(Boolean)

const sleep = (ms) => new Promise((ok) => setTimeout(ok, ms))
const preview = (text, max = 400) => {
  const value = String(text ?? '')
  return value.length > max ? `${value.slice(0, max)}…` : value
}
const log = (message) => console.log(message)
const warnings = []
const records = []
const responsiveResults = []
let failed = 0

function findChrome() {
  for (const candidate of CHROME_CANDIDATES) if (existsSync(candidate)) return candidate
  return null
}
function repoRel(abs) {
  return abs.replace(REPO + '\\', '').split('\\').join('/')
}

/* ========================================================================
 * Node 侧轻量 API（解析 seed 项目/页面 id、注册开发者 B）
 * ====================================================================== */

class NodeApi {
  constructor(label) {
    this.label = label
    this.token = null
  }
  async raw(method, path, body, expectStatus = 200) {
    const headers = {}
    if (body !== undefined) headers['Content-Type'] = 'application/json; charset=UTF-8'
    if (this.token) headers.Authorization = `Bearer ${this.token}`
    const response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(15_000),
    })
    const text = await response.text()
    let envelope = null
    try {
      envelope = text ? JSON.parse(text) : null
    } catch {
      envelope = null
    }
    if (response.status !== expectStatus) {
      throw new Error(`[${this.label}] ${method} ${path} → HTTP ${response.status}：${preview(text)}`)
    }
    // v4 JWT：登录/注册响应携带 token，自动保存供后续请求使用
    if (envelope?.data?.token) this.token = envelope.data.token
    return envelope?.data
  }
  get(path, expectStatus = 200) {
    return this.raw('GET', path, undefined, expectStatus)
  }
  post(path, body, expectStatus = 200) {
    return this.raw('POST', path, body, expectStatus)
  }
}

/* ========================================================================
 * 浏览器辅助
 * ====================================================================== */

async function waitForAnySelector(page, selectors, timeout = READY_TIMEOUT_MS) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    for (const selector of selectors) {
      try {
        if ((await page.locator(selector).count()) > 0) return selector
      } catch {
        /* 下一个 */
      }
    }
    await sleep(120)
  }
  return null
}

async function waitForAnyText(page, texts, timeout = READY_TIMEOUT_MS) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    let bodyText = ''
    try {
      bodyText = await page.locator('body').innerText({ timeout: 2000 })
    } catch {
      bodyText = ''
    }
    for (const text of texts) if (bodyText.includes(text)) return text
    await sleep(120)
  }
  return null
}

async function settle(page) {
  try {
    await page.waitForLoadState('networkidle', { timeout: 15_000 })
  } catch {
    /* 忽略 */
  }
  try {
    await page.evaluate(() => (document.fonts ? document.fonts.ready : Promise.resolve()))
  } catch {
    /* 忽略 */
  }
  await sleep(SETTLE_MS)
}

/** 页面内真实 API 登录（v4 JWT），把 token 写入 localStorage 后回管理端 */
async function loginInBrowser(page, username, password) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' })
  const result = await page.evaluate(
    async ([user, pass]) => {
      const loginRes = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user, password: pass }),
      })
      const text = await loginRes.text()
      if (loginRes.ok) {
        try {
          const token = JSON.parse(text)?.data?.token
          if (token) window.localStorage.setItem('gcc.auth.token', token)
        } catch {
          /* 忽略：token 解析失败时保持未登录，由后续断言暴露 */
        }
      }
      return { status: loginRes.status, body: text }
    },
    [username, password],
  )
  if (result.status !== 200) {
    throw new Error(`浏览器内登录失败（HTTP ${result.status}）：${preview(result.body)}`)
  }
  await page.goto(`${BASE_URL}/admin/overview`, { waitUntil: 'domcontentloaded' })
  await settle(page)
}

function readAdminThemeAttribute(page) {
  return page.evaluate(
    () => document.querySelector('[data-admin-theme]')?.getAttribute('data-admin-theme') ?? null,
  )
}

async function setAdminTheme(page, mode) {
  await page.evaluate(
    ([key, value]) => {
      try {
        window.localStorage.setItem(key, value)
      } catch {
        /* 忽略 */
      }
    },
    ['gcc.admin.theme', mode],
  )
  await page.reload({ waitUntil: 'domcontentloaded' })
  await settle(page)
  const expected = mode === 'LIGHT' ? 'light' : 'dark'
  const actual = await readAdminThemeAttribute(page)
  if (actual !== expected) {
    throw new Error(`管理端主题切换未生效：期望 data-admin-theme="${expected}"，实际 ${JSON.stringify(actual)}`)
  }
}

async function setPortalTheme(page, slug, mode) {
  await page.evaluate(
    ([key, value]) => {
      try {
        window.localStorage.setItem(key, value)
      } catch {
        /* 忽略 */
      }
    },
    [`gcc.theme.${slug}`, mode],
  )
  await page.reload({ waitUntil: 'domcontentloaded' })
  await settle(page)
}

async function countFooters(page) {
  return page.evaluate(() => document.querySelectorAll('footer').length)
}

/* ========================================================================
 * 截图清单（29 张 ≥ 24）
 * ====================================================================== */

function buildPlan(seed) {
  const projectId = seed.project.id
  const homePageId = seed.homePageId
  const slug = seed.project.slug
  const shot = (name, title, description, options) => ({ name, title, description, ...options })

  return [
    // ---------- 认证（匿名上下文） ----------
    shot('01-login', '开发者登录页', '近黑全屏登录页：开发者账号 + 密码 + 管理端主题选择 + 注册入口（FIX-3）', {
      auth: 'anon',
      url: `${BASE_URL}/login`,
      waits: { selectors: ['.auth-page', '.auth-card', 'form'], texts: ['Developer access', '登录', '用户名'] },
    }),
    shot('02-register', '开发者注册页', '开发者注册页：用户名 / 密码 / 显示名，自动登录并进入控制台', {
      auth: 'anon',
      url: `${BASE_URL}/register`,
      waits: { selectors: ['.auth-page', '.auth-card', 'form'], texts: ['Create developer account', '注册'] },
    }),

    // ---------- 管理端控制台（AdminShell） ----------
    shot('03-overview', '总览 Overview', '控制台总览：BUILD · CONFIGURE · PUBLISH Hero、统计分栏、我的项目、推荐模板', {
      auth: 'admin',
      url: `${BASE_URL}/admin/overview`,
      waits: { selectors: ['.overview-hero', '.overview-stats', '.admin-shell'], texts: ['BUILD · CONFIGURE · PUBLISH', '总览'] },
    }),
    shot('04-settings-light', '设置 · 管理端浅色', '设置页浅色模式（开发者账号 / 管理端主题）', {
      auth: 'admin',
      url: `${BASE_URL}/admin/settings`,
      prepare: async (page) => setAdminTheme(page, 'LIGHT'),
      waits: { selectors: ['.settings-page', '.settings-card'], texts: ['设置', '管理端主题', '浅色'] },
    }),
    shot('05-settings-dark', '设置 · 管理端深色', '设置页深色模式（默认近黑主题；开发者账号 / 管理端主题）', {
      auth: 'admin',
      url: `${BASE_URL}/admin/settings`,
      prepare: async (page) => setAdminTheme(page, 'DARK'),
      waits: { selectors: ['.settings-page', '.settings-card'], texts: ['设置', '管理端主题', '深色'] },
    }),
    shot('06-my-projects', '我的游戏（项目卡片）', '当前开发者项目网格：项目卡片（封面/状态/PAGES/UPDATED/页面管理/站点发布）', {
      auth: 'admin',
      url: `${BASE_URL}/admin/projects`,
      waits: { selectors: ['.projects-grid', '.project-card'], texts: [SEED_SLUG, '我的游戏'] },
    }),
    shot('07-project-create', '创建游戏站点', '新建项目表单（名称 / 标识 / 游戏名 / 简介）', {
      auth: 'admin',
      url: `${BASE_URL}/admin/projects/new`,
      waits: { selectors: ['.create-form, .gcc-field, form'], texts: ['创建游戏站点', '项目名称'] },
    }),
    shot('08-templates', '模板中心', '模板中心：7 个内置页面模板卡片（组件 chips + 组件数）', {
      auth: 'admin',
      url: `${BASE_URL}/admin/templates`,
      waits: { selectors: ['.templates-grid', '.template-card'], texts: ['模板', 'PAGE TEMPLATES'] },
    }),
    shot('09-components', '组件库', '组件库：11 种运行时页面组件（数据源 / JSON 驱动 / 20 上限）', {
      auth: 'admin',
      url: `${BASE_URL}/admin/components`,
      waits: { selectors: ['.components-grid', '.component-card'], texts: ['组件', '11 COMPONENTS'] },
    }),
    // ---------- 管理端内页 ----------
    shot('11-page-list', '页面管理列表', `${SEED_SLUG} 多页面列表：状态徽标、草稿/发布版本、内容模块快捷入口`, {
      auth: 'admin',
      url: `${BASE_URL}/admin/projects/${projectId}/pages`,
      waits: { selectors: ['.page-table', '.content-links'], texts: ['页面管理', '首页'] },
    }),
    shot('12-editor-three-column', '三栏可视化编辑器', '左组件库 / 中画布 / 右属性面板 + 顶部状态（草稿 v1 · 发布 v1）', {
      auth: 'admin',
      url: `${BASE_URL}/admin/projects/${projectId}/pages/${homePageId}/editor`,
      waits: { selectors: ['.editor-shell', '.editor-left', '.editor-center', '.editor-right'], texts: ['组件库', '保存'] },
    }),
    shot('13-editor-button-action', '组件属性 / 按钮动作面板', '选中按钮组件后右侧属性面板：组件设置 + 点击动作（站内跳转/外部链接/无动作白名单）', {
      auth: 'admin',
      url: `${BASE_URL}/admin/projects/${projectId}/pages/${homePageId}/editor`,
      prepare: async (page) => {
        const paletteButton = page.locator('.palette-item', { hasText: '按钮' }).first()
        if ((await paletteButton.count()) > 0) {
          await paletteButton.click({ timeout: 4000 })
          await sleep(600)
        } else {
          warnings.push('13-editor-button-action：组件库未找到「按钮」条目')
        }
        const structureButton = page.locator('.structure-item', { hasText: '按钮' }).last()
        if ((await structureButton.count()) > 0) {
          await structureButton.click({ timeout: 4000 })
          await sleep(600)
        } else {
          warnings.push('13-editor-button-action：页面结构未找到按钮组件')
        }
        await page
          .evaluate(() => document.querySelector('.action-panel')?.scrollIntoView({ block: 'center' }))
          .catch(() => {})
        await sleep(400)
      },
      waits: { selectors: ['.property-panel', '.action-panel'], texts: ['组件属性', '点击动作'] },
    }),
    shot('14-content-manager', '内容管理列表', '内容模块管理：更新日志 / 公开文档 / 公告 / 制作团队 / 问题反馈 / 友情链接', {
      auth: 'admin',
      url: `${BASE_URL}/admin/projects/${projectId}/content/changelogs`,
      waits: { selectors: ['.content-table', '.module-tabs'], texts: ['更新日志管理', '状态'] },
    }),
    shot('15-markdown-edit', 'Markdown 编辑器 · 编辑', '更新日志编辑对话框：MarkdownEditor 编辑模式 + 工具栏 + 字数', {
      auth: 'admin',
      url: `${BASE_URL}/admin/projects/${projectId}/content/changelogs`,
      prepare: async (page) => {
        await openFirstContentDialog(page)
        await clickMarkdownMode(page, '编辑')
      },
      waits: { selectors: ['.dialog', '.markdown-editor__body.is-edit'], texts: ['编辑更新日志', 'Markdown'] },
    }),
    shot('16-markdown-preview', 'Markdown 编辑器 · 预览', 'Markdown 预览：GFM 渲染 + 安全清洗（与公开端一致）', {
      auth: 'admin',
      url: `${BASE_URL}/admin/projects/${projectId}/content/changelogs`,
      prepare: async (page) => {
        await openFirstContentDialog(page)
        await clickMarkdownMode(page, '预览')
      },
      waits: { selectors: ['.dialog', '.markdown-editor__body.is-preview'], texts: ['预览', 'Markdown'] },
    }),
    shot('17-markdown-split', 'Markdown 编辑器 · 分栏', '分栏模式：左侧编辑 + 右侧实时预览', {
      auth: 'admin',
      url: `${BASE_URL}/admin/projects/${projectId}/content/changelogs`,
      prepare: async (page) => {
        await openFirstContentDialog(page)
        await clickMarkdownMode(page, '分栏')
      },
      waits: { selectors: ['.dialog', '.markdown-editor__body.is-split'], texts: ['分栏', 'Markdown'] },
    }),
    shot('18-draft-preview', '草稿预览', '草稿预览：管理端数据 +「草稿预览」提示（未发布内容仅管理员可见）', {
      auth: 'admin',
      url: `${BASE_URL}/admin/projects/${projectId}/pages/${homePageId}/preview`,
      waits: { selectors: ['.gcc-runtime--preview', '.gcc-runtime__block'], texts: ['草稿预览', '游戏介绍横幅'] },
    }),

    // ---------- 公开社区端 ----------
    shot('19-public-home-dark', '公开社区首页 · 深色', '门户外壳：Hero / mono 导航 / 公告与内容模块；深色主题', {
      auth: 'public',
      url: `${BASE_URL}/community/${slug}`,
      prepare: async (page) => setPortalTheme(page, slug, 'DARK'),
      waits: { selectors: ['.site-header', '.gcc-runtime--public'], texts: ['星汐边境官方社区', '游戏介绍横幅'] },
      footer: { expected: 1, kind: '站点页' },
    }),
    shot('20-public-home-light', '公开社区首页 · 浅色', '门户主题浅色（访客切换，品牌色保留）', {
      auth: 'public',
      url: `${BASE_URL}/community/${slug}`,
      prepare: async (page) => setPortalTheme(page, slug, 'LIGHT'),
      waits: { selectors: ['.site-header', '.gcc-runtime--public'], texts: ['星汐边境官方社区', '游戏介绍横幅'] },
      footer: { expected: 1, kind: '站点页' },
    }),
    shot('21-public-gallery', '游戏门户广场', '平台级公开广场：已发布站点列表（DISCOVER · OFFICIAL GAME PORTALS）', {
      auth: 'public',
      url: `${BASE_URL}/community`,
      waits: { selectors: ['.gallery', '.gallery-title, .gallery-content'], texts: ['游戏门户广场', 'PUBLISHED PORTALS'] },
      footer: { expected: 0, kind: '平台广场页（无站点壳页脚，例外）' },
    }),
    shot('22-public-changelog', '公开 Changelog · Release Notes', '更新日志列表按 Release Notes 排版：版本号 mono + 日期 + 标题 + 摘要', {
      auth: 'public',
      url: `${BASE_URL}/community/${slug}/changelog`,
      prepare: async (page) => setPortalTheme(page, slug, 'DARK'),
      waits: { selectors: ['.changelog-list', '.changelog-row'], texts: ['RELEASE NOTES', '更新日志'] },
      footer: { expected: 1, kind: '站点页' },
    }),
    shot('23-public-changelog-detail', '公开 Changelog 详情', '单条 Release Notes：版本块 + Markdown 内容（标题/列表/表格/引用）', {
      auth: 'public',
      url: seed.changelogDetailUrl,
      prepare: async (page) => setPortalTheme(page, slug, 'DARK'),
      waits: { selectors: ['.gcc-runtime--public', '.markdown-body, article, .gcc-container'], texts: ['更新日志'] },
      footer: { expected: 1, kind: '站点页' },
    }),
    shot('24-public-docs', '公开文档列表', '文档列表：分类 mono 标签 + 标题 + 摘要 + READ DOC', {
      auth: 'public',
      url: `${BASE_URL}/community/${slug}/docs`,
      prepare: async (page) => setPortalTheme(page, slug, 'DARK'),
      waits: { selectors: ['.gcc-runtime--public', '.gcc-container'], texts: ['DOCUMENTS / GUIDES', '公开文档'] },
      footer: { expected: 1, kind: '站点页' },
    }),
    shot('25-public-doc-detail', '公开文档详情 · Markdown', '文档详情 Markdown 安全渲染（标题/表格/列表/链接）', {
      auth: 'public',
      url: seed.docDetailUrl,
      prepare: async (page) => setPortalTheme(page, slug, 'DARK'),
      waits: { selectors: ['.markdown-body, article, .gcc-runtime--public'], texts: ['文档'] },
      footer: { expected: 1, kind: '站点页' },
    }),
    shot('26-public-team', '制作团队', '制作团队页：角色 mono + 姓名 + 一句话（极简排版）', {
      auth: 'public',
      url: `${BASE_URL}/community/${slug}/team`,
      prepare: async (page) => setPortalTheme(page, slug, 'DARK'),
      waits: { selectors: ['.gcc-runtime--public'], texts: ['制作团队', 'TEAM / THE MAKERS'] },
      footer: { expected: 1, kind: '站点页' },
    }),
    shot('27-public-links', '友情链接', '友情链接页：外链卡片 + 描述（仅启用项）', {
      auth: 'public',
      url: `${BASE_URL}/community/${slug}/links`,
      prepare: async (page) => setPortalTheme(page, slug, 'DARK'),
      waits: { selectors: ['.gcc-runtime--public'], texts: ['友情链接', 'LINKS / ELSEWHERE'] },
      footer: { expected: 1, kind: '站点页' },
    }),
    shot('28-public-report', '问题反馈', '问题反馈结构化表单：标题 / 类别 / 版本 / 描述 / 复现步骤', {
      auth: 'public',
      url: `${BASE_URL}/community/${slug}/report`,
      prepare: async (page) => setPortalTheme(page, slug, 'DARK'),
      waits: { selectors: ['.gcc-runtime--public', 'form'], texts: ['问题汇报', 'FEEDBACK / BUG REPORTS'] },
      footer: { expected: 1, kind: '站点页' },
    }),

    // ---------- 开发者隔离 ----------
    shot('29-dev-b-empty-games', '开发者 B · 空「我的游戏」', '开发者 B 登录后只看到自己的项目（空状态 + 创建第一个站点）', {
      auth: 'devB',
      url: `${BASE_URL}/admin/projects`,
      waits: { selectors: ['.projects-empty'], texts: ['我的游戏', 'NO PROJECTS YET'] },
    }),
    shot('30-dev-b-forbidden', '开发者 B · 越权访问提示', `开发者 B 直接访问初始开发者项目页面被拒（403「无权访问该资源」）`, {
      auth: 'devB',
      url: `${BASE_URL}/admin/projects/${projectId}/pages`,
      waits: { selectors: ['.editor-fallback, .gcc-error, #app'], texts: ['无权访问该资源', '返回站点列表', '重试'] },
    }),
  ]
}

async function openFirstContentDialog(page) {
  await settle(page)
  const editButton = page.locator('.content-table button', { hasText: '编辑' }).first()
  if ((await editButton.count()) > 0) {
    await editButton.click({ timeout: 5000 })
    await sleep(700)
  } else {
    warnings.push('Markdown 截图：内容表格未找到行内「编辑」按钮')
  }
}

async function clickMarkdownMode(page, modeText) {
  const mode = page.locator('.markdown-editor__mode', { hasText: modeText }).first()
  if ((await mode.count()) > 0) {
    await mode.click({ timeout: 4000 })
    await sleep(450)
  } else {
    warnings.push(`Markdown 截图：未找到「${modeText}」模式按钮`)
  }
}

/* ========================================================================
 * 采集单张截图
 * ====================================================================== */

async function captureShot(page, shotItem) {
  const startedAt = Date.now()
  const file = join(OUT_DIR, `${shotItem.name}.png`)
  const record = {
    name: shotItem.name,
    title: shotItem.title,
    description: shotItem.description,
    url: shotItem.url,
    file: repoRel(file),
    ok: false,
    warnings: [],
  }
  try {
    log(`\n▶ ${shotItem.title}（${shotItem.name}）`)
    log(`  URL: ${shotItem.url}`)
    await page.goto(shotItem.url, { waitUntil: 'domcontentloaded', timeout: 30_000 })
    await settle(page)
    if (typeof shotItem.prepare === 'function') {
      await shotItem.prepare(page)
      await settle(page)
    }
    const appOk = await waitForAnySelector(page, ['#app', '[data-v-app]', '.admin-shell', '.gcc-container'], 10_000)
    if (!appOk) throw new Error('应用根节点未渲染（前端是否已启动？BASE_URL 是否正确？）')
    const waits = shotItem.waits ?? {}
    if (Array.isArray(waits.selectors) && waits.selectors.length > 0) {
      const hit = await waitForAnySelector(page, waits.selectors)
      if (!hit) record.warnings.push(`未命中候选选择器：${waits.selectors.join(' | ')}`)
    }
    if (Array.isArray(waits.texts) && waits.texts.length > 0) {
      const hitText = await waitForAnyText(page, waits.texts)
      if (!hitText) record.warnings.push(`未命中候选文本：${waits.texts.join(' | ')}`)
    }
    if (shotItem.footer) {
      const count = await countFooters(page)
      record.footerCount = count
      record.footerExpected = shotItem.footer.expected
      record.footerKind = shotItem.footer.kind
      if (count !== shotItem.footer.expected) {
        throw new Error(
          `页脚唯一校验失败（${shotItem.footer.kind}）：<footer> 数量 = ${count}（期望 ${shotItem.footer.expected}）`,
        )
      }
    }
    const bodyText = (await page.locator('body').innerText()).trim()
    if (bodyText.length < 20) throw new Error(`页面内容为空（body 文本长度 ${bodyText.length}），可能渲染失败`)
    await page.screenshot({ path: file, fullPage: false, animations: 'disabled' })
    record.ok = true
    record.bytes = readFileSync(file).length
    record.durationMs = Date.now() - startedAt
    log(
      `  PASS → ${record.file}（${record.durationMs}ms${record.warnings.length ? `，${record.warnings.length} 条选择器警告` : ''}）`,
    )
    record.warnings.forEach((warning) => log(`    ⚠ ${warning}`))
    warnings.push(...record.warnings.map((warning) => `${shotItem.name}: ${warning}`))
  } catch (error) {
    failed += 1
    record.error = error instanceof Error ? error.message : String(error)
    record.durationMs = Date.now() - startedAt
    log(`  FAIL ${record.error}`)
    try {
      const failedFile = join(OUT_DIR, `FAILED-${shotItem.name}.png`)
      await page.screenshot({ path: failedFile, fullPage: false, animations: 'disabled' })
      record.failedScreenshot = repoRel(failedFile)
    } catch {
      /* 忽略 */
    }
  }
  records.push(record)
  return record
}

/* ========================================================================
 * 响应式横向溢出检测
 * ====================================================================== */

async function runResponsiveChecks(adminPage, publicPage, seed) {
  const viewports = [1440, 1280, 1024, 900, 768]
  const checks = [
    { name: 'admin-overview', page: adminPage, url: `${BASE_URL}/admin/overview` },
    { name: 'admin-projects', page: adminPage, url: `${BASE_URL}/admin/projects` },
    { name: 'admin-page-list', page: adminPage, url: `${BASE_URL}/admin/projects/${seed.project.id}/pages` },
    {
      name: 'admin-editor',
      page: adminPage,
      url: `${BASE_URL}/admin/projects/${seed.project.id}/pages/${seed.homePageId}/editor`,
    },
    { name: 'public-home', page: publicPage, url: `${BASE_URL}/community/${seed.project.slug}` },
    { name: 'public-changelog', page: publicPage, url: `${BASE_URL}/community/${seed.project.slug}/changelog` },
    { name: 'public-docs', page: publicPage, url: `${BASE_URL}/community/${seed.project.slug}/docs` },
    { name: 'public-gallery', page: publicPage, url: `${BASE_URL}/community` },
  ]
  log('\n▶ 响应式横向溢出检测（1440 / 1280 / 1024 / 900 / 768）')
  for (const check of checks) {
    const row = { page: check.name, results: [] }
    for (const width of viewports) {
      try {
        await check.page.setViewportSize({ width, height: width === 768 ? 900 : 900 })
        await check.page.goto(check.url, { waitUntil: 'domcontentloaded', timeout: 30_000 })
        await settle(check.page)
        const metrics = await check.page.evaluate(() => ({
          scrollWidth: document.documentElement.scrollWidth,
          innerWidth: window.innerWidth,
          clientWidth: document.documentElement.clientWidth,
        }))
        const overflow = Math.max(metrics.scrollWidth, metrics.clientWidth) - metrics.innerWidth
        row.results.push({ width, overflowPx: overflow, overflow: overflow > 1 })
        if (overflow > 1) log(`  ⚠ ${check.name} @${width} 横向溢出 ${overflow}px`)
      } catch (error) {
        row.results.push({ width, error: error instanceof Error ? error.message : String(error) })
        log(`  ⚠ ${check.name} @${width} 检测失败：${error instanceof Error ? error.message : error}`)
      }
    }
    const overflowCount = row.results.filter((item) => item.overflow).length
    log(`  ${overflowCount === 0 ? 'PASS' : `FAIL（${overflowCount} 个视口溢出）`} ${check.name}`)
    responsiveResults.push(row)
  }
  for (const page of [adminPage, publicPage]) {
    try {
      await page.setViewportSize(VIEWPORT)
    } catch {
      /* 忽略 */
    }
  }
}

/* ========================================================================
 * README / capture-log
 * ====================================================================== */

function writeReadme(environment) {
  const okRecords = records.filter((record) => record.ok)
  const footerRecords = records.filter((record) => record.footerCount !== undefined)
  const responsiveFail = responsiveResults.filter((row) => row.results.some((item) => item.overflow))
  const lines = [
    '# 运行截图（v3.1 · xAI 风格 UI）',
    '',
    '> 本目录图片由 `scripts/capture-screenshots.mjs` 在**真实运行的 v3.1 系统**上采集（playwright-core + 本机 Chrome，',
    '> 1440×900 视口，中文）。覆盖 xAI 风格控制台（AdminShell 侧边栏 + 近黑画布）、管理端内页与公开社区端；',
    '> **所有截图均非伪造**，失败项以 `FAILED-*.png` 保留现场并在 `capture-log.json` 记录。',
    '',
    '## 采集环境',
    '',
    `- 前端地址：${environment.baseUrl}`,
    `- 后端 API：${environment.apiUrl}`,
    `- 演示站点：\`${environment.seedSlug}\`（projectId=${environment.projectId}，homePageId=${environment.homePageId}）`,
    `- 登录账号：\`${environment.adminUsername}\`（初始开发者，密码来自环境变量，不写入仓库）`,
    `- 临时开发者 B：\`${environment.devBUsername}\`（用于空列表/越权提示截图；建议截图后重置演示数据库）`,
    `- 浏览器：Chrome（playwright-core 复用本机浏览器）；视口：1440×900`,
    `- 采集时间：${environment.capturedAt}`,
    '',
    `## 截图清单（成功 ${okRecords.length} / 计划 ${records.length}）`,
    '',
    '| # | 截图 | 文件名 | 内容 | footer 数 | 状态 |',
    '|---|---|---|---|---|---|',
    ...records.map(
      (record, index) =>
        `| ${index + 1} | ${record.title} | [${record.file.split('/').pop()}](${record.file.split('/').pop()}) | ${record.description} | ${
          record.footerCount === undefined ? '-' : record.footerCount
        } | ${record.ok ? '✅ 已采集' : '❌ 失败（见 capture-log.json）'} |`,
    ),
    '',
    '### 页脚唯一校验（FIX-1）',
    '',
    '公开**站点页**强制校验 `<footer>` 数量 = 1（≠1 判失败）；平台级「游戏门户广场」不属于站点壳，无站点页脚，记录为 0 并在清单中注明例外：',
    '',
    '| 截图 | 类型 | footer 数 | 期望 | 结果 |',
    '|---|---|---|---|---|',
    ...footerRecords.map(
      (record) =>
        `| ${record.name} | ${record.footerKind ?? '-'} | ${record.footerCount} | ${record.footerExpected} | ${record.ok ? '✅' : '❌'} |`,
    ),
    '',
    `### 响应式横向溢出（1440 / 1280 / 1024 / 900 / 768）`,
    '',
    responsiveFail.length === 0
      ? `全部 ${responsiveResults.length} 个页面 × 5 个视口检测：**无横向溢出**（详见 capture-log.json 的 responsive 字段）。`
      : `⚠️ 检测到 ${responsiveFail.length} 个页面在部分视口存在横向溢出，详见 capture-log.json 的 responsive 字段（如实记录，未做隐藏）。`,
    '',
    '## 归档',
    '',
    '- `v3.0-archive/`：v3（多页面门户 + 认证/主题/Markdown 修复）阶段的 22 张截图与当时的 capture-log/README，供版本演进对照；',
    '- `v2-archive/`：v2 阶段 14 张截图；',
    '- 根目录为本轮 **v3.1 xAI 风格 UI** 截图，材料与报告均引用根目录文件。',
    '',
    '## 重新采集',
    '',
    '```bash',
    '# 前置：后端（18080）与前端 preview（4173，代理 /api）已启动；seed 演示态',
    "SHOT_ADMIN_USERNAME=dev SHOT_ADMIN_PASSWORD='****' \\",
    'BASE_URL=http://localhost:4173 API_URL=http://localhost:18080/api \\',
    '  node scripts/capture-screenshots.mjs',
    '```',
    '',
    '> 采集过程不保存任何业务数据：编辑器内添加按钮仅内存态、Markdown 编辑对话框不保存、公开端只读。',
    '',
  ]
  writeFileSync(join(OUT_DIR, 'README.md'), lines.join('\n'), 'utf8')
}

/* ========================================================================
 * 主流程
 * ====================================================================== */

async function resolveSeedViaApi(nodeApi) {
  const projects = await nodeApi.get('/projects')
  const project = (projects ?? []).find((item) => item.slug === SEED_SLUG)
  if (!project) {
    throw new Error(
      `未找到 seed 演示站点 slug="${SEED_SLUG}"（当前账号项目：${
        (projects ?? []).map((item) => item.slug).join(', ') || '空'
      }）。请确认初始开发者已认领 seed 项目。`,
    )
  }
  const pages = await nodeApi.get(`/projects/${project.id}/pages`)
  const homePage = (pages ?? []).find((page) => page.isHome) ?? (pages ?? []).find((page) => page.pageType === 'home')
  if (!homePage) throw new Error('seed 站点缺少 home 页面')
  let changelogDetailUrl = `${BASE_URL}/community/${SEED_SLUG}/changelog`
  let docDetailUrl = `${BASE_URL}/community/${SEED_SLUG}/docs`
  try {
    const changelogs = await nodeApi.get(`/projects/${project.id}/changelogs?pageNum=1&pageSize=10&status=PUBLISHED`)
    const first = (changelogs?.items ?? [])[0]
    if (first) changelogDetailUrl = `${BASE_URL}/community/${SEED_SLUG}/changelog/${first.id}`
  } catch {
    /* 忽略 */
  }
  try {
    const docs = await nodeApi.get(`/projects/${project.id}/docs?pageNum=1&pageSize=10&status=PUBLISHED`)
    const first = (docs?.items ?? [])[0]
    if (first?.slug) docDetailUrl = `${BASE_URL}/community/${SEED_SLUG}/docs/${first.slug}`
  } catch {
    /* 忽略 */
  }
  return { project, pages, homePageId: homePage.pageId, changelogDetailUrl, docDetailUrl }
}

async function main() {
  log('='.repeat(84))
  log('GameCommunityCreator-MVP v3.1 · xAI 风格 UI 真实截图采集')
  log(`前端：${BASE_URL}   后端：${API_URL}   演示站点：${SEED_SLUG}`)
  log('='.repeat(84))

  if (!ADMIN_USERNAME || !ADMIN_PASSWORD) {
    throw new Error(
      '缺少初始开发者账号环境变量：请设置 SHOT_ADMIN_USERNAME / SHOT_ADMIN_PASSWORD（或 APP_BOOTSTRAP_USERNAME / APP_BOOTSTRAP_PASSWORD）。',
    )
  }
  const chromePath = findChrome()
  if (!chromePath) throw new Error('未找到本机 Chrome，请设置 CHROME_PATH')

  let health
  try {
    health = await (await fetch(`${API_URL}/health`, { signal: AbortSignal.timeout(8000) })).json()
  } catch (error) {
    throw new Error(`后端不可用（${API_URL}）：${error.message}`)
  }
  if (health?.data?.status !== 'UP' || health?.data?.database !== 'UP') {
    throw new Error(`后端健康检查未通过：${JSON.stringify(health)}`)
  }
  try {
    const probe = await fetch(BASE_URL, { signal: AbortSignal.timeout(8000) })
    if (!probe.ok) throw new Error(`HTTP ${probe.status}`)
  } catch (error) {
    throw new Error(`前端不可用（${BASE_URL}）：${error.message}。请先 npm run build && npm run preview。`)
  }

  const nodeDev = new NodeApi('admin')
  const devAuth = await nodeDev.post('/auth/login', { username: ADMIN_USERNAME, password: ADMIN_PASSWORD })
  const devUser = devAuth.user
  log(`已登录初始开发者：${devUser.displayName}（${devUser.username}）`)
  const seed = await resolveSeedViaApi(nodeDev)
  log(`seed 站点：${seed.project.name}（slug=${seed.project.slug}, id=${seed.project.id}）  首页=${seed.homePageId}`)

  let devBRegistered = false
  try {
    const nodeB = new NodeApi('devB')
    await nodeB.post(
      '/auth/register',
      { username: DEV_B_USERNAME, password: DEV_B_PASSWORD, displayName: '截图临时开发者 B' },
      201,
    )
    devBRegistered = true
    log(`已注册临时开发者 B：${DEV_B_USERNAME}`)
  } catch (error) {
    warnings.push(`注册开发者 B 失败（将跳过空列表/越权截图）：${error.message}`)
  }

  const plan = buildPlan({
    project: seed.project,
    homePageId: seed.homePageId,
    changelogDetailUrl: seed.changelogDetailUrl,
    docDetailUrl: seed.docDetailUrl,
  })
  mkdirSync(OUT_DIR, { recursive: true })

  const requireFromFrontend = createRequire(resolve(REPO, 'frontend/package.json'))
  const { chromium } = requireFromFrontend('playwright-core')
  const browser = await chromium.launch({
    executablePath: chromePath,
    headless: true,
    args: ['--no-sandbox', '--disable-gpu', '--hide-scrollbars', '--force-color-profile=srgb'],
  })

  const makeContext = async () => {
    const context = await browser.newContext({
      viewport: VIEWPORT,
      deviceScaleFactor: 1,
      locale: 'zh-CN',
      timezoneId: 'Asia/Shanghai',
      colorScheme: 'dark',
    })
    await context.addInitScript(() => {
      const css =
        '*,*::before,*::after{transition:none!important;animation:none!important;caret-color:transparent!important}'
      const inject = () => {
        try {
          const style = document.createElement('style')
          style.textContent = css
          ;(document.head ?? document.documentElement).appendChild(style)
        } catch {
          /* 忽略 */
        }
      }
      if (document.head) inject()
      else document.addEventListener('DOMContentLoaded', inject, { once: true })
    })
    return context
  }

  const capturedAt = new Date().toISOString()
  const contexts = {}
  try {
    contexts.anon = await makeContext()
    const anonPage = await contexts.anon.newPage()
    anonPage.setDefaultTimeout(15_000)
    anonPage.on('pageerror', (error) => warnings.push(`pageerror(anon): ${error.message}`))
    for (const shotItem of plan.filter((item) => item.auth === 'anon')) await captureShot(anonPage, shotItem)

    contexts.admin = await makeContext()
    const adminPage = await contexts.admin.newPage()
    adminPage.setDefaultTimeout(15_000)
    adminPage.on('pageerror', (error) => warnings.push(`pageerror(admin): ${error.message}`))
    await loginInBrowser(adminPage, ADMIN_USERNAME, ADMIN_PASSWORD)
    for (const shotItem of plan.filter((item) => item.auth === 'admin')) await captureShot(adminPage, shotItem)

    contexts.public = await makeContext()
    const publicPage = await contexts.public.newPage()
    publicPage.setDefaultTimeout(15_000)
    publicPage.on('pageerror', (error) => warnings.push(`pageerror(public): ${error.message}`))
    for (const shotItem of plan.filter((item) => item.auth === 'public')) await captureShot(publicPage, shotItem)

    if (devBRegistered) {
      contexts.devB = await makeContext()
      const devBPage = await contexts.devB.newPage()
      devBPage.setDefaultTimeout(15_000)
      devBPage.on('pageerror', (error) => warnings.push(`pageerror(devB): ${error.message}`))
      await loginInBrowser(devBPage, DEV_B_USERNAME, DEV_B_PASSWORD)
      for (const shotItem of plan.filter((item) => item.auth === 'devB')) await captureShot(devBPage, shotItem)
    } else {
      for (const shotItem of plan.filter((item) => item.auth === 'devB')) {
        records.push({ name: shotItem.name, title: shotItem.title, ok: false, error: '开发者 B 未注册成功，跳过' })
        failed += 1
      }
    }

    await runResponsiveChecks(adminPage, publicPage, seed)
  } finally {
    for (const context of Object.values(contexts)) await context.close().catch(() => {})
    await browser.close().catch(() => {})
  }

  const environment = {
    baseUrl: BASE_URL,
    apiUrl: API_URL,
    seedSlug: seed.project.slug,
    projectId: seed.project.id,
    homePageId: seed.homePageId,
    adminUsername: ADMIN_USERNAME,
    devBUsername: devBRegistered ? DEV_B_USERNAME : '(未注册)',
    chrome: chromePath,
    viewport: `${VIEWPORT.width}x${VIEWPORT.height}`,
    capturedAt,
    note:
      'v3.1 xAI UI 截图：真实运行生成；不保存业务数据（编辑器加按钮仅内存态、Markdown 对话框未保存）；公开站点页校验 footer=1，平台广场页无站点页脚（0，例外）。临时开发者 B 建议截图后由 Lead 重置 app_user 清除。',
  }
  writeReadme(environment)
  writeFileSync(
    join(OUT_DIR, 'capture-log.json'),
    JSON.stringify({ generatedAt: capturedAt, environment, records, warnings, responsive: responsiveResults }, null, 2),
    'utf8',
  )

  const okCount = records.filter((record) => record.ok).length
  const responsiveFail = responsiveResults.filter((row) => row.results.some((item) => item.overflow))
  log(`\n${'='.repeat(84)}`)
  log(`截图完成：成功 ${okCount}/${records.length} 张 → ${repoRel(OUT_DIR)}/`)
  log(
    `响应式检测：${responsiveResults.length} 个页面 × 5 视口，${responsiveFail.length === 0 ? '无横向溢出' : `${responsiveFail.length} 个页面存在溢出（见 capture-log.json）`}`,
  )
  if (warnings.length > 0) {
    log(`\n${warnings.length} 条警告：`)
    warnings.slice(0, 30).forEach((warning) => log(`  ⚠ ${warning}`))
  }
  log(okCount === records.length && failed === 0 ? '✅ 全部截图采集成功（含页脚唯一校验）' : '❌ 存在采集失败项，见 capture-log.json')
  log('='.repeat(84))
  process.exitCode = okCount === records.length && failed === 0 ? 0 : 1
}

main().catch((error) => {
  console.error(`\n❌ 截图采集失败：${error instanceof Error ? error.message : error}`)
  process.exitCode = 1
})
