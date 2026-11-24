<script setup lang="ts">
/**
 * 模板中心（/admin/templates）—— TEMPLATES 页面。
 *
 * 展示后端真实存在的页面模板（`templates/pages/{key}.json`），数据来自
 * `constants/pageTemplates.ts`，与总览「推荐模板」共用同一份目录。
 */
import { RouterLink } from 'vue-router'
import PageHeader from '@/components/admin/PageHeader.vue'
import XIcon from '@/components/admin/XIcon.vue'
import { PAGE_TEMPLATES } from '@/constants/pageTemplates'
</script>

<template>
  <div class="templates-page x-page gcc-container">
    <PageHeader kicker="Templates" title="模板中心">
      <template #actions>
        <RouterLink class="gcc-btn gcc-btn-primary" :to="{ name: 'project-create' }">
          <XIcon name="plus" :size="14" />
          新建项目
        </RouterLink>
      </template>
    </PageHeader>

    <div class="templates-summary x-mono">
      <span>{{ String(PAGE_TEMPLATES.length).padStart(2, '0') }} PAGE TEMPLATES</span>
      <span class="templates-summary__sep">/</span>
      <span>JSON DRIVEN</span>
    </div>

    <div class="templates-grid">
      <article
        v-for="template in PAGE_TEMPLATES"
        :key="template.key"
        class="template-card"
        :class="`accent-${template.accent}`"
      >
        <div class="template-card__art" aria-hidden="true">
          <div class="template-card__art-grid"></div>
          <span class="template-card__art-mark x-mono">{{ template.mono }}</span>
          <span class="template-card__art-letter">{{ template.name.slice(0, 1) }}</span>
        </div>
        <div class="template-card__body">
          <p class="template-card__mono x-mono">{{ template.key }}</p>
          <h2 class="template-card__name">{{ template.name }}</h2>
          <p class="template-card__desc">{{ template.description }}</p>
          <ul class="template-card__chips" aria-label="包含组件">
            <li v-for="chip in template.components" :key="chip" class="x-mono">
              {{ chip }}
            </li>
          </ul>
          <div class="template-card__foot">
            <span class="template-card__pages x-mono">
              {{ template.components.length }} COMPONENTS
            </span>
            <span v-if="template.recommended" class="gcc-badge">推荐</span>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.templates-page {
  padding-top: 36px;
}

.templates-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
  font-size: 9.5px;
  letter-spacing: 0.14em;
  color: var(--x-muted);
  text-transform: uppercase;
}

.templates-summary__sep {
  opacity: 0.4;
}

.templates-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.template-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
  transition: border-color 0.15s ease;
}

.template-card:hover {
  border-color: var(--x-border-strong);
}

.template-card.accent-orange {
  --tpl-a: rgba(255, 122, 23, 0.14);
}

.template-card.accent-violet {
  --tpl-a: rgba(196, 181, 253, 0.12);
}

.template-card.accent-blue {
  --tpl-a: rgba(160, 195, 236, 0.12);
}

.template-card.accent-green {
  --tpl-a: rgba(61, 220, 151, 0.1);
}

.template-card__art {
  position: relative;
  height: 132px;
  overflow: hidden;
  border-bottom: 1px solid var(--x-border);
  background:
    radial-gradient(120% 150% at 15% 0%, var(--tpl-a), transparent 62%),
    #0c0c0e;
}

.template-card__art-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 30px 30px;
  mask-image: linear-gradient(170deg, rgba(0, 0, 0, 0.9), transparent 88%);
}

.template-card__art-mark {
  position: absolute;
  left: 16px;
  top: 14px;
  font-size: 9px;
  letter-spacing: 0.16em;
  color: rgba(255, 255, 255, 0.6);
}

.template-card__art-letter {
  position: absolute;
  right: 18px;
  bottom: 2px;
  font-family: var(--x-mono);
  font-size: 58px;
  line-height: 1;
  color: rgba(255, 255, 255, 0.08);
}

.template-card__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
}

.template-card__mono {
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--x-muted);
  text-transform: uppercase;
}

.template-card__name {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: -0.01em;
}

.template-card__desc {
  min-height: 62px;
  color: var(--x-muted);
  font-size: 12px;
  line-height: 1.65;
}

.template-card__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.template-card__chips li {
  padding: 3px 7px;
  border: 1px solid var(--x-border);
  border-radius: 4px;
  font-size: 8.5px;
  letter-spacing: 0.08em;
  color: var(--x-muted);
  text-transform: uppercase;
}

.template-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--x-border);
}

.template-card__pages {
  font-size: 9px;
  letter-spacing: 0.12em;
  color: var(--x-muted);
}

@media (max-width: 1180px) {
  .templates-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .templates-grid {
    grid-template-columns: 1fr;
  }
}
</style>
