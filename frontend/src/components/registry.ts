/**
 * 组件注册表：编辑器组件面板、属性面板、运行时渲染都从这里读取元数据。
 *
 * 与 docs/PAGE_SCHEMA.md v2 §2 逐字对应。新增组件类型时必须同步：
 * 1) docs/PAGE_SCHEMA.md  2) 本文件  3) 后端 PageConfigValidator 白名单
 * 4) frontend/src/runtime/componentMap.ts  5) 测试与文档
 */
import type { ComponentDefinition, ComponentType, PropDef } from '@/types/page'

const heroProps: PropDef[] = [
  { key: 'headline', label: '横幅主标题', type: 'text', maxLength: 60, placeholder: '留空则显示游戏名' },
  { key: 'subheadline', label: '横幅副标题', type: 'text', maxLength: 120, placeholder: '一句话欢迎语' },
  {
    key: 'backgroundImage',
    label: '背景图片 URL',
    type: 'text',
    maxLength: 300,
    placeholder: 'https://…（留空使用主题渐变）',
  },
  { key: 'showGameName', label: '显示游戏名', type: 'boolean' },
  {
    key: 'align',
    label: '内容对齐',
    type: 'select',
    options: [
      { label: '居中', value: 'center' },
      { label: '左对齐', value: 'left' },
    ],
  },
]

const aboutProps: PropDef[] = [
  {
    key: 'body',
    label: '简介正文',
    type: 'textarea',
    maxLength: 1000,
    placeholder: '留空则显示游戏信息中的简介',
  },
  { key: 'showTagline', label: '显示一句话简介', type: 'boolean' },
]

const noticeProps: PropDef[] = [
  { key: 'limit', label: '展示条数', type: 'number', min: 1, max: 10 },
  { key: 'showPinned', label: '置顶公告优先', type: 'boolean' },
]

const footerProps: PropDef[] = [
  { key: 'copyright', label: '版权信息', type: 'text', maxLength: 120, placeholder: '© 2025 游戏名 社区' },
  { key: 'note', label: '备注', type: 'text', maxLength: 80, placeholder: '由 GameCommunityCreator 搭建' },
]

const buttonProps: PropDef[] = [
  { key: 'label', label: '按钮文字', type: 'text', maxLength: 30, placeholder: '例如：查看更新日志' },
  {
    key: 'variant',
    label: '按钮样式',
    type: 'select',
    options: [
      { label: '主按钮', value: 'primary' },
      { label: '次按钮', value: 'secondary' },
      { label: '幽灵按钮', value: 'ghost' },
    ],
  },
  { key: 'icon', label: '图标（可选）', type: 'text', maxLength: 8, placeholder: '例如：🚀' },
  {
    key: 'align',
    label: '对齐方式',
    type: 'select',
    options: [
      { label: '左对齐', value: 'left' },
      { label: '居中', value: 'center' },
      { label: '右对齐', value: 'right' },
    ],
  },
]

const changelogProps: PropDef[] = [
  { key: 'limit', label: '展示条数', type: 'number', min: 1, max: 20 },
  { key: 'showVersion', label: '显示版本号', type: 'boolean' },
]

const docsProps: PropDef[] = [
  { key: 'limit', label: '展示条数', type: 'number', min: 1, max: 20 },
  { key: 'category', label: '文档分类筛选', type: 'text', maxLength: 40, placeholder: '留空展示全部分类' },
]

const teamProps: PropDef[] = [
  { key: 'columns', label: '每行卡片数', type: 'number', min: 1, max: 4 },
  { key: 'showRole', label: '显示角色', type: 'boolean' },
]

const issueProps: PropDef[] = [
  { key: 'intro', label: '表单说明', type: 'textarea', maxLength: 200, placeholder: '向开发者反馈问题时请尽量描述清楚' },
  {
    key: 'categories',
    label: '问题分类（英文逗号分隔）',
    type: 'text',
    maxLength: 120,
    placeholder: 'Bug 反馈,体验建议,其他',
  },
  { key: 'successMessage', label: '提交成功提示', type: 'text', maxLength: 100 },
]

const linksProps: PropDef[] = [
  { key: 'columns', label: '每行卡片数', type: 'number', min: 1, max: 4 },
  { key: 'showDescription', label: '显示简介', type: 'boolean' },
]

const issueProgressProps: PropDef[] = [
  { key: 'limit', label: '展示条数', type: 'number', min: 1, max: 20 },
  {
    key: 'emptyText',
    label: '空态文案',
    type: 'text',
    maxLength: 60,
    placeholder: '暂无公开的问题处理进展',
  },
]

