<template>
    <div class="board-detail">
        <p v-if="loading" class="ui-state ui-state--inline">板块加载中…</p>
        <p v-else-if="!detail" class="ui-state ui-state--inline">{{ errorText }}</p>

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

            <section v-if="showVersions && detail.listBoardVersion.length" class="board-detail__section">
                <h2 class="board-detail__section-title">版本归档</h2>
                <div class="board-detail__chips">
                    <button
                        v-for="version in detail.listBoardVersion"
                        :key="version.id"
                        class="board-detail__chip"
                        :class="{
                            'board-detail__chip--active': version.isCurrent
                                ? !activeVersionId
                                : activeVersionId === version.id,
                        }"
                        @click="pickVersion(version)"
                    >
                        {{ version.versionNo }}
                        <span v-if="version.isCurrent" class="board-detail__chip-badge">当前</span>
                    </button>
                </div>
            </section>

            <section v-if="showStages && detail.listProgressStage.length" class="board-detail__section">
                <h2 class="board-detail__section-title">按进度阶段</h2>
                <div class="board-detail__chips">
                    <button
                        v-for="stage in detail.listProgressStage"
                        :key="stage.id"
                        class="board-detail__chip"
                        :class="{ 'board-detail__chip--active': activeStageId === stage.id }"
                        @click="pickStage(stage)"
                    >
                        {{ stage.stageName }}
                    </button>
                </div>
            </section>

            <section v-if="showTags && tags.length" class="board-detail__section">
                <h2 class="board-detail__section-title">内容分类</h2>
                <div class="board-detail__chips">
                    <button
                        v-for="tag in tags"
                        :key="tag.id"
                        class="board-detail__chip"
                        :class="{ 'board-detail__chip--active': activeTagId === tag.id }"
                        @click="pickTag(tag)"
                    >
                        {{ tag.tagName }}
                    </button>
                </div>
            </section>

            <p v-if="filtered" class="board-detail__filtered">
                已按筛选条件浏览，<a href="#" @click.prevent="clearFilters">清除筛选</a>
            </p>
        </template>
    </div>
</template>

<script lang="ts" setup>
/**
 * 板块详情（业务组件 builtin:board_detail，管理端与用户端同源副本）。
 * 数据取 /app/board/{id}（板块 + 启用中的版本 + 阶段）；分类标签取 /app/tag/list。
 * 点击版本/阶段/分类把 ?versionId/?stageId/?tagId 写进地址，同页的 post_list 会据此筛选，
 * 实现"版本归档 / 按进度看攻略 / 按分类找内容"——这就是攻略与资料页的内容入口。
 *
 * autoBoard（默认开）：没配 boardId 且地址里也没有板块 id 时，自动取第一个开放板块。
 * 于是模板里的攻略页不需要写死板块 id，新部署到哪个游戏都成立。
 */
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type { UiNode } from "../../types/uiBuilder";
import { getBoardDetail, type BoardDetail, type BoardVersionItem, type ProgressStageItem } from "../../api/content";
import { listBoards } from "../../api/board";
import { listTags, type TagItem } from "../../api/tag";
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
const tags = ref<TagItem[]>([]);
const loading = ref(true);
const errorText = ref("该板块暂未开放");

const showVersions = computed(() => content.value.showVersions !== false);
const showStages = computed(() => content.value.showStages !== false);
const showTags = computed(() => content.value.showTags !== false);
const autoBoard = computed(() => content.value.autoBoard !== false);

/** 当前生效的板块 id：内容属性 > 路由参数/查询串（autoBoard 时兜底第一个开放板块） */
const boardId = ref<number | undefined>(undefined);

const activeVersionId = computed(() => num(route.query.versionId));
const activeStageId = computed(() => num(route.query.stageId));
const activeTagId = computed(() => num(route.query.tagId));
const filtered = computed(
    () =>
        activeVersionId.value !== undefined ||
        activeStageId.value !== undefined ||
        activeTagId.value !== undefined,
);

const coverUrl = computed(() => resolveAssetUrl(detail.value?.board.cover ?? null));

function num(value: unknown): number | undefined {
    if (value === undefined || value === null || value === "") {
        return undefined;
    }
    const n = Number(value);
    return Number.isFinite(n) ? n : undefined;
}

