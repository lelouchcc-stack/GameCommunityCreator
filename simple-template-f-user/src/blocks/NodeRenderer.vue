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
                <div v-if="textOf(node.content.title)" class="ui-modal__title">{{ textOf(node.content.title) }}</div>
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
        <!-- 业务组件：注册表里有就渲染真实组件，没有则（仅编辑态）显示占位 -->
        <component v-if="builtinWidget" :is="builtinWidget" :node="node" :interactive="interactive" />
        <div v-else-if="node.isBuiltin === 1 && interactive" class="ui-placeholder">
            {{ node.typeName || node.typeCode }}（业务组件，待接入）
        </div>

        <template v-else-if="node.renderKind === 'text'">
            <span v-if="textOf(node.content.text)">{{ textOf(node.content.text) }}</span>
            <span v-else-if="interactive" class="ui-placeholder ui-placeholder--inline">文本</span>
        </template>

        <div v-else-if="node.renderKind === 'heading'" class="ui-heading">
            {{ textOf(node.content.text) || (interactive ? "标题" : "") }}
        </div>

        <template v-else-if="node.renderKind === 'image'">
            <img v-if="urlOf(node.content.imageUrl)" class="ui-image" :src="urlOf(node.content.imageUrl)" alt="" />
            <div v-else-if="interactive" class="ui-placeholder">图片</div>
        </template>

        <template v-else-if="node.renderKind === 'icon'">
            <img v-if="urlOf(node.content.icon)" class="ui-icon" :src="urlOf(node.content.icon)" alt="" />
            <span v-else-if="interactive" class="ui-placeholder ui-placeholder--inline">图标</span>
        </template>

        <button
            v-else-if="node.renderKind === 'button'"
            :type="buttonType"
            class="ui-button"
        >
            {{ textOf(node.content.text) || (interactive ? "按钮" : "") }}
        </button>

        <div v-else-if="node.renderKind === 'divider'" class="ui-divider" :style="dividerStyle"></div>

        <div v-else-if="node.renderKind === 'spacer'" class="ui-spacer"></div>

        <!-- 输入框：外层节点就是输入框的盒子（沿用 text/button 的约定：样式落在外层，
             内层元素被剥光只负责取值），所以背景/边框/圆角/内边距都在属性面板里配 -->
        <input
            v-else-if="node.renderKind === 'input'"
            class="ui-input"
            :type="inputType"
            :placeholder="inputPlaceholder"
            :value="inputValue"
            @input="onInput"
        />

        <!-- 表单容器：渲染成真 <form>，用 display:contents 让子节点参与外层容器的排版 -->
        <form v-else-if="node.renderKind === 'form'" class="ui-form" @submit.prevent="onFormSubmit(node)">
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
        </form>

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
import { computed, inject, onUnmounted, ref } from "vue";
import type { UiNode } from "../types/uiBuilder";
import { toClasses, toCss } from "./styleRegistry";
import { resolveAssetUrl } from "../utils/asset";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockAction, type BlockRuntime } from "./blockRuntime";
import { GRAPH_RUNTIME_KEY, type GraphBridge } from "./graphBridge";
import { resolveBuiltin } from "./builtinWidgets";
import { clearComponentValue, setComponentValue } from "./componentValues";

defineOptions({ name: "NodeRenderer" });

const props = defineProps<{
    node: UiNode
    interactive?: boolean
    selectedId?: number | null
}>();

const emit = defineEmits<{ (e: "select", id: number): void }>();

/** 业务组件（builtin:*）走注册表；未接入的返回 null，回落到占位 */
const builtinWidget = computed(() => resolveBuiltin(props.node.renderKind));

/** 没有宿主注入时的兜底运行时，保证组件单独使用也不报错 */
const fallbackRuntime = createBlockRuntime();
const runtime = inject<BlockRuntime>(BLOCK_RUNTIME_KEY, fallbackRuntime);
/** 图执行器（用户端才注入；管理端画布没有，绑定就当字面量） */
const graphBridge = inject<GraphBridge | null>(GRAPH_RUNTIME_KEY, null);

