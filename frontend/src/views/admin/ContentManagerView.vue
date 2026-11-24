<script setup lang="ts">
/**
 * 内容管理通用页（Lead 维护）：/admin/projects/:id/content/:module
 *
 * 统一承载 P1 内容模块的管理端 CRUD：
 *   changelogs（更新日志）、docs（公开文档）、team（制作团队）、issues（问题反馈）、links（友情链接）
 * 各模块字段/校验/接口集中在本文件的 MODULE_DEFS 中配置，避免为每个模块写重复表格与表单。
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { isApiError } from '@/api/client'
import { getProject } from '@/api/projects'
import {
  createAnnouncement,
  createChangelog,
  createDoc,
  createLink,
  createTeamMember,
  deleteAnnouncement,
  deleteChangelog,
  deleteDoc,
  deleteIssue,
  deleteLink,
  deleteTeamMember,
  listAnnouncements,
  listChangelogs,
  listDocs,
  listIssues,
  listLinks,
  listTeamMembers,
  updateAnnouncement,
  updateChangelog,
  updateDoc,
  updateIssue,
  updateLink,
  updateTeamMember,
} from '@/api/content'
import type { ProjectView } from '@/types/api'
import type { AnnouncementInput } from '@/api/content'
import type {
  ChangelogInput,
  ContentStatus,
  DocInput,
  FriendLinkInput,
  IssueStatus,
  TeamMemberInput,
} from '@/types/content'
import { CONTENT_STATUS_LABELS, ISSUE_STATUSES, ISSUE_STATUS_LABELS } from '@/types/content'
import { isValidHttpUrl } from '@/utils/actions'
import { formatDateTime } from '@/utils/format'
import { markdownToPlainText } from '@/utils/markdown'
import MarkdownEditor from '@/components/markdown/MarkdownEditor.vue'
import PageHeader from '@/components/admin/PageHeader.vue'
import XIcon from '@/components/admin/XIcon.vue'

interface ContentRow {
  id: number
  [key: string]: unknown
}

interface FieldDef {
  key: string
  label: string
  type: 'text' | 'textarea' | 'markdown' | 'date' | 'number' | 'checkbox' | 'select' | 'url'
  required?: boolean
  maxLength?: number
  placeholder?: string
  help?: string
  options?: ReadonlyArray<{ label: string; value: string }>
  min?: number
  max?: number
}

interface ColumnDef {
  key: string
  label: string
  width?: string
  format?: (row: ContentRow) => string
  badge?: (row: ContentRow) => string
}

interface PageResult {
  items: ContentRow[]
  total: number
}

interface ModuleDef {
  key: string
  title: string
  icon: string
  description: string
  supportsCreate: boolean
  pageSize: number
  columns: ColumnDef[]
  fields: FieldDef[]
  /** 编辑时允许修改的字段；缺省为 fields 全部 */
  editableFields?: string[]
  /** 列表页过滤条件 */
  filter?: { key: string; label: string; options: ReadonlyArray<{ label: string; value: string }> }
  load: (
    projectId: number,
    params: { pageNum: number; pageSize: number; filter?: string },
  ) => Promise<PageResult>
  create?: (projectId: number, input: Record<string, unknown>) => Promise<unknown>
  update?: (id: number, input: Record<string, unknown>) => Promise<unknown>
  remove: (id: number) => Promise<void>
}

const STATUS_BADGE: Record<string, string> = {
  DRAFT: 'gcc-badge-warning',
  PUBLISHED: 'gcc-badge-success',
  PENDING: 'gcc-badge-warning',
  PROCESSING: 'gcc-badge-primary',
  RESOLVED: 'gcc-badge-success',
}

function statusText(value: unknown): string {
  const key = String(value ?? '')
  return (
    CONTENT_STATUS_LABELS[key as ContentStatus] ??
    ISSUE_STATUS_LABELS[key as IssueStatus] ??
    key
  )
}

async function toPageResult(task: Promise<unknown>): Promise<PageResult> {
  const result = (await task) as { items?: unknown[]; total?: number } | unknown[]
  if (Array.isArray(result)) {
    return { items: result as ContentRow[], total: result.length }
  }
  return { items: (result.items ?? []) as ContentRow[], total: result.total ?? 0 }
}

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: CONTENT_STATUS_LABELS.DRAFT, value: 'DRAFT' },
  { label: CONTENT_STATUS_LABELS.PUBLISHED, value: 'PUBLISHED' },
]

