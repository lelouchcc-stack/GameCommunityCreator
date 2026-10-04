<template>
    <div class="component-type">
        <el-card shadow="never">
            <template #header>
                <div class="component-type__header">
                    <span>组件库</span>
                    <span class="component-type__hint">
                        一行一种可拖入用户端的组件；勾选开放哪些属性，用户端与搭建器的属性面板据此生成
                    </span>
                    <el-button type="primary" @click="openCreate">新增组件</el-button>
                </div>
            </template>

            <el-table v-loading="loading" :data="rows" border>
                <el-table-column label="组件" min-width="200">
                    <template #default="{ row }">
                        <div class="component-type__name">{{ row.typeName }}</div>
                        <el-tag size="small" type="info">{{ row.typeCode }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="分类" width="90" align="center">
                    <template #default="{ row }">{{ CATEGORY_LABELS[row.category] || row.category }}</template>
                </el-table-column>
                <el-table-column label="渲染内核" width="150">
                    <template #default="{ row }">
                        <el-tag size="small">{{ row.renderKind }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="容器" width="70" align="center">
                    <template #default="{ row }">{{ row.isContainer === 1 ? "是" : "-" }}</template>
                </el-table-column>
                <el-table-column label="类型" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag size="small" :type="row.isBuiltin === 1 ? 'warning' : 'success'">
                            {{ row.isBuiltin === 1 ? "业务" : "展示" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="sort" label="排序" width="70" align="center" />
                <el-table-column label="操作" width="140" align="center">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="danger" @click="onDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="dialogVisible" :title="editingId ? '编辑组件' : '新增组件'" width="720px" top="6vh">
            <el-form :model="form" label-width="90px">
                <el-form-item label="组件名称" required>
                    <el-input v-model="form.typeName" maxlength="50" placeholder="如 按钮 / 主按钮 / 通知卡片" />
                </el-form-item>
                <el-form-item label="分类" required>
                    <el-select v-model="form.category" style="width: 100%">
                        <el-option
                            v-for="(label, key) in CATEGORY_LABELS"
                            :key="key"
                            :label="label"
                            :value="key"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="渲染内核" required>
                    <el-select v-model="form.renderKind" style="width: 100%">
                        <el-option v-for="kind in RENDER_KINDS" :key="kind" :label="kind" :value="kind" />
                    </el-select>
                </el-form-item>
                <el-form-item label="可容纳子节点">
                    <el-switch v-model="form.isContainer" :active-value="1" :inactive-value="0" />
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="form.sort" :min="0" />
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="form.remark" maxlength="200" />
                </el-form-item>

                <el-divider content-position="left">开放属性</el-divider>
                <el-form-item v-for="group in STYLE_GROUPS" :key="group.group" :label="group.label">
                    <el-checkbox-group v-model="form.styleSchema">
                        <el-checkbox v-for="property in group.properties" :key="property.key" :value="property.key">
                            {{ property.label }}
                        </el-checkbox>
                    </el-checkbox-group>
                </el-form-item>
                <el-form-item label="内容">
                    <el-checkbox-group v-model="form.contentSchema">
                        <el-checkbox
                            v-for="property in CONTENT_GROUP.properties"
                            :key="property.key"
                            :value="property.key"
                        >
                            {{ property.label }}
                        </el-checkbox>
                    </el-checkbox-group>
                </el-form-item>
                <el-form-item label="行为">
                    <el-checkbox-group v-model="form.behaviorSchema">
                        <el-checkbox
                            v-for="property in BEHAVIOR_GROUP.properties"
                            :key="property.key"
                            :value="property.key"
                        >
                            {{ property.label }}
                        </el-checkbox>
                    </el-checkbox-group>
                </el-form-item>

                <el-divider content-position="left">默认值（留空表示跟随主题）</el-divider>
                <el-form-item v-for="key in form.styleSchema" :key="key" :label="propertyLabel(key)">
                    <PropertyControl v-model="form.defaultStyle[key]" :meta="metaOf(key)" />
                </el-form-item>
                <el-form-item v-for="key in form.contentSchema" :key="key" :label="propertyLabel(key)">
                    <PropertyControl v-model="form.defaultContent[key]" :meta="metaOf(key)" />
                </el-form-item>
            </el-form>

            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import PropertyControl from "../../../components/PropertyControl.vue";
import {
    CATEGORY_LABELS,
    CONTENT_GROUP,
    BEHAVIOR_GROUP,
    PROPERTY_MAP,
    metaOf,
    RENDER_KINDS,
    STYLE_GROUPS,
} from "../../../blocks/propertyMeta";
import { createComponentType, deleteComponentType, listComponentTypes, updateComponentType } from "../../../api/uiBuilder";
import type { ComponentType, ComponentTypePayload } from "../../../types/uiBuilder";

const loading = ref(false);
const saving = ref(false);
const rows = ref<ComponentType[]>([]);

const dialogVisible = ref(false);
const editingId = ref<number | null>(null);

const form = reactive<ComponentTypePayload>(emptyForm());

function emptyForm(): ComponentTypePayload {
    return {
        typeName: "",
        category: "base",
        renderKind: "text",
        isContainer: 0,
        styleSchema: [],
        contentSchema: [],
        behaviorSchema: [],
        defaultStyle: {},
        defaultContent: {},
        previewIcon: null,
        sort: 0,
        remark: null,
    };
}

function propertyLabel(key: string) {
    return PROPERTY_MAP[key]?.label ?? key;
}

async function loadList() {
    loading.value = true;
    try {
        rows.value = await listComponentTypes();
    } finally {
        loading.value = false;
    }
}

function openCreate() {
    Object.assign(form, emptyForm());
    editingId.value = null;
    dialogVisible.value = true;
}

function openEdit(row: ComponentType) {
    Object.assign(form, {
        typeName: row.typeName,
        category: row.category,
        renderKind: row.renderKind,
        isContainer: row.isContainer,
        styleSchema: [...row.styleSchema],
        contentSchema: [...row.contentSchema],
        behaviorSchema: [...row.behaviorSchema],
        defaultStyle: JSON.parse(JSON.stringify(row.defaultStyle ?? {})),
        defaultContent: JSON.parse(JSON.stringify(row.defaultContent ?? {})),
        previewIcon: row.previewIcon,
        sort: row.sort,
        remark: row.remark,
    });
    editingId.value = row.id;
    dialogVisible.value = true;
}

async function handleSave() {
    if (!form.typeName.trim()) {
        ElMessage.error("请输入组件名称");
        return;
    }
    saving.value = true;
    try {
        if (editingId.value) {
            await updateComponentType(editingId.value, { ...form });
        } else {
            await createComponentType({ ...form });
        }
        ElMessage.success("保存成功，用户端刷新后生效");
        dialogVisible.value = false;
        await loadList();
    } catch {
        // 失败原因由请求拦截器统一提示
    } finally {
        saving.value = false;
    }
}

async function onDelete(row: ComponentType) {
    try {
        await ElMessageBox.confirm(`确定删除组件「${row.typeName}」吗？`, "提示", { type: "warning" });
    } catch {
        return;
    }
    try {
        await deleteComponentType(row.id);
        ElMessage.success("删除成功");
        await loadList();
    } catch {
        // 被引用或业务组件会被后端拒绝，原因已由拦截器提示
    }
}

onMounted(loadList);
</script>

<style scoped>
.component-type__header {
    display: flex;
    align-items: center;
    gap: 12px;
}

.component-type__header > span:first-child {
    font-weight: 600;
}

.component-type__hint {
    flex: 1;
    font-size: 12px;
    color: #909399;
}

.component-type__name {
    font-weight: 600;
}
</style>
