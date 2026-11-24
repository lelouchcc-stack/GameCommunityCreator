/**
 * 运行时组件注册表：组件类型（PAGE_SCHEMA §2）→ 渲染组件。
 *
 * 与 frontend/src/components/registry.ts 的 COMPONENT_DEFS 一一对应（11 种）。
 * 新增组件类型时必须同时改：docs/PAGE_SCHEMA.md → components/registry.ts → 本文件。
 */
import type { Component } from 'vue'
import type { ComponentType } from '@/types/page'
import HeroComponent from './components/HeroComponent.vue'
import AboutComponent from './components/AboutComponent.vue'
import NoticeListComponent from './components/NoticeListComponent.vue'
import FooterComponent from './components/FooterComponent.vue'
import ButtonComponent from './components/ButtonComponent.vue'
import ChangelogListComponent from './components/ChangelogListComponent.vue'
import DocsListComponent from './components/DocsListComponent.vue'
import TeamGridComponent from './components/TeamGridComponent.vue'
import IssueFormComponent from './components/IssueFormComponent.vue'
import LinksGridComponent from './components/LinksGridComponent.vue'
import IssueProgressComponent from './components/IssueProgressComponent.vue'

export const RUNTIME_COMPONENT_MAP: Record<ComponentType, Component> = {
  hero: HeroComponent,
  about: AboutComponent,
  notice_list: NoticeListComponent,
  footer: FooterComponent,
  button: ButtonComponent,
  changelog_list: ChangelogListComponent,
  docs_list: DocsListComponent,
  team_grid: TeamGridComponent,
  issue_form: IssueFormComponent,
  links_grid: LinksGridComponent,
  issue_progress: IssueProgressComponent,
}

/**
 * 按类型取运行时组件。
 * 配置理论上已通过后端校验，但公开快照/编辑器本地草稿仍可能带有未知类型，
 * 此时返回 null，由运行时渲染一行兜底提示而不是整页崩溃。
 */
export function getRuntimeComponent(type: string): Component | null {
  if (!Object.prototype.hasOwnProperty.call(RUNTIME_COMPONENT_MAP, type)) return null
  return RUNTIME_COMPONENT_MAP[type as ComponentType] ?? null
}
