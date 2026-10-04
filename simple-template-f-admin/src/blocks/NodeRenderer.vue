<template>
    <div
        v-if="node.visible !== 0"
        class="ui-node"
        :class="nodeClasses"
        :style="cssStyle"
        :data-node-id="node.id"
        @click.stop="onClick"
    >
        <!-- 业务组件：随对应业务模块接入后替换为真实组件 -->
        <div v-if="node.isBuiltin === 1" class="ui-placeholder">
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

        <button v-else-if="node.renderKind === 'button'" type="button" class="ui-button">
            {{ node.content.text || (interactive ? "按钮" : "") }}
        </button>

        <div v-else-if="node.renderKind === 'divider'" class="ui-divider" :style="dividerStyle"></div>

        <div v-else-if="node.renderKind === 'spacer'" class="ui-spacer"></div>

        <template v-else-if="node.renderKind === 'container'">
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
import { computed } from "vue";
import type { UiNode } from "../types/uiBuilder";
import { toClasses, toCss } from "./styleRegistry";

defineOptions({ name: "NodeRenderer" });

const props = defineProps<{
    node: UiNode
    interactive?: boolean
    selectedId?: number | null
}>();

const emit = defineEmits<{ (e: "select", id: number): void }>();

const cssStyle = computed(() => toCss(props.node.style));

const nodeClasses = computed(() => [
    ...toClasses(props.node.style),
    props.node.isContainer === 1 ? "ui-container" : "",
    props.interactive ? "ui-node--interactive" : "",
    props.interactive && props.selectedId === props.node.id ? "ui-node--selected" : "",
]);

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

function onClick() {
    if (props.interactive) {
        emit("select", props.node.id);
    }
}
</script>
