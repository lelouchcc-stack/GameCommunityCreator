#!/usr/bin/env node
/**
 * GameCommunityCreator-MVP —— v3 API 端到端冒烟/验收脚本（零依赖，Node 18+ 内置 fetch）。
 *
 * 覆盖 v3 修复（FIX-1~4）与 v2 多页面门户回归：
 *   认证（JWT/注册/登录/登出/me）→ 匿名 401 → 开发者 A/B 所有权隔离（403）
 *   → 多项目 / owner 不可伪造 / bootstrap 初始账号与 seed 旧项目可读 → 页面/草稿/发布/导航/设置
 *   → 按钮动作 → 内容模块 CRUD（Markdown：changelog/doc/announcement bodyMarkdown + 公告 PUT）
 *   → 公开端快照/公开 Markdown/匿名问题反馈 → 409/400/404 → 删除级联清理。
 *
 * 认证模型（docs/API.md §11–§13，v4.1 Sa-Token + JWT Simple）：
 *   - token 为 JWT 格式（Bearer Token）：登录/注册响应 data.token，后续请求带 Authorization: Bearer <token>
 *   - 脚本用「ApiClient 持有 token + 请求自动加 Authorization 头」模拟客户端；A/B 各自独立的
 *     客户端实例与 token 互不影响。Sa-Token Simple 模式：登录态存服务端，登出后旧 token 立即失效
 *   - 初始开发者账号从环境变量读取（脚本不内置任何真实口令）：
 *       SMOKE_ADMIN_USERNAME / SMOKE_ADMIN_PASSWORD（或 APP_BOOTSTRAP_USERNAME / APP_BOOTSTRAP_PASSWORD）
 *     未提供时跳过「bootstrap 认领 seed 项目」步骤（SKIP，不计失败）。
 *
 * 用法：
 *   node scripts/smoke-test.mjs                          # 默认后端 http://localhost:8080
 *   node scripts/smoke-test.mjs --base-url http://localhost:18080
 *   SMOKE_ADMIN_USERNAME=dev SMOKE_ADMIN_PASSWORD=**** node scripts/smoke-test.mjs
 *   node scripts/smoke-test.mjs --keep                   # 保留测试数据（排查用）
 *
 * 退出码：0 = 全部 PASS（SKIP 不算失败）；1 = 存在 FAIL 或后端不可用。
 * 输出：逐项 PASS/FAIL/SKIP + 关键响应摘要 + 末尾汇总；不打印任何密码。
 */

const argv = process.argv.slice(2)
function readArg(name) {
  const index = argv.indexOf(name)
  return index >= 0 ? argv[index + 1] : undefined
}