async function resolveBoardId(): Promise<number | undefined> {
    const explicit =
        num(content.value.boardId) ?? num(route.params.id) ?? num(route.query.boardId);
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
        const id = await resolveBoardId();
        boardId.value = id;
        if (id === undefined) {
            detail.value = null;
            tags.value = [];
            errorText.value = "未指定板块";
            return;
        }
        const [boardData, tagData] = await Promise.all([
            getBoardDetail(id),
            listTags(id).catch(() => [] as TagItem[]),
        ]);
        detail.value = boardData;
        tags.value = tagData;
    } catch {
        detail.value = null;
        tags.value = [];
        errorText.value = "该板块暂未开放";
    } finally {
        loading.value = false;
    }
}

/** 点版本/阶段/分类：保留当前板块路径，覆盖对应查询参数后跳转，供 post_list 读取筛选 */
function applyQuery(patch: Record<string, number | undefined>) {
    if (props.interactive || boardId.value === undefined) {
        return;
    }
    const query = new URLSearchParams();
    const merged = {
        versionId: activeVersionId.value,
        stageId: activeStageId.value,
        tagId: activeTagId.value,
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

/** 点版本：再点一次同一个版本即取消筛选（不筛选＝看全部版本，含通用帖） */
function pickVersion(version: BoardVersionItem) {
    applyQuery({ versionId: activeVersionId.value === version.id ? undefined : version.id });
}

function pickStage(stage: ProgressStageItem) {
    applyQuery({ stageId: activeStageId.value === stage.id ? undefined : stage.id });
}

function pickTag(tag: TagItem) {
    applyQuery({ tagId: activeTagId.value === tag.id ? undefined : tag.id });
}

function clearFilters() {
    applyQuery({ versionId: undefined, stageId: undefined, tagId: undefined });
}

watch(() => [content.value.boardId, route.params.id, route.query.boardId], load);
onMounted(load);
</script>

<style scoped>
.board-detail {
    width: 100%;
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

.board-detail__head-body {
    min-width: 0;
}

.board-detail__name {
    margin: 0;
    color: var(--w-title-color, var(--site-text-primary));
    font-size: var(--w-title-font-size, 22px);
    font-weight: var(--w-title-font-weight, 700);
    word-break: break-word;
}

.board-detail__intro {
    margin: 6px 0 0;
    color: var(--w-body-color, var(--site-text-secondary));
    font-size: var(--w-body-font-size, 14px);
    line-height: var(--w-body-line-height, 1.6);
}

.board-detail__current {
    margin: 6px 0 0;
    color: var(--w-meta-color, var(--site-text-muted));
    font-size: var(--w-meta-font-size, 13px);
}

.board-detail__section {
    margin-top: 16px;
}

.board-detail__section-title {
    margin: 0 0 8px;
    color: var(--site-text-primary);
    font-size: var(--w-meta-font-size, 14px);
    font-weight: 600;
}

.board-detail__chips {
    display: flex;
    flex-wrap: wrap;
    gap: var(--w-item-gap, 8px);
}

.board-detail__chip {
    padding: 4px 12px;
    border: 1px solid var(--site-border);
    border-radius: var(--w-action-radius, var(--site-radius-pill));
    background: transparent;
    color: var(--w-action-color, var(--site-text-secondary));
    font-size: var(--w-action-font-size, 13px);
    cursor: pointer;
    transition: border-color 0.15s, color 0.15s, background-color 0.15s;
}

.board-detail__chip:hover {
    border-color: var(--site-primary);
    color: var(--site-primary);
}

.board-detail__chip--active {
    border-color: var(--site-primary);
    background-color: var(--site-primary-weak);
    color: var(--site-primary);
}

.board-detail__chip-badge {
    margin-left: 6px;
    padding: 0 5px;
    border-radius: 3px;
    background-color: var(--w-badge-background-color, var(--site-primary-weak));
    color: var(--w-badge-text-color, var(--site-primary));
    font-size: 11px;
}

.board-detail__filtered {
    margin: 14px 0 0;
    color: var(--site-text-muted);
    font-size: 12px;
}

.board-detail__filtered a {
    color: var(--site-primary);
}

@media (max-width: 640px) {
    .board-detail__header {
        align-items: flex-start;
    }

    .board-detail__cover {
        width: 64px;
        height: 64px;
    }

    .board-detail__name {
        font-size: 18px;
    }
}
</style>
