<script setup lang="ts">
/**
 * 管理端总览（/admin/overview）—— OVERVIEW 仪表盘。
 * xAI 控制台风格：大号轻字重标题 + 统计分栏 + 我的项目 + 推荐模板。
 * 只使用已有 listProjects API，不引入新业务逻辑。
 *
 * v4.3 修复：ProjectCard 上的「站点发布」「编辑站点信息」「删除站点」在总览页
 * 点击无响应（按钮存在但没功能）——原因是本页只监听了 open/settings/visit，
 * 未监听 publish/edit/delete。现与「我的游戏」页共用 useProjectActions 与
 * ProjectEditDialog，保证两处行为一致。
 */
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listProjects } from '@/api/projects'
import { errorMessageOf } from '@/editor/useEditorState'
import { useProjectActions } from '@/composables/useProjectActions'
import ProjectCard from '@/components/admin/ProjectCard.vue'
import ProjectEditDialog from '@/components/admin/ProjectEditDialog.vue'
import ToastHost from '@/editor/ToastHost.vue'
import XIcon from '@/components/admin/XIcon.vue'
import { RECOMMENDED_PAGE_TEMPLATES } from '@/constants/pageTemplates'
import type { ProjectView } from '@/types/api'

const projects = ref<ProjectView[]>([])
const loading = ref(true)
const error = ref('')

/* 站点发布 / 删除站点：与「我的游戏」页共用同一份实现 */
const { busyId, onPublishSite, onDelete } = useProjectActions({ reload: load })

/* 编辑站点信息弹窗 */
const editTarget = ref<ProjectView | null>(null)

function openEditDialog(project: ProjectView): void {
  editTarget.value = project
}

function closeEditDialog(): void {
  editTarget.value = null
}

async function onEditSaved(): Promise<void> {
  editTarget.value = null
  await load()
}

const publishedCount = computed(
  () => projects.value.filter((project) => project.status === 'PUBLISHED').length,
)
const draftCount = computed(() => projects.value.length - publishedCount.value)
const totalPages = computed(() =>
  projects.value.reduce((sum, project) => sum + (project.pageCount ?? 0), 0),
)
const publishedPages = computed(() =>
  projects.value.reduce((sum, project) => sum + (project.publishedPageCount ?? 0), 0),
)
const recentProjects = computed(() => projects.value.slice(0, 2))

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    projects.value = await listProjects()
  } catch (err) {
    error.value = errorMessageOf(err, '加载总览失败，请检查后端服务是否启动')
  } finally {
    loading.value = false
  }
}

