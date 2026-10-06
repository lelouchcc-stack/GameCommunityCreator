import http from "../utils/request";
import type { CommunityTemplate, TemplateApplyReport } from "../types/template";

/** 可用社区模板清单（含各自的输入项声明） */
export function listTemplates() {
    return http.get<CommunityTemplate[], CommunityTemplate[]>("/admin/site/template/list");
}

/**
 * 应用模板建站。values 的键就是模板 input 的 key——
 * 后端按 manifest 声明的输入项读取，前端不写死字段清单。
 */
export function applyTemplate(code: string, values: Record<string, unknown>) {
    return http.post<TemplateApplyReport, TemplateApplyReport>("/admin/site/template/apply", { code, values });
}