const MODULE_DEFS: Record<string, ModuleDef> = {
  changelogs: {
    key: 'changelogs',
    title: '更新日志',
    icon: '🗒️',
    description: '维护版本更新记录；仅「已发布」的日志会出现在公开门户。',
    supportsCreate: true,
    pageSize: 10,
    columns: [
      { key: 'version', label: '版本号', width: '120px' },
      { key: 'title', label: '标题' },
      { key: 'releaseDate', label: '发布日期', width: '120px' },
      {
        key: 'status',
        label: '状态',
        width: '100px',
        badge: (row) => STATUS_BADGE[String(row.status)] ?? 'gcc-badge',
        format: (row) => statusText(row.status),
      },
      {
        key: 'updatedAt',
        label: '更新时间',
        width: '170px',
        format: (row) => formatDateTime(String(row.updatedAt)),
      },
    ],
    fields: [
      { key: 'version', label: '版本号', type: 'text', required: true, maxLength: 40, placeholder: 'v0.9.2' },
      { key: 'title', label: '标题', type: 'text', required: true, maxLength: 120 },
      { key: 'releaseDate', label: '发布日期', type: 'date', required: true },
      {
        key: 'status',
        label: '状态',
        type: 'select',
        options: [
          { label: '草稿', value: 'DRAFT' },
          { label: '已发布', value: 'PUBLISHED' },
        ],
      },
      { key: 'bodyMarkdown', label: '更新内容（Markdown）', type: 'markdown', required: true, maxLength: 10000 },
    ],
    filter: { key: 'status', label: '状态', options: statusOptions },
    load: (projectId, params) =>
      toPageResult(
        listChangelogs(projectId, {
          pageNum: params.pageNum,
          pageSize: params.pageSize,
          status: (params.filter ?? '') as ContentStatus | '',
        }),
      ),
    create: (projectId, input) => createChangelog(projectId, input as unknown as ChangelogInput),
    update: (id, input) => updateChangelog(id, input as unknown as Partial<ChangelogInput>),
    remove: deleteChangelog,
  },
  docs: {
    key: 'docs',
    title: '公开文档',
    icon: '📚',
    description: '维护面向玩家的公开文档；仅「已发布」文档会出现在公开门户。',
    supportsCreate: true,
    pageSize: 10,
    columns: [
      { key: 'title', label: '标题' },
      { key: 'category', label: '分类', width: '120px' },
      { key: 'slug', label: 'slug', width: '190px' },
      {
        key: 'status',
        label: '状态',
        width: '100px',
        badge: (row) => STATUS_BADGE[String(row.status)] ?? 'gcc-badge',
        format: (row) => statusText(row.status),
      },
      {
        key: 'updatedAt',
        label: '更新时间',
        width: '170px',
        format: (row) => formatDateTime(String(row.updatedAt)),
      },
    ],
    fields: [
      { key: 'title', label: '标题', type: 'text', required: true, maxLength: 120 },
      { key: 'category', label: '分类', type: 'text', maxLength: 40, placeholder: '上手 / 玩法 / 设定' },
      {
        key: 'slug',
        label: 'slug',
        type: 'text',
        required: true,
        maxLength: 63,
        help: '小写字母/数字/中划线，作为公开文档详情地址',
      },
      {
        key: 'status',
        label: '状态',
        type: 'select',
        options: [
          { label: '草稿', value: 'DRAFT' },
          { label: '已发布', value: 'PUBLISHED' },
        ],
      },
      { key: 'bodyMarkdown', label: '正文（Markdown）', type: 'markdown', required: true, maxLength: 20000 },
    ],
    filter: { key: 'status', label: '状态', options: statusOptions },
    load: (projectId, params) =>
      toPageResult(
        listDocs(projectId, {
          pageNum: params.pageNum,
          pageSize: params.pageSize,
          status: (params.filter ?? '') as ContentStatus | '',
        }),
      ),
    create: (projectId, input) => createDoc(projectId, input as unknown as DocInput),
    update: (id, input) => updateDoc(id, input as unknown as Partial<DocInput>),
    remove: deleteDoc,
  },
  announcements: {
    key: 'announcements',
    title: '社区公告',
    icon: '📢',
    description: '维护社区公告（正文 Markdown）；置顶公告优先展示，公开端展示纯文本摘要。',
    supportsCreate: true,
    pageSize: 10,
    columns: [
      { key: 'title', label: '标题' },
      { key: 'bodyMarkdown', label: '正文摘要', format: (row) => markdownToPlainText(String(row.bodyMarkdown ?? row.content ?? ''), 60) },
      {
        key: 'pinned',
        label: '置顶',
        width: '80px',
        badge: (row) => (row.pinned ? 'gcc-badge-primary' : 'gcc-badge'),
        format: (row) => (row.pinned ? '置顶' : '普通'),
      },
      { key: 'publishedAt', label: '发布时间', width: '170px', format: (row) => formatDateTime(String(row.publishedAt)) },
    ],
    fields: [
      { key: 'title', label: '标题', type: 'text', required: true, maxLength: 120 },
      { key: 'bodyMarkdown', label: '公告正文（Markdown）', type: 'markdown', required: true, maxLength: 10000 },
      { key: 'pinned', label: '置顶显示', type: 'checkbox' },
    ],
    load: (projectId, params) =>
      toPageResult(listAnnouncements(projectId, { pageNum: params.pageNum, pageSize: params.pageSize })),
    create: (projectId, input) => createAnnouncement(projectId, input as unknown as AnnouncementInput),
    update: (id, input) => updateAnnouncement(id, input as unknown as Partial<AnnouncementInput>),
    remove: deleteAnnouncement,
  },
  team: {
    key: 'team',
    title: '制作团队',
    icon: '👥',
    description: '维护制作组成员、角色与简介，公开端按排序值展示。',
    supportsCreate: true,
    pageSize: 50,
    columns: [
      { key: 'name', label: '姓名', width: '140px' },
      { key: 'role', label: '角色', width: '140px' },
      { key: 'bio', label: '简介' },
      { key: 'sortOrder', label: '排序', width: '80px' },
    ],
    fields: [
      { key: 'name', label: '姓名', type: 'text', required: true, maxLength: 60 },
      { key: 'role', label: '角色', type: 'text', maxLength: 60, placeholder: '制作人 / 美术 / 程序' },
      { key: 'avatarUrl', label: '头像 URL（可选）', type: 'url', maxLength: 300, placeholder: 'https://…' },
      { key: 'bio', label: '简介', type: 'textarea', maxLength: 500 },
      { key: 'sortOrder', label: '排序（越小越前）', type: 'number', min: 0, max: 999 },
    ],
    load: (projectId) => toPageResult(listTeamMembers(projectId)),
    create: (projectId, input) => createTeamMember(projectId, input as unknown as TeamMemberInput),
    update: (id, input) => updateTeamMember(id, input as unknown as Partial<TeamMemberInput>),
    remove: deleteTeamMember,
  },
  issues: {
    key: 'issues',
    title: '问题反馈',
    icon: '🛠️',
    description: '查看访客提交的问题反馈，并更新处理状态（待处理 / 处理中 / 已解决）。',
    supportsCreate: false,
    pageSize: 10,
    columns: [
      { key: 'title', label: '标题' },
      { key: 'category', label: '类别', width: '110px' },
      { key: 'gameVersion', label: '游戏版本', width: '110px' },
      {
        key: 'status',
        label: '状态',
        width: '100px',
        badge: (row) => STATUS_BADGE[String(row.status)] ?? 'gcc-badge',
        format: (row) => statusText(row.status),
      },
      {
        key: 'isPublic',
        label: '公开',
        width: '90px',
        badge: (row) => (row.isPublic ? 'gcc-badge-success' : 'gcc-badge'),
        format: (row) => (row.isPublic ? '已公开' : '未公开'),
      },
      {
        key: 'updatedAt',
        label: '更新时间',
        width: '170px',
        format: (row) => formatDateTime(String(row.updatedAt ?? row.createdAt)),
      },
    ],
    fields: [
      { key: 'title', label: '标题', type: 'text', maxLength: 120 },
      { key: 'category', label: '类别', type: 'text', maxLength: 40 },
      { key: 'gameVersion', label: '游戏版本', type: 'text', maxLength: 40 },
      { key: 'description', label: '问题描述', type: 'textarea', maxLength: 3000 },
      { key: 'steps', label: '复现步骤', type: 'textarea', maxLength: 2000 },
      {
        key: 'status',
        label: '处理状态',
        type: 'select',
        options: ISSUE_STATUSES.map((status) => ({ label: ISSUE_STATUS_LABELS[status], value: status })),
      },
      {
        key: 'resolution',
        label: '处理结果（仅在「已解决」后可公开展示）',
        type: 'textarea',
        maxLength: 1000,
        placeholder: '填写修复说明、处理结论或后续计划；设为公开后会出现在首页「问题处理进展」',
      },
      { key: 'isPublic', label: '允许公开展示（仅已解决）', type: 'checkbox' },
    ],
    editableFields: ['status', 'resolution', 'isPublic'],
    filter: {
      key: 'status',
      label: '状态',
      options: [
        { label: '全部状态', value: '' },
        ...ISSUE_STATUSES.map((status) => ({ label: ISSUE_STATUS_LABELS[status], value: status })),
      ],
    },
    load: (projectId, params) =>
      toPageResult(
        listIssues(projectId, {
          pageNum: params.pageNum,
          pageSize: params.pageSize,
          status: (params.filter ?? '') as IssueStatus | '',
        }),
      ),
    update: (id, input) =>
      updateIssue(id, {
        status: input.status as IssueStatus,
        resolution: String(input.resolution ?? ''),
        isPublic: Boolean(input.isPublic),
      }),
    remove: deleteIssue,
  },
  links: {
    key: 'links',
    title: '友情链接',
    icon: '🔗',
    description: '维护站点友情链接；仅「启用」的链接会出现在公开门户。',
    supportsCreate: true,
    pageSize: 50,
    columns: [
      { key: 'name', label: '名称', width: '160px' },
      { key: 'url', label: 'URL' },
      { key: 'description', label: '简介' },
      { key: 'sortOrder', label: '排序', width: '80px' },
      {
        key: 'enabled',
        label: '状态',
        width: '90px',
        badge: (row) => (row.enabled ? 'gcc-badge-success' : 'gcc-badge'),
        format: (row) => (row.enabled ? '启用' : '停用'),
      },
    ],
    fields: [
      { key: 'name', label: '名称', type: 'text', required: true, maxLength: 60 },
      { key: 'url', label: 'URL', type: 'url', required: true, maxLength: 300, placeholder: 'https://…' },
      { key: 'description', label: '简介', type: 'text', maxLength: 200 },
      { key: 'sortOrder', label: '排序（越小越前）', type: 'number', min: 0, max: 999 },
      { key: 'enabled', label: '启用', type: 'checkbox' },
    ],
    load: (projectId) => toPageResult(listLinks(projectId)),
    create: (projectId, input) => createLink(projectId, input as unknown as FriendLinkInput),
    update: (id, input) => updateLink(id, input as unknown as Partial<FriendLinkInput>),
    remove: deleteLink,
  },
}

