<template>
    <div class="post-detail">
        <p v-if="loading" class="post-detail__hint">内容加载中…</p>
        <p v-else-if="!post" class="post-detail__hint">{{ errorText }}</p>

        <article v-else class="post-detail__article">
            <h1 class="post-detail__title">
                <span v-if="post.isTop === 1" class="post-detail__badge">置顶</span>
                <span v-if="post.expired" class="post-detail__badge post-detail__badge--expired">版本过期</span>
                {{ post.title }}
            </h1>

            <div class="post-detail__meta">
                <span>{{ post.authorName || "匿名玩家" }}</span>
                <span v-if="post.boardName">{{ post.boardName }}</span>
                <span v-if="post.versionNo">{{ post.versionNo }}</span>
                <span v-if="post.stageName">{{ post.stageName }}</span>
                <span>{{ dateOf(post.createTime) }}</span>
                <span>浏览 {{ post.viewCount }}</span>
                <span>赞 {{ post.likeCount }}</span>
                <span>收藏 {{ post.favoriteCount }}</span>
            </div>

            <div v-if="post.tags.length" class="post-detail__tags">
                <span v-for="tag in post.tags" :key="tag" class="post-detail__tag">{{ tag }}</span>
            </div>

            <img v-if="coverUrl" class="post-detail__cover" :src="coverUrl" :alt="post.title" />

            <div class="post-detail__body">{{ post.content || "（暂无正文）" }}</div>

            <div v-if="post.imageUrls.length" class="post-detail__images">
                <img
                    v-for="(url, i) in post.imageUrls"
                    :key="i"
                    class="post-detail__image"
                    :src="resolveAssetUrl(url)"
                    alt=""
                />
            </div>
        </article>
    </div>
</template>

<script lang="ts" setup>
/**
 * 帖子详情（业务组件 builtin:post_detail，管理端与用户端同源副本）。
 * 数据取 /app/post/{id}（后端同时累加浏览量）；postId 来自内容属性或路由参数。 
 * 各区域样式由外层节点的 --w-* CSS 变量驱动（标题/元信息/正文/封面/角标/空状态）。
 */
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type { UiNode } from "../../types/uiBuilder";
import { getPost, type PostDetail } from "../../api/content";
import { resolveAssetUrl } from "../../utils/asset";

const props = defineProps<{
    node: UiNode
    interactive?: boolean
}>();

const route = useRoute();
const content = computed(() => props.node.content ?? {});

const post = ref<PostDetail | null>(null);
const loading = ref(true);
const errorText = ref("内容不可见或已下架");

const postId = computed<number | undefined>(() => {
    const raw = content.value.postId ?? route.params.id ?? route.query.postId;
    if (raw === undefined || raw === null || raw === "") {
        return undefined;
    }
    const n = Number(raw);
    return Number.isFinite(n) ? n : undefined;
});

const coverUrl = computed(() => resolveAssetUrl(post.value?.cover ?? null));

function dateOf(value: string) {
    return value ? value.slice(0, 10) : "";
}

async function load() {
    if (postId.value === undefined) {
        loading.value = false;
        post.value = null;
        errorText.value = "未指定帖子";
        return;
    }
    loading.value = true;
    try {
        post.value = await getPost(postId.value);
    } catch {
        post.value = null;
        errorText.value = "内容不可见或已下架";
    } finally {
        loading.value = false;
    }
}

watch(postId, load);
onMounted(load);
</script>

<style scoped>
.post-detail {
    width: 100%;
}

.post-detail__hint {
    margin: 0;
    color: var(--w-empty-color, var(--site-text-secondary, #646a73));
    font-size: var(--w-empty-font-size, 14px);
}

.post-detail__title {
    margin: 0 0 12px;
    font-size: var(--w-title-font-size, 22px);
    font-weight: var(--w-title-font-weight, 700);
    color: var(--w-title-color, var(--site-text-primary, #1f2329));
    line-height: 1.4;
}

.post-detail__badge {
    margin-right: 6px;
    padding: 0 6px;
    border-radius: 3px;
    vertical-align: middle;
    background-color: var(--w-badge-background-color, #fff1f0);
    color: var(--w-badge-text-color, #f5222d);
    font-size: 12px;
    font-weight: 400;
}

.post-detail__badge--expired {
    background-color: var(--w-badge-background-color, #fff7e6);
    color: var(--w-badge-text-color, #fa8c16);
}

.post-detail__meta {
    display: flex;
    flex-wrap: wrap;
    gap: var(--w-meta-gap, 12px);
    color: var(--w-meta-color, var(--site-text-secondary, #646a73));
    font-size: var(--w-meta-font-size, 13px);
}

.post-detail__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 12px;
}

.post-detail__tag {
    padding: 2px 10px;
    border-radius: var(--w-action-radius, 12px);
    background-color: var(--w-item-hover-background-color, var(--site-bg-page, #f7f8fa));
    color: var(--w-meta-color, var(--site-text-secondary, #646a73));
    font-size: var(--w-meta-font-size, 12px);
}

.post-detail__cover {
    display: block;
    width: var(--w-cover-width, 100%);
    max-height: var(--w-cover-height, 360px);
    margin: 16px 0;
    border-radius: var(--w-cover-radius, 8px);
    object-fit: var(--w-cover-fit, cover);
}

.post-detail__body {
    margin-top: 16px;
    color: var(--w-body-color, var(--site-text-primary, #1f2329));
    font-size: var(--w-body-font-size, 15px);
    line-height: var(--w-body-line-height, 1.8);
    white-space: pre-wrap;
    word-break: break-word;
}

.post-detail__images {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-top: 16px;
}

.post-detail__image {
    width: 100%;
    border-radius: 8px;
}
</style>
