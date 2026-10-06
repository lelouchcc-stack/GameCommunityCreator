<template>
    <section class="notice-list">
        <div v-if="title" class="notice-list__head">
            <h3 class="notice-list__title">{{ title }}</h3>
        </div>

        <p v-if="loading" class="ui-state ui-state--inline">公告加载中…</p>
        <p v-else-if="!rows.length" class="ui-state ui-state--inline">{{ emptyText }}</p>

        <ul v-else class="notice-list__items">
            <li
                v-for="notice in rows"
                :key="notice.id"
                class="notice-list__item"
                :class="{ 'notice-list__item--link': !interactive }"
                @click="open(notice, $event)"
            >
                <span class="notice-list__tag" :class="{ 'notice-list__tag--version': notice.type === 2 }">
                    {{ notice.type === 2 ? "版本" : "公告" }}
                </span>
                <div class="notice-list__body">
                    <div class="notice-list__item-title">{{ notice.title }}</div>
                    <p v-if="showContent && excerptOf(notice)" class="notice-list__excerpt">
                        {{ excerptOf(notice) }}
                    </p>
                </div>
                <time class="notice-list__date">{{ dateOf(notice.createTime) }}</time>
            </li>
        </ul>

        <div v-if="total > pageSize" class="notice-list__pager">
            <button class="notice-list__page-btn" :disabled="pageNum <= 1" @click="go(-1)">上一页</button>
            <span class="notice-list__page-info">{{ pageNum }} / {{ totalPages }}</span>
            <button class="notice-list__page-btn" :disabled="pageNum >= totalPages" @click="go(1)">下一页</button>
        </div>
    </section>
</template>

<script lang="ts" setup>
/**
 * 公告列表（业务组件 builtin:notice_list）。
 * 数据取 /app/notice/page（已发布公告分页，匿名可读）；点条目进 /notice/:id。
 * 首页那条公告栏（builtin:notice）是"摘要条"，这里是完整的公告页列表。
 */
import { computed, inject, onMounted, ref } from "vue";
import type { UiNode } from "../../types/uiBuilder";
import { pageNotices, type NoticeItem } from "../../api/notice";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";
import { GRAPH_RUNTIME_KEY, type GraphBridge } from "../graphBridge";
import { activateNode } from "../nodeActivation";

const props = defineProps<{
    node: UiNode
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());
/** 图执行器（用户端才注入；管理端画布没有，退化成"只看点击动作与默认行为"） */
const graphBridge = inject<GraphBridge | null>(GRAPH_RUNTIME_KEY, null);

const content = computed(() => props.node.content ?? {});
const title = computed(() => String(content.value.title ?? ""));
const emptyText = computed(() => String(content.value.emptyText ?? "暂无公告"));
const showContent = computed(() => content.value.showContent === true);
const pageSize = computed(() => {
    const n = Number(content.value.pageSize);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : 10;
});

const rows = ref<NoticeItem[]>([]);
const total = ref(0);
const pageNum = ref(1);
const loading = ref(true);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));

async function load() {
    loading.value = true;
    try {
        const data = await pageNotices(pageNum.value, pageSize.value);
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

function open(notice: NoticeItem, event: MouseEvent) {
    // 点击归节点处理：节点上有逻辑图/点击动作时优先，否则才进这条公告的详情
    const handled = activateNode({
        node: props.node,
        runtime,
        graphBridge,
        interactive: props.interactive,
        defaultAction: { action: "navigate", url: `/notice/${notice.id}` },
    });
    if (handled) {
        event.stopPropagation();
    }
}

function dateOf(value: string) {
    return value ? value.slice(0, 10) : "";
}

/** 摘要：正文去掉换行后截断，列表里只做"值不值得点进去"的判断依据 */
function excerptOf(notice: NoticeItem) {
    const raw = (notice.content ?? "").replace(/\s+/g, " ").trim();
    return raw.length > 70 ? `${raw.slice(0, 70)}…` : raw;
}

onMounted(load);
</script>

<style scoped>
.notice-list {
    width: 100%;
}

.notice-list__title {
    margin: 0 0 4px;
    color: var(--w-title-color, var(--site-text-primary));
    font-size: var(--w-title-font-size, 16px);
    font-weight: var(--w-title-font-weight, 600);
}

.notice-list__items {
    margin: 0;
    padding: 0;
    list-style: none;
}

.notice-list__item {
    display: flex;
    gap: 12px;
    align-items: flex-start;
    padding: var(--w-item-padding, 12px 0);
    border-bottom: 1px solid var(--w-item-border-color, var(--site-border));
    transition: background-color 0.15s;
}

.notice-list__item--link {
    cursor: pointer;
}

.notice-list__item--link:hover {
    background-color: var(--w-item-hover-background-color, transparent);
}

.notice-list__tag {
    flex: none;
    margin-top: 2px;
    padding: 1px 8px;
    border-radius: var(--w-action-radius, var(--site-radius-sm));
    background-color: var(--w-badge-background-color, var(--site-primary-weak));
    color: var(--w-badge-text-color, var(--site-primary));
    font-size: 12px;
    line-height: 1.7;
}

.notice-list__tag--version {
    background-color: var(--w-badge-background-color, var(--site-primary-weak));
    color: var(--w-badge-text-color, var(--site-primary));
}

.notice-list__body {
    flex: 1;
    min-width: 0;
}

.notice-list__item-title {
    overflow: hidden;
    color: var(--w-title-color, var(--site-text-primary));
    font-size: var(--w-title-font-size, 15px);
    font-weight: var(--w-title-font-weight, 500);
    line-height: 1.5;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.notice-list__excerpt {
    display: -webkit-box;
    margin: 4px 0 0;
    overflow: hidden;
    color: var(--w-body-color, var(--site-text-secondary));
    font-size: var(--w-body-font-size, 13px);
    line-height: 1.6;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
}

.notice-list__date {
    flex: none;
    color: var(--w-meta-color, var(--site-text-muted));
    font-size: var(--w-meta-font-size, 12px);
}

.notice-list__pager {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: center;
    margin-top: 16px;
}

.notice-list__page-btn {
    padding: 4px 12px;
    border: 1px solid var(--site-border);
    border-radius: var(--w-action-radius, var(--site-radius-sm));
    background: transparent;
    color: var(--w-action-color, var(--site-primary));
    font-size: var(--w-action-font-size, 13px);
    cursor: pointer;
}

.notice-list__page-btn:disabled {
    color: var(--site-text-muted);
    cursor: not-allowed;
}

.notice-list__page-info {
    color: var(--site-text-muted);
    font-size: var(--w-action-font-size, 13px);
}

/* 窄屏：日期换到下一行，标题保持可读 */
@media (max-width: 640px) {
    .notice-list__item {
        flex-wrap: wrap;
    }

    .notice-list__body {
        flex: 1 1 100%;
        order: 3;
    }
}
</style>
