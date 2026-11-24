<script setup lang="ts">
/**
 * 游戏门户广场（/community）：列出全部「已发布」游戏站点（多页面门户）。
 *
 * 公开只读：GET /api/public/communities（docs/API.md §6.1）。
 * loading / error / empty 三态齐全，卡片进入 /community/{slug}。
 *
 * 视觉：xAI 黑色聚合入口 —— mono kicker + 大号 display 标题 + hairline 门户卡片。
 * （该页不属于某个站点，无门户主题，直接使用设计系统令牌。）
 */
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listPublicCommunities } from '@/api/public'
import PublicStateBlock from '@/components/public/PublicStateBlock.vue'
import XIcon from '@/components/admin/XIcon.vue'
import { toErrorMessage } from '@/runtime/errors'
import type { PublicCommunitySummary } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const communities = ref<PublicCommunitySummary[]>([])
const loading = ref(true)
const errorMessage = ref('')

async function loadCommunities(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const list = await listPublicCommunities()
    communities.value = Array.isArray(list) ? list : []
  } catch (error) {
    communities.value = []
    errorMessage.value = toErrorMessage(error, '游戏站点列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function isHttpUrl(value: string | null | undefined): boolean {
  return typeof value === 'string' && /^https?:\/\//i.test(value.trim())
}

function coverStyle(community: PublicCommunitySummary): Record<string, string> {
  const cover = community.coverUrl
  if (isHttpUrl(cover)) {
    return {
      backgroundImage: `linear-gradient(180deg, rgba(6, 6, 8, 0.12), rgba(6, 6, 8, 0.72)), url("${cover.trim()}")`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
    }
  }
  return {}
}

function initialOf(name: string | null | undefined): string {
  const trimmed = (name ?? '').trim()
  return trimmed ? trimmed.slice(0, 1).toUpperCase() : 'G'
}

onMounted(() => {
  document.title = '游戏门户广场 · GameCommunityCreator'
  void loadCommunities()
})
</script>

<template>
  <div class="gallery">
    <header class="gallery-header">
      <div class="gallery-header__inner gcc-container">
        <RouterLink class="gallery-brand" to="/community">
          <span class="gallery-brand__mark" aria-hidden="true">GC</span>
          <span class="gallery-brand__name">GAME COMMUNITY CREATOR</span>
        </RouterLink>
        <RouterLink class="gallery-header__action" to="/admin/projects">
          管理端
          <XIcon name="arrow-up-right" :size="12" />
        </RouterLink>
      </div>
    </header>

    <main class="gallery-main">
      <div class="gcc-container">
        <section class="gallery-hero">
          <p class="gallery-kicker">DISCOVER · OFFICIAL GAME PORTALS</p>
          <h1 class="gallery-title">游戏门户广场</h1>
          <p class="gallery-lede">
            由独立游戏创作者在可视化编辑器中搭建的官方网站：首页、更新日志、公开文档、制作团队、友情链接与问题反馈，全部配置化。
          </p>
          <p class="gallery-meta">
            PUBLISHED PORTALS
            <span class="gallery-meta__count">{{ communities.length }}</span>
          </p>
        </section>

        <section class="gallery-content">
          <div v-if="loading" class="gallery-state">
            <PublicStateBlock variant="loading" kicker="LOADING" title="正在加载游戏站点列表…" />
          </div>

          <div v-else-if="errorMessage" class="gallery-state">
            <PublicStateBlock
              variant="error"
              kicker="ERROR"
              title="游戏站点列表加载失败"
              :description="errorMessage"
              @retry="loadCommunities"
            />
          </div>

          <div v-else-if="communities.length === 0" class="gallery-state">
            <PublicStateBlock
              variant="empty"
              kicker="NO PORTALS YET"
              title="暂无已发布游戏站点"
              description="先在管理端创建游戏站点并发布页面，它就会出现在这里。"
            >
              <RouterLink class="gcc-btn gcc-btn-primary" to="/admin/projects">
                去创建游戏站点
              </RouterLink>
            </PublicStateBlock>
          </div>

          <div v-else class="gallery-grid">
            <article v-for="community in communities" :key="community.id" class="gallery-card">
              <RouterLink class="gallery-card__link" :to="`/community/${community.slug}`">
                <div
                  class="gallery-card__cover"
                  :class="{ 'gallery-card__cover--image': isHttpUrl(community.coverUrl) }"
                  :style="coverStyle(community)"
                >
                  <span
                    v-if="!isHttpUrl(community.coverUrl)"
                    class="gallery-card__initial"
                    aria-hidden="true"
                  >
                    {{ initialOf(community.gameName) }}
                  </span>
                  <span class="gallery-card__version">V{{ community.publishedVersion }}</span>
                </div>
                <div class="gallery-card__body">
                  <p class="gallery-card__game">{{ community.gameName }}</p>
                  <h2 class="gallery-card__name">{{ community.name }}</h2>
                  <p class="gallery-card__desc">
                    {{ community.description || '这位创作者还没有填写站点简介。' }}
                  </p>
                  <div class="gallery-card__foot">
                    <time class="gallery-card__date">
                      {{ formatDateTime(community.publishedAt) }}
                    </time>
                    <span class="gallery-card__enter">
                      ENTER
                      <XIcon name="arrow-right" :size="12" />
                    </span>
                  </div>
                </div>
              </RouterLink>
            </article>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.gallery {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--x-canvas);
  color: var(--x-text);
}