const MODULE_TABS = Object.values(MODULE_DEFS).map((def) => ({
  key: def.key,
  label: def.title,
  icon: def.icon,
}))

const route = useRoute()
const projectId = computed(() => Number(route.params.id))
const moduleKey = computed(() => String(route.params.module ?? 'changelogs'))
const def = computed<ModuleDef>(() => MODULE_DEFS[moduleKey.value] ?? MODULE_DEFS.changelogs)

const project = ref<ProjectView | null>(null)
const items = ref<ContentRow[]>([])
const total = ref(0)
const pageNum = ref(1)
const statusFilter = ref('')
const loading = ref(false)
const loadError = ref('')
const notice = ref('')

const dialogOpen = ref(false)
const editingId = ref<number | null>(null)
/** 打开弹窗时的表单快照，用于判断是否有未保存修改（P0-B） */
const dialogSnapshot = ref('')
/** 未保存内容确认提示（自定义弹层，避免使用 window.confirm 与内容管理弹窗叠加） */
const discardConfirmOpen = ref(false)
const form = reactive<Record<string, string | number>>({})
const flags = reactive<Record<string, boolean>>({})
const formError = ref('')
const fieldErrors = reactive<Record<string, string>>({})
const saving = ref(false)

const isEditing = computed(() => editingId.value !== null)
const dialogTitle = computed(() => `${isEditing.value ? '编辑' : '新增'}${def.value.title}`)
const canCreate = computed(() => def.value.supportsCreate)
const formFields = computed<FieldDef[]>(() => {
  const base = def.value.fields
  if (!isEditing.value) return base
  const editable = def.value.editableFields
  return editable ? base.filter((field) => editable.includes(field.key)) : base
})
/** 当前可编辑字段的稳定快照（checkbox 取 flags，其余取 form） */
function formSnapshot(): string {
  const fields: Record<string, unknown> = {}
  formFields.value.forEach((field) => {
    fields[field.key] = field.type === 'checkbox' ? Boolean(flags[field.key]) : form[field.key]
  })
  return JSON.stringify(fields)
}