function openPublic(project: ProjectView): void {
  window.open(`/community/${encodeURIComponent(project.slug)}`, '_blank', 'noopener')
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="overview x-page gcc-container">
    <ToastHost />

    <!-- Hero -->
    <section class="overview-hero">
      <div class="overview-hero__copy">
        <p class="x-kicker">Game Community Creator / Build · Configure · Publish</p>
        <h1 class="x-display overview-hero__title">
          BUILD · CONFIGURE<br />
          <span class="overview-hero__title-soft">· PUBLISH</span>
        </h1>
        <div class="overview-hero__actions">
          <RouterLink class="gcc-btn gcc-btn-primary" :to="{ name: 'project-create' }">
            <XIcon name="plus" :size="14" />
            创建项目
          </RouterLink>
          <RouterLink class="gcc-btn" :to="recentProjects[0] ? { name: 'page-list', params: { id: recentProjects[0].id } } : { name: 'project-list' }">
            <XIcon name="arrow-up-right" :size="14" />
            打开最近项目
          </RouterLink>
          <RouterLink class="gcc-btn gcc-btn-ghost" to="/community">
            预览社区广场
          </RouterLink>
        </div>
      </div>
      <div class="overview-hero__art" aria-hidden="true">
        <div class="overview-hero__art-grid"></div>
        <div class="overview-hero__art-orb"></div>
        <div class="overview-hero__art-lines">
          <span>STELLAR DRIFT</span>
          <span>ECHO GARDEN</span>
          <span>BUILD / CONFIGURE / PUBLISH</span>
        </div>
      </div>
    </section>

    <!-- 统计分栏（hairline divider，无彩色 KPI 卡片） -->
    <section class="overview-stats" aria-label="项目统计">
      <div class="overview-stat">
        <p class="overview-stat__label x-mono">Projects</p>
        <p class="overview-stat__value x-mono">{{ loading ? '—' : String(projects.length).padStart(2, '0') }}</p>
      </div>
      <div class="overview-stat">
        <p class="overview-stat__label x-mono">Published</p>
        <p class="overview-stat__value x-mono">{{ loading ? '—' : String(publishedCount).padStart(2, '0') }}</p>
      </div>
      <div class="overview-stat">
        <p class="overview-stat__label x-mono">Drafts</p>
        <p class="overview-stat__value x-mono">{{ loading ? '—' : String(draftCount).padStart(2, '0') }}</p>
      </div>
      <div class="overview-stat">
        <p class="overview-stat__label x-mono">Pages</p>
        <p class="overview-stat__value x-mono">{{ loading ? '—' : `${publishedPages}/${totalPages}` }}</p>
      </div>
    </section>

    <p v-if="error" class="gcc-error overview-error">
      {{ error }}
    </p>

    <!-- 我的项目 -->
    <section class="overview-section">
      <div class="overview-section__head">
        <div>
          <p class="x-kicker">My projects</p>
          <h2 class="overview-section__title">我的项目</h2>
        </div>
        <RouterLink class="overview-section__all x-mono" :to="{ name: 'project-list' }">
          查看全部项目
          <XIcon name="arrow-right" :size="13" />
        </RouterLink>
      </div>

      <div v-if="loading" class="overview-projects">
        <article v-for="index in 2" :key="index" class="overview-project-skeleton">
          <div class="gcc-skeleton" style="height: 148px"></div>
          <div class="overview-project-skeleton__body">
            <div class="gcc-skeleton" style="height: 16px; width: 40%"></div>
            <div class="gcc-skeleton" style="height: 12px; width: 75%"></div>
          </div>
        </article>
      </div>

      <div v-else-if="recentProjects.length === 0" class="overview-empty">
        <p class="x-kicker">No projects yet</p>
        <p class="overview-empty__title">还没有游戏站点</p>
        <RouterLink class="gcc-btn gcc-btn-primary" :to="{ name: 'project-create' }">
          创建项目
        </RouterLink>
      </div>

      <div v-else class="overview-projects">
        <ProjectCard
          v-for="project in recentProjects"
          :key="project.id"
          :project="project"
          :busy="busyId === project.id"
          compact
          @open="$router.push({ name: 'page-list', params: { id: project.id } })"
          @settings="$router.push({ name: 'site-settings', params: { id: project.id } })"
          @publish="onPublishSite(project)"
          @edit="openEditDialog(project)"
          @delete="onDelete(project)"
          @visit="openPublic(project)"
        />
      </div>
    </section>

    <!-- 推荐模板 -->
    <section class="overview-section">
      <div class="overview-section__head">
        <div>
          <p class="x-kicker">Templates</p>
          <h2 class="overview-section__title">推荐模板</h2>
        </div>
        <RouterLink class="overview-section__all x-mono" :to="{ name: 'admin-templates' }">
          浏览模板中心
          <XIcon name="arrow-right" :size="13" />
        </RouterLink>
      </div>
      <div class="overview-templates">
        <RouterLink
          v-for="template in RECOMMENDED_PAGE_TEMPLATES"
          :key="template.key"
          :to="{ name: 'admin-templates' }"
          class="overview-template"
          :class="`accent-${template.accent}`"
        >
          <div class="overview-template__art" aria-hidden="true">
            <span class="overview-template__art-mark">{{ template.name.slice(0, 1) }}</span>
          </div>
          <div class="overview-template__body">
            <p class="overview-template__name x-mono">{{ template.mono }}</p>
            <p class="overview-template__desc">{{ template.description }}</p>
            <span class="overview-template__cta x-mono">
              查看模板
              <XIcon name="arrow-up-right" :size="12" />
            </span>
          </div>
        </RouterLink>
      </div>
    </section>

    <!-- 编辑站点信息弹窗（与「我的游戏」页共用同一组件） -->
    <ProjectEditDialog
      v-if="editTarget"
      :project="editTarget"
      @close="closeEditDialog"
      @saved="onEditSaved"
    />
  </div>
</template>

<style scoped>
.overview {
  padding-top: 40px;
}

/* ---------------- hero ---------------- */
.overview-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  gap: 40px;
  align-items: stretch;
  padding-bottom: 42px;
  border-bottom: 1px solid var(--x-border);
}

