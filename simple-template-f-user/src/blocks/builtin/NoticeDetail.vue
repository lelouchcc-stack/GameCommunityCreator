<template>
    <article class="notice-detail">
        <p v-if="loading" class="ui-state">公告加载中…</p>
        <p v-else-if="!notice" class="ui-state ui-state--error">{{ errorText }}</p>

        <template v-else>
            <header class="notice-detail__head">
                <span class="notice-detail__tag" :class="{ 'notice-detail__tag--version': notice.type === 2 }">
                    {{ notice.type === 2 ? "版本更新" : "社区公告" }}
                </span>
                <h1 class="notice-detail__title">{{ notice.title }}</h1>
                <div class="notice-detail__meta">
                    <time>{{ dateOf(notice.createTime) }}</time>
                </div>
            </header>

            <div class="notice-detail__body">{{ notice.content || "（暂无内容）" }}</div>

            <footer class="notice-detail__foot">
                <router-link class="notice-detail__back" to="/notice">← 返回公告列表</router-link>
            </footer>
        </template>
    </article>
</template>

<script lang="ts" setup>
/**
 * 公告详情（业务组件 builtin:notice_detail）。
 * 公告 id 来自节点内容属性或路由参数（模板挂在 /notice/:id）。
 * 公告正文是纯文本，用 pre-wrap 保留换行；不做富文本渲染，避免引入 HTML 注入面。
 */
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type { UiNode } from "../../types/uiBuilder";
import { getNotice, type NoticeItem } from "../../api/notice";

const props = defineProps<{
    node: UiNode
    interactive?: boolean
}>();

const route = useRoute();
const content = computed(() => props.node.content ?? {});

const notice = ref<NoticeItem | null>(null);
const loading = ref(true);
const errorText = ref("公告不存在或已下线");

const noticeId = computed<number | undefined>(() => {
    const raw = content.value.noticeId ?? route.params.id ?? route.query.noticeId;
    if (raw === undefined || raw === null || raw === "") {
        return undefined;
    }
    const n = Number(raw);
    return Number.isFinite(n) ? n : undefined;
});

function dateOf(value: string) {
    return value ? value.slice(0, 10) : "";
}

async function load() {
    if (noticeId.value === undefined) {
        loading.value = false;
        notice.value = null;
        errorText.value = "未指定公告";
        return;
    }
    loading.value = true;
    try {
        notice.value = await getNotice(noticeId.value);
    } catch {
        notice.value = null;
        errorText.value = "公告不存在或已下线";
    } finally {
        loading.value = false;
    }
}

watch(noticeId, load);
onMounted(load);
</script>

<style scoped>
.notice-detail {
    width: 100%;
}

.notice-detail__head {
    padding-bottom: 16px;
    border-bottom: 1px solid var(--w-item-border-color, var(--site-border));
}

.notice-detail__tag {
    display: inline-block;
    padding: 2px 10px;
    border-radius: var(--w-action-radius, var(--site-radius-pill));
    background-color: var(--w-badge-background-color, var(--site-primary-weak));
    color: var(--w-badge-text-color, var(--site-primary));
    font-size: 12px;
    line-height: 1.8;
}

.notice-detail__title {
    margin: 12px 0 0;
    color: var(--w-title-color, var(--site-text-primary));
    font-size: var(--w-title-font-size, 24px);
    font-weight: var(--w-title-font-weight, 700);
    line-height: 1.4;
    word-break: break-word;
}

.notice-detail__meta {
    display: flex;
    flex-wrap: wrap;
    gap: var(--w-meta-gap, 12px);
    margin-top: 10px;
    color: var(--w-meta-color, var(--site-text-muted));
    font-size: var(--w-meta-font-size, 13px);
}

.notice-detail__body {
    margin-top: 20px;
    color: var(--w-body-color, var(--site-text-primary));
    font-size: var(--w-body-font-size, 15px);
    line-height: var(--w-body-line-height, 1.85);
    white-space: pre-wrap;
    word-break: break-word;
}

.notice-detail__foot {
    margin-top: 28px;
    padding-top: 16px;
    border-top: 1px solid var(--w-item-border-color, var(--site-border));
}

.notice-detail__back {
    color: var(--w-action-color, var(--site-primary));
    font-size: var(--w-action-font-size, 13px);
}

@media (max-width: 640px) {
    .notice-detail__title {
        font-size: calc(var(--w-title-font-size, 24px) * 0.85);
    }

    .notice-detail__body {
        font-size: 14px;
    }
}
</style>
