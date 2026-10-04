<template>
    <div class="hot-post">
        <p v-if="loading" class="hot-post__hint">帖子加载中…</p>
        <p v-else-if="!posts.length" class="hot-post__hint">暂无帖子</p>
        <ul v-else class="hot-post__list">
            <li
                v-for="(post, i) in posts"
                :key="post.id"
                class="hot-post__item"
                :class="{ 'hot-post__item--link': !interactive }"
                @click="onPostClick(post)"
            >
                <span class="hot-post__rank" :class="rankClass(i)">{{ i + 1 }}</span>
                <img
                    v-if="coverOf(post)"
                    class="hot-post__cover"
                    :src="coverOf(post)"
                    :alt="post.title"
                />
                <div class="hot-post__body">
                    <div class="hot-post__title">
                        <span v-if="post.isTop === 1" class="hot-post__top">置顶</span>{{ post.title }}
                    </div>
                    <div class="hot-post__meta">
                        <span v-if="post.boardName" class="hot-post__board">{{ post.boardName }}</span>
                        <span>{{ post.authorName || "匿名玩家" }}</span>
                        <span>赞 {{ post.likeCount }}</span>
                        <span>浏览 {{ post.viewCount }}</span>
                    </div>
                </div>
            </li>
        </ul>
    </div>
</template>

<script lang="ts" setup>
/**
 * 首页热门帖子（业务组件 builtin:hot_post，管理端与用户端同源副本）。
 * 数据取 /app/post/hot（置顶优先，再按点赞、浏览降序），条数由节点内容属性 limit 决定。
 * 帖子详情页还没做（pit.md 模块 04），点击先给一条提示，避免跳到一个空页面。
 */
import { computed, inject, onMounted, ref } from "vue";
import type { UiNode } from "../../types/uiBuilder";
import { listHotPosts, type HotPostItem } from "../../api/post";
import { resolveAssetUrl } from "../../utils/asset";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";

const props = defineProps<{
    node: UiNode
    /** 管理端画布为 true：点击不弹提示，把点击留给画布选中节点 */
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());

const posts = ref<HotPostItem[]>([]);
const loading = ref(true);

const limit = computed(() => {
    const n = Number(props.node.content?.limit);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : 10;
});

function coverOf(post: HotPostItem) {
    return resolveAssetUrl(post.cover);
}

/** 前三名用高亮色，和榜单的观感一致 */
function rankClass(index: number) {
    if (index === 0) {
        return "hot-post__rank--first";
    }
    if (index === 1) {
        return "hot-post__rank--second";
    }
    if (index === 2) {
        return "hot-post__rank--third";
    }
    return "";
}

function onPostClick(post: HotPostItem) {
    if (props.interactive) {
        return;
    }
    runtime.run({
        action: "toast",
        message: `「${post.title}」详情页待接入`,
        toastType: "info",
    });
}

onMounted(async () => {
    try {
        posts.value = await listHotPosts(limit.value);
    } catch {
        posts.value = [];
    } finally {
        loading.value = false;
    }
});
</script>

<style scoped>
.hot-post {
    width: 100%;
}

.hot-post__hint {
    margin: 0;
    color: var(--site-text-secondary, #646a73);
    font-size: 14px;
}

.hot-post__list {
    margin: 0;
    padding: 0;
    list-style: none;
}

.hot-post__item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 0;
    border-top: 1px solid var(--site-border, #e5e6eb);
}

.hot-post__item:first-child {
    border-top: none;
    padding-top: 0;
}

.hot-post__item--link {
    cursor: pointer;
}

.hot-post__rank {
    flex: none;
    width: 20px;
    color: var(--site-text-secondary, #646a73);
    font-size: 13px;
    font-weight: 600;
    text-align: center;
}

.hot-post__rank--first {
    color: #f5222d;
}

.hot-post__rank--second {
    color: #fa8c16;
}

.hot-post__rank--third {
    color: #faad14;
}

.hot-post__cover {
    flex: none;
    width: 64px;
    height: 44px;
    border-radius: 6px;
    object-fit: cover;
}

.hot-post__body {
    min-width: 0;
}

.hot-post__title {
    overflow: hidden;
    color: var(--site-text-primary, #1f2329);
    line-height: 1.5;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.hot-post__top {
    margin-right: 6px;
    padding: 0 5px;
    border-radius: 3px;
    background-color: #fff1f0;
    color: #f5222d;
    font-size: 12px;
}

.hot-post__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-top: 4px;
    color: var(--site-text-secondary, #646a73);
    font-size: 12px;
}

.hot-post__board {
    color: var(--site-primary, #3b82f6);
}
</style>
