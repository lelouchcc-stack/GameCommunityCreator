/**
 * 内置页面模板目录（前端唯一事实源）。
 *
 * 每一项对应后端 `PageTypes.TEMPLATE_KEYS` 中的一个模板 key，
 * 模板权威副本在 `backend/src/main/resources/templates/pages/{key}.json`。
 * 「模板中心」与「总览 · 推荐模板」都从这里取数，避免出现两套互不一致的静态数据。
 */
export type TemplateAccent = 'orange' | 'violet' | 'blue' | 'green'

export interface PageTemplateItem {
  /** 后端模板 key（必须与 PageTypes.TEMPLATE_KEYS 一致） */
  key: string
  /** 中文名称 */
  name: string
  /** 卡片上的 mono 技术标签 */
  mono: string
  /** 一句话说明模板包含的内容 */
  description: string
  /** 模板包含的组件类型（与模板 JSON 保持一致） */
  components: string[]
  /** 是否在总览「推荐模板」中展示 */
  recommended: boolean
  accent: TemplateAccent
}

export const PAGE_TEMPLATES: readonly PageTemplateItem[] = [
  {
    key: 'default-home',
    name: '首页',
    mono: 'DEFAULT HOME',
    description: 'Hero 游戏横幅、简介、公告、问题处理进展、更新日志与公开文档聚合的社区首页。',
    components: [
      'hero',
      'about',
      'notice_list',
      'issue_progress',
      'changelog_list',
      'docs_list',
      'footer',
    ],
    recommended: true,
    accent: 'orange',
  },
  {
    key: 'default-changelog',
    name: '更新日志',
    mono: 'CHANGELOG',
    description: '按版本号与发布日期组织的 Release Notes 列表。',
    components: ['changelog_list', 'footer'],
    recommended: true,
    accent: 'violet',
  },
  {
    key: 'default-docs',
    name: '公开文档',
    mono: 'DOCS',
    description: '分类组织的文档列表，详情支持 Markdown 正文。',
    components: ['docs_list', 'footer'],
    recommended: true,
    accent: 'blue',
  },
  {
    key: 'default-team',
    name: '制作团队',
    mono: 'TEAM',
    description: '角色 + 姓名 + 一句话简介的制作组名单。',
    components: ['team_grid', 'footer'],
    recommended: true,
    accent: 'green',
  },
  {
    key: 'default-issue-report',
    name: '问题反馈',
    mono: 'ISSUE REPORT',
    description: '标题 / 分类 / 描述 / 版本 / 复现步骤的结构化反馈表单。',
    components: ['issue_form', 'footer'],
    recommended: false,
    accent: 'violet',
  },
  {
    key: 'default-links',
    name: '友情链接',
    mono: 'LINKS',
    description: '合作方与社区入口卡片。',
    components: ['links_grid', 'footer'],
    recommended: false,
    accent: 'blue',
  },
  {
    key: 'blank',
    name: '空白页',
    mono: 'BLANK',
    description: '仅包含页脚的空白页，用于新建自定义页面。',
    components: ['footer'],
    recommended: false,
    accent: 'orange',
  },
]

/** 总览「推荐模板」使用的子集（与模板中心同源）。 */
export const RECOMMENDED_PAGE_TEMPLATES: readonly PageTemplateItem[] = PAGE_TEMPLATES.filter(
  (template) => template.recommended,
)
