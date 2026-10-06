<template>
    <div class="page-builder">
        <el-card shadow="never" class="page-builder__bar">
            <div class="page-builder__bar-inner">
                <span class="page-builder__title">页面搭建</span>
                <el-select
                    v-model="currentPageId"
                    placeholder="选择页面"
                    style="width: 220px"
                    @change="onPageChange"
                >
                    <el-option
                        v-for="page in pages"
                        :key="page.id"
                        :label="`${page.pageName}（${page.pageKey}）`"
                        :value="page.id"
                    />
                </el-select>
                <el-button @click="pageDialogVisible = true">新建页面</el-button>
                <el-button :disabled="!currentPageId" @click="openFlowEditor">逻辑编排</el-button>
                <span class="page-builder__hint">
                    左：组件库（点击加入选中容器，未选中则加到顶层）；中：画布（点击选中）；右：属性；
                    「逻辑编排」画这个页面的逻辑图
                </span>
                <el-button type="primary" :loading="saving" :disabled="!currentPageId" @click="handleSave">
                    保存
                </el-button>
            </div>
        </el-card>

        <div class="page-builder__body">
            <el-card shadow="never" class="page-builder__palette">
                <template #header><span class="page-builder__panel-title">组件库</span></template>
                <div v-for="(group, category) in groupedTypes" :key="category" class="page-builder__group">
                    <div class="page-builder__group-label">{{ CATEGORY_LABELS[category] || category }}</div>
                    <div class="page-builder__items">
                        <div
                            v-for="type in group"
                            :key="type.typeCode"
                            class="page-builder__item"
                            @click="addComponent(type)"
                        >
                            <span>{{ type.typeName }}</span>
                            <el-tag v-if="type.isContainer === 1" size="small" type="info">容器</el-tag>
                        </div>
                    </div>
                </div>
                <el-empty v-if="!types.length" description="暂无组件" :image-size="60" />
            </el-card>

            <el-card shadow="never" class="page-builder__canvas-wrap">
                <template #header>
                    <span class="page-builder__panel-title">画布</span>
                    <span class="page-builder__panel-hint">所见即所得，点击组件可选中</span>
                </template>
                <div class="page-builder__canvas" @click="selectedId = null">
                    <NodeRenderer
                        v-for="node in canvasTree"
                        :key="node.id"
                        :node="node"
                        interactive
                        :selected-id="selectedId"
                        @select="selectedId = $event"
                    />
                    <el-empty v-if="!canvasTree.length" description="从左侧组件库点击添加组件" />
                </div>
            </el-card>

            <el-card shadow="never" class="page-builder__props">
                <template #header>
                    <span class="page-builder__panel-title">属性</span>
                    <span v-if="selectedNode" class="page-builder__panel-hint">
                        {{ selectedType?.typeName }}
                    </span>
                </template>

                <template v-if="selectedNode && selectedType">
                    <el-form label-width="76px" size="small">
                        <el-form-item label="节点名称">
                            <el-input v-model="selectedNode.nodeName" maxlength="50" />
                        </el-form-item>
                        <el-form-item label="显示">
                            <el-switch v-model="selectedNode.visible" :active-value="1" :inactive-value="0" />
                        </el-form-item>
                        <el-form-item label="排序">
                            <el-button size="small" @click="moveSelected(-1)">{{ moveLabels[0] }}</el-button>
                            <el-button size="small" @click="moveSelected(1)">{{ moveLabels[1] }}</el-button>
                            <el-button size="small" type="danger" plain @click="removeSelected">删除</el-button>
                        </el-form-item>
                        <el-alert
                            v-if="selectedType.isContainer === 1"
                            type="info"
                            :closable="false"
                            show-icon
                            title="容器：点击左侧组件会加入该容器"
                            style="margin-bottom: 12px"
                        />
                    </el-form>

                    <el-tabs v-model="activeTab">
                        <el-tab-pane label="样式" name="style">
                            <el-form label-width="76px" size="small">
                                <template v-for="section in styleSections" :key="section.label">
                                    <el-divider content-position="left">{{ section.label }}</el-divider>
                                    <el-form-item
                                        v-for="key in section.keys"
                                        :key="key"
                                        :label="propertyLabel(key)"
                                    >
                                        <PropertyControl v-model="selectedNode.style[key]" :meta="metaOf(key)" />
                                    </el-form-item>
                                </template>
                            </el-form>
                        </el-tab-pane>
                        <el-tab-pane label="内容" name="content">
                            <el-form label-width="76px" size="small">
                                <el-form-item
                                    v-for="key in selectedType.contentSchema"
                                    :key="key"
                                    :label="propertyLabel(key)"
                                >
                                    <!-- 可绑定的字段多给一个"绑到图节点输出"的下拉，生成 $ref -->
                                    <PropertyControl
                                        v-model="selectedNode.content[key]"
                                        :meta="metaOf(key)"
                                        :bindings="BINDABLE_KEYS.has(key) ? flowOptions : undefined"
                                    />
                                </el-form-item>
                            </el-form>
                        </el-tab-pane>
                        <el-tab-pane label="行为" name="behavior">
                            <el-alert
                                v-if="!selectedType.behaviorSchema.length"
                                type="info"
                                :closable="false"
                                show-icon
                                title="这个组件没有开放「行为」属性"
                                class="page-builder__behavior-hint"
                            >
                                <div>
                                    要让它在被点击时执行逻辑，用工具栏的「逻辑编排」：
                                    左栏「触发 → 点击组件」，把它的 <code>nodeId</code> 指向这个组件。
                                </div>
                                <div>
                                    节点上挂的逻辑图优先于组件自带的默认行为（例如板块卡片默认进该板块、
                                    帖子条目默认进帖子详情）；没挂图时才走默认行为。
                                </div>
                            </el-alert>
                            <el-form label-width="76px" size="small">
                                <el-form-item
                                    v-for="key in selectedType.behaviorSchema"
                                    :key="key"
                                    :label="propertyLabel(key)"
                                >
                                    <ActionControl
                                        v-if="key === 'onClickAction'"
                                        v-model="selectedNode.behavior[key]"
                                        :node-options="actionTargetOptions"
                                    />
                                    <PropertyControl v-else v-model="selectedNode.behavior[key]" :meta="metaOf(key)" />
                                </el-form-item>
                            </el-form>
                        </el-tab-pane>
                    </el-tabs>
                </template>
                <el-empty v-else description="在画布中选中一个组件" :image-size="60" />
            </el-card>
        </div>

        <el-dialog
            v-model="flowDialogVisible"
            title="逻辑编排"
            width="96%"
            top="2vh"
            destroy-on-close
            class="page-builder__flow-dialog"
        >
            <FlowEditor
                v-if="flowDialogVisible && currentPageId"
                :page-id="currentPageId"
                :page-nodes="flowEditorNodes"
                @close="closeFlowEditor"
            />
        </el-dialog>

        <el-dialog v-model="pageDialogVisible" title="新建页面" width="440px">
            <el-form :model="pageForm" label-width="90px">
                <el-form-item label="页面标识" required>
                    <el-input v-model="pageForm.pageKey" placeholder="小写字母开头，如 about" />
                </el-form-item>
                <el-form-item label="页面名称" required>
                    <el-input v-model="pageForm.pageName" placeholder="如 关于我们" />
                </el-form-item>
                <el-form-item label="路由地址">
                    <el-input v-model="pageForm.routePath" placeholder="如 /about" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="pageDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleCreatePage">创建</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, provide, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import NodeRenderer from "../../../blocks/NodeRenderer.vue";
