<template>
    <div class="board-detail">
        <p v-if="loading" class="board-detail__hint">板块加载中…</p>
        <p v-else-if="!detail" class="board-detail__hint">{{ errorText }}</p>

        <template v-else>
            <header class="board-detail__header">
                <img v-if="coverUrl" class="board-detail__cover" :src="coverUrl" :alt="detail.board.name" />
                <div class="board-detail__head-body">
                    <h1 class="board-detail__name">{{ detail.board.name }}</h1>
                    <p v-if="detail.board.intro" class="board-detail__intro">{{ detail.board.intro }}</p>
                    <p class="board-detail__current">
                        当前版本：<strong>{{ detail.board.currentVersionNo || "未设置" }}</strong>
                    </p>
                </div>
            </header>

            <section v-if="detail.listBoardVersion.length" class="board-detail__section">
                <h2 class="board-detail__section-title">版本归档</h2>
                <div class="board-detail__chips">
                    <button
                        v-for="version in detail.listBoardVersion"
                        :key="version.id"
                        class="board-detail__chip"
                        :class="{ 'board-detail__chip--active': version.isCurrent }"
                        @click="pickVersion(version.id)"
                    >
                        {{ version.versionNo }}
                        <span v-if="version.isCurrent" class="board-detail__chip-badge">当前</span>
                    </button>
                </div>
            </section>

            <section v-if="detail.listProgressStage.length" class="board-detail__section">
                <h2 class="board-detail__section-title">按进度阶段</h2>
                <div class="board-detail__chips">
                    <button
                        v-for="stage in detail.listProgressStage"
                        :key="stage.id"
                        class="board-detail__chip"
                        @click="pickStage(stage.id)"
                    >
                        {{ stage.stageName }}
                    </button>
                </div>
            </section>
        </template>
    </div>
</template>

<script lang="ts" setup>
/**
 * 板块详情（业务组件 builtin:board_detail，管理端与用户端同源副本）。
 * 数据取 /app/board/{id}（板块 + 启用中的版本 + 阶段）；点击版本/阶段把 ?versionId/?stageId
 * 写进地址，同页的 post_list 会据此筛选，实现"版本归档 / 按进度看攻略"。
 * 各区域样式由外层节点的 --w-* CSS 变量驱动（标题/元信息/正文/封面/角标/动作）。
 */
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type { UiNode } from "../../types/uiBuilder";
import { getBoardDetail, type BoardDetail } from "../../api/content";
import { resolveAssetUrl } from "../../utils/asset";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";

const props = defineProps<{
    node: UiNode
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());
const route = useRoute();
const content = computed(() => props.node.content ?? {});

const detail = ref<BoardDetail | null>(null);
const loading = ref(true);
const errorText = ref("该板块暂未开放");

const boardId = computed<number | undefined>(() => {
    const raw = content.value.boardId ?? route.params.id ?? route.query.boardId;
    if (raw === undefined || raw === null || raw === "") {
        return undefined;
    }
    const n = Number(raw);
    return Number.isFinite(n) ? n : undefined;
});

const coverUrl = computed(() => resolveAssetUrl(detail.value?.board.cover ?? null));

async function load() {
    if (boardId.value === undefined) {
        loading.value = false;
        detail.value = null;
        errorText.value = "未指定板块";
        return;
    }
    loading.value = true;
    try {
        detail.value = await getBoardDetail(boardId.value);
    } catch {
        detail.value = null;
        errorText.value = "该板块暂未开放";
    } finally {
        loading.value = false;
    }
}

/** 点版本/阶段：保留当前板块路径，覆盖对应查询参数后跳转，供 post_list 读取筛选 */
function applyQuery(patch: Record<string, number | undefined>) {
    if (props.interactive || boardId.value === undefined) {
        return;
    }
    const query = new URLSearchParams();
    const merged = {
        versionId: num(route.query.versionId),
        stageId: num(route.query.stageId),
        ...patch,
    };
    for (const [key, value] of Object.entries(merged)) {
        if (value !== undefined && value !== null) {
            query.set(key, String(value));
        }
    }
    const suffix = query.toString();
    runtime.run({ action: "navigate", url: `/board/${boardId.value}${suffix ? `?${suffix}` : ""}` });
}

function pickVersion(versionId: number) {
    applyQuery({ versionId });
}

function pickStage(stageId: number) {
    applyQuery({ stageId });
}

function num(value: unknown): number | undefined {
    if (value === undefined || value === null || value === "") {
        return undefined;
    }
    const n = Number(value);
    return Number.isFinite(n) ? n : undefined;
}

watch(boardId, load);
onMounted(load);
</script>

<style scoped>
.board-detail {
    width: 100%;
}

.board-detail__hint {
    margin: 0;
    color: var(--w-empty-color, var(--site-text-secondary, #646a73));
    font-size: var(--w-empty-font-size, 14px);
}

.board-detail__header {
    display: flex;
    gap: 16px;
    align-items: center;
}

.board-detail__cover {
    flex: none;
    width: var(--w-cover-width, 96px);
    height: var(--w-cover-height, 96px);
    border-radius: var(--w-cover-radius, 12px);
    object-fit: var(--w-cover-fit, cover);
}

.board-detail__name {
    margin: 0;
    font-size: var(--w-title-font-size, 22px);
    font-weight: var(--w-title-font-weight, 700);
    color: var(--w-title-color, var(--site-text-primary, #1f2329));
}

.board-detail__intro {
    margin: 6px 0 0;
    color: var(--w-body-color, var(--site-text-secondary, #646a73));
    font-size: var(--w-body-font-size, 14px);
    line-height: var(--w-body-line-height, 1.6);
}

.board-detail__current {
    margin: 6px 0 0;
    color: var(--w-meta-color, var(--site-text-secondary, #646a73));
    font-size: var(--w-meta-font-size, 13px);
}

.board-detail__section {
    margin-top: 16px;
}

.board-detail__section-title {
    margin: 0 0 8px;
    font-size: var(--w-meta-font-size, 14px);
    font-weight: 600;
    color: var(--site-text-primary, #1f2329);
}

.board-detail__chips {
    display: flex;
    flex-wrap: wrap;
    gap: var(--w-item-gap, 8px);
}

.board-detail__chip {
    padding: 4px 12px;
    border: 1px solid var(--site-border, #e5e6eb);
    border-radius: var(--w-action-radius, 999px);
    background: transparent;
    color: var(--w-action-color, var(--site-text-primary, #1f2329));
    font-size: var(--w-action-font-size, 13px);
    cursor: pointer;
}

.board-detail__chip--active {
    border-color: var(--site-primary, #3b82f6);
    color: var(--site-primary, #3b82f6);
}

.board-detail__chip-badge {
    margin-left: 6px;
    padding: 0 5px;
    border-radius: 3px;
    background-color: var(--w-badge-background-color, #e8f3ff);
    color: var(--w-badge-text-color, #3b82f6);
    font-size: 11px;
}
</style>
