<template>
    <div class="role-page">
        <el-card class="role-page__filter" shadow="never">
            <el-form :model="query" inline @submit.prevent>
                <el-form-item label="角色名称">
                    <el-input
                        v-model.trim="query.roleName"
                        class="role-page__input"
                        placeholder="请输入角色名称"
                        clearable
                        @keyup.enter="handleSearch"
                    />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="query.status" class="role-page__select" placeholder="全部" clearable>
                        <el-option label="正常" :value="1" />
                        <el-option label="停用" :value="0" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <el-card shadow="never">
            <div class="role-page__toolbar">
                <el-button type="primary" @click="openCreate">新增角色</el-button>
            </div>

            <el-table v-loading="loading" :data="rows" border>
                <el-table-column prop="roleName" label="角色名称" min-width="140" show-overflow-tooltip />
                <el-table-column prop="roleKey" label="角色标识" min-width="140" show-overflow-tooltip />
                <el-table-column prop="sort" label="排序" width="80" align="center" />
                <el-table-column label="状态" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                            {{ row.status === 1 ? "正常" : "停用" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
                <el-table-column prop="createTime" label="创建时间" width="170" />
                <el-table-column label="操作" width="220" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="primary" @click="openPermission(row)">分配权限</el-button>
                        <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-pagination
                v-model:current-page="query.pageNum"
                v-model:page-size="query.pageSize"
                class="role-page__pager"
                background
                layout="total, sizes, prev, pager, next"
                :total="total"
                :page-sizes="[10, 20, 50]"
                @current-change="loadList"
                @size-change="handleSearch"
            />
        </el-card>

        <el-dialog v-model="formVisible" :title="formTitle" width="520px" @closed="handleFormClosed">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="角色名称" prop="roleName">
                    <el-input v-model.trim="form.roleName" maxlength="50" show-word-limit />
                </el-form-item>
                <el-form-item label="角色标识" prop="roleKey">
                    <el-input v-model.trim="form.roleKey" maxlength="50" placeholder="如 admin、ops" />
                </el-form-item>
                <el-form-item label="排序" prop="sort">
                    <el-input-number v-model="form.sort" :min="0" :max="9999" />
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="form.status">
                        <el-radio :value="1">正常</el-radio>
                        <el-radio :value="0">停用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="formVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="permVisible" :title="`分配权限 - ${permRoleName}`" width="460px">
            <el-alert
                class="role-page__hint"
                type="info"
                :closable="false"
                title="不勾选任何菜单时也能保存，该角色下的账号登录后将看不到任何菜单。"
            />
            <el-tree
                ref="treeRef"
                :data="menuTree"
                :props="{ label: 'menuName', children: 'children' }"
                node-key="id"
                show-checkbox
                default-expand-all
                class="role-page__tree"
            >
                <template #default="{ data }">
                    <span>
                        {{ data.menuName }}
                        <el-tag v-if="data.menuType === 'F'" size="small" type="info">按钮</el-tag>
                    </span>
                </template>
            </el-tree>
            <template #footer>
                <el-button @click="permVisible = false">取消</el-button>
                <el-button type="primary" :loading="assigning" @click="handleAssign">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { nextTick, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { assignRoleMenus, createRole, deleteRole, getRoleMenuIds, pageRoles, updateRole } from "../../../api/role";
import { getMenuOptions } from "../../../api/menu";
import type { MenuNode } from "../../../types/menu";
import type { RoleQuery, SysRole, SysRoleForm } from "../../../types/role";

const loading = ref(false);
const submitting = ref(false);
const assigning = ref(false);
const rows = ref<SysRole[]>([]);
const total = ref(0);

const query = reactive<RoleQuery>({ pageNum: 1, pageSize: 10, roleName: "", status: undefined });

const formVisible = ref(false);
const formTitle = ref("新增角色");
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<SysRoleForm>({ roleName: "", roleKey: "", sort: 0, status: 1, remark: "" });

const rules: FormRules = {
    roleName: [{ required: true, message: "请输入角色名称", trigger: "blur" }],
    roleKey: [{ required: true, message: "请输入角色标识", trigger: "blur" }],
};

const permVisible = ref(false);
const permRoleId = ref<number | null>(null);
const permRoleName = ref("");
const menuTree = ref<MenuNode[]>([]);
const treeRef = ref();

async function loadList() {
    loading.value = true;
    try {
        const data = await pageRoles(query);
        rows.value = data.list;
        total.value = data.total;
    } finally {
        loading.value = false;
    }
}

function handleSearch() {
    query.pageNum = 1;
    void loadList();
}

function handleReset() {
    query.roleName = "";
    query.status = undefined;
    handleSearch();
}

function openCreate() {
    editingId.value = null;
    formTitle.value = "新增角色";
    formVisible.value = true;
}

function openEdit(row: SysRole) {
    editingId.value = row.id;
    formTitle.value = "编辑角色";
    form.roleName = row.roleName;
    form.roleKey = row.roleKey;
    form.sort = row.sort;
    form.status = row.status;
    form.remark = row.remark ?? "";
    formVisible.value = true;
}

function handleFormClosed() {
    formRef.value?.clearValidate();
    form.roleName = "";
    form.roleKey = "";
    form.sort = 0;
    form.status = 1;
    form.remark = "";
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
            await createRole(form);
            ElMessage.success("新增成功");
        } else {
            await updateRole(editingId.value, form);
            ElMessage.success("保存成功");
        }
        formVisible.value = false;
        await loadList();
    } catch {
        // 失败原因由请求拦截器统一提示
    } finally {
        submitting.value = false;
    }
}

async function handleDelete(row: SysRole) {
    const confirmed = await ElMessageBox.confirm(
        `确定要删除角色「${row.roleName}」吗？已分配给账号时会被拒绝。`,
        "提示",
        { type: "warning" },
    ).catch(() => false);
    if (!confirmed) {
        return;
    }
    try {
        await deleteRole(row.id);
        ElMessage.success("删除成功");
    } catch {
        // 拦截器已提示原因，例如"该角色已分配给 1 个账号"
    }
    await loadList();
}

/** 收集所有非叶子节点 id：回显时只设置叶子节点，父节点由 el-tree 自己推导 checked/half-checked */
function collectParentIds(nodes: MenuNode[], result: Set<number>): Set<number> {
    for (const node of nodes) {
        const children = node.children ?? [];
        if (children.length > 0) {
            result.add(node.id);
            collectParentIds(children, result);
        }
    }
    return result;
}

async function openPermission(row: SysRole) {
    permRoleId.value = row.id;
    permRoleName.value = row.roleName;

    if (menuTree.value.length === 0) {
        menuTree.value = await getMenuOptions();
    }
    const assignedIds = await getRoleMenuIds(row.id);
    permVisible.value = true;

    const parentIds = collectParentIds(menuTree.value, new Set<number>());
    const leafIds = assignedIds.filter((id) => !parentIds.has(id));
    await nextTick();
    treeRef.value?.setCheckedKeys(leafIds);
}

async function handleAssign() {
    if (permRoleId.value === null) {
        return;
    }
    // 保存时要把半选中的父节点一起提交，否则后端拿不到上级目录、侧边栏会缺层级
    const checked: number[] = treeRef.value?.getCheckedKeys() ?? [];
    const halfChecked: number[] = treeRef.value?.getHalfCheckedKeys() ?? [];

    assigning.value = true;
    try {
        await assignRoleMenus(permRoleId.value, [...checked, ...halfChecked]);
        ElMessage.success("权限已保存");
        permVisible.value = false;
    } catch {
        // 拦截器已提示
    } finally {
        assigning.value = false;
    }
}

onMounted(loadList);
</script>

<style scoped>
.role-page__filter {
    margin-bottom: 16px;
}

.role-page__input {
    width: 200px;
}

.role-page__select {
    width: 140px;
}

.role-page__toolbar {
    margin-bottom: 12px;
}

.role-page__pager {
    margin-top: 16px;
    justify-content: flex-end;
}

.role-page__hint {
    margin-bottom: 12px;
}

.role-page__tree {
    max-height: 380px;
    overflow: auto;
}
</style>