const KEEP_DATA = argv.includes('--keep')
const BASE_URL = (readArg('--base-url') || process.env.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
const API_ROOT = `${BASE_URL}/api`
const REQUEST_TIMEOUT_MS = 20_000

const ADMIN_USERNAME =
  process.env.SMOKE_ADMIN_USERNAME || process.env.APP_BOOTSTRAP_USERNAME || ''
const ADMIN_PASSWORD =
  process.env.SMOKE_ADMIN_PASSWORD || process.env.APP_BOOTSTRAP_PASSWORD || ''

const RUN_ID = Date.now().toString(36) + Math.floor(Math.random() * 36 ** 2).toString(36).padStart(2, '0')
const USER_A = `smokea${RUN_ID}`
const USER_B = `smokeb${RUN_ID}`
const PASSWORD = 'SmokePass2026'
const DISPLAY_A = '冒烟开发者 A'
const DISPLAY_B = '冒烟开发者 B'
const SLUG_A = `smoke-a-${RUN_ID}`
const SLUG_A2 = `smoke-a2-${RUN_ID}`
const SLUG_A3 = `smoke-a3-${RUN_ID}`
const SLUG_B = `smoke-b-${RUN_ID}`

/* ========================================================================
 * 断言与输出
 * ====================================================================== */

class SmokeAssertionError extends Error {
  constructor(message) {
    super(message)
    this.name = 'SmokeAssertionError'
  }
}

function assert(condition, message) {
  if (!condition) throw new SmokeAssertionError(message)
}
function assertEqual(actual, expected, label) {
  if (actual !== expected) {
    throw new SmokeAssertionError(`${label}：期望 ${JSON.stringify(expected)}，实际 ${JSON.stringify(actual)}`)
  }
}
function assertDeepEqual(actual, expected, label) {
  const a = JSON.stringify(actual)
  const e = JSON.stringify(expected)
  if (a !== e) throw new SmokeAssertionError(`${label}：期望 ${e}，实际 ${a}`)
}
function assertIncludes(list, needle, label) {
  if (!Array.isArray(list) || !list.includes(needle)) {
    throw new SmokeAssertionError(`${label}：期望包含 ${JSON.stringify(needle)}，实际 ${JSON.stringify(list)}`)
  }
}
function assertNonEmptyString(value, label) {
  assert(typeof value === 'string' && value.trim().length > 0, `${label} 应为非空字符串`)
}
function assertMatch(value, pattern, label) {
  assert(pattern.test(String(value ?? '')), `${label}：${JSON.stringify(value)} 不匹配 ${pattern}`)
}
function preview(text, max = 500) {
  const value = String(text ?? '')
  return value.length > max ? `${value.slice(0, max)}…` : value
}
function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

let passedCount = 0
let failedCount = 0
let skippedCount = 0
const failures = []

async function step(name, fn) {
  console.log(`\n▶ ${name}`)
  try {
    const detail = await fn()
    if (detail === 'SKIP') {
      skippedCount += 1
      console.log('  SKIP（缺少所需环境变量，按约定跳过且不计失败）')
      return { ok: true, skipped: true }
    }
    passedCount += 1
    console.log(`  PASS ${detail ? `— ${detail}` : ''}`)
    return { ok: true }
  } catch (error) {
    failedCount += 1
    const message = error instanceof Error ? error.message : String(error)
    failures.push({ name, message })
    console.log(`  FAIL ${message.split('\n').join('\n       ')}`)
    return { ok: false }
  }
}

/* ========================================================================
 * ApiClient（Sa-Token + JWT / Bearer Token）
 * ====================================================================== */

class ApiClient {
  constructor(label, baseUrl = API_ROOT) {
    this.label = label
    this.baseUrl = baseUrl
    /** 登录/注册成功后自动保存；登出时服务端会话失效，客户端同步清空 */
    this.token = null
  }

  /** 保存认证响应里的 JWT（登录、注册共用） */
  adoptToken(res) {
    const token = res?.data?.token
    assertNonEmptyString(token, `[${this.label}] 认证响应 data.token`)
    this.token = token
    return token
  }

  clearToken() {
    this.token = null
  }

  async raw(method, path, options = {}) {
    const { body, expectStatus = 200, auth = true, headers: extraHeaders = {} } = options
    const headers = { ...extraHeaders }
    if (body !== undefined) headers['Content-Type'] = 'application/json; charset=UTF-8'
    // 持有 token 即自动带 Bearer 头；显式 auth:false 用于验证「未认证」路径
    if (auth && this.token) headers.Authorization = `Bearer ${this.token}`
    let response
    try {
      response = await fetch(`${this.baseUrl}${path}`, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
      })
    } catch (error) {
      throw new SmokeAssertionError(
        `[${this.label}] ${method} ${path} 无法连接：${error?.message ?? error}（后端是否已启动？BASE_URL=${BASE_URL}）`,
      )
    }
    const text = await response.text()
    let envelope = null
    try {
      envelope = text ? JSON.parse(text) : null
    } catch {
      envelope = null
    }
    const label = `[${this.label}] ${method} ${path} → HTTP ${response.status}`
    if (response.status !== expectStatus) {
      throw new SmokeAssertionError(`${label}\n       期望 HTTP ${expectStatus}\n       实际响应：${preview(text)}`)
    }
    return {
      status: response.status,
      envelope,
      data: envelope?.data,
      message: envelope?.message,
      fieldErrors: envelope?.fieldErrors,
      text,
      headers: response.headers,
    }
  }

  /** 登录：成功后（200）自动保存 data.token */
  async login(username, password, options = {}) {
    const res = await this.raw('POST', '/auth/login', {
      expectStatus: 200,
      ...options,
      body: { username, password },
    })
    if (res.status >= 200 && res.status < 300) this.adoptToken(res)
    return res
  }

  /** 注册：注册即签发 token（201），成功后自动保存 */
  async register(username, password, displayName, options = {}) {
    const res = await this.raw('POST', '/auth/register', {
      expectStatus: 201,
      ...options,
      body: { username, password, displayName },
    })
    if (res.status >= 200 && res.status < 300) this.adoptToken(res)
    return res
  }

  /** 登出（需带 Bearer）：Sa-Token 注销服务端会话（旧 token 立即失效），客户端同步清空 token */
  async logout(options = {}) {
    const res = await this.raw('POST', '/auth/logout', { expectStatus: 204, ...options })
    this.clearToken()
    return res
  }

  get(path, options = {}) {
    return this.raw('GET', path, options)
  }
  post(path, body, options = {}) {
    return this.raw('POST', path, { expectStatus: 201, ...options, body })
  }
  put(path, body, options = {}) {
    return this.raw('PUT', path, { ...options, body })
  }
  patch(path, body, options = {}) {
    return this.raw('PATCH', path, { ...options, body })
  }
  delete(path, options = {}) {
    return this.raw('DELETE', path, options)
  }
}

/** 断言 400/403/409 响应带 fieldErrors（可选） */
function assertFieldErrors(res, patterns, label) {
  assert(res.fieldErrors && typeof res.fieldErrors === 'object', `${label}：响应应带 fieldErrors`)
  const keys = Object.keys(res.fieldErrors)
  assert(keys.length > 0, `${label}：fieldErrors 不应为空`)
  assert(
    keys.some((key) => patterns.some((pattern) => pattern.test(key))),
    `${label}：fieldErrors 应包含 ${patterns.map(String).join(' 或 ')}，实际 ${JSON.stringify(keys)}`,
  )
}

/** 校验认证响应契约 data = { token, tokenType: "Bearer", expiresIn, user: { id, username, displayName } } */
function assertAuthPayload(res, username, label) {
  const data = res.data
  assert(data && typeof data === 'object', `${label} 响应应带 data`)
  assertNonEmptyString(data.token, `${label} data.token`)
  assertEqual(data.tokenType, 'Bearer', `${label} data.tokenType`)
  assert(typeof data.expiresIn === 'number' && data.expiresIn > 0, `${label} data.expiresIn 应为正数（秒）`)
  assert(data.user && typeof data.user === 'object', `${label} data.user`)
  assert(typeof data.user.id === 'number', `${label} data.user.id 应为数字`)
  assertEqual(data.user.username, username, `${label} data.user.username`)
  assertNonEmptyString(data.user.displayName, `${label} data.user.displayName`)
  return data
}

async function registerUser(client, username, displayName) {
  const res = await client.register(username, PASSWORD, displayName)
  assertEqual(res.status, 201, `[${client.label}] 注册应返回 201`)
  const data = assertAuthPayload(res, username, `[${client.label}] 注册`)
  assertEqual(client.token, data.token, `[${client.label}] 注册成功后应自动保存 token`)
  return data
}

async function loginUser(client, username, password) {
  const res = await client.login(username, password)
  assertEqual(res.status, 200, `[${client.label}] 登录应返回 200`)
  const data = assertAuthPayload(res, username, `[${client.label}] 登录`)
  assertEqual(client.token, data.token, `[${client.label}] 登录成功后应自动保存 token`)
  return res
}

