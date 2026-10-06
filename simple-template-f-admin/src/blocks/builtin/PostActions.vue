<template>
    <div class="post-actions">
        <button
            class="post-actions__btn"
            :class="{ 'post-actions__btn--active': liked }"
            type="button"
            @click="onLike"
        >
            {{ liked ? "已赞" : "点赞" }}
            <span v-if="loaded" class="post-actions__count">{{ likeCount }}</span>
        </button>
        <button
            class="post-actions__btn"
            :class="{ 'post-actions__btn--active': favorited }"
            type="button"
            @click="onFavorite"
        >
            {{ favorited ? "已收藏" : "收藏" }}
            <span v-if="loaded" class="post-actions__count">{{ favoriteCount }}</span>
        </button>
    </div>
</template>

<script lang="ts" setup>
/**
 * 帖子互动（业务组件 builtin:post_actions，管理端与用户端同源副本）。
 * 点赞/收藏点击即切换；未登录先跳登录页。计数与状态取 /app/post/{id}/interaction（需登录）。
 * 区域样式由外层节点的 --w-* CSS 变量驱动（动作颜色/背景/字号/圆角，激活态用角标色）。
 */
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type { UiNode } from "../../types/uiBuilder";
import { favoritePost, getInteraction, likePost } from "../../api/content";
import { isLoggedIn } from "../../utils/session";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";

const props = defineProps<{
    node: UiNode
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());
const route = useRoute();
const content = computed(() => props.node.content ?? {});

const liked = ref(false);
const favorited = ref(false);
const likeCount = ref(0);
const favoriteCount = ref(0);
const loaded = ref(false);

const postId = computed<number | undefined>(() => {
    const raw = content.value.postId ?? route.params.id ?? route.query.postId;
    if (raw === undefined || raw === null || raw === "") {
        return undefined;
    }
    const n = Number(raw);
    return Number.isFinite(n) ? n : undefined;
});

async function load() {
    if (postId.value === undefined || !isLoggedIn()) {
        loaded.value = false;
        return;
    }
    try {
        const data = await getInteraction(postId.value);
        liked.value = data.liked;
        favorited.value = data.favorited;
        likeCount.value = data.likeCount;
        favoriteCount.value = data.favoriteCount;
        loaded.value = true;
    } catch {
        loaded.value = false;
    }
}

function requireLogin(): boolean {
    if (isLoggedIn()) {
        return true;
    }
    runtime.run({ action: "navigate", url: "/login" });
    return false;
}

async function onLike() {
    if (props.interactive || postId.value === undefined || !requireLogin()) {
        return;
    }
    try {
        const data = await likePost(postId.value);
        liked.value = data.liked;
        likeCount.value = data.likeCount;
        loaded.value = true;
    } catch {
        // 请求拦截器已处理（401 会清登录态）
    }
}

async function onFavorite() {
    if (props.interactive || postId.value === undefined || !requireLogin()) {
        return;
    }
    try {
        const data = await favoritePost(postId.value);
        favorited.value = data.favorited;
        favoriteCount.value = data.favoriteCount;
        loaded.value = true;
    } catch {
        // 同上
    }
}

watch(postId, load);
onMounted(load);
</script>

<style scoped>
.post-actions {
    display: flex;
    gap: 12px;
    align-items: center;
}

.post-actions__btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 16px;
    border: 1px solid var(--site-border, #e5e6eb);
    border-radius: var(--w-action-radius, 999px);
    background-color: var(--w-action-background-color, transparent);
    color: var(--w-action-color, var(--site-text-primary, #1f2329));
    font-size: var(--w-action-font-size, 13px);
    cursor: pointer;
    transition: color 0.15s, background-color 0.15s, border-color 0.15s;
}

.post-actions__btn--active {
    border-color: var(--site-primary, #3b82f6);
    background-color: var(--w-badge-background-color, #e8f3ff);
    color: var(--w-badge-text-color, var(--site-primary, #3b82f6));
}

.post-actions__count {
    font-size: 12px;
    opacity: 0.85;
}
</style>
