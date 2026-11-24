<script setup lang="ts">
/** 页面头部：mono kicker + 大标题 + 说明 + 右侧操作（xAI 控制台风格） */
withDefaults(
  defineProps<{
    kicker?: string
    title: string
    description?: string
  }>(),
  { kicker: '', description: '' },
)
</script>

<template>
  <header class="page-header">
    <div class="page-header__left">
      <p v-if="kicker" class="page-header__kicker x-kicker">{{ kicker }}</p>
      <h1 class="page-header__title x-display-sm">{{ title }}</h1>
      <p v-if="description" class="page-header__desc">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 26px;
  margin-bottom: 30px;
  border-bottom: 1px solid var(--x-border);
}

.page-header__left {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.page-header__title {
  margin: 0;
}

.page-header__desc {
  max-width: 760px;
  color: var(--x-muted);
  font-size: 13px;
  line-height: 1.75;
}

.page-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
  padding-bottom: 2px;
}

@media (max-width: 760px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .page-header__actions {
    justify-content: flex-start;
  }
}
</style>