export const COMPONENT_DEFS: Record<ComponentType, ComponentDefinition> = {
  hero: {
    type: 'hero',
    label: '游戏介绍横幅',
    description: '页面顶部主视觉，展示游戏名与欢迎语，数据来自游戏信息。',
    icon: '🎮',
    defaultTitle: '游戏介绍横幅',
    dataSource: 'game_info',
    defaultProps: { headline: '', subheadline: '', backgroundImage: '', showGameName: true, align: 'center' },
    propDefs: heroProps,
    supportsAction: false,
  },
  about: {
    type: 'about',
    label: '游戏简介',
    description: '介绍游戏世界观与玩法，数据来自游戏信息。',
    icon: '📖',
    defaultTitle: '游戏简介',
    dataSource: 'game_info',
    defaultProps: { body: '', showTagline: true },
    propDefs: aboutProps,
    supportsAction: false,
  },
  notice_list: {
    type: 'notice_list',
    label: '公告列表',
    description: '展示社区公告，置顶公告优先，数据来自公告接口。',
    icon: '📢',
    defaultTitle: '社区公告',
    dataSource: 'announcements',
    defaultProps: { limit: 3, showPinned: true },
    propDefs: noticeProps,
    supportsAction: false,
  },
  footer: {
    type: 'footer',
    label: '页脚',
    description: '页面底部版权与说明，不使用数据源。',
    icon: '🧩',
    defaultTitle: '页脚',
    dataSource: null,
    defaultProps: { copyright: '', note: '' },
    propDefs: footerProps,
    supportsAction: false,
  },
  button: {
    type: 'button',
    label: '按钮',
    description: '可配置站内跳转或外部链接的通用按钮（动作只能从白名单中选择）。',
    icon: '🔘',
    defaultTitle: '按钮',
    dataSource: null,
    defaultProps: { label: '按钮', variant: 'primary', icon: '', align: 'left' },
    propDefs: buttonProps,
    supportsAction: true,
  },
  changelog_list: {
    type: 'changelog_list',
    label: '更新日志列表',
    description: '展示公开的版本更新记录，点击进入详情。',
    icon: '🗒️',
    defaultTitle: '更新日志',
    dataSource: 'changelogs',
    defaultProps: { limit: 6, showVersion: true },
    propDefs: changelogProps,
    supportsAction: false,
  },
  docs_list: {
    type: 'docs_list',
    label: '公开文档列表',
    description: '展示公开发布的文档，点击进入文档详情。',
    icon: '📚',
    defaultTitle: '公开文档',
    dataSource: 'docs',
    defaultProps: { limit: 8, category: '' },
    propDefs: docsProps,
    supportsAction: false,
  },
  team_grid: {
    type: 'team_grid',
    label: '制作组介绍',
    description: '展示制作组成员、角色与简介。',
    icon: '👥',
    defaultTitle: '制作团队',
    dataSource: 'team',
    defaultProps: { columns: 3, showRole: true },
    propDefs: teamProps,
    supportsAction: false,
  },
  issue_form: {
    type: 'issue_form',
    label: '问题汇报表单',
    description: '访客提交 Bug/建议的表单，固定调用公开反馈 API。',
    icon: '🛠️',
    defaultTitle: '问题汇报',
    dataSource: null,
    defaultProps: {
      intro: '遇到问题或有建议？请尽量描述清楚，方便我们定位。',
      categories: 'Bug 反馈,体验建议,其他',
      successMessage: '感谢反馈！我们会尽快处理。',
    },
    propDefs: issueProps,
    supportsAction: false,
  },
  links_grid: {
    type: 'links_grid',
    label: '友情链接',
    description: '展示站点配置的友情链接（仅启用项）。',
    icon: '🔗',
    defaultTitle: '友情链接',
    dataSource: 'links',
    defaultProps: { columns: 3, showDescription: true },
    propDefs: linksProps,
    supportsAction: false,
  },
  issue_progress: {
    type: 'issue_progress',
    label: '问题处理进展',
    description: '展示「已解决且允许公开」的问题反馈及处理结果，点击进入问题处理进展页。',
    icon: '🧭',
    defaultTitle: '问题处理进展',
    dataSource: 'issues_progress',
    defaultProps: { limit: 5, emptyText: '暂无公开的处理进展，问题解决后会在官网上同步。' },
    propDefs: issueProgressProps,
    supportsAction: false,
  },
}

export const COMPONENT_DEF_LIST: readonly ComponentDefinition[] = Object.values(COMPONENT_DEFS)

export function getComponentDef(type: ComponentType): ComponentDefinition {
  return COMPONENT_DEFS[type]
}

export function isComponentType(value: string): value is ComponentType {
  return Object.prototype.hasOwnProperty.call(COMPONENT_DEFS, value)
}
