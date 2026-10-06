<template>
    <div class="stat-page" v-loading="loading">
        <el-card class="stat-page__filter" shadow="never">
            <div class="stat-page__toolbar">
                <span class="stat-page__title">数据统计</span>
                <el-radio-group v-model="days" @change="loadOverview">
                    <el-radio-button :value="7">近 7 天</el-radio-button>
                    <el-radio-button :value="30">近 30 天</el-radio-button>
                    <el-radio-button :value="90">近 90 天</el-radio-button>
                </el-radio-group>
                <span class="stat-page__hint">
                    累计数字不受时间范围影响；口径与用户端一致：帖子只算已发布、评论只算正常状态
                </span>
            </div>
        </el-card>

        <div class="stat-page__cards">
            <el-card v-for="card in cards" :key="card.label" shadow="never" class="stat-page__card">
                <div class="stat-page__card-label">{{ card.label }}</div>
                <div class="stat-page__card-value">{{ card.value }}</div>
                <div class="stat-page__card-sub" :class="card.tone">{{ card.sub }}</div>
            </el-card>
        </div>

        <el-card shadow="never" class="stat-page__block">
            <template #header>
                <div class="stat-page__block-header">
                    <span class="stat-page__block-title">新增趋势</span>
                    <span class="stat-page__legend">
                        <i class="stat-page__dot stat-page__dot--post"></i> 新增帖子
                        <i class="stat-page__dot stat-page__dot--comment"></i> 新增评论
                        <span class="stat-page__muted">（柱高按区间内最大值等比缩放，悬停看具体数字）</span>
                    </span>
                </div>
            </template>

            <p v-if="!trend.length" class="stat-page__muted">暂无数据</p>
            <div v-else class="stat-page__chart">
                <div
                    v-for="point in trend"
                    :key="point.date"
                    class="stat-page__column"
                    :title="`${point.date}　帖子 ${point.posts}　评论 ${point.comments}`"
                >
                    <div class="stat-page__bars">
                        <div class="stat-page__bar stat-page__bar--post" :style="{ height: barHeight(point.posts) }"></div>
                        <div class="stat-page__bar stat-page__bar--comment" :style="{ height: barHeight(point.comments) }"></div>
                    </div>
                    <div class="stat-page__axis">{{ axisLabel(point.date) }}</div>
                </div>
            </div>
        </el-card>

        <div class="stat-page__two-col">
            <el-card shadow="never" class="stat-page__block">
                <template #header><span class="stat-page__block-title">热门板块</span></template>
                <p v-if="!overview?.topBoards?.length" class="stat-page__muted">暂无板块</p>
                <ul v-else class="stat-page__rank">
                    <li v-for="(board, index) in overview.topBoards" :key="board.boardId" class="stat-page__rank-item">
                        <span class="stat-page__rank-no" :class="`stat-page__rank-no--${index < 3 ? index + 1 : 'n'}`">
                            {{ index + 1 }}
                        </span>
                        <span class="stat-page__rank-name">{{ board.boardName }}</span>
                        <el-progress
                            class="stat-page__rank-bar"
                            :percentage="percentOf(board.posts, maxBoardPosts)"
                            :show-text="false"
                            :stroke-width="8"
                        />
                        <span class="stat-page__rank-value">{{ board.posts }} 帖</span>
                    </li>
                </ul>
            </el-card>

            <el-card shadow="never" class="stat-page__block">
                <template #header>
                    <span class="stat-page__block-title">版本分布</span>
                    <span class="stat-page__legend">已发布帖子按关联版本分布，未挂版本的计入「通用帖」</span>
                </template>
                <p v-if="!overview?.versions?.length" class="stat-page__muted">暂无数据</p>
                <ul v-else class="stat-page__rank">
                    <li v-for="version in overview.versions" :key="version.versionNo" class="stat-page__rank-item">
                        <span class="stat-page__rank-name">
                            {{ version.versionNo }}
                            <el-tag v-if="version.current" size="small" type="success">当前版本</el-tag>
                        </span>
                        <el-progress
                            class="stat-page__rank-bar"
                            :percentage="percentOf(version.posts, maxVersionPosts)"
                            :show-text="false"
                            :stroke-width="8"
                        />
                        <span class="stat-page__rank-value">{{ version.posts }} 帖</span>
                    </li>
                </ul>
            </el-card>
        </div>
    </div>
</template>

<script lang="ts" setup>
/**
 * 数据统计。此前「数据统计」菜单指向不存在的 stat/index.vue，会回退成空白占位页；这里补上。
 *
 * 图表刻意不引 ECharts：本项目前端零图表依赖，为两张小图加一个几百 KB 的库不划算。
 * 趋势用纯 CSS 柱子（高度按区间最大值等比缩放），板块/版本用 el-progress 横条，信息量足够。
 */
import { computed, onMounted, ref } from "vue";
import { getStatOverview, type StatOverview } from "../../api/stat";

const loading = ref(false);
const days = ref(30);
const overview = ref<StatOverview | null>(null);

const trend = computed(() => overview.value?.trend ?? []);

/** 趋势图纵轴上限：取帖子和评论里的最大值，全 0 时给 1 避免除零 */
const trendMax = computed(() => {
    const all = trend.value.flatMap((p) => [p.posts, p.comments]);
    return Math.max(1, ...all);
});

const maxBoardPosts = computed(() => Math.max(0, ...(overview.value?.topBoards ?? []).map((b) => b.posts)));
const maxVersionPosts = computed(() => Math.max(0, ...(overview.value?.versions ?? []).map((v) => v.posts)));

