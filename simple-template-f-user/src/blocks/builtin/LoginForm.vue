<template>
    <form class="auth-form" @submit.prevent="onSubmit">
        <h2 v-if="title" class="auth-form__title">{{ title }}</h2>
        <label class="auth-form__field">
            <span class="auth-form__label">用户名</span>
            <input
                v-model="username"
                class="auth-form__input"
                type="text"
                autocomplete="username"
                :placeholder="usernamePlaceholder"
                :disabled="interactive || submitting"
            />
        </label>
        <label class="auth-form__field">
            <span class="auth-form__label">密码</span>
            <input
                v-model="password"
                class="auth-form__input"
                type="password"
                autocomplete="current-password"
                :placeholder="passwordPlaceholder"
                :disabled="interactive || submitting"
            />
        </label>
        <button class="auth-form__submit" type="submit" :disabled="interactive || submitting">
            {{ submitting ? "登录中…" : submitText }}
        </button>
        <p v-if="showRegisterLink" class="auth-form__foot">
            还没有账号？
            <button class="auth-form__link" type="button" @click="goRegister">立即注册</button>
        </p>
    </form>
</template>

<script lang="ts" setup>
/**
 * 登录表单（业务组件 builtin:login_form，管理端与用户端同源副本）。
 *
 * 自包含（对照 builtin:board_nav）：自己渲染输入框、自己经 utils/session 调 /app/login，
 * 成功后按「成功后跳转」配置跳走。业务组件不走逻辑图；preset.login 留给
 * 「点收藏 → 未登录 → 弹登录 → 登录后继续」这类图编排的组合流。
 * toast 与跳转都走区块运行时 —— 两端各自注入自己的实现，组件不直接依赖某一端的工具模块。
 */
import { computed, inject, ref } from "vue";
import type { UiNode } from "../../types/uiBuilder";
import { login } from "../../utils/session";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";

const props = defineProps<{
    node: UiNode
    /** 管理端画布为 true：不提交、不跳转，点击留给画布选中该节点 */
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());

const content = computed(() => props.node.content ?? {});
const title = computed(() => text(content.value.title));
const submitText = computed(() => text(content.value.submitText) || "登录");
const usernamePlaceholder = computed(() => text(content.value.usernamePlaceholder) || "请输入用户名");
const passwordPlaceholder = computed(() => text(content.value.passwordPlaceholder) || "请输入密码");
const showRegisterLink = computed(() => content.value.showRegisterLink !== false);
const redirectTo = computed(() => text(content.value.redirectTo) || "/");

const username = ref("");
const password = ref("");
const submitting = ref(false);

function text(value: unknown): string {
    return value === undefined || value === null ? "" : String(value);
}

/** 提示与跳转统一走区块运行时，避免同源组件依赖某一端专属的工具模块 */
function notify(message: string, type: "info" | "success" | "warning" | "error") {
    runtime.run({ action: "toast", message, toastType: type });
}

function go(url: string) {
    runtime.run({ action: "navigate", url });
}

async function onSubmit() {
    if (props.interactive || submitting.value) {
        return;
    }
    const name = username.value.trim();
    if (!name || !password.value) {
        notify("请填写用户名和密码", "warning");
        return;
    }
    submitting.value = true;
    try {
        await login(name, password.value);
        notify("登录成功", "success");
        go(redirectTo.value);
    } catch (error) {
        notify(error instanceof Error ? error.message : "登录失败", "error");
    } finally {
        submitting.value = false;
    }
}

function goRegister() {
    if (!props.interactive) {
        go("/register");
    }
}
</script>

<style scoped>
.auth-form {
    display: flex;
    flex-direction: column;
    gap: 14px;
    width: 100%;
    box-sizing: border-box;
}

.auth-form__title {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: var(--site-text-primary, #1f2329);
    text-align: center;
}

.auth-form__field {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.auth-form__label {
    font-size: 13px;
    color: var(--site-text-secondary, #646a73);
}

.auth-form__input {
    width: 100%;
    box-sizing: border-box;
    padding: 9px 12px;
    border: 1px solid var(--site-border, #e5e6eb);
    border-radius: 8px;
    background-color: var(--site-bg-page, #f7f8fa);
    color: var(--site-text-primary, #1f2329);
    font-size: 14px;
    outline: none;
    transition: border-color 0.15s;
}

.auth-form__input:focus {
    border-color: var(--site-primary, #3b82f6);
}

.auth-form__submit {
    padding: 10px 16px;
    border: none;
    border-radius: 8px;
    background-color: var(--site-primary, #3b82f6);
    color: #fff;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
}

.auth-form__submit:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.auth-form__foot {
    margin: 0;
    font-size: 13px;
    color: var(--site-text-secondary, #646a73);
    text-align: center;
}

.auth-form__link {
    padding: 0;
    border: none;
    background: none;
    color: var(--site-primary, #3b82f6);
    font-size: 13px;
    cursor: pointer;
}
</style>
