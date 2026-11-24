/**
 * 运行时内部共享类型（不属于冻结契约，仅供 runtime/** 使用）。
 */

/** 运行时工作模式：编辑器画布 / 管理端草稿预览 / 公开站点 */
export type RuntimeMode = 'editor' | 'preview' | 'public'
