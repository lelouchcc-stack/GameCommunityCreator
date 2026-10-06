<template>
    <div class="comment-list">
        <h3 class="comment-list__heading">
            评论
            <span class="comment-list__total">{{ total }}</span>
        </h3>

        <div class="comment-list__editor">
            <textarea
                v-model="draft"
                class="comment-list__input"
                :placeholder="replyingTo ? `回复 ${replyingTo.authorName || '匿名玩家'}` : '友善发言，共建社区'"
                maxlength="1000"
                rows="3"
            ></textarea>
            <div class="comment-list__editor-bar">
                <button
                    v-if="replyingTo"
                    class="comment-list__cancel"
                    type="button"
                    @click="cancelReply"
                >
                    取消回复
                </button>
                <button
                    class="comment-list__submit"
                    type="button"
                    :disabled="submitting"
                    @click="submit"
                >
                    {{ replyingTo ? "回复" : "发表评论" }}
                </button>
            </div>
        </div>

        <p v-if="loading" class="ui-state ui-state--inline">评论加载中…</p>
        <p v-else-if="!comments.length" class="ui-state ui-state--inline">{{ emptyText }}</p>

        <ul v-else class="comment-list__items">
            <li v-for="comment in comments" :key="comment.id" class="comment-list__item">
                <div class="comment-list__author">
                    {{ comment.authorName || "匿名玩家" }}
                    <time class="comment-list__time">{{ dateOf(comment.createTime) }}</time>
                </div>
                <p class="comment-list__content">{{ comment.content }}</p>
                <button class="comment-list__reply" type="button" @click="startReply(comment)">回复</button>

                <ul v-if="comment.replies.length" class="comment-list__replies">
                    <li v-for="reply in comment.replies" :key="reply.id" class="comment-list__reply-item">
                        <div class="comment-list__author">
                            {{ reply.authorName || "匿名玩家" }}
                            <time class="comment-list__time">{{ dateOf(reply.createTime) }}</time>
                        </div>
                        <p class="comment-list__content">{{ reply.content }}</p>
                        <button class="comment-list__reply" type="button" @click="startReply(reply)">回复</button>
                    </li>
                </ul>
            </li>
        </ul>

        <div v-if="total > pageSize" class="comment-list__pager">
            <button class="comment-list__page-btn" :disabled="pageNum <= 1" @click="go(-1)">上一页</button>
            <span class="comment-list__page-info">{{ pageNum }} / {{ totalPages }}</span>
            <button class="comment-list__page-btn" :disabled="pageNum >= totalPages" @click="go(1)">下一页</button>
        </div>
    </div>
</template>

<script lang="ts" setup>
/**
 * 评论区（业务组件 builtin:comment_list，管理端与用户端同源副本）。
 * 读取 /app/comment/page（匿名可读），发表/回复走 POST /app/comment（需登录，未登录跳登录页）。
 * 两级结构：一级评论下挂 replies。区域样式由外层节点的 --w-* CSS 变量驱动。
 */
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type { UiNode } from "../../types/uiBuilder";
import { createComment, pageComments, type CommentItem } from "../../api/content";
import { isLoggedIn } from "../../utils/session";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";

const props = defineProps<{
    node: UiNode
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());
const route = useRoute();
const content = computed(() => props.node.content ?? {});

const postId = computed<number | undefined>(() => {
    const raw = content.value.postId ?? route.params.id ?? route.query.postId;
    if (raw === undefined || raw === null || raw === "") {
        return undefined;
    }
    const n = Number(raw);
    return Number.isFinite(n) ? n : undefined;
});

const comments = ref<CommentItem[]>([]);
const total = ref(0);
const pageNum = ref(1);
const loading = ref(true);
const submitting = ref(false);
const draft = ref("");
const replyingTo = ref<CommentItem | null>(null);

/** 每页条数与空状态文案都可配：与组件类型里开放的属性保持一致，不留"配了不生效"的键 */
const emptyText = computed(() => String(content.value.emptyText ?? "还没有评论，来抢沙发～"));
const pageSize = computed(() => {
    const n = Number(content.value.pageSize);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : 10;
});

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));

async function load() {
    if (postId.value === undefined) {
        loading.value = false;
        return;
    }
    loading.value = true;
    try {
        const data = await pageComments(postId.value, pageNum.value, pageSize.value);
        comments.value = data.list;
        total.value = data.total;
    } catch {
        comments.value = [];
        total.value = 0;
    } finally {
        loading.value = false;
    }
}

function startReply(comment: CommentItem) {
    if (props.interactive) {
        return;
    }
    replyingTo.value = comment;
}

