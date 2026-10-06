<template>
    <div class="banner-carousel">
        <p v-if="loading" class="banner-carousel__hint">轮播图加载中…</p>
        <p v-else-if="!slides.length" class="banner-carousel__hint">暂无轮播图</p>
        <template v-else>
            <div class="banner-carousel__track" :style="{ transform: `translateX(-${index * 100}%)` }">
                <div
                    v-for="slide in slides"
                    :key="slide.id"
                    class="banner-carousel__slide"
                    :class="{ 'banner-carousel__slide--link': clickable(slide) }"
                    @click="onSlideClick(slide, $event)"
                >
                    <img
                        class="banner-carousel__img"
                        :src="resolveAssetUrl(slide.image)"
                        :alt="slide.title || '轮播图'"
                    />
                    <div v-if="slide.title || slide.subtitle" class="banner-carousel__caption">
                        <div v-if="slide.title" class="banner-carousel__title">{{ slide.title }}</div>
                        <div v-if="slide.subtitle" class="banner-carousel__subtitle">{{ slide.subtitle }}</div>
                    </div>
                </div>
            </div>

            <template v-if="slides.length > 1">
                <button
                    type="button"
                    class="banner-carousel__arrow banner-carousel__arrow--prev"
                    aria-label="上一张"
                    @click.stop="go(-1)"
                >
                    ‹
                </button>
                <button
                    type="button"
                    class="banner-carousel__arrow banner-carousel__arrow--next"
                    aria-label="下一张"
                    @click.stop="go(1)"
                >
                    ›
                </button>
                <div class="banner-carousel__dots">
                    <span
                        v-for="(slide, i) in slides"
                        :key="slide.id"
                        class="banner-carousel__dot"
                        :class="{ 'banner-carousel__dot--active': i === index }"
                        @click.stop="index = i"
                    />
                </div>
            </template>
        </template>
    </div>
</template>

<script lang="ts" setup>
/**
 * 首页轮播（业务组件 builtin:banner，管理端与用户端同源副本）。
 * 数据取 /app/banner/list（后台「站点配置 → 轮播图管理」维护），
 * 展示几张由节点内容属性 limit 决定；图片样式由外层节点负责（高度/圆角/下间距）。
 */
import { computed, inject, onMounted, onUnmounted, ref, watch } from "vue";
import type { UiNode } from "../../types/uiBuilder";
import { listVisibleBanners, type BannerItem } from "../../api/banner";
import { resolveAssetUrl } from "../../utils/asset";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockRuntime } from "../blockRuntime";
import { GRAPH_RUNTIME_KEY, type GraphBridge } from "../graphBridge";
import { activateNode } from "../nodeActivation";

const props = defineProps<{
    node: UiNode
    /** 管理端画布为 true：不自动播放、点击不跳转，把点击留给画布选中节点 */
    interactive?: boolean
}>();

const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, createBlockRuntime());
/** 图执行器（用户端才注入；管理端画布没有，退化成"只看点击动作与默认行为"） */
const graphBridge = inject<GraphBridge | null>(GRAPH_RUNTIME_KEY, null);

const all = ref<BannerItem[]>([]);
const loading = ref(true);
const index = ref(0);

const content = computed(() => props.node.content ?? {});
const limit = computed(() => {
    const n = Number(content.value.limit);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : 5;
});
const slides = computed(() => all.value.slice(0, limit.value));
const autoplay = computed(() => content.value.autoplay !== false && !props.interactive);
const interval = computed(() => {
    const n = Number(content.value.interval);
    return Number.isFinite(n) && n >= 1000 ? Math.floor(n) : 5000;
});

function clickable(slide: BannerItem) {
    return Boolean(slide.linkUrl) && !props.interactive;
}

function go(step: number) {
    const count = slides.value.length;
    if (count <= 1) {
        return;
    }
    index.value = (index.value + step + count) % count;
}

