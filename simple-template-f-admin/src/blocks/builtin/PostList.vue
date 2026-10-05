<template>
    <div class="post-list">
        <div v-if="title" class="post-list__title">{{ title }}</div>

        <p v-if="loading" class="post-list__hint">帖子加载中…</p>
        <p v-else-if="!rows.length" class="post-list__hint">{{ emptyText }}</p>

        <ul v-else class="post-list__items">
            <li
                v-for="post in rows"
                :key="post.id"
                class="post-list__item"
                :class="{ 'post-list__item--link': !interactive }"
                @click="onItemClick(post)"
            >
                <img
                    v-if="showCover && coverOf(post)"
                    class="post-list__cover"
                    :src="coverOf(post)"
                    :alt="post.title"
                />
                <div class="post-list__body">
                    <div class="post-list__item-title">
                        <span v-if="post.isTop === 1" class="post-list__badge">置顶</span>
                        <span v-if="post.expired" class="post-list__badge post-list__badge--expired">版本过期</span>
                        {{ post.title }}
                    </div>
                    <div class="post-list__meta">
                        <span v-if="post.boardName">{{ post.boardName }}</span>
                        <span>{{ post.authorName || "匿名玩家" }}</span>
                        <span v-if="post.versionNo">{{ post.versionNo }}</span>
                        <span v-if="post.stageName">{{ post.stageName }}</span>
                        <span>赞 {{ post.likeCount }}</span>
                        <span>浏览 {{ post.viewCount }}</span>
                    </div>
                </div>
                <time class="post-list__date">{{ dateOf(post.createTime) }}</time>
            </li>
        </ul>

        <div v-if="total > pageSize" class="post-list__pager">
            <button class="post-list__page-btn" :disabled="pageNum <= 1" @click="go(-1)">上一页</button>
            <span class="post-list__page-info">{{ pageNum }} / {{ totalPages }}</span>
            <button class="post-list__page-btn" :disabled="pageNum >= totalPages" @click="go(1)">下一页</button>
        </div>
    </div>
</template>

<script lang="ts" setup>
/**
 * 帖子列表（业务组件 builtin:post_list，管理端与用户端同源副本）。
 * 数据取 /app/post/page，支持板块/版本/阶段/标签/关键字筛选与 latest|hot 排序。
 * 板块详情页把路由参数（/board/:id、?versionId=…）作为筛选来源，管理端预览可读内容属性兜底。
 * 各区域样式由外层节点的 --w-* CSS 变量驱动（列表项/标题/元信息/封面/序号角标/空状态/动作）。
 */
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type { UiNode } from "../../types/uiBuilder";
import { pagePosts, type PostListItem } from "../../api/content";
import { resolveAssetUrl } from "../../utils/asset";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";

const props = defineProps<{
    node: UiNode
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());
const route = useRoute();

const content = computed(() => props.node.content ?? {});
const title = computed(() => String(content.value.title ?? ""));
const emptyText = computed(() => String(content.value.emptyText ?? "暂无帖子"));
const showCover = computed(() => content.value.showCover !== false);
const sort = computed<"latest" | "hot">(() => (content.value.sort === "hot" ? "hot" : "latest"));
const pageSize = computed(() => {
    const n = Number(content.value.pageSize);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : 10;
});

/** 内容属性优先，其次路由参数/查询串——板块详情页据此筛当前板块/版本/阶段 */
const boardId = computed(() => numOrUndef(content.value.boardId) ?? numOrUndef(route.params.id) ?? numOrUndef(route.query.boardId));
const versionId = computed(() => numOrUndef(content.value.versionId) ?? numOrUndef(route.query.versionId));
const stageId = computed(() => numOrUndef(content.value.stageId) ?? numOrUndef(route.query.stageId));
const tagId = computed(() => numOrUndef(content.value.tagId) ?? numOrUndef(route.query.tagId));
const keyword = computed(() => String(content.value.keyword ?? route.query.keyword ?? ""));

const rows = ref<PostListItem[]>([]);
const total = ref(0);
const pageNum = ref(1);
const loading = ref(true);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));