/** 篡改 JWT 签名段中部字符（该位置 6 bit 全部有效），确保签名校验必然失败 */
function tamperToken(token) {
  assertNonEmptyString(token, '待篡改的 JWT')
  const parts = token.split('.')
  assertEqual(parts.length, 3, 'JWT 应为 header.payload.signature 三段')
  const signature = parts[2]
  assert(signature.length >= 4, 'JWT 签名段过短')
  const index = Math.floor(signature.length / 2)
  const replacement = signature[index] === 'A' ? 'B' : 'A'
  parts[2] = signature.slice(0, index) + replacement + signature.slice(index + 1)
  return parts.join('.')
}

/* ========================================================================
 * 工具
 * ====================================================================== */

function componentByType(config, type) {
  return (config.components ?? []).find((component) => component.type === type)
}

async function getPageDraft(client, projectId, pageId) {
  const res = await client.get(`/projects/${projectId}/pages/${pageId}/page`)
  assert(res.data?.config, 'GET 页面草稿缺少 data.config')
  return res.data
}

async function savePageDraft(client, projectId, pageId, mutate) {
  const before = await getPageDraft(client, projectId, pageId)
  const config = clone(before.config)
  const next = mutate(config) ?? config
  const res = await client.put(`/projects/${projectId}/pages/${pageId}/page`, {
    config: next,
    expectedVersion: before.configVersion,
  })
  return { before, after: res.data }
}

async function savePageDraftExpect(client, expectStatus, projectId, pageId, mutate) {
  const before = await getPageDraft(client, projectId, pageId)
  const config = clone(before.config)
  mutate(config)
  const res = await client.put(
    `/projects/${projectId}/pages/${pageId}/page`,
    { config, expectedVersion: before.configVersion },
    { expectStatus },
  )
  return res
}

function makeButton(targetPageId) {
  return {
    id: 'cmp_smoke_btn',
    type: 'button',
    title: '冒烟按钮',
    visible: true,
    dataSource: null,
    props: { label: '去更新日志', variant: 'primary', icon: '', align: 'left' },
    action: { type: 'navigate', targetPageId },
  }
}

const state = {
  projectA1: null,
  projectA2: null,
  projectA3: null,
  projectB: null,
  pageIdsA1: [],
  homeA1: null,
  changelogA1: null,
  docsA1: null,
  issueA1: null,
  deleted: false,
}

async function createProject(client, name, slug, gameName = '冒烟测试游戏', extra = {}) {
  const res = await client.post('/projects', {
    name,
    slug,
    gameName,
    description: '由 scripts/smoke-test.mjs 自动创建，测试结束会删除',
    ...extra,
  })
  assertEqual(res.status, 201, '创建站点应返回 201')
  return res.data
}

/* ========================================================================
 * 主流程
 * ====================================================================== */

