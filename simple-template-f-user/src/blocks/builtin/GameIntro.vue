<template>
    <section class="game-intro">
        <p class="game-intro__kicker">{{ kicker }}</p>
        <h2 class="game-intro__name">{{ gameName }}</h2>
        <p v-if="introText" class="game-intro__desc">{{ introText }}</p>

        <div v-if="chips.length" class="game-intro__chips">
            <span v-for="chip in chips" :key="chip" class="game-intro__chip">{{ chip }}</span>
        </div>
    </section>
</template>

<script lang="ts" setup>
/**
 * 游戏简介（业务组件 builtin:game_intro）。
 *
 * 内容全部来自站点配置与板块数据，**组件里不写死任何游戏名**：
 *   - 游戏名 = 站点配置 game_name（可在节点内容属性里用「标题」覆盖）
 *   - 简介   = 节点「简介文案」优先，其次站点简介，再次主板块简介
 *   - 当前版本 = 第一个开放板块的 current_version_id 对应的版本号
 * 于是「创建社区时填了游戏名」这条链一路通到这里，改配置整页跟着变。
 *
 * 右侧的分栏与按钮由模板里的兄弟节点负责，本组件只管"这是什么游戏"。
 */
import { computed, onMounted, ref } from "vue";
import type { UiNode } from "../../types/uiBuilder";
import { listBoards, type BoardItem } from "../../api/board";
import useSiteStore from "../../store/siteStore";

const props = defineProps<{
    node: UiNode
    /** 管理端画布为 true：不做事，仅用于将来做差异化 */
    interactive?: boolean
}>();

const siteStore = useSiteStore();
const boards = ref<BoardItem[]>([]);

const content = computed(() => props.node.content ?? {});
const showVersion = computed(() => content.value.showVersion !== false);

const siteName = computed(() => siteStore.theme?.siteName || "游戏社区");

/** 主板块：默认第一个开放板块（板块排序即展示顺序） */
const primaryBoard = computed<BoardItem | null>(() => boards.value[0] ?? null);

const gameName = computed(() => {
    const override = String(content.value.title ?? "").trim();
    if (override) {
        return override;
    }
    return siteStore.theme?.gameName?.trim() || primaryBoard.value?.name || siteName.value;
});

const kicker = computed(() => `${siteName.value} · 游戏简介`);

const introText = computed(() => {
    const override = String(content.value.intro ?? "").trim();
    if (override) {
        return override;
    }
    return siteStore.theme?.siteIntro?.trim() || primaryBoard.value?.intro || "";
});

/** 简介下方的信息小标签：当前版本、所属板块等 */
const chips = computed(() => {
    const list: string[] = [];
    const version = primaryBoard.value?.currentVersionNo;
    if (showVersion.value && version) {
        list.push(`当前版本 ${version}`);
    }
    if (primaryBoard.value && primaryBoard.value.name !== gameName.value) {
        list.push(primaryBoard.value.name);
    }
    return list;
});

onMounted(async () => {
    try {
        boards.value = await listBoards();
    } catch {
        boards.value = [];
    }
});
</script>

<style scoped>
.game-intro {
    width: 100%;
}

.game-intro__kicker {
    margin: 0;
    color: var(--w-meta-color, var(--site-primary));
    font-size: var(--w-meta-font-size, 13px);
    font-weight: 600;
    letter-spacing: 0.4px;
}

.game-intro__name {
    margin: 8px 0 0;
    color: var(--w-title-color, var(--site-text-primary));
    font-size: var(--w-title-font-size, 28px);
    font-weight: var(--w-title-font-weight, 700);
    line-height: 1.3;
}

.game-intro__desc {
    max-width: 720px;
    margin: 10px 0 0;
    color: var(--w-body-color, var(--site-text-secondary));
    font-size: var(--w-body-font-size, 14px);
    line-height: var(--w-body-line-height, 1.75);
    white-space: pre-wrap;
}

.game-intro__chips {
    display: flex;
    flex-wrap: wrap;
    gap: var(--w-item-gap, 8px);
    margin-top: 14px;
}

.game-intro__chip {
    padding: 3px 10px;
    border-radius: var(--w-action-radius, var(--site-radius-pill));
    background-color: var(--w-badge-background-color, var(--site-primary-weak));
    color: var(--w-badge-text-color, var(--site-primary));
    font-size: var(--w-meta-font-size, 12px);
    line-height: 1.7;
}
</style>
