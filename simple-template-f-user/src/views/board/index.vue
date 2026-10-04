<template>
    <div class="board-page">
        <header class="board-page__header">
            <router-link class="board-page__back" to="/">← 返回首页</router-link>
            <h1 class="board-page__title">{{ title }}</h1>
            <p v-if="board?.intro" class="board-page__intro">{{ board.intro }}</p>
        </header>

        <main class="board-page__body">
            <p class="board-page__hint">
                这里将展示「{{ board?.name || `板块 #${boardId}` }}」的帖子列表（板块详情与版本归档，pit.md 模块 03/04 待接入）。
            </p>
        </main>
    </div>
</template>

<script lang="ts" setup>
/**
 * 板块详情占位页：板块导航卡片点击的落点。
 * 真正的帖子列表 / 版本归档属于 pit.md 模块 03/04，尚未开工——这里先把跳转目标立起来，
 * 免得用户端点了卡片落到空白页。
 */
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { listBoards, type BoardItem } from "../../api/board";

const route = useRoute();
const boardId = computed(() => Number(route.params.id));
const boards = ref<BoardItem[]>([]);
const board = computed(() => boards.value.find((item) => item.id === boardId.value) ?? null);
const title = computed(() => board.value?.name || `板块 #${boardId.value}`);

onMounted(async () => {
    try {
        boards.value = await listBoards();
    } catch {
        boards.value = [];
    }
});
</script>

<style scoped>
.board-page {
    min-height: 100vh;
    padding: 24px;
    box-sizing: border-box;
}

.board-page__header {
    max-width: 1080px;
    margin: 0 auto;
}

.board-page__back {
    color: var(--site-text-secondary, #646a73);
    font-size: 14px;
    text-decoration: none;
}

.board-page__title {
    margin: 12px 0 0;
    font-size: 22px;
    color: var(--site-text-primary, #1f2329);
}

.board-page__intro {
    margin: 8px 0 0;
    color: var(--site-text-secondary, #646a73);
    font-size: 14px;
}

.board-page__body {
    max-width: 1080px;
    margin: 24px auto 0;
}

.board-page__hint {
    padding: 24px;
    border: 1px dashed var(--site-border, #e5e6eb);
    border-radius: 8px;
    color: var(--site-text-secondary, #646a73);
    text-align: center;
}
</style>