import PropertyControl from "../../../components/PropertyControl.vue";
import ActionControl, { type ActionTargetOption } from "../../../components/ActionControl.vue";
import { CATEGORY_LABELS, BINDABLE_KEYS, PROPERTY_MAP, STYLE_GROUPS, metaOf } from "../../../blocks/propertyMeta";
import { BLOCK_RUNTIME_KEY, createBlockRuntime, type BlockAction } from "../../../blocks/blockRuntime";
import { portsOfNode } from "../../../blocks/portSchema";
import FlowEditor from "./FlowEditor.vue";
import {
    createPage,
    listComponentTypes,
    listNodes,
    listPages,
    saveNodes,
} from "../../../api/uiBuilder";
import { listFlows, listLogicNodes, listPresets } from "../../../api/uiLogic";
import type { ComponentType, NodePayload, PageVO, UiNode } from "../../../types/uiBuilder";

/** 编辑态用扁平列表（与后端整树保存结构一致），渲染时再组装成树 */
interface FlatNode {
    id: number
    parentId: number | null
    typeCode: string
    nodeName: string
    sort: number
    visible: number
    style: Record<string, any>
    content: Record<string, any>
    behavior: Record<string, any>
}

const loading = ref(false);
const saving = ref(false);
const types = ref<ComponentType[]>([]);
const pages = ref<PageVO[]>([]);
const currentPageId = ref<number | null>(null);
const nodes = ref<FlatNode[]>([]);
const selectedId = ref<number | null>(null);
const activeTab = ref("style");

const pageDialogVisible = ref(false);
const pageForm = reactive({ pageKey: "", pageName: "", routePath: "" });