.overview-hero__copy {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
  padding-top: 6px;
}

.overview-hero__title {
  margin: 4px 0 0;
}

.overview-hero__title-soft {
  color: var(--x-muted);
}

.overview-hero__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.overview-hero__art {
  position: relative;
  min-height: 260px;
  overflow: hidden;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: #0c0c0e;
}

.overview-hero__art-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 38px 38px;
  mask-image: linear-gradient(160deg, rgba(0, 0, 0, 0.9), transparent 85%);
}

.overview-hero__art-orb {
  position: absolute;
  right: -40px;
  top: 12%;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  border: 1px solid rgba(255, 122, 23, 0.35);
  box-shadow: inset 0 0 80px rgba(255, 122, 23, 0.1);
}

.overview-hero__art-orb::after {
  content: '';
  position: absolute;
  inset: 54px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.overview-hero__art-lines {
  position: absolute;
  left: 22px;
  bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-family: var(--x-mono);
  font-size: 9px;
  letter-spacing: 0.18em;
  color: rgba(255, 255, 255, 0.32);
}

/* ---------------- stats ---------------- */
.overview-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 26px 0;
  border-bottom: 1px solid var(--x-border);
}

.overview-stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0 26px;
  border-left: 1px solid var(--x-border);
}

.overview-stat:first-child {
  padding-left: 0;
  border-left: none;
}

.overview-stat__label {
  font-size: 9.5px;
  letter-spacing: 0.16em;
  color: var(--x-muted);
  text-transform: uppercase;
}

.overview-stat__value {
  font-size: 30px;
  line-height: 1.1;
  letter-spacing: -0.02em;
  color: var(--x-text);
}

.overview-error {
  margin-top: 24px;
}

/* ---------------- sections ---------------- */
.overview-section {
  padding-top: 40px;
}

.overview-section__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.overview-section__title {
  margin: 6px 0 0;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: -0.01em;
}

.overview-section__all {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--x-muted);
  text-transform: uppercase;
}

.overview-section__all:hover {
  color: var(--x-text);
}

.overview-projects {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.overview-project-skeleton {
  overflow: hidden;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
}

.overview-project-skeleton__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
}

.overview-empty {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  padding: 32px;
  border: 1px dashed var(--x-border-strong);
  border-radius: var(--x-radius);
}

.overview-empty__title {
  font-size: 18px;
}

/* ---------------- templates ---------------- */
.overview-templates {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.overview-template {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius);
  background: var(--x-surface);
  transition: border-color 0.15s ease;
}

.overview-template:hover {
  border-color: var(--x-border-strong);
}

.overview-template__art {
  position: relative;
  height: 108px;
  border-bottom: 1px solid var(--x-border);
  background:
    radial-gradient(120% 140% at 20% 0%, var(--tpl-a), transparent 60%),
    #0c0c0e;
}

.overview-template.accent-orange {
  --tpl-a: rgba(255, 122, 23, 0.14);
}

.overview-template.accent-violet {
  --tpl-a: rgba(196, 181, 253, 0.12);
}

.overview-template.accent-blue {
  --tpl-a: rgba(160, 195, 236, 0.12);
}

.overview-template.accent-green {
  --tpl-a: rgba(61, 220, 151, 0.1);
}

.overview-template__art-mark {
  position: absolute;
  left: 14px;
  bottom: 6px;
  font-family: var(--x-mono);
  font-size: 42px;
  color: rgba(255, 255, 255, 0.1);
  line-height: 1;
}

.overview-template__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
}

.overview-template__name {
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--x-text);
  text-transform: uppercase;
}

.overview-template__desc {
  color: var(--x-muted);
  font-size: 11.5px;
  line-height: 1.6;
  min-height: 54px;
}

.overview-template__cta {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding-top: 10px;
  border-top: 1px solid var(--x-border);
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--x-text-secondary);
}
@media (max-width: 1180px) {
  .overview-templates {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1024px) {
  .overview-hero {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .overview-hero__art {
    min-height: 180px;
  }

  .overview-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px 0;
  }

  .overview-stat:nth-child(3) {
    padding-left: 0;
    border-left: none;
  }

  .overview-projects {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .overview-templates {
    grid-template-columns: 1fr;
  }

  .overview-stat__value {
    font-size: 24px;
  }
}
</style>