function cancelReply() {
    replyingTo.value = null;
}

async function submit() {
    if (props.interactive || postId.value === undefined) {
        return;
    }
    const text = draft.value.trim();
    if (!text) {
        return;
    }
    if (!isLoggedIn()) {
        runtime.run({ action: "navigate", url: "/login" });
        return;
    }
    submitting.value = true;
    try {
        await createComment({
            postId: postId.value,
            parentId: replyingTo.value?.id,
            content: text,
        });
        draft.value = "";
        replyingTo.value = null;
        pageNum.value = 1;
        await load();
    } catch {
        // 拦截器已处理
    } finally {
        submitting.value = false;
    }
}

function go(delta: number) {
    const next = pageNum.value + delta;
    if (next < 1 || next > totalPages.value) {
        return;
    }
    pageNum.value = next;
    void load();
}

function dateOf(value: string) {
    return value ? value.slice(0, 16).replace("T", " ") : "";
}

watch(postId, () => {
    pageNum.value = 1;
    void load();
});
onMounted(load);
</script>

<style scoped>
.comment-list {
    width: 100%;
}

.comment-list__heading {
    margin: 0 0 12px;
    font-size: var(--w-title-font-size, 16px);
    font-weight: var(--w-title-font-weight, 600);
    color: var(--w-title-color, var(--site-text-primary, #1f2329));
}

.comment-list__total {
    margin-left: 4px;
    color: var(--w-meta-color, var(--site-text-secondary, #646a73));
    font-size: var(--w-meta-font-size, 13px);
    font-weight: 400;
}

.comment-list__editor {
    margin-bottom: 16px;
}

.comment-list__input {
    width: 100%;
    padding: var(--w-item-padding, 10px 12px);
    border: 1px solid var(--site-border, #e5e6eb);
    border-radius: var(--w-action-radius, 8px);
    box-sizing: border-box;
    font-size: var(--w-body-font-size, 14px);
    resize: vertical;
}

.comment-list__editor-bar {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 8px;
}

.comment-list__submit {
    padding: 6px 16px;
    border: none;
    border-radius: var(--w-action-radius, 6px);
    background-color: var(--site-primary, #3b82f6);
    color: #fff;
    font-size: var(--w-action-font-size, 13px);
    cursor: pointer;
}

.comment-list__cancel {
    padding: 6px 12px;
    border: 1px solid var(--site-border, #e5e6eb);
    border-radius: var(--w-action-radius, 6px);
    background: transparent;
    color: var(--w-action-color, var(--site-text-secondary, #646a73));
    font-size: var(--w-action-font-size, 13px);
    cursor: pointer;
}

.comment-list__items,
.comment-list__replies {
    margin: 0;
    padding: 0;
    list-style: none;
}

.comment-list__item {
    padding: var(--w-item-padding, 12px 0);
    border-bottom: 1px solid var(--w-item-border-color, var(--site-border, #e5e6eb));
}

.comment-list__replies {
    margin: 8px 0 0 16px;
    padding-left: 12px;
    border-left: 2px solid var(--w-item-border-color, var(--site-border, #e5e6eb));
}

.comment-list__reply-item {
    padding: 8px 0;
}

.comment-list__author {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--w-meta-color, var(--site-text-secondary, #646a73));
    font-size: var(--w-meta-font-size, 13px);
}

.comment-list__time {
    color: var(--w-meta-color, var(--site-text-secondary, #646a73));
    font-size: 12px;
}

.comment-list__content {
    margin: 6px 0 4px;
    color: var(--w-body-color, var(--site-text-primary, #1f2329));
    font-size: var(--w-body-font-size, 14px);
    line-height: var(--w-body-line-height, 1.6);
    white-space: pre-wrap;
    word-break: break-word;
}

.comment-list__reply {
    padding: 0;
    border: none;
    background: transparent;
    color: var(--w-action-color, var(--site-primary, #3b82f6));
    font-size: var(--w-action-font-size, 12px);
    cursor: pointer;
}

.comment-list__pager {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    margin-top: 16px;
}

.comment-list__page-btn {
    padding: 4px 12px;
    border: 1px solid var(--site-border, #e5e6eb);
    border-radius: var(--w-action-radius, 6px);
    background: transparent;
    color: var(--w-action-color, var(--site-primary, #3b82f6));
    font-size: var(--w-action-font-size, 13px);
    cursor: pointer;
}

.comment-list__page-btn:disabled {
    color: var(--site-text-secondary, #646a73);
    cursor: not-allowed;
}

.comment-list__page-info {
    color: var(--site-text-secondary, #646a73);
    font-size: var(--w-action-font-size, 13px);
}
</style>