const flowDialogVisible = ref(false);
/** 本页图里所有可绑定的输出端口，做成 "$ref.<节点>.<端口>" 供内容字段挑选 */
const flowOptions = ref<Array<{ label: string; value: string }>>([]);

let nextTempId = -1;

/** 画布内共享一个运行时：编辑态点击只选中，真正执行动作交给用户端；这里保留提示/滚动便于预览 */
provide(
    BLOCK_RUNTIME_KEY,
    createBlockRuntime({
        navigate: (url) => ElMessage.info(`预览模式：不实际跳转（${url}）`),
        toast: (message, type) => ElMessage({ message, type: (type ?? "info") as never }),
        scrollTo: (id) => {
            document
                .querySelector(`[data-node-id="${id}"]`)
                ?.scrollIntoView({ behavior: "smooth", block: "center" });
        },
    }),
);

const groupedTypes = computed(() => {
    const map: Record<string, ComponentType[]> = {};
    for (const type of types.value) {
        if (type.status !== 1) {
            continue;
        }
        (map[type.category] ??= []).push(type);
    }
    return map;
});

const selectedNode = computed(() => nodes.value.find((node) => node.id === selectedId.value) ?? null);
const selectedType = computed(() => {
    const node = selectedNode.value;
    return node ? types.value.find((type) => type.typeCode === node.typeCode) ?? null : null;
});

/**
 * 样式面板分组：基础样式 + 业务组件的分区域样式（列表项/标题/元信息…）。
 * 组件类型勾选了哪些 key（styleSchema），这里就只显示哪些，并按分组加标题。
 */
const styleSections = computed(() => {
    const schema = selectedType.value?.styleSchema ?? [];
    const sections: Array<{ label: string; keys: string[] }> = [];
    for (const group of STYLE_GROUPS) {
        const keys = group.properties.filter((property) => schema.includes(property.key)).map((property) => property.key);
        if (keys.length) {
            sections.push({ label: group.label, keys });
        }
    }
    return sections;
});

/** 动作的目标下拉项：标出哪些是弹窗组件，供"打开弹窗"过滤使用 */
const actionTargetOptions = computed<ActionTargetOption[]>(() =>
    nodes.value.map((node) => {
        const type = types.value.find((item) => item.typeCode === node.typeCode);
        return {
            value: node.id,
            label: `${type?.typeName ?? node.typeCode}「${node.nodeName || node.id}」`,
            isModal: type?.renderKind === "modal",
        };
    }),
);

/**
 * 同级是横排还是竖排：决定排序按钮说"左右"还是"上下"。
 * 页面根固定竖排（行要显式加「行」组件），所以顶层节点一律按上下。
 */
const siblingAxis = computed<"row" | "column">(() => {
    const node = selectedNode.value;
    if (!node || node.parentId === null) {
        return "column";
    }
    const parent = nodes.value.find((item) => item.id === node.parentId);
    const parentType = parent ? types.value.find((type) => type.typeCode === parent.typeCode) : null;
    const direction = { ...(parentType?.defaultStyle ?? {}), ...(parent?.style ?? {}) }.flexDirection;
    return direction === "row" ? "row" : "column";
});

const moveLabels = computed(() => (siblingAxis.value === "row" ? ["左移", "右移"] : ["上移", "下移"]));

/** 逻辑编排里 nodeRef 型设置项只认本页**已保存**的组件（临时负数 id 还不存在） */
const flowEditorNodes = computed(() =>
    nodes.value
        .filter((node) => node.id > 0)
        .map((node) => ({
            id: node.id,
            typeName: types.value.find((type) => type.typeCode === node.typeCode)?.typeName ?? node.typeCode,
            nodeName: node.nodeName,
        })),
);

function openFlowEditor() {
    if (!currentPageId.value) {
        ElMessage.warning("请先选择页面");
        return;
    }
    if (nodes.value.some((node) => node.id < 0)) {
        ElMessage.warning("画布上还有没保存的组件，先保存再画逻辑图（逻辑图里的\"选本页组件\"要指向已保存的组件）");
        return;
    }
    flowDialogVisible.value = true;
}

async function closeFlowEditor() {
    flowDialogVisible.value = false;
    await loadFlowOptions();
}

