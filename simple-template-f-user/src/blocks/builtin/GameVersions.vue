<template>
    <section class="game-versions">
        <p v-if="loading" class="ui-state ui-state--inline">版本信息加载中…</p>
        <p v-else-if="!rows.length" class="ui-state ui-state--inline">{{ emptyText }}</p>

        <ol v-else class="game-versions__list">
            <li v-for="version in rows" :key="version.id" class="game-versions__item">
                <div class="game-versions__line" aria-hidden="true">
                    <span class="game-versions__dot" :class="{ 'game-versions__dot--current': version.isCurrent }"></span>
                </div>

                <div class="game-versions__body">
                    <div class="game-versions__head">
                        <span class="game-versions__no">{{ version.versionNo }}</span>
                        <span v-if="version.versionName" class="game-versions__name">{{ version.versionName }}</span>
                        <span v-if="version.isCurrent" class="game-versions__badge">当前版本</span>
                    </div>
                    <div v-if="dateOf(version.releaseTime)" class="game-versions__date">
                        {{ dateOf(version.releaseTime) }} 发布
                    </div>
                    <p v-if="showNote && version.updateNote" class="game-versions__note">{{ version.updateNote }}</p>
                </div>
            </li>
        </ol>
    </section>
</template>

<script lang="ts" setup>
/**
 * 游戏版本 / 更新记录（业务组件 builtin:game_versions）。
 *
 * 数据取 /app/board/{id} 的 listBoardVersion（按发布时间倒序由后端保证），
 * 渲染成时间线：版本号 + 版本名 + 当前版本角标 + 更新说明。
 *
 * 「哪个板块」优先取节点内容属性 boardId；没配且 autoBoard 为真（默认）时
 * 自动用第一个开放板块——模板因此不需要写死板块 id，「改站名即可运行」才成立。
 */
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type { UiNode } from "../../types/uiBuilder";
import { getBoardDetail, type BoardVersionItem } from "../../api/content";
import { listBoards } from "../../api/board";

const props = defineProps<{
    node: UiNode
    interactive?: boolean
}>();

const route = useRoute();
const content = computed(() => props.node.content ?? {});

const versions = ref<BoardVersionItem[]>([]);
const loading = ref(true);

const limit = computed(() => {
    const n = Number(content.value.limit);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : 5;
});
const showNote = computed(() => content.value.showNote !== false);
const emptyText = computed(() => String(content.value.emptyText ?? "暂无版本信息"));
const autoBoard = computed(() => content.value.autoBoard !== false);

const rows = computed(() => versions.value.slice(0, limit.value));

function dateOf(value: string | null) {
    return value ? value.slice(0, 10) : "";
}

function numOrUndef(value: unknown): number | undefined {
    if (value === undefined || value === null || value === "") {
        return undefined;
    }
    const n = Number(value);
    return Number.isFinite(n) ? n : undefined;
}

/** 板块来源：内容属性 > 路由参数/查询串 > 第一个开放板块 */
async function resolveBoardId(): Promise<number | undefined> {
    const explicit =
        numOrUndef(content.value.boardId) ?? numOrUndef(route.params.id) ?? numOrUndef(route.query.boardId);
    if (explicit !== undefined) {
        return explicit;
    }
    if (!autoBoard.value) {
        return undefined;
    }
    const boards = await listBoards().catch(() => []);
    return boards[0]?.id;
}

async function load() {
    loading.value = true;
    try {
        const boardId = await resolveBoardId();
        if (boardId === undefined) {
            versions.value = [];
            return;
        }
        const detail = await getBoardDetail(boardId);
        versions.value = detail.listBoardVersion ?? [];
    } catch {
        versions.value = [];
    } finally {
        loading.value = false;
    }
}

watch(() => [content.value.boardId, content.value.autoBoard, route.params.id, route.query.boardId], load);
onMounted(load);
</script>

<style scoped>
.game-versions {
    width: 100%;
}

.game-versions__list {
    margin: 0;
    padding: 0;
    list-style: none;
}

.game-versions__item {
    display: flex;
    gap: 12px;
}

.game-versions__line {
    position: relative;
    flex: none;
    width: 12px;
    padding-top: 5px;
}

/* 竖线：最后一项不画，避免时间线拖出多余的尾巴 */
.game-versions__item:not(:last-child) .game-versions__line::after {
    content: "";
    position: absolute;
    top: 16px;
    bottom: -6px;
    left: 50%;
    width: 1px;
    transform: translateX(-50%);
    background-color: var(--w-item-border-color, var(--site-border));
}

.game-versions__dot {
    display: block;
    width: 8px;
    height: 8px;
    margin: 0 auto;
    border: 2px solid var(--site-border-strong, var(--site-border));
    border-radius: 50%;
    background-color: var(--site-bg-card);
    box-sizing: border-box;
}

.game-versions__dot--current {
    border-color: var(--site-primary);
    background-color: var(--site-primary);
}

.game-versions__body {
    flex: 1;
    min-width: 0;
    padding-bottom: var(--w-item-gap, 16px);
}

.game-versions__head {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
}

.game-versions__no {
    color: var(--w-title-color, var(--site-text-primary));
    font-size: var(--w-title-font-size, 15px);
    font-weight: var(--w-title-font-weight, 600);
}

.game-versions__name {
    color: var(--w-body-color, var(--site-text-secondary));
    font-size: var(--w-body-font-size, 14px);
}

.game-versions__badge {
    padding: 1px 8px;
    border-radius: var(--w-action-radius, var(--site-radius-pill));
    background-color: var(--w-badge-background-color, var(--site-primary-weak));
    color: var(--w-badge-text-color, var(--site-primary));
    font-size: 11px;
    line-height: 1.7;
}

.game-versions__date {
    margin-top: 2px;
    color: var(--w-meta-color, var(--site-text-muted));
    font-size: var(--w-meta-font-size, 12px);
}

.game-versions__note {
    margin: 6px 0 0;
    color: var(--w-body-color, var(--site-text-secondary));
    font-size: var(--w-body-font-size, 13px);
    line-height: var(--w-body-line-height, 1.7);
    white-space: pre-wrap;
    word-break: break-word;
}
</style>