const isDirty = computed(
  () => dialogOpen.value && dialogSnapshot.value !== '' && formSnapshot() !== dialogSnapshot.value,
)

function captureSnapshot(): void {
  dialogSnapshot.value = formSnapshot()
  discardConfirmOpen.value = false
}

const isPaged = computed(
  () =>
    def.value.key === 'changelogs' ||
    def.value.key === 'docs' ||
    def.value.key === 'issues' ||
    def.value.key === 'announcements',
)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / (def.value.pageSize || 10))))

function showNotice(message: string): void {
  notice.value = message
  window.setTimeout(() => {
    if (notice.value === message) notice.value = ''
  }, 3200)
}

function errorMessageOf(error: unknown, fallback: string): string {
  if (isApiError(error)) return error.message || fallback
  return error instanceof Error ? error.message : fallback
}

function formatCell(column: ColumnDef, row: ContentRow): string {
  return column.format ? column.format(row) : String(row[column.key] ?? '—')
}

async function loadProject(): Promise<void> {
  try {
    project.value = await getProject(projectId.value)
  } catch {
    project.value = null
  }
}

async function load(): Promise<void> {
  if (!Number.isFinite(projectId.value)) return
  loading.value = true
  loadError.value = ''
  try {
    const result = await def.value.load(projectId.value, {
      pageNum: pageNum.value,
      pageSize: def.value.pageSize,
      filter: statusFilter.value,
    })
    items.value = result.items
    total.value = result.total
  } catch (error) {
    loadError.value = errorMessageOf(error, '加载内容列表失败，请稍后重试')
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function reload(): void {
  pageNum.value = 1
  void load()
}

function resetForm(): void {
  Object.keys(form).forEach((key) => delete form[key])
  Object.keys(flags).forEach((key) => delete flags[key])
  Object.keys(fieldErrors).forEach((key) => delete fieldErrors[key])
  formError.value = ''
  def.value.fields.forEach((field) => {
    if (field.type === 'checkbox') flags[field.key] = field.key === 'enabled'
    else if (field.type === 'number') form[field.key] = 0
    else if (field.type === 'select') form[field.key] = field.options?.[0]?.value ?? ''
    else form[field.key] = ''
  })
}

function openCreate(): void {
  editingId.value = null
  resetForm()
  dialogOpen.value = true
  captureSnapshot()
}

function openEdit(row: ContentRow): void {
  editingId.value = row.id
  resetForm()
  def.value.fields.forEach((field) => {
    const value = row[field.key]
    if (field.type === 'checkbox') flags[field.key] = Boolean(value)
    else if (value !== undefined && value !== null) form[field.key] = value as string | number
  })
  dialogOpen.value = true
  captureSnapshot()
}

/** 关闭弹窗（无脏数据时）：清理状态与快照 */
function closeDialog(): void {
  dialogOpen.value = false
  discardConfirmOpen.value = false
  dialogSnapshot.value = ''
}

/** 用户主动关闭（×/取消/点击遮罩）：有未保存修改时先弹确认 */
function requestClose(): void {
  if (saving.value) return
  if (isDirty.value) {
    discardConfirmOpen.value = true
    return
  }
  closeDialog()
}

/** 确认放弃修改 */
function discardAndClose(): void {
  discardConfirmOpen.value = false
  closeDialog()
}

/** 继续编辑：保留全部输入 */
function keepEditing(): void {
  discardConfirmOpen.value = false
}

function validateForm(): boolean {
  Object.keys(fieldErrors).forEach((key) => delete fieldErrors[key])
  formError.value = ''
  for (const field of formFields.value) {
    const value = form[field.key]
    if (field.required) {
      if (field.type === 'number') {
        if (value === undefined || value === null || value === '' || Number.isNaN(Number(value))) {
          fieldErrors[field.key] = '请填写'
        }
      } else if (String(value ?? '').trim().length === 0) {
        fieldErrors[field.key] = '请填写'
      }
    }
    if (field.maxLength && typeof value === 'string' && value.length > field.maxLength) {
      fieldErrors[field.key] = `不能超过 ${field.maxLength} 个字符`
    }
    if ((field.type === 'markdown' || field.type === 'textarea') && field.required) {
      if (String(value ?? '').trim() === '') fieldErrors[field.key] = '请填写正文内容'
    }
    if (field.type === 'url' && typeof value === 'string' && value.trim() !== '' && !isValidHttpUrl(value)) {
      fieldErrors[field.key] = '请输入合法的 http/https 链接'
    }
    if (def.value.key === 'docs' && field.key === 'slug' && typeof value === 'string') {
      if (!/^[a-z0-9][a-z0-9-]{0,62}$/.test(value)) {
        fieldErrors[field.key] = 'slug 需为小写字母/数字/中划线，2-63 位'
      }
    }
    if (field.type === 'number' && value !== undefined && value !== null && value !== '') {
      const numeric = Number(value)
      if (!Number.isFinite(numeric)) fieldErrors[field.key] = '请输入数字'
      else if (field.min !== undefined && numeric < field.min) fieldErrors[field.key] = `不能小于 ${field.min}`
      else if (field.max !== undefined && numeric > field.max) fieldErrors[field.key] = `不能大于 ${field.max}`
    }
  }
  return Object.keys(fieldErrors).length === 0
}

function buildPayload(): Record<string, unknown> {
  const payload: Record<string, unknown> = {}
  formFields.value.forEach((field) => {
    if (field.type === 'checkbox') {
      payload[field.key] = flags[field.key] ?? false
      return
    }
    const value = form[field.key]
    if (field.type === 'number') payload[field.key] = Number(value ?? 0)
    else payload[field.key] = typeof value === 'string' ? value.trim() : value
  })
  return payload
}

async function submit(): Promise<void> {
  if (saving.value) return
  if (!validateForm()) return
  saving.value = true
  formError.value = ''
  const payload = buildPayload()
  try {
    if (isEditing.value && def.value.update) {
      await def.value.update(editingId.value as number, payload)
      showNotice('保存成功')
    } else if (def.value.create) {
      await def.value.create(projectId.value, payload)
      showNotice('创建成功')
    }
    dialogOpen.value = false
    discardConfirmOpen.value = false
    dialogSnapshot.value = ''
    await load()
  } catch (error) {
    if (isApiError(error) && Object.keys(error.fieldErrors).length > 0) {
      Object.entries(error.fieldErrors).forEach(([key, message]) => {
        fieldErrors[key] = message
      })
      formError.value = error.message
    } else {
      formError.value = errorMessageOf(error, '保存失败，请稍后重试')
    }
  } finally {
    saving.value = false
  }
}

async function remove(row: ContentRow): Promise<void> {
  const label = String(row.title ?? row.name ?? `#${row.id}`)
  if (!window.confirm(`确认删除「${label}」？该操作不可恢复。`)) return
  try {
    await def.value.remove(row.id)
    showNotice('已删除')
    if (items.value.length === 1 && pageNum.value > 1) pageNum.value -= 1
    await load()
  } catch (error) {
    showNotice(errorMessageOf(error, '删除失败，请稍后重试'))
  }
}

function goPage(delta: number): void {
  const next = pageNum.value + delta
  if (next < 1 || next > totalPages.value) return
  pageNum.value = next
  void load()
}

watch(
  () => [route.params.id, route.params.module],
  () => {
    statusFilter.value = ''
    pageNum.value = 1
    resetForm()
    void loadProject()
    void load()
  },
)

onMounted(() => {
  resetForm()
  void loadProject()
  void load()
})
</script>

<template>
  <div class="content-page x-page gcc-container">
    <PageHeader :kicker="`Content / ${def.key}`" :title="`${def.title}管理`">
      <template #actions>
        <RouterLink
          class="gcc-btn gcc-btn-sm gcc-btn-ghost"
          :to="`/admin/projects/${projectId}/pages`"
        >
          ← 页面管理
        </RouterLink>
        <RouterLink class="gcc-btn gcc-btn-sm" to="/admin/projects">站点列表</RouterLink>
        <a
          v-if="project"
          class="gcc-btn gcc-btn-sm"
          :href="`/community/${project.slug}`"
          target="_blank"
          rel="noopener noreferrer"
        >
          访问公开站点
          <XIcon name="arrow-up-right" :size="12" />
        </a>
      </template>
    </PageHeader>

    <div class="content-meta">
      <span v-if="project" class="x-meta">
        PROJECT / {{ project.name }} · GAME / {{ project.gameName }}
      </span>
      <span class="gcc-spacer" />
      <span class="x-meta">
        {{ items.length }} / {{ total }} ROWS
        <template v-if="isPaged"> · PAGE {{ pageNum }} / {{ totalPages }}</template>
      </span>
    </div>

    <nav class="module-tabs" aria-label="内容模块">
      <RouterLink
        v-for="tab in MODULE_TABS"
        :key="tab.key"
        class="module-tab"
        :class="{ 'is-active': tab.key === moduleKey }"
        :to="`/admin/projects/${projectId}/content/${tab.key}`"
      >
        <span aria-hidden="true">{{ tab.icon }}</span>
        {{ tab.label }}
      </RouterLink>
    </nav>

    <div v-if="notice" class="notice-banner">{{ notice }}</div>

    <section class="content-panel">
      <header class="panel-head">
        <div class="panel-head__text">
          <p class="x-kicker">{{ def.key }}</p>
          <h2 class="panel-title">{{ def.title }}</h2>
          <p class="panel-desc">{{ def.description }}</p>
        </div>
        <div class="card-actions">
          <template v-if="def.filter">
            <label class="x-kicker filter-label" :for="`filter-${def.key}`">
              {{ def.filter.label }}
            </label>
            <select
              :id="`filter-${def.key}`"
              v-model="statusFilter"
              class="gcc-select filter-select"
              @change="reload"
            >
              <option v-for="option in def.filter.options" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </template>
          <button
            v-if="canCreate"
            type="button"
            class="gcc-btn gcc-btn-sm gcc-btn-primary"
            @click="openCreate"
          >
            <XIcon name="plus" :size="13" />
            新增{{ def.title }}
          </button>
        </div>
      </header>

      <div v-if="loadError" class="gcc-error table-state">
        {{ loadError }}
        <button type="button" class="gcc-btn gcc-btn-sm" @click="load">重试</button>
      </div>
      <div v-else-if="loading" class="gcc-loading table-state">加载中…</div>
      <div v-else-if="items.length === 0" class="gcc-empty table-state">
        <p class="x-kicker">EMPTY</p>
        <p>暂无{{ def.title }}数据</p>
        <p class="x-muted table-state__hint">
          {{ canCreate ? '点击右上角「新增」创建第一条内容。' : '访客提交后会出现在这里。' }}
        </p>
      </div>

      <div v-else class="table-wrap">
        <table class="content-table">
          <thead>
            <tr>
              <th
                v-for="column in def.columns"
                :key="column.key"
                :style="{ width: column.width }"
              >
                <span class="x-kicker">{{ column.label }}</span>
              </th>
              <th class="actions-col"><span class="x-kicker">操作</span></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in items" :key="row.id">
              <td v-for="column in def.columns" :key="column.key">
                <span v-if="column.badge" class="gcc-badge" :class="column.badge(row)">
                  {{ formatCell(column, row) }}
                </span>
                <span v-else class="cell-text" :title="formatCell(column, row)">
                  {{ formatCell(column, row) }}
                </span>
              </td>
              <td class="actions-col">
                <button type="button" class="gcc-btn gcc-btn-sm" @click="openEdit(row)">
                  {{ def.editableFields ? '处理' : '编辑' }}
                </button>
                <button type="button" class="gcc-btn gcc-btn-sm gcc-btn-danger" @click="remove(row)">
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer v-if="isPaged && items.length > 0" class="table-foot">
        <span class="x-meta">共 {{ total }} 条 · 第 {{ pageNum }}/{{ totalPages }} 页</span>
        <div class="table-foot__actions">
          <button type="button" class="gcc-btn gcc-btn-sm" :disabled="pageNum <= 1" @click="goPage(-1)">
            上一页
          </button>
          <button
            type="button"
            class="gcc-btn gcc-btn-sm"
            :disabled="pageNum >= totalPages"
            @click="goPage(1)"
          >
            下一页
          </button>
        </div>
      </footer>
    </section>

    <!-- ============================ 新增 / 编辑对话框 ============================ -->
    <div v-if="dialogOpen" class="dialog-mask" @click.self="requestClose">
      <div class="dialog" role="dialog" aria-modal="true">
        <header class="dialog-head">
          <div>
            <p class="x-kicker">{{ isEditing ? 'Edit entry' : 'New entry' }}</p>
            <h2 class="dialog-title">{{ dialogTitle }}</h2>
          </div>
          <button type="button" class="dialog-close" aria-label="关闭" @click="requestClose">×</button>
        </header>
        <form class="dialog-form" novalidate @submit.prevent="submit">
          <div v-if="formError" class="gcc-error form-error">{{ formError }}</div>
          <div v-for="field in formFields" :key="field.key" class="gcc-field">
            <label class="gcc-label" :for="`field-${field.key}`">
              {{ field.label }}
              <em v-if="field.required" class="required">*</em>
            </label>
            <MarkdownEditor
              v-if="field.type === 'markdown'"
              :key="field.key"
              :model-value="String(form[field.key] ?? '')"
              :required="field.required"
              :max-length="field.maxLength"
              :placeholder="field.placeholder"
              :error="fieldErrors[field.key]"
              :disabled="saving"
              @update:model-value="form[field.key] = $event"
            />
            <textarea
              v-else-if="field.type === 'textarea'"
              :id="`field-${field.key}`"
              v-model="form[field.key]"
              class="gcc-textarea"
              :maxlength="field.maxLength"
              :placeholder="field.placeholder"
            />
            <select
              v-else-if="field.type === 'select'"
              :id="`field-${field.key}`"
              v-model="form[field.key]"
              class="gcc-select"
            >
              <option v-for="option in field.options" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <label v-else-if="field.type === 'checkbox'" class="gcc-checkbox">
              <input :id="`field-${field.key}`" v-model="flags[field.key]" type="checkbox" />
              启用
            </label>
            <input
              v-else
              :id="`field-${field.key}`"
              v-model="form[field.key]"
              class="gcc-input"
              :type="field.type === 'number' ? 'number' : field.type === 'date' ? 'date' : 'text'"
              :maxlength="field.maxLength"
              :placeholder="field.placeholder"
              :min="field.min"
              :max="field.max"
            />
            <span v-if="field.help" class="field-help">{{ field.help }}</span>
            <span v-if="fieldErrors[field.key]" class="field-error">{{ fieldErrors[field.key] }}</span>
          </div>
          <footer class="dialog-actions">
            <button type="button" class="gcc-btn gcc-btn-ghost" @click="requestClose">取消</button>
            <button type="submit" class="gcc-btn gcc-btn-primary" :disabled="saving">
              {{ saving ? '保存中…' : '保存' }}
            </button>
          </footer>
        </form>
      </div>
    </div>

    <!-- 未保存内容确认提示（P0-B） -->
    <div v-if="discardConfirmOpen" class="dialog-mask confirm-mask" @click.self="keepEditing">
      <div class="dialog confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="discard-title">
        <header class="dialog-head">
          <div>
            <p class="x-kicker">Unsaved changes</p>
            <h2 id="discard-title" class="dialog-title">当前编辑内容尚未保存，确认放弃修改吗？</h2>
          </div>
        </header>
        <div class="confirm-body">
          <p>放弃后，本次输入的内容将不会保存；选择「继续编辑」可返回弹窗继续修改。</p>
        </div>
        <footer class="dialog-actions confirm-actions">
          <button type="button" class="gcc-btn" @click="keepEditing">继续编辑</button>
          <button type="button" class="gcc-btn gcc-btn-danger" @click="discardAndClose">放弃修改</button>
        </footer>
      </div>
    </div>
  </div>
</template>

<style scoped>
.content-page {
  --page-danger-fg: var(--x-danger);
  --page-success-fg: var(--x-success);
  padding-top: 36px;
}

.content-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--x-border);
}