/** 汇总本页各图的所有输出端口，作为内容字段「绑定到图节点输出」的候选 */
async function loadFlowOptions() {
    if (!currentPageId.value) {
        flowOptions.value = [];
        return;
    }
    try {
        const [flows, atoms, presets] = await Promise.all([
            listFlows(currentPageId.value),
            listLogicNodes(1),
            listPresets(1),
        ]);
        const options: Array<{ label: string; value: string }> = [];
        for (const flow of flows) {
            for (const node of flow.graph?.nodes ?? []) {
                for (const port of portsOfNode(node, atoms, presets).outputs) {
                    if (port.type === "signal") {
                        continue;
                    }
                    options.push({
                        label: `${flow.flowKey} · ${node.label || node.typeCode}.${port.key}`,
                        value: `$ref.${node.id}.${port.key}`,
                    });
                }
            }
        }
        flowOptions.value = options;
    } catch {
        flowOptions.value = [];
    }
}

/** 组装成 NodeRenderer 需要的树，并把组件类型默认值合并进来做预览 */
const canvasTree = computed<UiNode[]>(() => buildTree(null));

function buildTree(parentId: number | null): UiNode[] {
    return nodes.value
        .filter((node) => node.parentId === parentId)
        .sort((a, b) => a.sort - b.sort || a.id - b.id)
        .map((node) => {
            const type = types.value.find((item) => item.typeCode === node.typeCode);
            return {
                id: node.id,
                parentId: node.parentId,
                typeCode: node.typeCode,
                typeName: type?.typeName ?? null,
                renderKind: type?.renderKind ?? null,
                isContainer: type?.isContainer ?? 0,
                isBuiltin: type?.isBuiltin ?? 0,
                nodeName: node.nodeName,
                sort: node.sort,
                visible: node.visible,
                style: { ...(type?.defaultStyle ?? {}), ...node.style },
                content: { ...(type?.defaultContent ?? {}), ...node.content },
                behavior: { ...node.behavior },
                remark: null,
                children: buildTree(node.id),
            };
        });
}

function propertyLabel(key: string) {
    return PROPERTY_MAP[key]?.label ?? key;
}

function flatten(list: UiNode[]): FlatNode[] {
    const result: FlatNode[] = [];
    const walk = (items: UiNode[], parentId: number | null) => {
        for (const item of items) {
            result.push({
                id: item.id,
                parentId,
                typeCode: item.typeCode,
                nodeName: item.nodeName ?? "",
                sort: item.sort,
                visible: item.visible,
                style: { ...item.style },
                content: { ...item.content },
                behavior: { ...item.behavior },
            });
            walk(item.children ?? [], item.id);
        }
    };
    walk(list, null);
    return result;
}

async function loadPages() {
    pages.value = await listPages();
    const home = pages.value.find((page) => page.isHome === 1) ?? pages.value[0];
    if (home) {
        currentPageId.value = home.id;
        await onPageChange();
    }
}

async function onPageChange() {
    selectedId.value = null;
    if (!currentPageId.value) {
        nodes.value = [];
        flowOptions.value = [];
        return;
    }
    loading.value = true;
    try {
        nodes.value = flatten(await listNodes(currentPageId.value));
    } finally {
        loading.value = false;
    }
    await loadFlowOptions();
}

function addComponent(type: ComponentType) {
    if (!currentPageId.value) {
        ElMessage.warning("请先选择或新建一个页面");
        return;
    }
    const selected = selectedNode.value;
    const selectedIsContainer = selected && selectedType.value?.isContainer === 1;
    const parentId = selectedIsContainer ? selected.id : null;
    const siblings = nodes.value.filter((node) => node.parentId === parentId);
    const sort = siblings.reduce((max, node) => Math.max(max, node.sort), 0) + 1;
    const node: FlatNode = {
        id: nextTempId--,
        parentId,
        typeCode: type.typeCode,
        nodeName: type.typeName,
        sort,
        visible: 1,
        style: {},
        content: {},
        behavior: {},
    };
    nodes.value.push(node);
    selectedId.value = node.id;
    activeTab.value = "style";
}

function removeSelected() {
    const node = selectedNode.value;
    if (!node) {
        return;
    }
    const descendants = collectDescendants(node.id);
    nodes.value = nodes.value.filter((item) => item.id !== node.id && !descendants.has(item.id));
    selectedId.value = null;
}

function collectDescendants(id: number): Set<number> {
    const result = new Set<number>();
    const walk = (parentId: number) => {
        for (const node of nodes.value) {
            if (node.parentId === parentId) {
                result.add(node.id);
                walk(node.id);
            }
        }
    };
    walk(id);
    return result;
}

function moveSelected(offset: number) {
    const node = selectedNode.value;
    if (!node) {
        return;
    }
    const siblings = nodes.value
        .filter((item) => item.parentId === node.parentId)
        .sort((a, b) => a.sort - b.sort || a.id - b.id);
    const index = siblings.findIndex((item) => item.id === node.id);
    const target = index + offset;
    if (target < 0 || target >= siblings.length) {
        return;
    }
    const swapped = siblings[target];
    if (!swapped) {
        return;
    }
    const temp = node.sort;
    node.sort = swapped.sort;
    swapped.sort = temp;
}

