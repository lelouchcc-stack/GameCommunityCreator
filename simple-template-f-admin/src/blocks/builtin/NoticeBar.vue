<template>
    <div class="notice-bar">
        <span class="notice-bar__label">公告</span>
        <p v-if="loading" class="notice-bar__hint">公告加载中…</p>
        <p v-else-if="!notices.length" class="notice-bar__hint">暂无公告</p>
        <ul v-else class="notice-bar__list">
            <li v-for="notice in notices" :key="notice.id" class="notice-bar__item">
                <span
                    class="notice-bar__tag"
                    :class="notice.type === 2 ? 'notice-bar__tag--version' : ''"
                >
                    {{ notice.type === 2 ? "版本" : "公告" }}
                </span>
                <span class="notice-bar__title" :title="notice.content || notice.title">
                    {{ notice.title }}
                </span>
                <span v-if="dateOf(notice.createTime)" class="notice-bar__date">
                    {{ dateOf(notice.createTime) }}
                </span>
            </li>
        </ul>
    </div>
</template>

<script lang="ts" setup>
/**
 * 首页公告栏（业务组件 builtin:notice，管理端与用户端同源副本）。
 * 数据取 /app/notice/list（已发布公告），条数由节点内容属性 limit 决定。
 * 目前公告只在首页展示，点标题不做跳转——详情页还没做，不给假的可点感。
 */
import { computed, onMounted, ref } from "vue";
import type { UiNode } from "../../types/uiBuilder";
import { listNotices, type NoticeItem } from "../../api/notice";

const props = defineProps<{
    node: UiNode
    /** 管理端画布为 true；公告栏在编辑态与用户端长得一样，仅用于将来做差异化 */
    interactive?: boolean
}>();

const notices = ref<NoticeItem[]>([]);
const loading = ref(true);

const limit = computed(() => {
    const n = Number(props.node.content?.limit);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : 5;
});

/** createTime 形如 2026-10-05T01:22:23，只取日期用来弱化显示 */
function dateOf(value?: string | null) {
    if (!value) {
        return "";
    }
    return value.slice(0, 10);
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

.notice-bar__hint {
    margin: 0;
    color: var(--site-text-secondary, #646a73);
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
</style>
