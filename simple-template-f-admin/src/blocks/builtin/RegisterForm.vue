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
                autocomplete="new-password"
                :placeholder="passwordPlaceholder"
                :disabled="interactive || submitting"
            />
        </label>
        <label class="auth-form__field">
            <span class="auth-form__label">昵称</span>
            <input
                v-model="nickname"
                class="auth-form__input"
                type="text"
                :placeholder="nicknamePlaceholder"
                :disabled="interactive || submitting"
            />
        </label>
        <button class="auth-form__submit" type="submit" :disabled="interactive || submitting">
            {{ submitting ? "注册中…" : submitText }}
        </button>
        <p v-if="showLoginLink" class="auth-form__foot">
            已有账号？
            <button class="auth-form__link" type="button" @click="goLogin">立即登录</button>
        </p>
    </form>
</template>

<script lang="ts" setup>
/**
 * 注册表单（业务组件 builtin:register_form，管理端与用户端同源副本）。
 *
 * 与 builtin:login_form 同构：自包含、经 utils/session 调 /app/register，注册即登录后跳转。
 * 校验规则与后端 MemberRegisterDTO 保持一致（用户名 4-20 位字母数字下划线、密码 6-20 位），
 * 前端先拦一次只是为了少一次往返，真正的事实源仍是后端。
 */
import { computed, inject, ref } from "vue";
import type { UiNode } from "../../types/uiBuilder";
import { register } from "../../utils/session";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";

const props = defineProps<{
    node: UiNode
    /** 管理端画布为 true：不提交、不跳转，点击留给画布选中该节点 */
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());

/** 与后端 MemberRegisterDTO 的 @Pattern 同一条规则 */
const USERNAME_RE = /^[A-Za-z0-9_]{4,20}$/;

const content = computed(() => props.node.content ?? {});
const title = computed(() => text(content.value.title));
const submitText = computed(() => text(content.value.submitText) || "注册");
const usernamePlaceholder = computed(() => text(content.value.usernamePlaceholder) || "4-20 位字母、数字或下划线");
const passwordPlaceholder = computed(() => text(content.value.passwordPlaceholder) || "6-20 位密码");
const nicknamePlaceholder = computed(() => text(content.value.nicknamePlaceholder) || "昵称（可选，默认同用户名）");
const showLoginLink = computed(() => content.value.showLoginLink !== false);
const redirectTo = computed(() => text(content.value.redirectTo) || "/");

const username = ref("");
const password = ref("");
const nickname = ref("");
const submitting = ref(false);

function text(value: unknown): string {
    return value === undefined || value === null ? "" : String(value);
}

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
    if (!USERNAME_RE.test(name)) {
        notify("用户名需为 4-20 位字母、数字或下划线", "warning");
        return;
    }
    if (password.value.length < 6 || password.value.length > 20) {
        notify("密码长度需为 6-20 位", "warning");
        return;
    }
    submitting.value = true;
    try {
        await register(name, password.value, nickname.value.trim() || undefined);
        notify("注册成功，已自动登录", "success");
        go(redirectTo.value);
    } catch (error) {
        notify(error instanceof Error ? error.message : "注册失败", "error");
    } finally {
        submitting.value = false;
    }
}

function goLogin() {
    if (!props.interactive) {
        go("/login");
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
