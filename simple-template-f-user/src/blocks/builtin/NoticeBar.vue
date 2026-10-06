<template>
    <div class="notice-bar">
        <span class="notice-bar__label">公告</span>
        <p v-if="loading" class="ui-state ui-state--inline">公告加载中…</p>
        <p v-else-if="!notices.length" class="ui-state ui-state--inline">暂无公告</p>
        <ul v-else class="notice-bar__list">
            <li
                v-for="notice in notices"
                :key="notice.id"
                class="notice-bar__item"
                :class="{ 'notice-bar__item--link': !interactive }"
                :title="notice.title"
                @click="open(notice, $event)"
            >
                <span
                    class="notice-bar__tag"
                    :class="notice.type === 2 ? 'notice-bar__tag--version' : ''"
                >
                    {{ notice.type === 2 ? "版本" : "公告" }}
                </span>
                <span class="notice-bar__title">
                    {{ notice.title }}
                </span>
                <span v-if="dateOf(notice.createTime)" class="notice-bar__date">
                    {{ dateOf(notice.createTime) }}
                </span>
            </li>
        </ul>
        <router-link v-if="showMore && !interactive" class="notice-bar__more" to="/notice">全部 ›</router-link>
    </div>
</template>

<script lang="ts" setup>
/**
 * 首页公告栏（业务组件 builtin:notice，管理端与用户端同源副本）。
 * 数据取 /app/notice/list（已发布公告），条数由节点内容属性 limit 决定。
 * 条目点击进 /notice/:id（公告详情页由 builtin:notice_detail 渲染），
 * 右侧「全部 ›」进公告列表页——公告不再是一个只能看、点不动的装饰区块。
 */
import { computed, inject, onMounted, ref } from "vue";
import type { UiNode } from "../../types/uiBuilder";
import { listNotices, type NoticeItem } from "../../api/notice";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";
import { GRAPH_RUNTIME_KEY, type GraphBridge } from "../graphBridge";
import { activateNode } from "../nodeActivation";

const props = defineProps<{
    node: UiNode
    /** 管理端画布为 true：不跳转，点击留给画布选中节点 */
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());
/** 图执行器（用户端才注入；管理端画布没有，退化成"只看点击动作与默认行为"） */
const graphBridge = inject<GraphBridge | null>(GRAPH_RUNTIME_KEY, null);

const notices = ref<NoticeItem[]>([]);
const loading = ref(true);

const limit = computed(() => {
    const n = Number(props.node.content?.limit);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : 5;
});
const showMore = computed(() => props.node.content?.showMore !== false);

/** createTime 形如 2026-10-05T01:22:23，只取日期用来弱化显示 */
function dateOf(value?: string | null) {
    if (!value) {
        return "";
    }
    return value.slice(0, 10);
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

onMounted(async () => {
    try {
        notices.value = await listNotices(limit.value);
    } catch {
        notices.value = [];
    } finally {
        loading.value = false;
    }
});
</script>

<style scoped>
.notice-bar {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    width: 100%;
    /* 字号与颜色继承外层节点（节点上配了 fontSize/backgroundColor/radius） */
    color: var(--site-text-primary, #1f2329);
}

.notice-bar__label {
    flex: none;
    padding: 1px 8px;
    border-radius: 4px;
    background-color: var(--site-primary, #3b82f6);
    color: #fff;
    font-size: 12px;
    line-height: 1.7;
}

.notice-bar__list {
    flex: 1;
    min-width: 0;
    margin: 0;
    padding: 0;
    list-style: none;
}

.notice-bar__item {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    padding: 3px 0;
    border-radius: var(--site-radius-sm);
}

.notice-bar__item--link {
    cursor: pointer;
}

.notice-bar__item--link:hover .notice-bar__title {
    color: var(--site-primary, #3b82f6);
}

.notice-bar__more {
    flex: none;
    align-self: center;
    color: var(--site-primary, #3b82f6);
    font-size: 12px;
    white-space: nowrap;
}

.notice-bar__tag {
    flex: none;
    padding: 0 6px;
    border: 1px solid var(--site-border, #e5e6eb);
    border-radius: 3px;
    color: var(--site-text-secondary, #646a73);
    font-size: 12px;
    line-height: 1.6;
}

.notice-bar__tag--version {
    border-color: var(--site-primary, #3b82f6);
    color: var(--site-primary, #3b82f6);
}

.notice-bar__title {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.notice-bar__date {
    flex: none;
    color: var(--site-text-secondary, #646a73);
    font-size: 12px;
}

/* 窄屏：日期藏起来，把宽度留给公告标题（标题本身是省略号截断，不会溢出） */
@media (max-width: 640px) {
    .notice-bar {
        flex-wrap: wrap;
    }

    .notice-bar__date {
        display: none;
    }
}
</style>
