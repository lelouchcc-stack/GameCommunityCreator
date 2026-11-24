<script setup lang="ts">
/** 管理端页面共用的顶部品牌栏（项目列表 / 创建社区），右侧操作按钮通过 slot 传入。 */
import { RouterLink } from 'vue-router'
</script>

<template>
  <header class="brand-header">
    <div class="gcc-container brand-inner">
      <RouterLink to="/admin/projects" class="brand">
        <span class="brand-mark" aria-hidden="true">G</span>
        <span class="brand-text">
          <span class="brand-name">GameCommunityCreator</span>
          <span class="brand-sub">面向独立游戏开发者的社区快速搭建系统</span>
        </span>
      </RouterLink>
      <nav class="brand-nav">
        <slot name="actions" />
      </nav>
    </div>
  </header>
</template>

<style scoped>
.brand-header {
  position: sticky;
  top: 0;
  z-index: 20;
  border-bottom: 1px solid var(--gcc-border);
  background: var(--gcc-header-bg);
  backdrop-filter: blur(12px);
}

.brand-inner {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  color: var(--gcc-text);
}

.brand:hover {
  color: var(--gcc-text);
}

.brand-mark {
  flex: none;
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--gcc-primary), var(--gcc-accent));
  color: #f7f9ff;
  font-size: 19px;
  font-weight: 700;
  box-shadow: 0 8px 20px rgba(91, 140, 255, 0.32);
}

.brand-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.01em;
  line-height: 1.2;
}

.brand-sub {
  font-size: 11px;
  color: var(--gcc-muted);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-nav {
  flex: none;
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 720px) {
  .brand-sub {
    display: none;
  }
}
</style>