/* ---------------- module tabs ---------------- */
.module-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--x-border);
}

.module-tab {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 14px;
  border: 1px solid transparent;
  border-radius: var(--x-radius-pill);
  color: var(--x-muted);
  font-size: 12.5px;
  transition:
    color 0.15s ease,
    border-color 0.15s ease,
    background 0.15s ease;
}

.module-tab:hover {
  color: var(--x-text);
  background: var(--x-surface-2);
}

.module-tab.is-active {
  color: var(--x-text);
  border-color: var(--x-border-strong);
  background: var(--x-surface-2);
}

/* ---------------- notice ---------------- */
.notice-banner {
  margin-bottom: 16px;
  padding: 10px 14px;
  border: 1px solid rgba(61, 220, 151, 0.3);
  border-radius: var(--x-radius);
  background: rgba(61, 220, 151, 0.06);
  color: var(--page-success-fg);
  font-size: 13px;
}

/* ---------------- panel ---------------- */
.content-panel {
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px 18px;
  border-bottom: 1px solid var(--x-border);
}

.panel-head__text {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.panel-title {
  margin: 0;
  font-size: 20px;
  font-weight: 400;
  letter-spacing: -0.015em;
}

.panel-desc {
  max-width: 700px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--x-muted);
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 9.5px;
}