/** 内容里的绑定值（$ref / $runtime）交给图执行器解析；不是绑定或没注入就原样返回 */
function bound(value: unknown): unknown {
    if (!graphBridge || typeof value !== "string" || !value.startsWith("$")) {
        return value;
    }
    return graphBridge.evaluate(value);
}

function textOf(value: unknown): string {
    const resolved = bound(value);
    return resolved === undefined || resolved === null ? "" : String(resolved);
}

function urlOf(value: unknown): string {
    const resolved = bound(value);
    // 内容里存的是对象存储的 key 时，按存储基址现拼成可访问地址
    return typeof resolved === "string" ? resolveAssetUrl(resolved) : "";
}

/** 可见条件（visibleWhen）也是个绑定：解析不出来时先当可见，免得数据没回来整块闪掉 */
const whenVisible = computed(() => {
    const raw = props.node.content?.visibleWhen;
    if (!graphBridge || typeof raw !== "string" || !raw) {
        return true;
    }
    const resolved = graphBridge.evaluate(raw);
    return resolved === undefined ? true : Boolean(resolved);
});

const isModal = computed(() => props.node.renderKind === "modal");
const shown = computed(
    () =>
        props.node.visible !== 0 &&
        !runtime.isHidden(props.node.id) &&
        (graphBridge ? graphBridge.isVisible(props.node.id) : true) &&
        whenVisible.value,
);
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

/** 提交按钮：渲染成 type=submit，"提交"这件事交给所在的 form 容器 */
const isSubmitButton = computed(
    () => props.node.renderKind === "button" && props.node.content?.buttonType === "submit",
);
const buttonType = computed(() => (isSubmitButton.value ? "submit" : "button"));

/* ---------------- 输入类组件 ---------------- */

/** 登记进组件值注册表的字段名（逻辑图按 `<节点id>.value` 读） */
const FIELD_NAME = "value";
/** 显示值放本地 ref：注册表只给图读，反过来驱动输入框会打断输入法组合输入 */
const inputValue = ref(String(props.node.content?.defaultValue ?? ""));
const inputPlaceholder = computed(() => textOf(props.node.content?.placeholder));
const inputType = computed(() => {
    const type = String(props.node.content?.inputType ?? "text");
    return ["text", "password", "number", "email"].includes(type) ? type : "text";
});

function onInput(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    inputValue.value = value;
    setComponentValue(props.node.id, FIELD_NAME, value);
}

/** 表单提交（点提交按钮，或在输入框里回车）：把提交交给图，on.submit 的 nodeId 指向这个 form */
function onFormSubmit(node: UiNode) {
    if (props.interactive) {
        return;
    }
    graphBridge?.triggerEvent("submit", node.id).catch(() => undefined);
}

if (!props.interactive && props.node.renderKind === "input") {
    // 一渲染就登记初值：用户一个字都不输时，图上读到的也应是空串而不是 undefined
    setComponentValue(props.node.id, FIELD_NAME, inputValue.value);
    onUnmounted(() => clearComponentValue(props.node.id, FIELD_NAME));
}

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
    // 提交按钮的提交由所在 form 统一触发（on.submit 挂在表单节点上）：
    // 这里直接返回，免得一次点击既提交、又走一遍点击图 / 点击动作
    if (isSubmitButton.value) {
        return;
    }
    // 该组件挂了"点击组件"的图就走图（L4 编排）；没有才回落到简单动作（L2）
    if (graphBridge?.hasClickGraph(props.node.id)) {
        graphBridge.triggerEvent("click", props.node.id).catch(() => undefined);
        return;
    }
    if (effectiveAction.value) {
        runtime.run(effectiveAction.value);
    }
}
</script>
