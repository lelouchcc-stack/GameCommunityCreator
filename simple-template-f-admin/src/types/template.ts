import type { SiteConfig } from "./site";

/** 模板的一个输入项（与后端 TemplateVO.InputVO 对应），管理端按它动态渲染表单 */
export interface TemplateInput {
    key: string
    label: string
    type: "text" | "textarea" | "color" | "image" | "boolean"
    required: boolean
    maxLength: number | null
    placeholder: string | null
    help: string | null
    defaultValue: string | null
}

/** 一个可用的社区模板（与后端 TemplateVO 对应） */
export interface CommunityTemplate {
    code: string
    name: string
    nameZh: string
    version: number
    description: string
    inputs: TemplateInput[]
    pageKeys: string[]
    /** 模板依赖但不覆盖的既有页面：pageKey -> 说明 */
    relatedPages: Record<string, string>
    /** 模板认识、但已废弃可由管理员删除的页面：pageKey -> 说明 */
    customPages: Record<string, string>
}

/** 应用模板的结果报告（与后端 TemplateApplyVO 对应） */
export interface TemplateApplyReport {
    code: string
    name: string
    theme: SiteConfig
    createdPages: string[]
    rebuiltPages: string[]
    keptPages: string[]
    createdComponents: string[]
    updatedComponents: string[]
    seeded: string[]
    skippedSeed: string[]
    warnings: string[]
}