.filter-select {
  width: 150px;
  padding: 6px 10px;
  border-radius: var(--x-radius-pill);
  font-size: 12px;
}

.table-state {
  margin: 20px 24px;
}

.table-state__hint {
  font-size: 12px;
}

/* ---------------- table ---------------- */
.table-wrap {
  overflow-x: auto;
}

.content-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: auto;
}

.content-table th {
  padding: 10px 18px;
  border-bottom: 1px solid var(--x-border);
  background: var(--x-canvas-soft);
  text-align: left;
  font-size: 9px;
  white-space: nowrap;
}

.content-table td {
  padding: 12px 18px;
  border-top: 1px solid var(--x-border);
  font-size: 12.5px;
  color: var(--x-text-secondary);
  vertical-align: middle;
}

.content-table tbody tr {
  transition: background 0.12s ease;
}

.content-table tbody tr:hover {
  background: var(--x-surface-2);
}

.cell-text {
  display: inline-block;
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.actions-col {
  text-align: right;
  white-space: nowrap;
}

.content-table td.actions-col {
  display: table-cell;
}

.content-table td.actions-col .gcc-btn {
  margin-left: 6px;
  padding: 4px 11px;
  font-size: 11.5px;
}

/* ---------------- pagination ---------------- */
.table-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 24px;
  border-top: 1px solid var(--x-border);
}

