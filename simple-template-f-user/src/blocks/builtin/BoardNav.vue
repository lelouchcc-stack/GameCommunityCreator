<template>
    <div class="board-nav">
        <p v-if="loading" class="ui-state ui-state--inline">板块加载中…</p>
        <p v-else-if="!shown.length" class="ui-state ui-state--inline">{{ emptyText }}</p>
        <div v-else class="board-nav__grid" :style="gridStyle">
            <div
                v-for="board in shown"
                :key="board.id"
                class="board-nav__card"
                :class="{ 'board-nav__card--link': !interactive }"
                @click="onCardClick(board, $event)"
            >
                <img
                    v-if="showCover && coverOf(board)"
                    class="board-nav__cover"
                    :src="coverOf(board)"
                    :alt="board.name"
                />
                <div class="board-nav__body">
                    <div class="board-nav__name">{{ board.name }}</div>
                    <div v-if="showIntro && board.intro" class="board-nav__intro">{{ board.intro }}</div>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts" setup>
/**
 * 板块导航（业务组件 builtin:board_nav，管理端与用户端同源副本）。
 * 数据直接取 /app/board/list（开放板块）；管理端画布用它做所见即所得预览，
 * 用户端点击卡片进入板块详情。卡片本身的显隐/样式仍由外层节点负责。
 */
import { computed, inject, onMounted, ref } from "vue";
import type { UiNode } from "../../types/uiBuilder";
import { listBoards, type BoardItem } from "../../api/board";
import { resolveAssetUrl } from "../../utils/asset";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";
import { GRAPH_RUNTIME_KEY, type GraphBridge } from "../graphBridge";
import { activateNode } from "../nodeActivation";

const props = defineProps<{
    node: UiNode
    /** 管理端画布为 true：卡片不跳转，点击冒泡给外层选中该节点 */
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());
/** 图执行器（用户端才注入；管理端画布没有，退化成"只看点击动作与默认行为"） */
const graphBridge = inject<GraphBridge | null>(GRAPH_RUNTIME_KEY, null);

const boards = ref<BoardItem[]>([]);
const loading = ref(true);

const content = computed(() => props.node.content ?? {});
const limit = computed(() => {
    const n = Number(content.value.limit);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : 8;
});
const columns = computed(() => {
    const n = Number(content.value.columns);
    return Number.isFinite(n) && n >= 1 && n <= 4 ? Math.floor(n) : 3;
});
const showCover = computed(() => content.value.showCover !== false);
const showIntro = computed(() => content.value.showIntro !== false);
const emptyText = computed(() => String(content.value.emptyText ?? "暂无开放板块"));

const shown = computed(() => boards.value.slice(0, limit.value));
const gridStyle = computed(() => ({ gridTemplateColumns: `repeat(${columns.value}, minmax(0, 1fr))` }));

function coverOf(board: BoardItem): string {
    return resolveAssetUrl(board.cover);
}

function onCardClick(board: BoardItem, event: MouseEvent) {
    // 点击归节点处理：节点上有逻辑图/点击动作时它们优先，否则才用"进这个板块"的默认行为。
    // 不再无条件 stopPropagation —— 那会让挂在节点上的逻辑收不到事件。
    const handled = activateNode({
        node: props.node,
        runtime,
        graphBridge,
        interactive: props.interactive,
        defaultAction: { action: "navigate", url: `/board/${board.id}` },
    });
    if (handled) {
        event.stopPropagation();
    }
}

onMounted(async () => {
    try {
        boards.value = await listBoards();
    } catch {
        boards.value = [];
    } finally {
        loading.value = false;
    }
});
</script>

<style scoped>
.board-nav {
    width: 100%;
}

.board-nav__grid {
    display: grid;
    gap: 12px;
}

.board-nav__card {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
    padding: 12px;
    border: 1px solid var(--site-border, #e5e6eb);
    border-radius: var(--site-radius-md);
    background-color: var(--site-bg-secondary, #f7f8fa);
    transition: border-color 0.15s, box-shadow 0.15s;
}

.board-nav__card--link {
    cursor: pointer;
}

.board-nav__card--link:hover {
    border-color: var(--site-primary, #3b82f6);
    box-shadow: 0 2px 8px rgb(0 0 0 / 8%);
}

.board-nav__cover {
    flex: none;
    width: 56px;
    height: 56px;
    border-radius: 6px;
    object-fit: cover;
}

.board-nav__body {
    min-width: 0;
}

.board-nav__name {
    font-weight: 600;
    color: var(--site-text-primary, #1f2329);
}

.board-nav__intro {
    margin-top: 4px;
    overflow: hidden;
    font-size: 13px;
    color: var(--site-text-secondary, #646a73);
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 窄屏：无论配了几列，卡片都整行落下——三列在手机上每张只剩几十像素，读不了 */
@media (max-width: 640px) {
    .board-nav__grid {
        grid-template-columns: minmax(0, 1fr) !important;
    }
}
</style>