async function load() {
    loading.value = true;
    try {
        const data = await pagePosts({
            pageNum: pageNum.value,
            pageSize: pageSize.value,
            boardId: boardId.value,
            versionId: versionId.value,
            stageId: stageId.value,
            tagId: tagId.value,
            keyword: keyword.value,
            sort: sort.value,
        });
        rows.value = data.list;
        total.value = data.total;
    } catch {
        rows.value = [];
        total.value = 0;
    } finally {
        loading.value = false;
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

function coverOf(post: PostListItem) {
    return resolveAssetUrl(post.cover);
}

function dateOf(value: string) {
    return value ? value.slice(0, 10) : "";
}

function onItemClick(post: PostListItem) {
    if (props.interactive) {
        return;
    }
    runtime.run({ action: "navigate", url: `/post/${post.id}` });
}

function numOrUndef(value: unknown): number | undefined {
    if (value === undefined || value === null || value === "") {
        return undefined;
    }
    const n = Number(value);
    return Number.isFinite(n) ? n : undefined;
}

// 路由查询变化（版本/阶段筛选）时回到第一页重查
watch([boardId, versionId, stageId, tagId, keyword, sort], () => {
    pageNum.value = 1;
    void load();
});

onMounted(load);
</script>

<style scoped>
.post-list {
    width: 100%;
}

.post-list__title {
    margin: 0 0 8px;
    font-size: var(--w-title-font-size, 16px);
    font-weight: 600;
    color: var(--site-text-primary, #1f2329);
}

.post-list__hint {
    margin: 0;
    color: var(--w-empty-color, var(--site-text-secondary, #646a73));
    font-size: var(--w-empty-font-size, 14px);
}

.post-list__items {
    margin: 0;
    padding: 0;
    list-style: none;
}

.post-list__item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: var(--w-item-padding, 12px 0);
    border-bottom: 1px solid var(--w-item-border-color, var(--site-border, #e5e6eb));
    transition: background-color 0.15s;
}

.post-list__item--link {
    cursor: pointer;
}

.post-list__item--link:hover {
    background-color: var(--w-item-hover-background-color, transparent);
}

.post-list__cover {
    flex: none;
    width: var(--w-cover-width, 72px);
    height: var(--w-cover-height, 52px);
    border-radius: var(--w-cover-radius, 6px);
    object-fit: var(--w-cover-fit, cover);
}

.post-list__body {
    flex: 1;
    min-width: 0;
}

.post-list__item-title {
    overflow: hidden;
    font-size: var(--w-title-font-size, 15px);
    font-weight: var(--w-title-font-weight, 600);
    color: var(--w-title-color, var(--site-text-primary, #1f2329));
    line-height: 1.5;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.post-list__badge {
    margin-right: 6px;
    padding: 0 5px;
    border-radius: 3px;
    background-color: var(--w-badge-background-color, #fff1f0);
    color: var(--w-badge-text-color, #f5222d);
    font-size: 12px;
}

.post-list__badge--expired {
    background-color: var(--w-badge-background-color, #fff7e6);
    color: var(--w-badge-text-color, #fa8c16);
}

.post-list__meta {
    display: flex;
    flex-wrap: wrap;
    gap: var(--w-meta-gap, 10px);
    margin-top: 4px;
    color: var(--w-meta-color, var(--site-text-secondary, #646a73));
    font-size: var(--w-meta-font-size, 12px);
}

.post-list__date {
    flex: none;
    color: var(--w-meta-color, var(--site-text-secondary, #646a73));
    font-size: var(--w-meta-font-size, 12px);
}

.post-list__pager {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    margin-top: 16px;
}

.post-list__page-btn {
    padding: 4px 12px;
    border: 1px solid var(--site-border, #e5e6eb);
    border-radius: var(--w-action-radius, 6px);
    background: transparent;
    color: var(--w-action-color, var(--site-primary, #3b82f6));
    font-size: var(--w-action-font-size, 13px);
    cursor: pointer;
}

.post-list__page-btn:disabled {
    color: var(--site-text-secondary, #646a73);
    cursor: not-allowed;
}

.post-list__page-info {
    color: var(--site-text-secondary, #646a73);
    font-size: var(--w-action-font-size, 13px);
}
</style>
