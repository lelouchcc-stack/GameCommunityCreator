<template>
    <div class="menu-page">
        <el-card shadow="never">
            <div class="menu-page__toolbar">
                <el-button type="primary" @click="openCreate(0)">新增根节点</el-button>
                <span class="menu-page__tip">
                    三级结构：目录(M) → 菜单(C) → 按钮(F)。按钮节点不参与侧边栏，只用来配权限标识。
                </span>
            </div>

            <el-table
                v-loading="loading"
                :data="tree"
                row-key="id"
                :tree-props="{ children: 'children' }"
                default-expand-all
                border
            >
                <el-table-column prop="menuName" label="菜单名称" min-width="200" />
                <el-table-column label="类型" width="80" align="center">
                    <template #default="{ row }">
                        <el-tag :type="typeTag(row.menuType)" size="small">{{ typeText(row.menuType) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="path" label="路由地址" min-width="130" show-overflow-tooltip />
                <el-table-column prop="component" label="组件路径" min-width="170" show-overflow-tooltip />
                <el-table-column prop="perms" label="权限标识" min-width="150" show-overflow-tooltip />
                <el-table-column prop="icon" label="图标" width="120" show-overflow-tooltip />
                <el-table-column prop="sort" label="排序" width="70" align="center" />
                <el-table-column label="显示" width="80" align="center">
                    <template #default="{ row }">
                        <span v-if="row.menuType === 'F'" class="menu-page__muted">—</span>
                        <el-tag v-else :type="row.visible === 1 ? 'success' : 'info'" size="small">
                            {{ row.visible === 1 ? "显示" : "隐藏" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                            {{ row.status === 1 ? "正常" : "停用" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="240" fixed="right">
                    <template #default="{ row }">
                        <el-button
                            v-if="row.menuType !== 'F'"
                            link
                            type="primary"
                            @click="openCreate(row.id)"
                        >
                            新增子节点
                        </el-button>
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="formVisible" :title="formTitle" width="620px" @closed="handleFormClosed">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
                <el-form-item label="上级菜单">
                    <el-tree-select
                        v-model="form.parentId"
                        :data="parentOptions"
                        :props="{ label: 'menuName', children: 'children' }"
                        node-key="id"
                        check-strictly
                        default-expand-all
                        placeholder="选择根节点即为顶级目录"
                        class="menu-page__full"
                    />
                </el-form-item>
                <el-form-item label="菜单类型" prop="menuType">
                    <el-radio-group v-model="form.menuType">
                        <el-radio-button value="M">目录</el-radio-button>
                        <el-radio-button value="C">菜单</el-radio-button>
                        <el-radio-button value="F">按钮</el-radio-button>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="菜单名称" prop="menuName">
                    <el-input v-model.trim="form.menuName" maxlength="50" show-word-limit />
                </el-form-item>
                <el-form-item v-if="form.menuType !== 'F'" label="路由地址" prop="path">
                    <el-input
                        v-model.trim="form.path"
                        maxlength="200"
                        placeholder="目录用绝对路径如 /system，菜单用相对路径如 user"
                    />
                </el-form-item>
                <el-form-item v-if="form.menuType === 'C'" label="组件路径" prop="component">
                    <el-input
                        v-model.trim="form.component"
                        maxlength="255"
                        placeholder="相对 src/views 的路径，如 system/user/index"
                    />
                </el-form-item>
                <el-form-item v-if="form.menuType !== 'M'" label="权限标识" prop="perms">
                    <el-input v-model.trim="form.perms" maxlength="100" placeholder="如 system:user:list" />
                </el-form-item>
                <el-form-item v-if="form.menuType !== 'F'" label="图标" prop="icon">
                    <el-input v-model.trim="form.icon" maxlength="100" placeholder="Element Plus 图标名，如 User" />
                </el-form-item>
                <el-form-item label="排序" prop="sort">
                    <el-input-number v-model="form.sort" :min="0" :max="9999" />
                </el-form-item>
                <el-form-item v-if="form.menuType !== 'F'" label="显示" prop="visible">
                    <el-radio-group v-model="form.visible">
                        <el-radio :value="1">显示</el-radio>
                        <el-radio :value="0">隐藏</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="form.status">
                        <el-radio :value="1">正常</el-radio>
                        <el-radio :value="0">停用</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="formVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { createMenu, deleteMenu, getMenuOptions, updateMenu } from "../../../api/menu";
import type { MenuNode, SysMenuForm } from "../../../types/menu";

const loading = ref(false);
const submitting = ref(false);
const tree = ref<MenuNode[]>([]);

const formVisible = ref(false);
const formTitle = ref("新增菜单");
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<SysMenuForm>({
    parentId: 0,
    menuName: "",
    menuType: "C",
    path: "",
    component: "",
    perms: "",
    icon: "",
    sort: 0,
    visible: 1,
    status: 1,
});

const rules: FormRules = {
    menuName: [{ required: true, message: "请输入菜单名称", trigger: "blur" }],
    menuType: [{ required: true, message: "请选择菜单类型", trigger: "change" }],
};

const ROOT_OPTION: MenuNode = {
    id: 0,
    parentId: 0,
    menuName: "根节点",
    menuType: "M",
    path: null,
    component: null,
    perms: null,
    icon: null,
    sort: 0,
    visible: 1,
    status: 1,
    children: null,
};

/** 上级菜单候选：排除按钮节点，编辑时还要排除自己（后端也会再拦一次成环） */
function buildParentOptions(nodes: MenuNode[], excludeId: number | null): MenuNode[] {
    const result: MenuNode[] = [];
    for (const node of nodes) {
        if (node.menuType === "F" || node.id === excludeId) {
            continue;
        }
        result.push({ ...node, children: buildParentOptions(node.children ?? [], excludeId) });
    }
    return result;
}

const parentOptions = computed<MenuNode[]>(() => [
    ROOT_OPTION,
    ...buildParentOptions(tree.value, editingId.value),
]);

function typeText(type: string) {
    if (type === "M") {
        return "目录";
    }
    return type === "C" ? "菜单" : "按钮";
}

function typeTag(type: string): "primary" | "success" | "info" {
    if (type === "M") {
        return "primary";
    }
    return type === "C" ? "success" : "info";
}

async function loadTree() {
    loading.value = true;
    try {
        tree.value = await getMenuOptions();
    } finally {
        loading.value = false;
    }
}

function resetForm() {
    form.parentId = 0;
    form.menuName = "";
    form.menuType = "C";
    form.path = "";
    form.component = "";
    form.perms = "";
    form.icon = "";
    form.sort = 0;
    form.visible = 1;
    form.status = 1;
}

function openCreate(parentId: number) {
    editingId.value = null;
    formTitle.value = parentId === 0 ? "新增根节点" : "新增子节点";
    resetForm();
    form.parentId = parentId;
    formVisible.value = true;
}

function openEdit(row: MenuNode) {
    editingId.value = row.id;
    formTitle.value = "编辑菜单";
    form.parentId = row.parentId;
    form.menuName = row.menuName;
    form.menuType = row.menuType;
    form.path = row.path ?? "";
    form.component = row.component ?? "";
    form.perms = row.perms ?? "";
    form.icon = row.icon ?? "";
    form.sort = row.sort;
    form.visible = row.visible ?? 1;
    form.status = row.status ?? 1;
    formVisible.value = true;
}

function handleFormClosed() {
    formRef.value?.clearValidate();
    resetForm();
}

async function handleSubmit() {
    if (!formRef.value) {
        return;
    }
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid) {
        return;
    }

    submitting.value = true;
    try {
        if (editingId.value === null) {
            await createMenu(form);
            ElMessage.success("新增成功");
        } else {
            await updateMenu(editingId.value, form);
            ElMessage.success("保存成功");
        }
        formVisible.value = false;
        await loadTree();
    } catch {
        // 失败原因由请求拦截器统一提示
    } finally {
        submitting.value = false;
    }
}

async function handleDelete(row: MenuNode) {
    const confirmed = await ElMessageBox.confirm(
        `确定要删除「${row.menuName}」吗？有子节点或已被角色引用时会被拒绝。`,
        "提示",
        { type: "warning" },
    ).catch(() => false);
    if (!confirmed) {
        return;
    }
    try {
        await deleteMenu(row.id);
        ElMessage.success("删除成功");
    } catch {
        // 拦截器已提示原因
    }
    await loadTree();
}

onMounted(loadTree);
</script>

<style scoped>
.menu-page__toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
}

.menu-page__tip {
    color: #909399;
    font-size: 13px;
}

.menu-page__full {
    width: 100%;
}

.menu-page__muted {
    color: #c0c4cc;
}
</style>
