/**
 * 业务组件注册表（管理端与用户端同源副本）：render_kind 以 builtin: 开头时按这里查组件。
 * 未注册的 builtin:* 由 NodeRenderer 回落到占位，不会白屏。
 */
import type { Component } from "vue";
import BoardNav from "./builtin/BoardNav.vue";
import LoginForm from "./builtin/LoginForm.vue";
import RegisterForm from "./builtin/RegisterForm.vue";

export const BUILTIN_WIDGETS: Record<string, Component> = {
    "builtin:board_nav": BoardNav,
    "builtin:login_form": LoginForm,
    "builtin:register_form": RegisterForm,
};

/** 取出 renderKind 对应的业务组件；不是业务组件或尚未接入时返回 null */
export function resolveBuiltin(renderKind?: string | null): Component | null {
    if (!renderKind || !renderKind.startsWith("builtin:")) {
        return null;
    }
    return BUILTIN_WIDGETS[renderKind] ?? null;
}