async function run() {
  console.log('='.repeat(86))
  console.log('GameCommunityCreator-MVP · v3 API 端到端冒烟验收（Sa-Token + JWT 认证 + 所有权 + Markdown + v2 回归）')
  console.log(`环境：BASE_URL=${BASE_URL}  runId=${RUN_ID}  A=${USER_A}  B=${USER_B}`)
  console.log(`bootstrap 初始账号：${ADMIN_USERNAME ? '已通过环境变量提供' : '未提供（相关步骤将 SKIP）'}`)
  console.log('='.repeat(86))

  const anon = new ApiClient('匿名')
  const clientA = new ApiClient('开发者A')
  const clientB = new ApiClient('开发者B')
  const clientAdmin = new ApiClient('初始开发者')

  // 1. 健康检查 ---------------------------------------------------------------
  const health = await step('1. 健康检查 GET /api/health', async () => {
    const res = await anon.get('/health')
    assertEqual(res.data?.status, 'UP', 'data.status')
    assertEqual(res.data?.database, 'UP', 'data.database')
    return `status=UP, database=UP`
  })
  if (!health.ok) return

  // 2. 匿名访问管理接口 401 ---------------------------------------------------
  await step('2. 匿名访问管理接口 → 401（FIX-3）', async () => {
    const list = await anon.get('/projects', { expectStatus: 401 })
    assertMatch(list.message, /登录|未登录/, '匿名 GET /projects 提示')
    const pages = await anon.get('/projects/1/pages', { expectStatus: 401 })
    assertNonEmptyString(pages.message, '匿名 GET 页面列表 message')
    const create = await anon.post('/projects', { name: 'x', slug: `anon-${RUN_ID}` }, { expectStatus: 401 })
    assertNonEmptyString(create.message, '匿名创建项目 message')
    return `GET /projects、GET /projects/1/pages、POST /projects 均 401`
  })

  // 3. 注册（注册即签发 JWT）/ me / token 契约（A/B 各自独立 token） ----------
  await step('3. 注册开发者 A/B（注册即签发 JWT）+ me + token 契约', async () => {
    const registeredA = await registerUser(clientA, USER_A, DISPLAY_A)
    assertNonEmptyString(clientA.token, 'A 注册后应持有 token')
    const meA = await clientA.get('/auth/me')
    assertEqual(meA.data?.username, USER_A, 'A /auth/me username')
    assertEqual(meA.data?.id, registeredA.user.id, 'A /auth/me id 与注册响应一致')
    const registeredB = await registerUser(clientB, USER_B, DISPLAY_B)
    assertNonEmptyString(clientB.token, 'B 注册后应持有 token')
    const meB = await clientB.get('/auth/me')
    assertEqual(meB.data?.username, USER_B, 'B /auth/me username')
    assertEqual(meB.data?.id, registeredB.user.id, 'B /auth/me id 与注册响应一致')
    assert(meA.data?.id !== meB.data?.id, 'A/B 应为不同用户')
    assert(clientA.token !== clientB.token, 'A/B 应各自持有独立的 token')
    return `A id=${meA.data.id}（${USER_A}），B id=${meB.data.id}（${USER_B}）；注册响应 tokenType=Bearer，客户端已自动保存 token`
  })

  await step('3b. 注册参数校验 400 / 重名 409 / 登录失败 401', async () => {
    const badClient = new ApiClient('注册校验')
    const bad = await badClient.post(
      '/auth/register',
      { username: 'x', password: '123', displayName: '' },
      { expectStatus: 400 },
    )
    assertNonEmptyString(bad.message, '非法注册 400 message')
    assertFieldErrors(bad, [/username/, /password/, /displayName/], '非法注册 fieldErrors')
    assertEqual(badClient.token, null, '注册失败不应保存 token')

    const dup = await badClient.post(
      '/auth/register',
      { username: USER_A, password: PASSWORD, displayName: '重复用户' },
      { expectStatus: 409 },
    )
    assertNonEmptyString(dup.message, '重名注册 409 message')

    const wrong = new ApiClient('错误密码')
    const loginFail = await wrong.post(
      '/auth/login',
      { username: USER_A, password: 'WrongPass123' },
      { expectStatus: 401 },
    )
    assertNonEmptyString(loginFail.message, '登录失败 401 message')
    assertEqual(wrong.token, null, '登录失败不应保存 token')
    return `400 fieldErrors / 409 重名 / 401 错误密码 均符合契约（失败响应不签发 token）`
  })

  await step('3c. 登出（服务端会话失效）/ 重新登录（Sa-Token + JWT Simple）', async () => {
    const oldToken = clientA.token
    await clientA.logout()
    assertEqual(clientA.token, null, '登出后客户端应清空 token')
    const meAfterLogout = await clientA.get('/auth/me', { expectStatus: 401 })
    assertNonEmptyString(meAfterLogout.message, '登出后 /auth/me 401 message')
    assertMatch(meAfterLogout.message, /登录/, '登出后 /auth/me 提示')
    // Simple 模式：登出会删除服务端会话，旧 token 立即失效（不是“客户端丢弃才失效”）
    const staleClient = new ApiClient('旧令牌')
    staleClient.token = oldToken
    const meWithOldToken = await staleClient.get('/auth/me', { expectStatus: 401 })
    assertMatch(meWithOldToken.message, /登录/, '登出后旧 token 应失效')
    const relogin = await loginUser(clientA, USER_A, PASSWORD)
    assertEqual(relogin.data?.user?.username, USER_A, '重新登录 data.user.username')
    assertNonEmptyString(clientA.token, '重新登录后应持有 token')
    const meAgain = await clientA.get('/auth/me')
    assertEqual(meAgain.data?.username, USER_A, '重新登录后 /auth/me')
    return `logout 204 → 旧 token 立即 401（服务端会话已删） → 不带 token 的 /auth/me 401 → login 200 → me 恢复`
  })

  await step('3d. JWT 安全：无 Authorization 写请求 401 / 篡改 token 写请求 401', async () => {
    const anonymousWrite = await clientA.raw('POST', '/projects', {
      expectStatus: 401,
      auth: false,
      body: { name: '无令牌项目', slug: `no-token-${RUN_ID}`, gameName: 'x', description: '' },
    })
    assertNonEmptyString(anonymousWrite.message, '无 Authorization 写请求 401 message')

    const forgedClient = new ApiClient('篡改令牌')
    forgedClient.token = tamperToken(clientA.token)
    assert(forgedClient.token !== clientA.token, '篡改后的 token 应不同于原 token')
    const forgedWrite = await forgedClient.post(
      '/projects',
      { name: '篡改令牌项目', slug: `forged-token-${RUN_ID}`, gameName: 'x', description: '' },
      { expectStatus: 401 },
    )
    assertNonEmptyString(forgedWrite.message, '篡改 token 写请求 401 message')
    return `无 Authorization → 401；被篡改的 token → 401（token 无法解析或服务端会话不存在）`
  })

  // 4. 创建项目：A 多项目 / B 项目 / owner 不可伪造 --------------------------
  await step('4. A 创建 2 个项目（多项目）+ B 创建 1 个项目', async () => {
    state.projectA1 = await createProject(clientA, '冒烟站点 A1', SLUG_A, '冒烟游戏 A1')
    state.projectA2 = await createProject(clientA, '冒烟站点 A2', SLUG_A2, '冒烟游戏 A2')
    state.projectB = await createProject(clientB, '冒烟站点 B', SLUG_B, '冒烟游戏 B')
    assertEqual(state.projectA1.pageCount, 6, 'A1 自动 6 页面')
    return `A: ${SLUG_A}（id=${state.projectA1.id}）、${SLUG_A2}（id=${state.projectA2.id}）；B: ${SLUG_B}（id=${state.projectB.id}）`
  })

  await step('4b. owner 不可伪造（请求体 owner_user_id 被忽略/拒绝）', async () => {
    state.projectA3 = await createProject(clientA, '冒烟站点 A3（伪造 owner）', SLUG_A3, '冒烟游戏 A3', {
      owner_user_id: state.projectB.id,
    })
    const listA = await clientA.get('/projects')
    const ids = (listA.data ?? []).map((project) => project.id)
    assertIncludes(ids, state.projectA3.id, 'A 的项目列表应含 A3')
    const bAccess = await clientB.get(`/projects/${state.projectA3.id}`, { expectStatus: 403 })
    assertNonEmptyString(bAccess.message, 'B 访问 A3 403 message')
    return `A3 创建成功且归属 A；B 访问 A3 → 403`
  })

  // 5. 所有权隔离矩阵 --------------------------------------------------------
  await step('5. A/B 项目列表隔离 + B 访问 A 资源 403（FIX-3）', async () => {
    const listA = await clientA.get('/projects')
    const listB = await clientB.get('/projects')
    const idsA = (listA.data ?? []).map((project) => project.id)
    const idsB = (listB.data ?? []).map((project) => project.id)
    assertIncludes(idsA, state.projectA1.id, 'A 列表含 A1')
    assertIncludes(idsA, state.projectA2.id, 'A 列表含 A2')
    assert(!idsA.includes(state.projectB.id), 'A 列表不应含 B 项目')
    assertDeepEqual(idsB, [state.projectB.id], 'B 列表只含 B 项目')

    const a1 = state.projectA1
    for (const path of [
      `/projects/${a1.id}`,
      `/projects/${a1.id}/pages`,
      `/projects/${a1.id}/pages/${a1.homePageId}/page`,
      `/projects/${a1.id}/navigation`,
      `/projects/${a1.id}/settings`,
      `/projects/${a1.id}/game-info`,
      `/projects/${a1.id}/changelogs`,
      `/projects/${a1.id}/docs`,
      `/projects/${a1.id}/issues`,
    ]) {
      const res = await clientB.get(path, { expectStatus: 403 })
      assertNonEmptyString(res.message, `B 访问 ${path} 403 message`)
    }
    const bUpdateA = await clientB.put(`/projects/${a1.id}`, { name: 'x', gameName: 'x', description: '' }, { expectStatus: 403 })
    assertNonEmptyString(bUpdateA.message, 'B 更新 A 项目 403 message')
    const aAccessB = await clientA.get(`/projects/${state.projectB.id}`, { expectStatus: 403 })
    assertNonEmptyString(aAccessB.message, 'A 访问 B 项目 403 message')
    return `隔离矩阵全部 403；A 3 个项目 / B 1 个项目`
  })

  // 6. bootstrap 初始账号与 seed 旧项目（缺环境变量则 SKIP） ------------------
  await step('6. bootstrap 初始开发者登录 + seed 旧项目可读/可编辑（可恢复）', async () => {
    if (!ADMIN_USERNAME || !ADMIN_PASSWORD) return 'SKIP'
    const login = await loginUser(clientAdmin, ADMIN_USERNAME, ADMIN_PASSWORD)
    assertEqual(login.status, 200, '初始开发者登录 HTTP 状态')
    assertNonEmptyString(clientAdmin.token, '初始开发者登录后应持有 token')
    const projects = await clientAdmin.get('/projects')
    const slugs = (projects.data ?? []).map((project) => project.slug)
    assertIncludes(slugs, 'stellar-drift', '初始开发者项目列表含 seed stellar-drift')
    assertIncludes(slugs, 'echo-garden', '初始开发者项目列表含 seed echo-garden')
    const seed = (projects.data ?? []).find((project) => project.slug === 'stellar-drift')
    assert(seed, '应能找到 seed 站点')
    const draft = await getPageDraft(clientAdmin, seed.id, seed.homePageId)
    const original = clone(draft.config)
    const changed = clone(draft.config)
    componentByType(changed, 'about').title = '冒烟临时标题（稍后恢复）'
    const saved = await clientAdmin.put(`/projects/${seed.id}/pages/${seed.homePageId}/page`, {
      config: changed,
      expectedVersion: draft.configVersion,
    })
    assertEqual(saved.data?.configVersion, draft.configVersion + 1, 'seed 草稿保存版本 +1')
    const restored = await clientAdmin.put(`/projects/${seed.id}/pages/${seed.homePageId}/page`, {
      config: original,
      expectedVersion: saved.data.configVersion,
    })
    assertEqual(
      componentByType(restored.data.config, 'about').title,
      componentByType(original, 'about').title,
      'seed 草稿已恢复原内容',
    )
    return `dev 账号（${ADMIN_USERNAME}）可读 seed 两站点；stellar-drift 草稿编辑后已恢复（未发布）`
  })

  // 7. 页面默认模板与草稿（A1）---------------------------------------------
  await step('7. 默认 6 页面 + 默认模板草稿（A1，v2 回归）', async () => {
    const pages = await clientA.get(`/projects/${state.projectA1.id}/pages`)
    const list = pages.data ?? []
    assertEqual(list.length, 6, '默认页面数量')
    const typeToPage = {}
    for (const page of list) typeToPage[page.pageType] = page
    const expectedTypes = ['home', 'changelog', 'docs', 'team', 'issue-report', 'links']
    for (const type of expectedTypes) assert(typeToPage[type], `缺少默认页面类型 ${type}`)
    assertEqual(typeToPage.home.pageId, state.projectA1.homePageId, 'homePageId 一致')
    state.pageIdsA1 = list.map((page) => page.pageId)
    state.homeA1 = typeToPage.home
    state.changelogA1 = typeToPage.changelog
    state.docsA1 = typeToPage.docs
    state.issueA1 = typeToPage['issue-report']

    const homeDraft = await getPageDraft(clientA, state.projectA1.id, state.homeA1.pageId)
    assertEqual(homeDraft.config.schemaVersion, 2, 'home 草稿 schemaVersion')
    assertDeepEqual(
      homeDraft.config.components.map((component) => component.type),
      ['hero', 'about', 'notice_list', 'issue_progress', 'changelog_list', 'docs_list', 'footer'],
      'home 默认模板组件顺序',
    )
    return `6 页面；home 默认模板 7 组件（hero→about→notice_list→issue_progress→changelog_list→docs_list→footer）`
  })

  await step('7b. 保存 home 草稿（标题/主题/按钮动作）+ 页面隔离（A1/A2）', async () => {
    const a2Before = await getPageDraft(clientA, state.projectA2.id, state.projectA2.homePageId)
    const { before, after } = await savePageDraft(clientA, state.projectA1.id, state.homeA1.pageId, (config) => {
      componentByType(config, 'notice_list').title = '最新公告（冒烟）'
      config.theme.primaryColor = '#F25F5C'
      config.components.push(makeButton(state.changelogA1.pageId))
    })
    assertEqual(after.configVersion, before.configVersion + 1, '保存后版本 +1')
    assertEqual(componentByType(after.config, 'notice_list').title, '最新公告（冒烟）', '标题保存回显')
    assertEqual(after.config.theme.primaryColor, '#F25F5C', '主题保存回显')
    assertEqual(
      componentByType(after.config, 'button')?.action?.targetPageId,
      state.changelogA1.pageId,
      '按钮 navigate 目标',
    )
    const a2After = await getPageDraft(clientA, state.projectA2.id, state.projectA2.homePageId)
    assertEqual(a2After.configVersion, a2Before.configVersion, 'A2 草稿版本不受 A1 保存影响')
    return `A1 v${after.configVersion}；A2 仍 v${a2After.configVersion}（页面隔离）`
  })

  await step('7c. 非法配置 400 / 过期版本 409（A1）', async () => {
    const bad = await savePageDraftExpect(clientA, 400, state.projectA1.id, state.homeA1.pageId, (config) => {
      componentByType(config, 'notice_list').props.limit = 99
    })
    assertFieldErrors(bad, [/props\.limit/], '非法 limit')

    const current = await getPageDraft(clientA, state.projectA1.id, state.homeA1.pageId)
    const stale = await clientA.put(
      `/projects/${state.projectA1.id}/pages/${state.homeA1.pageId}/page`,
      { config: current.config, expectedVersion: 1 },
      { expectStatus: 409 },
    )
    assertNonEmptyString(stale.message, '409 冲突 message')
    return `limit 99 → 400 fieldErrors；过期版本 → 409 中文提示`
  })

  // 8. 发布（页面级 + 站点级）与公开端匿名访问 ------------------------------
  await step('8. 页面发布 + 站点发布（A1），公开端匿名可读（FIX-3 公开匿名）', async () => {
    const homeDraft = await getPageDraft(clientA, state.projectA1.id, state.homeA1.pageId)
    const pagePublish = await clientA.post(`/projects/${state.projectA1.id}/pages/${state.homeA1.pageId}/publish`, undefined, { expectStatus: 200 })
    assert(typeof pagePublish.data?.publicationId === 'number', '页面发布 publicationId')
    const sitePublish = await clientA.post(`/projects/${state.projectA1.id}/publish-site`, undefined, { expectStatus: 200 })
    assert(Array.isArray(sitePublish.data?.publishedPages), '站点发布 publishedPages')
    assert(Array.isArray(sitePublish.data?.skippedPages), '站点发布 skippedPages')
    const project = await clientA.get(`/projects/${state.projectA1.id}`)
    assertEqual(project.data?.status, 'PUBLISHED', '站点发布后 status')
    assertEqual(project.data?.publishedPageCount, 6, '已发布页面数')
    const homePage = await clientA.get(`/projects/${state.projectA1.id}/pages/${state.homeA1.pageId}`)
    assertEqual(homePage.data?.status, 'PUBLISHED', 'home 页面状态')

    // 匿名（无 token、不带 Authorization 头）读取公开站点
    const publicSite = await anon.get(`/public/communities/${SLUG_A}`)
    assertEqual(publicSite.data?.project?.slug, SLUG_A, '公开 project.slug')
    assertEqual(publicSite.data?.page?.pageId, state.homeA1.pageId, '公开首页 pageId')
    assertEqual(publicSite.data?.page?.configVersion, homeDraft.configVersion, '公开快照版本=发布时草稿版本')
    assertEqual(
      componentByType(publicSite.data.page.config, 'notice_list').title,
      '最新公告（冒烟）',
      '公开快照内容',
    )
    assertEqual(publicSite.data?.settings?.themeMode, 'DARK', '公开 settings.themeMode')
    assert(Array.isArray(publicSite.data?.navigation), '公开 navigation')
    return `页面发布 + publish-site 成功；匿名 /public/communities/${SLUG_A} 200，快照版本 ${homeDraft.configVersion}`
  })

  await step('8b. 导航与站点主题（A1）+ 公开端生效', async () => {
    const navItems = [
      {
        location: 'header',
        title: '首页',
        targetType: 'page',
        targetPageId: state.homeA1.pageId,
        url: '',
        sortOrder: 0,
        visible: true,
      },
      {
        location: 'header',
        title: '外部链接',
        targetType: 'external',
        targetPageId: '',
        url: 'https://example.com/smoke',
        sortOrder: 1,
        visible: true,
      },
    ]
    const nav = await clientA.put(`/projects/${state.projectA1.id}/navigation`, { items: navItems })
    assertEqual((nav.data?.items ?? []).length, 2, '导航保存条目数')
    const settings = await clientA.put(`/projects/${state.projectA1.id}/settings`, { themeMode: 'LIGHT' })
    assertEqual(settings.data?.themeMode, 'LIGHT', '站点主题保存')
    const publicSite = await anon.get(`/public/communities/${SLUG_A}`)
    assertEqual(publicSite.data?.settings?.themeMode, 'LIGHT', '公开端主题设置生效')
    assert((publicSite.data?.navigation ?? []).length >= 2, '公开导航含 header 条目')
    const invalidTheme = await clientA.put(`/projects/${state.projectA1.id}/settings`, { themeMode: 'BLUE' }, { expectStatus: 400 })
    assertNonEmptyString(invalidTheme.message, '非法 themeMode 400 message')
    await clientA.put(`/projects/${state.projectA1.id}/settings`, { themeMode: 'DARK' })
    return `导航 2 条保存；LIGHT/SYSTEM 校验；公开端 settings 生效；非法值 400`
  })

  // 9. Markdown 内容模块（FIX-4）-------------------------------------------
  await step('9. 更新日志 Markdown（bodyMarkdown 保存/回读/公开响应）', async () => {
    const markdown = '## 新增\n- 边境站建造\n- **星尘采集**\n\n| 模块 | 说明 |\n|---|---|\n| 建造 | 可用 |\n\n> 引用\n\n```js\nconsole.log(1)\n```'
    const created = await clientA.post(`/projects/${state.projectA1.id}/changelogs`, {
      version: 'v1.0.0-smoke',
      title: '冒烟 Markdown 更新日志',
      bodyMarkdown: markdown,
      releaseDate: '2025-05-06',
      status: 'PUBLISHED',
    })
    assertEqual(created.status, 201, '创建更新日志 HTTP 状态')
    assertEqual(created.data?.bodyMarkdown, markdown, '创建响应 bodyMarkdown')
    assertEqual(created.data?.content, markdown, '兼容 content 与 bodyMarkdown 同步')
    state.changelogId = created.data.id

    const adminRead = await clientA.get(`/projects/${state.projectA1.id}/changelogs?pageNum=1&pageSize=10`)
    const item = adminRead.data.items.find((entry) => entry.id === created.data.id)
    assert(item, '管理端列表应含新建日志')
    assertEqual(item.bodyMarkdown, markdown, '管理端回读 bodyMarkdown 原文不丢')

    const publicList = await anon.get(`/public/communities/${SLUG_A}/changelogs?pageNum=1&pageSize=10`)
    const publicItem = publicList.data.items.find((entry) => entry.id === created.data.id)
    assert(publicItem, '公开列表应含 PUBLISHED 日志')
    assertEqual(publicItem.bodyMarkdown, markdown, '公开列表返回 bodyMarkdown')
    const publicDetail = await anon.get(`/public/communities/${SLUG_A}/changelogs/${created.data.id}`)
    assertEqual(publicDetail.data?.bodyMarkdown, markdown, '公开详情 bodyMarkdown')
    return `bodyMarkdown 保存/回读/公开列表/公开详情一致；content 兼容同步`
  })

  await step('9b. 公开文档 Markdown（bodyMarkdown + 公开详情）', async () => {
    const markdown = '# 快速开始\n\n1. 第一步\n2. 第二步\n\n- [x] 完成任务\n\n[官网](https://example.com)'
    const created = await clientA.post(`/projects/${state.projectA1.id}/docs`, {
      title: '冒烟 Markdown 文档',
      category: '上手',
      bodyMarkdown: markdown,
      slug: `smoke-md-${RUN_ID}`,
      status: 'PUBLISHED',
    })
    assertEqual(created.status, 201, '创建文档 HTTP 状态')
    assertEqual(created.data?.bodyMarkdown, markdown, '文档 bodyMarkdown')
    const publicDetail = await anon.get(`/public/communities/${SLUG_A}/docs/${`smoke-md-${RUN_ID}`}`)
    assertEqual(publicDetail.data?.bodyMarkdown, markdown, '公开文档详情 bodyMarkdown')
    const adminRead = await clientA.get(`/projects/${state.projectA1.id}/docs?pageNum=1&pageSize=10`)
    assert((adminRead.data.items ?? []).some((doc) => doc.bodyMarkdown === markdown), '管理端文档列表 bodyMarkdown')
    return `文档 bodyMarkdown 保存/公开详情可读`
  })

  await step('9c. 公告 Markdown + PUT 更新（FIX-4）', async () => {
    const created = await clientA.post(`/projects/${state.projectA1.id}/announcements`, {
      title: '冒烟 Markdown 公告',
      bodyMarkdown: '## 公告\n\n- 内容一\n- 内容二',
      pinned: false,
    })
    assertEqual(created.status, 201, '创建公告 HTTP 状态')
    assertEqual(created.data?.bodyMarkdown, '## 公告\n\n- 内容一\n- 内容二', '公告 bodyMarkdown')
    const updated = await clientA.put(`/announcements/${created.data.id}`, {
      title: '冒烟 Markdown 公告（已更新）',
      bodyMarkdown: '## 公告（更新）\n\n- 已编辑',
      pinned: true,
    })
    assertEqual(updated.data?.title, '冒烟 Markdown 公告（已更新）', '公告 PUT 标题')
    assertEqual(updated.data?.pinned, true, '公告 PUT pinned')
    const publicList = await anon.get(`/public/communities/${SLUG_A}/announcements?pageNum=1&pageSize=10`)
    const publicItem = publicList.data.items.find((item) => item.id === created.data.id)
    assert(publicItem, '公开公告列表应含该公告')
    assertEqual(publicItem.bodyMarkdown, '## 公告（更新）\n\n- 已编辑', '公开公告 bodyMarkdown')
    return `公告创建 + PUT title/bodyMarkdown/pinned；公开列表 bodyMarkdown 已更新`
  })

  await step('9d. 制作组 / 友情链接 CRUD 与公开过滤', async () => {
    const member = await clientA.post(`/projects/${state.projectA1.id}/team-members`, {
      name: '冒烟制作人',
      role: '制作人',
      bio: '负责设计',
      sortOrder: 1,
    })
    assertEqual(member.status, 201, '制作组成员创建')
    const linkOn = await clientA.post(`/projects/${state.projectA1.id}/links`, {
      name: '冒烟友链',
      url: 'https://example.com/friend',
      sortOrder: 1,
      enabled: true,
    })
    assertEqual(linkOn.status, 201, '友情链接创建')
    const team = await anon.get(`/public/communities/${SLUG_A}/team`)
    assert((team.data ?? []).some((item) => item.id === member.data.id), '公开制作组含新成员')
    const links = await anon.get(`/public/communities/${SLUG_A}/links`)
    assert((links.data ?? []).some((item) => item.id === linkOn.data.id), '公开友情链接含启用链接')
    return `制作组/友情链接创建；公开端可读`
  })

  await step('9e. 匿名问题反馈提交 + 管理端查看/状态流转（FIX-3/FIX-4 结构化表单不变）', async () => {
    const submit = await anon.post(`/public/communities/${SLUG_A}/issues`, {
      title: '冒烟匿名问题反馈',
      category: 'Bug 反馈',
      description: '启动黑屏',
      gameVersion: 'v1.0.0-smoke',
      steps: '1. 启动\n2. 黑屏',
    })
    assertEqual(submit.status, 201, '匿名问题反馈 HTTP 状态')
    assertEqual(submit.data?.status, 'PENDING', '问题反馈落库 PENDING')
    const adminList = await clientA.get(`/projects/${state.projectA1.id}/issues?pageNum=1&pageSize=10`)
    assert((adminList.data.items ?? []).some((item) => item.id === submit.data.id), '管理端可见匿名反馈')
    const processing = await clientA.patch(`/issues/${submit.data.id}`, { status: 'PROCESSING' })
    assertEqual(processing.data?.status, 'PROCESSING', '问题反馈状态 → PROCESSING')
    const resolved = await clientA.patch(`/issues/${submit.data.id}`, { status: 'RESOLVED' })
    assertEqual(resolved.data?.status, 'RESOLVED', '问题反馈状态 → RESOLVED')
    const invalid = await clientA.patch(`/issues/${submit.data.id}`, { status: 'CLOSED' }, { expectStatus: 400 })
    assertNonEmptyString(invalid.message, '非法问题状态 400 message')
    return `匿名提交 201 PENDING → PROCESSING → RESOLVED；非法状态 400`
  })

  await step('9f. 内容所有权：B 操作 A 的更新日志 → 403', async () => {
    const res = await clientB.put(`/changelogs/${state.changelogId}`, { title: '越权修改' }, { expectStatus: 403 })
    assertNonEmptyString(res.message, 'B 修改 A 内容 403 message')
    const docRes = await clientB.get(`/projects/${state.projectA1.id}/docs`, { expectStatus: 403 })
    assertNonEmptyString(docRes.message, 'B 读取 A 文档 403 message')
    return `按 ID 内容接口与项目内容接口越权均 403`
  })

  // 10. 未发布页面 404 + 公开快照隔离 --------------------------------------
  await step('10. 未发布页面公开 404 + 草稿/发布隔离（A1）', async () => {
    const draftOnly = await clientA.post(`/projects/${state.projectA1.id}/pages`, {
      name: '冒烟未发布页',
      pageType: 'custom',
      slug: `draft-only-${RUN_ID}`,
    })
    assertEqual(draftOnly.status, 201, '创建草稿页')
    const missing = await anon.get(`/public/communities/${SLUG_A}/pages/draft-only-${RUN_ID}`, {
      expectStatus: 404,
    })
    assertNonEmptyString(missing.message, '未发布页 404 message')

    const before = await anon.get(`/public/communities/${SLUG_A}`)
    const beforeVersion = before.data.page.configVersion
    await savePageDraft(clientA, state.projectA1.id, state.homeA1.pageId, (config) => {
      componentByType(config, 'notice_list').title = '隔离后的新标题（冒烟）'
    })
    const isolated = await anon.get(`/public/communities/${SLUG_A}`)
    assertEqual(isolated.data.page.configVersion, beforeVersion, '未发布时公开快照版本不变')
    assertEqual(
      componentByType(isolated.data.page.config, 'notice_list').title,
      '最新公告（冒烟）',
      '未发布时公开页仍为旧标题',
    )
    const publish = await clientA.post(`/projects/${state.projectA1.id}/pages/${state.homeA1.pageId}/publish`, undefined, { expectStatus: 200 })
    assert(typeof publish.data?.version === 'number', '再次发布 version')
    const updated = await anon.get(`/public/communities/${SLUG_A}`)
    assertEqual(
      componentByType(updated.data.page.config, 'notice_list').title,
      '隔离后的新标题（冒烟）',
      '再次发布后公开页更新',
    )
    return `未发布页 404；草稿修改不影响公开快照；页面级再次发布后公开页更新`
  })

  // 11. 删除级联清理 + 登出（A/B）------------------------------------------
  await step('11. 删除项目级联清理 + 登出（A/B）', async () => {
    for (const project of [state.projectA3, state.projectA2, state.projectA1, state.projectB]) {
      if (!project) continue
      const client = project === state.projectB ? clientB : clientA
      await client.delete(`/projects/${project.id}`, { expectStatus: 204 })
    }
    const listA = await clientA.get('/projects')
    assertDeepEqual((listA.data ?? []).map((project) => project.id), [], 'A 项目已清空')
    const listB = await clientB.get('/projects')
    assertDeepEqual((listB.data ?? []).map((project) => project.id), [], 'B 项目已清空')
    await clientA.logout()
    await clientB.logout()
    assertEqual(clientA.token, null, 'A 登出后 token 应已清空')
    assertEqual(clientB.token, null, 'B 登出后 token 应已清空')
    state.deleted = true
    const me = await clientA.get('/auth/me', { expectStatus: 401 })
    assertNonEmptyString(me.message, '登出后 /auth/me 401')
    return `A/B 项目（含页面/草稿/快照/内容）已删除；登出（丢弃 token）后 /auth/me 401`
  })
}