.table-foot__actions {
  display: flex;
  gap: 8px;
}

/* ---------------- dialog ---------------- */
.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--gcc-mask);
}

.dialog {
  width: min(720px, 100%);
  max-height: min(88vh, 860px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--x-border-strong);
  border-radius: var(--x-radius);
  background: var(--x-surface-2);
}

.dialog-head {
  flex: none;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 22px 24px 16px;
  border-bottom: 1px solid var(--x-border);
}

.dialog-title {
  margin-top: 6px;
  font-size: 20px;
  font-weight: 400;
  letter-spacing: -0.02em;
}

.dialog-close {
  flex: none;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--x-border);
  border-radius: 50%;
  background: transparent;
  color: var(--x-muted);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}

.dialog-close:hover {
  color: var(--x-text);
  border-color: var(--x-border-strong);
}

.confirm-mask {
  z-index: 1100;
}

.confirm-dialog {
  width: min(460px, 100%);
}

.confirm-body {
  padding: 16px 24px 0;
  color: var(--gcc-muted);
  font-size: 13px;
  line-height: 1.7;
}

.confirm-actions {
  padding: 18px 24px 22px;
  margin-top: 0;
  border-top: none;
}

.dialog-form {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-error {
  padding: 10px 12px;
  border: 1px solid rgba(242, 95, 92, 0.4);
  border-radius: var(--x-radius);
  background: rgba(242, 95, 92, 0.06);
  color: var(--page-danger-fg);
  font-size: 12.5px;
}

.required {
  color: var(--x-danger);
  font-style: normal;
}

.field-help {
  font-size: 11px;
  color: var(--x-muted);
}

.field-error {
  font-size: 11px;
  color: var(--page-danger-fg);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 18px;
  margin-top: 4px;
  border-top: 1px solid var(--x-border);
}

/* Markdown 编辑器外观适配 xAI token（组件内部样式沿用全局变量，这里只做容器微调） */
:deep(.markdown-editor) {
  border-radius: var(--x-radius);
  background: var(--x-canvas-soft);
}

:deep(.markdown-editor__head),
:deep(.markdown-editor__toolbar) {
  background: transparent;
  border-color: var(--x-border);
}

:deep(.markdown-editor__preview) {
  background: var(--x-surface);
  border-color: var(--x-border);
}

:global(.admin-theme-scope[data-admin-theme='light'] .content-page) {
  --page-danger-fg: #b42318;
  --page-success-fg: #0f7a4e;
}

@media (max-width: 900px) {
  .panel-head {
    flex-direction: column;
  }

  .dialog {
    max-height: 92vh;
  }
}
</style>