function onSlideClick(slide: BannerItem, event: MouseEvent) {
    // 点击归节点处理：节点上有逻辑图/点击动作时优先，否则才用这张图的跳转地址。
    // 图没配跳转地址时默认动作为空 —— 保持"没配就是不可点"的观感
    const handled = activateNode({
        node: props.node,
        runtime,
        graphBridge,
        interactive: props.interactive,
        defaultAction: slide.linkUrl ? { action: "navigate", url: slide.linkUrl } : null,
    });
    if (handled) {
        event.stopPropagation();
    }
}

let timer: number | undefined;

function restartTimer() {
    window.clearInterval(timer);
    if (!autoplay.value || slides.value.length <= 1) {
        return;
    }
    timer = window.setInterval(() => go(1), interval.value);
}

watch([autoplay, interval, () => slides.value.length], () => {
    // 数据或条数变了，当前下标可能越界
    if (index.value >= slides.value.length) {
        index.value = 0;
    }
    restartTimer();
});

onMounted(async () => {
    try {
        all.value = await listVisibleBanners();
    } catch {
        all.value = [];
    } finally {
        loading.value = false;
        restartTimer();
    }
});

onUnmounted(() => window.clearInterval(timer));
</script>

<style scoped>
.banner-carousel {
    position: relative;
    width: 100%;
    height: 100%;
    min-height: 200px;
    overflow: hidden;
    /* 圆角跟着外层节点配的 radius 走 */
    border-radius: inherit;
    background-color: var(--site-bg-page, #f7f8fa);
}

.banner-carousel__hint {
    margin: 0;
    padding: 24px;
    color: var(--site-text-secondary, #646a73);
    font-size: 14px;
    text-align: center;
}

.banner-carousel__track {
    display: flex;
    height: 100%;
    transition: transform 0.45s ease;
}

.banner-carousel__slide {
    position: relative;
    flex: 0 0 100%;
    height: 100%;
}

.banner-carousel__slide--link {
    cursor: pointer;
}

.banner-carousel__img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.banner-carousel__caption {
    position: absolute;
    right: 0;
    bottom: 0;
    left: 0;
    padding: 32px 20px 16px;
    background: linear-gradient(to top, rgb(0 0 0 / 62%), rgb(0 0 0 / 0%));
    color: #fff;
}

.banner-carousel__title {
    font-size: 20px;
    font-weight: 600;
    line-height: 1.4;
}

.banner-carousel__subtitle {
    margin-top: 4px;
    font-size: 13px;
    opacity: 0.88;
}

.banner-carousel__arrow {
    position: absolute;
    top: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    padding: 0;
    transform: translateY(-50%);
    border: none;
    border-radius: 50%;
    background-color: rgb(0 0 0 / 32%);
    color: #fff;
    font-size: 20px;
    line-height: 1;
    cursor: pointer;
    opacity: 0;
    transition: opacity 0.2s;
}

.banner-carousel:hover .banner-carousel__arrow {
    opacity: 1;
}

.banner-carousel__arrow--prev {
    left: 12px;
}

.banner-carousel__arrow--next {
    right: 12px;
}

.banner-carousel__dots {
    position: absolute;
    right: 0;
    bottom: 10px;
    left: 0;
    display: flex;
    justify-content: center;
    gap: 6px;
}

.banner-carousel__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: rgb(255 255 255 / 55%);
    cursor: pointer;
    transition: width 0.2s, background-color 0.2s;
}

.banner-carousel__dot--active {
    width: 16px;
    border-radius: 3px;
    background-color: #fff;
}

/* 窄屏：标题字号收一档、遮罩浅一点，箭头的悬停显隐在触屏上没意义就常显 */
@media (max-width: 640px) {
    .banner-carousel {
        min-height: 160px;
    }

    .banner-carousel__caption {
        padding: 24px 14px 12px;
    }

    .banner-carousel__title {
        font-size: 16px;
    }

    .banner-carousel__subtitle {
        font-size: 12px;
    }

    .banner-carousel__arrow {
        opacity: 1;
    }
}
</style>
