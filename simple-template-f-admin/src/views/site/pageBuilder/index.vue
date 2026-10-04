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
                <span class="page-builder__hint">
                    左：组件库（点击加入选中容器，未选中则加到顶层）；中：画布（点击选中）；右：属性
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
                            <el-button size="small" @click="moveSelected(-1)">上移</el-button>
                            <el-button size="small" @click="moveSelected(1)">下移</el-button>
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
                                <el-form-item
                                    v-for="key in selectedType.styleSchema"
                                    :key="key"
                                    :label="propertyLabel(key)"
                                >
                                    <PropertyControl v-model="selectedNode.style[key]" :meta="metaOf(key)" />
                                </el-form-item>
                            </el-form>
                        </el-tab-pane>
                        <el-tab-pane label="内容" name="content">
                            <el-form label-width="76px" size="small">
                                <el-form-item
                                    v-for="key in selectedType.contentSchema"
                                    :key="key"
                                    :label="propertyLabel(key)"
                                >
                                    <PropertyControl v-model="selectedNode.content[key]" :meta="metaOf(key)" />
                                </el-form-item>
                            </el-form>
                        </el-tab-pane>
                        <el-tab-pane label="行为" name="behavior">
                            <el-form label-width="76px" size="small">
                                <el-form-item
                                    v-for="key in selectedType.behaviorSchema"
                                    :key="key"
                                    :label="propertyLabel(key)"
                                >
                                    <PropertyControl v-model="selectedNode.behavior[key]" :meta="metaOf(key)" />
                                </el-form-item>
                            </el-form>
                        </el-tab-pane>
                    </el-tabs>
                </template>
                <el-empty v-else description="在画布中选中一个组件" :image-size="60" />
            </el-card>
        </div>

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
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import NodeRenderer from "../../../blocks/NodeRenderer.vue";
import PropertyControl from "../../../components/PropertyControl.vue";
import { CATEGORY_LABELS, PROPERTY_MAP, metaOf } from "../../../blocks/propertyMeta";
import {
    createPage,
    listComponentTypes,
    listNodes,
    listPages,
    saveNodes,
} from "../../../api/uiBuilder";
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

let nextTempId = -1;

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
        return;
    }
    loading.value = true;
    try {
        nodes.value = flatten(await listNodes(currentPageId.value));
    } finally {
        loading.value = false;
    }
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
        styleJson: node.style,
        contentJson: node.content,
        behaviorJson: node.behavior,
    }));
    saving.value = true;
    try {
        nodes.value = flatten(await saveNodes(currentPageId.value, payload));
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