/* ---------------- header ---------------- */
.gallery-header {
  position: sticky;
  top: 0;
  z-index: 30;
  border-bottom: 1px solid var(--x-border);
  background: rgba(10, 10, 10, 0.88);
  backdrop-filter: blur(10px);
}

.gallery-header__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  height: 64px;
}

.gallery-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--x-text);
}

.gallery-brand__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 6px;
  border: 1px solid var(--x-border-strong);
  font-family: var(--x-mono);
  font-size: 12px;
}

.gallery-brand__name {
  font-family: var(--x-mono);
  font-size: 10px;
  letter-spacing: 0.18em;
  color: var(--x-text-secondary);
}

.gallery-header__action {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.gallery-header__action:hover {
  color: var(--x-text);
}

/* ---------------- hero ---------------- */
.gallery-main {
  flex: 1;
}

.gallery-hero {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 96px 0 56px;
  border-bottom: 1px solid var(--x-border);
}

.gallery-kicker {
  font-family: var(--x-mono);
  font-size: 10.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.gallery-title {
  color: var(--x-text);
  font-size: clamp(40px, 5.2vw, 72px);
  font-weight: 400;
  line-height: 1.02;
  letter-spacing: -0.035em;
}

.gallery-lede {
  max-width: 720px;
  color: var(--x-muted);
  font-size: 14px;
  line-height: 1.85;
}

.gallery-meta {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  padding: 6px 14px;
  border-radius: 9999px;
  border: 1px solid var(--x-border);
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.gallery-meta__count {
  color: var(--x-text);
}

/* ---------------- content ---------------- */
.gallery-content {
  padding: 40px 0 96px;
}

.gallery-state {
  padding: 24px 0;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.gallery-card {
  display: flex;
}

.gallery-card__link {
  display: flex;
  flex-direction: column;
  width: 100%;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
  overflow: hidden;
  color: var(--x-text);
  transition: border-color 0.15s ease;
}

.gallery-card__link:hover {
  border-color: var(--x-border-strong);
}

.gallery-card__cover {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 16 / 9;
  border-bottom: 1px solid var(--x-border);
  background:
    radial-gradient(420px 200px at 18% 0%, rgba(255, 122, 23, 0.16), transparent 62%),
    linear-gradient(160deg, #17181b, #0d0e10);
}

.gallery-card__cover--image {
  background-color: #060608;
}

.gallery-card__initial {
  font-size: 46px;
  font-weight: 400;
  letter-spacing: -0.03em;
  color: rgba(255, 255, 255, 0.86);
}

.gallery-card__version {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 3px 9px;
  border-radius: 9999px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(10, 10, 10, 0.55);
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.1em;
  color: rgba(255, 255, 255, 0.84);
}

.gallery-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  padding: 20px;
}

.gallery-card__game {
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.gallery-card__name {
  color: var(--x-text);
  font-size: 18px;
  font-weight: 500;
  line-height: 1.35;
  letter-spacing: -0.01em;
}

.gallery-card__desc {
  color: var(--x-muted);
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  min-height: 66px;
}

.gallery-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--x-border);
}

.gallery-card__date {
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--x-muted);
}

.gallery-card__enter {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-family: var(--x-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  color: var(--x-text-secondary);
  transition: color 0.15s ease;
}

.gallery-card__link:hover .gallery-card__enter {
  color: var(--x-accent);
}

/* ---------------- responsive ---------------- */
@media (max-width: 1080px) {
  .gallery-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .gallery-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .gallery-hero {
    padding: 64px 0 40px;
  }

  .gallery-brand__name {
    display: none;
  }
}
</style>