/** 把动作里指向"临时 id"的目标回填为真实 id；目标已被删除则清空 */
function remapActionTargets(tree: UiNode[]) {
    const tempToReal = new Map<number, number>();
    const collect = (list: UiNode[]) => {
        for (const item of list) {
            if (typeof item.clientId === "number") {
                tempToReal.set(item.clientId, item.id);
            }
            collect(item.children ?? []);
        }
    };
    collect(tree);
    if (!tempToReal.size) {
        return;
    }
    for (const node of nodes.value) {
        const action = node.behavior?.onClickAction as BlockAction | undefined;
        const target = action?.target;
        if (typeof target !== "number" || target >= 0) {
            continue;
        }
        node.behavior.onClickAction = { ...action, target: tempToReal.get(target) ?? null };
    }
}

/** 未填的项不提交：清空某项只表示"跟随组件默认值"，不该往库里写空键 */
function compact(source: Record<string, any>): Record<string, any> {
    const result: Record<string, any> = {};
    for (const [key, value] of Object.entries(source ?? {})) {
        if (value === undefined || value === null || value === "") {
            continue;
        }
        result[key] = value;
    }
    return result;
}

async function handleSave() {
    if (!currentPageId.value) {
        return;
    }
    const payload: NodePayload[] = nodes.value.map((node) => ({
        id: node.id,
        parentId: node.parentId,
        typeCode: node.typeCode,
        nodeName: node.nodeName,
        sort: node.sort,
        visible: node.visible,
        styleJson: compact(node.style),
        contentJson: compact(node.content),
        behaviorJson: compact(node.behavior),
    }));
    saving.value = true;
    try {
        const tree = await saveNodes(currentPageId.value, payload);
        nodes.value = flatten(tree);
        remapActionTargets(tree);
        ElMessage.success("保存成功，用户端刷新后按新配置渲染");
    } catch {
        // 失败原因由请求拦截器统一提示
    } finally {
        saving.value = false;
    }
}

async function handleCreatePage() {
    if (!pageForm.pageKey || !pageForm.pageName) {
        ElMessage.error("请填写页面标识与名称");
        return;
    }
    try {
        await createPage({ ...pageForm });
        ElMessage.success("页面已创建");
        pageDialogVisible.value = false;
        Object.assign(pageForm, { pageKey: "", pageName: "", routePath: "" });
        await loadPages();
    } catch {
        // 失败原因由请求拦截器统一提示
    }
}

onMounted(async () => {
    types.value = await listComponentTypes();
    await loadPages();
});
</script>

<style scoped>
.page-builder__bar {
    margin-bottom: 12px;
}

.page-builder__bar-inner {
    display: flex;
    align-items: center;
    gap: 12px;
}

.page-builder__title {
    font-weight: 600;
}

.page-builder__hint {
    flex: 1;
    font-size: 12px;
    color: #909399;
}

.page-builder__body {
    display: flex;
    gap: 12px;
    align-items: flex-start;
}

.page-builder__palette {
    width: 220px;
    flex-shrink: 0;
}

.page-builder__canvas-wrap {
    flex: 1;
    min-width: 0;
}

.page-builder__props {
    width: 360px;
    flex-shrink: 0;
}

/* 「行为」页签为空时的去处提示：不写这块，选中业务组件会看到一个空白页签，
   误以为"业务组件没法编排逻辑" */
.page-builder__behavior-hint {
    margin-bottom: 12px;
    line-height: 1.7;
}

.page-builder__behavior-hint code {
    padding: 0 4px;
    border-radius: 3px;
    background: rgb(0 0 0 / 6%);
    font-family: monospace;
}

.page-builder__panel-title {
    font-weight: 600;
}

.page-builder__panel-hint {
    margin-left: 8px;
    font-size: 12px;
    color: #909399;
}

.page-builder__group {
    margin-bottom: 12px;
}

.page-builder__group-label {
    font-size: 12px;
    color: #909399;
    margin-bottom: 6px;
}

.page-builder__items {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.page-builder__item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
    cursor: pointer;
    font-size: 13px;
}

.page-builder__item:hover {
    border-color: var(--el-color-primary);
    color: var(--el-color-primary);
}

.page-builder__canvas {
    min-height: 420px;
    padding: 16px;
    background-color: var(--site-bg-page, #f7f8fa);
    border: 1px dashed var(--el-border-color);
    border-radius: 6px;
}
</style>
