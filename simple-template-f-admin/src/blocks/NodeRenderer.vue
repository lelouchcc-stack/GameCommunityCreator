<template>
    <!-- 用户端：弹窗挂到 body，显隐由运行时控制；管理端编辑态则内联成普通容器以便选中编辑 -->
    <Teleport v-if="isModal && !interactive" to="body">
        <div
            v-if="runtime.isModalOpen(node.id)"
            class="ui-modal-mask"
            :style="{ zIndex: String(zIndex) }"
            @click.self="overlayClosable && runtime.closeModal(node.id)"
        >
            <div class="ui-modal-box" :class="modalClasses" :style="cssStyle">
                <button
                    v-if="showClose"
                    type="button"
                    class="ui-modal__close"
                    @click.stop="runtime.closeModal(node.id)"
                >
                    ×
                </button>
                <div v-if="node.content.title" class="ui-modal__title">{{ node.content.title }}</div>
                <NodeRenderer
                    v-for="child in node.children"
                    :key="child.id"
                    :node="child"
                    :interactive="interactive"
                    :selected-id="selectedId"
                    @select="(id: number) => $emit('select', id)"
                />
            </div>
        </div>
    </Teleport>

    <div
        v-else-if="shown"
        class="ui-node"
        :class="nodeClasses"
        :style="cssStyle"
        :data-node-id="node.id"
        @click.stop="onClick"
    >
        <!-- 业务组件：随对应业务模块接入后替换为真实组件；占位仅在管理端编辑态显示 -->
        <div v-if="node.isBuiltin === 1 && interactive" class="ui-placeholder">
            {{ node.typeName || node.typeCode }}（业务组件，待接入）
        </div>

        <template v-else-if="node.renderKind === 'text'">
            <span v-if="node.content.text">{{ node.content.text }}</span>
            <span v-else-if="interactive" class="ui-placeholder ui-placeholder--inline">文本</span>
        </template>

        <div v-else-if="node.renderKind === 'heading'" class="ui-heading">
            {{ node.content.text || (interactive ? "标题" : "") }}
        </div>

        <template v-else-if="node.renderKind === 'image'">
            <img v-if="node.content.imageUrl" class="ui-image" :src="node.content.imageUrl" alt="" />
            <div v-else-if="interactive" class="ui-placeholder">图片</div>
        </template>

        <template v-else-if="node.renderKind === 'icon'">
            <img v-if="node.content.icon" class="ui-icon" :src="node.content.icon" alt="" />
            <span v-else-if="interactive" class="ui-placeholder ui-placeholder--inline">图标</span>
        </template>

        <button v-else-if="node.renderKind === 'button'" type="button" class="ui-button">
            {{ node.content.text || (interactive ? "按钮" : "") }}
        </button>

        <div v-else-if="node.renderKind === 'divider'" class="ui-divider" :style="dividerStyle"></div>

        <div v-else-if="node.renderKind === 'spacer'" class="ui-spacer"></div>

        <template v-else-if="node.isContainer === 1">
            <div v-if="interactive && !node.children?.length" class="ui-placeholder">
                {{ node.typeName || node.typeCode }}：点击左侧组件加入这里
            </div>
            <NodeRenderer
                v-for="child in node.children"
                :key="child.id"
                :node="child"
                :interactive="interactive"
                :selected-id="selectedId"
                @select="(id: number) => $emit('select', id)"
            />
        </template>

        <div v-else-if="interactive" class="ui-placeholder">
            {{ node.typeName || node.renderKind }}（尚未实现渲染）
        </div>
    </div>
</template>

<script lang="ts" setup>
import { computed, inject } from "vue";
import type { UiNode } from "../types/uiBuilder";
import { toClasses, toCss } from "./styleRegistry";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockAction, type BlockRuntime } from "./blockRuntime";

defineOptions({ name: "NodeRenderer" });

const props = defineProps<{
    node: UiNode
    interactive?: boolean
    selectedId?: number | null
}>();

const emit = defineEmits<{ (e: "select", id: number): void }>();

/** 没有宿主注入时的兜底运行时，保证组件单独使用也不报错 */
const fallbackRuntime = createBlockRuntime();
const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, fallbackRuntime);

const isModal = computed(() => props.node.renderKind === "modal");
const shown = computed(() => props.node.visible !== 0 && !runtime.isHidden(props.node.id));
const cssStyle = computed(() => toCss(props.node.style));

const nodeClasses = computed(() => [
    ...toClasses(props.node.style),
    props.node.isContainer === 1 ? "ui-container" : "",
    props.interactive ? "ui-node--interactive" : "",
    props.interactive && props.selectedId === props.node.id ? "ui-node--selected" : "",
]);

const modalClasses = computed(() => [...toClasses(props.node.style), `ui-modal--${animation.value}`]);

const zIndex = computed(() => Number(props.node.behavior?.zIndex ?? 2000));
const overlayClosable = computed(() => props.node.behavior?.overlayClosable !== false);
const showClose = computed(() => props.node.behavior?.showClose !== false);
const animation = computed(() => (props.node.behavior?.animation as string) || "fade");

/** 分割线只保留上边框，避免四边描边叠成双线 */
const dividerStyle = computed(() => {
    const css = toCss(props.node.style);
    const style: Record<string, string> = {};
    if (css.width) {
        style.width = css.width;
    }
    if (css.marginTop) {
        style.marginTop = css.marginTop;
    }
    if (css.marginBottom) {
        style.marginBottom = css.marginBottom;
    }
    style.borderTopWidth = css.borderWidth || "1px";
    style.borderTopStyle = css.borderStyle || "solid";
    style.borderTopColor = css.borderColor || "var(--site-border, #E5E6EB)";
    return style;
});

/**
 * 点击行为：onClickAction 优先；否则把 linkType/linkUrl 折算成一次跳转，
 * 这样选项卡里的"跳转类型/地址"两个字段才真正生效。
 */
const effectiveAction = computed<BlockAction | null>(() => {
    const action = props.node.behavior?.onClickAction as BlockAction | undefined;
    if (action && action.action && action.action !== "none") {
        return action;
    }
    const content = props.node.content ?? {};
    if (content.linkType && content.linkType !== "none" && content.linkUrl) {
        return { action: "navigate", url: content.linkUrl, newTab: content.openInNewTab };
    }
    return null;
});

function onClick() {
    if (props.interactive) {
        emit("select", props.node.id);
        return;
    }
    if (effectiveAction.value) {
        runtime.run(effectiveAction.value);
    }
}
</script>