/* ========================================================================
 * 清理 / 汇总
 * ====================================================================== */

async function bestEffortCleanup() {
  if (KEEP_DATA || state.deleted) return
  const cleanup = async (username, password, slugs) => {
    if (!username) return
    try {
      const client = new ApiClient(`cleanup-${username}`)
      await client.login(username, password)
      const projects = await client.get('/projects')
      for (const project of projects.data ?? []) {
        if (slugs.some((slug) => project.slug.startsWith(slug))) {
          await client.delete(`/projects/${project.id}`).catch(() => {})
        }
      }
    } catch {
      /* 忽略清理失败 */
    }
  }
  await cleanup(USER_A, PASSWORD, ['smoke-a-', 'smoke-a2-', 'smoke-a3-'])
  await cleanup(USER_B, PASSWORD, ['smoke-b-'])
}

function printSummary() {
  console.log(`\n${'='.repeat(86)}`)
  console.log(
    `汇总：PASS ${passedCount} 项，FAIL ${failedCount} 项，SKIP ${skippedCount} 项（共 ${passedCount + failedCount + skippedCount} 项）`,
  )
  if (failures.length > 0) {
    console.log('\n失败明细：')
    failures.forEach((failure, index) => {
      console.log(`  ${index + 1}. ${failure.name}`)
      console.log(`     ${failure.message.split('\n').join('\n     ')}`)
    })
  }
  console.log(
    failedCount === 0 ? '\n✅ v3 API（JWT）冒烟验收全部通过（SKIP 项见上）' : '\n❌ v3 API（JWT）冒烟验收存在失败项',
  )
  console.log('='.repeat(86))
}

run()
  .catch((error) => {
    failedCount += 1
    failures.push({
      name: '未捕获异常',
      message: error instanceof Error ? (error.stack ?? error.message) : String(error),
    })
    console.log(`\n❌ 未捕获异常：${error instanceof Error ? error.message : error}`)
  })
  .finally(async () => {
    await bestEffortCleanup()
    printSummary()
    process.exitCode = failedCount === 0 ? 0 : 1
  })