const cards = computed(() => {
    const o = overview.value;
    if (!o) {
        return [];
    }
    return [
        { label: "玩家", value: o.totalMembers, sub: `近 ${o.days} 天 +${o.rangeNewMembers}`, tone: "up" },
        { label: "已发布帖子", value: o.totalPosts, sub: `近 ${o.days} 天 +${o.rangeNewPosts}`, tone: "up" },
        { label: "待审核帖子", value: o.pendingPosts, sub: o.pendingPosts > 0 ? "有待处理" : "无积压", tone: o.pendingPosts > 0 ? "warn" : "" },
        { label: "评论", value: o.totalComments, sub: `近 ${o.days} 天 +${o.rangeNewComments}`, tone: "up" },
        { label: "开放板块", value: o.totalBoards, sub: "状态为开放", tone: "" },
        { label: "已发布公告", value: o.totalNotices, sub: "草稿不计入", tone: "" },
        { label: "待处理举报", value: o.pendingReports, sub: o.pendingReports > 0 ? "有待处理" : "无积压", tone: o.pendingReports > 0 ? "warn" : "" },
    ];
});

async function loadOverview() {
    loading.value = true;
    try {
        overview.value = await getStatOverview(days.value);
    } catch {
        overview.value = null;
    } finally {
        loading.value = false;
    }
}

/** 柱子高度百分比；有数据的日子最低给 4%，否则 1 和 0 看起来一样 */
function barHeight(value: number) {
    if (!value) {
        return "2px";
    }
    return `${Math.max(4, Math.round((value / trendMax.value) * 100))}%`;
}

/** 横轴只留「月-日」，天数多时隔几个标一个，避免挤成一团 */
const axisStep = computed(() => Math.max(1, Math.ceil(trend.value.length / 8)));

function axisLabel(date: string) {
    const index = trend.value.findIndex((p) => p.date === date);
    if (index % axisStep.value !== 0) {
        return "";
    }
    return date.slice(5);
}

function percentOf(value: number, max: number) {
    return max > 0 ? Math.round((value / max) * 100) : 0;
}

onMounted(loadOverview);
</script>

<style scoped>
.stat-page__filter {
    margin-bottom: 16px;
}

/* 注意：工具栏与图表柱不能用同一个类名——柱子的 width/max-width 会反过来压扁工具栏
   （这里踩过一次：两者都叫 stat-page__bar，工具栏被压成 14px 宽） */
.stat-page__toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 16px;
}

.stat-page__title {
    font-size: 16px;
    font-weight: 600;
}

.stat-page__hint {
    color: #909399;
    font-size: 12px;
}

.stat-page__cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
    margin-bottom: 16px;
}

.stat-page__card-label {
    color: #909399;
    font-size: 13px;
}

.stat-page__card-value {
    margin-top: 6px;
    font-size: 26px;
    font-weight: 600;
    line-height: 1.2;
}

.stat-page__card-sub {
    margin-top: 4px;
    color: #909399;
    font-size: 12px;
}

.stat-page__card-sub.up {
    color: #67c23a;
}

.stat-page__card-sub.warn {
    color: #e6a23c;
}

.stat-page__block {
    margin-bottom: 16px;
}

.stat-page__block-header {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 16px;
}

.stat-page__block-title {
    font-weight: 600;
}

.stat-page__legend {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #606266;
    font-size: 12px;
}

.stat-page__dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    margin-left: 8px;
    border-radius: 2px;
}

.stat-page__dot--post {
    background: #409eff;
}

.stat-page__dot--comment {
    background: #67c23a;
}

.stat-page__muted {
    color: #909399;
    font-size: 12px;
}

.stat-page__chart {
    display: flex;
    align-items: flex-end;
    gap: 4px;
    height: 200px;
    padding-top: 8px;
}

.stat-page__column {
    display: flex;
    flex: 1 1 0;
    flex-direction: column;
    justify-content: flex-end;
    min-width: 0;
    height: 100%;
}

.stat-page__bars {
    display: flex;
    flex: 1;
    align-items: flex-end;
    justify-content: center;
    gap: 2px;
    min-height: 0;
}

.stat-page__bar {
    width: 50%;
    max-width: 14px;
    border-radius: 3px 3px 0 0;
    transition: height 0.2s;
}

.stat-page__bar--post {
    background: #409eff;
}

.stat-page__bar--comment {
    background: #67c23a;
}

.stat-page__axis {
    height: 18px;
    color: #909399;
    font-size: 11px;
    text-align: center;
    white-space: nowrap;
}

.stat-page__two-col {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
    gap: 16px;
}

.stat-page__rank {
    margin: 0;
    padding: 0;
    list-style: none;
}

.stat-page__rank-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 6px 0;
}

.stat-page__rank-no {
    flex: none;
    width: 20px;
    color: #909399;
    font-size: 13px;
    font-weight: 600;
    text-align: center;
}

.stat-page__rank-no--1 {
    color: #f5222d;
}

.stat-page__rank-no--2 {
    color: #fa8c16;
}

.stat-page__rank-no--3 {
    color: #faad14;
}

.stat-page__rank-name {
    flex: 0 0 132px;
    overflow: hidden;
    font-size: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.stat-page__rank-bar {
    flex: 1;
    min-width: 60px;
}

.stat-page__rank-value {
    flex: none;
    width: 56px;
    color: #606266;
    font-size: 12px;
    text-align: right;
}
</style>
