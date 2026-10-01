<template>
    <div class="user-page">
        <el-card class="user-page__filter" shadow="never">
            <el-form :model="query" inline @submit.prevent>
                <el-form-item label="用户名">
                    <el-input
                        v-model.trim="query.username"
                        class="user-page__input"
                        placeholder="请输入用户名"
                        clearable
                        @keyup.enter="handleSearch"
                    />
                </el-form-item>
                <el-form-item label="手机号">
                    <el-input
                        v-model.trim="query.phone"
                        class="user-page__input"
                        placeholder="请输入手机号"
                        clearable
                        @keyup.enter="handleSearch"
                    />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="query.status" class="user-page__select" placeholder="全部" clearable>
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
            <div class="user-page__toolbar">
                <el-button type="primary" @click="openCreate">新增账号</el-button>
            </div>

            <el-table v-loading="loading" :data="rows" border>
                <el-table-column prop="username" label="用户名" min-width="130" show-overflow-tooltip />
                <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
                <el-table-column prop="phone" label="手机号" width="140" />
                <el-table-column label="角色" min-width="160" show-overflow-tooltip>
                    <template #default="{ row }">
                        <span v-if="row.roleNames">{{ row.roleNames }}</span>
                        <span v-else class="user-page__muted">未分配</span>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                            {{ row.status === 1 ? "正常" : "停用" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="170" />
                <el-table-column label="操作" width="280" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="primary" @click="handleToggleStatus(row)">
                            {{ row.status === 1 ? "停用" : "启用" }}
                        </el-button>
                        <el-button link type="primary" @click="openResetPassword(row)">重置密码</el-button>
                        <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-pagination
                v-model:current-page="query.pageNum"
                v-model:page-size="query.pageSize"
                class="user-page__pager"
                background
                layout="total, sizes, prev, pager, next"
                :total="total"
                :page-sizes="[10, 20, 50]"
                @current-change="loadList"
                @size-change="handleSearch"
            />
        </el-card>

        <el-dialog v-model="formVisible" :title="formTitle" width="560px" @closed="handleFormClosed">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model.trim="form.username" maxlength="50" show-word-limit />
                </el-form-item>
                <el-form-item v-if="editingId === null" label="初始密码" prop="password">
                    <el-input
                        v-model="form.password"
                        type="password"
                        show-password
                        maxlength="32"
                        placeholder="6-32 位"
                    />
                </el-form-item>
                <el-form-item label="昵称" prop="nickname">
                    <el-input v-model.trim="form.nickname" maxlength="50" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                    <el-input v-model.trim="form.phone" maxlength="20" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                    <el-input v-model.trim="form.email" maxlength="50" />
                </el-form-item>
                <el-form-item label="角色" prop="roleIds">
                    <el-select v-model="form.roleIds" multiple placeholder="可多选" class="user-page__roles">
                        <el-option
                            v-for="role in roleList"
                            :key="role.id"
                            :label="role.roleName"
                            :value="role.id"
                        />
                    </el-select>
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

        <el-dialog v-model="pwdVisible" :title="`重置密码 - ${pwdUsername}`" width="420px" @closed="handlePwdClosed">
            <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
                <el-form-item label="新密码" prop="password">
                    <el-input v-model="pwdForm.password" type="password" show-password maxlength="32" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="pwdVisible = false">取消</el-button>
                <el-button type="primary" :loading="resetting" @click="handleResetPassword">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import {
    changeUserStatus,
    createUser,
    deleteUser,
    getUserRoleIds,
    pageUsers,
    resetUserPassword,
    updateUser,
} from "../../../api/admin-user";
import { roleOptions } from "../../../api/role";
import type { SysRole } from "../../../types/role";
import type { SysUser, SysUserForm, SysUserQuery } from "../../../types/admin-user";

const loading = ref(false);
const submitting = ref(false);
const resetting = ref(false);
const rows = ref<SysUser[]>([]);
const total = ref(0);
const roleList = ref<SysRole[]>([]);

const query = reactive<SysUserQuery>({ pageNum: 1, pageSize: 10, username: "", phone: "", status: undefined });

const formVisible = ref(false);
const formTitle = ref("新增账号");
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<SysUserForm>({
    username: "",
    nickname: "",
    phone: "",
    email: "",
    status: 1,
    remark: "",
    password: "",
    roleIds: [],
});

const rules: FormRules = {
    username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
    password: [
        { required: true, message: "请输入初始密码", trigger: "blur" },
        { min: 6, max: 32, message: "密码长度需为 6-32 位", trigger: "blur" },
    ],
};

const pwdVisible = ref(false);
const pwdUsername = ref("");
const pwdUserId = ref<number | null>(null);
const pwdFormRef = ref<FormInstance>();
const pwdForm = reactive({ password: "" });
const pwdRules: FormRules = {
    password: [
        { required: true, message: "请输入新密码", trigger: "blur" },
        { min: 6, max: 32, message: "密码长度需为 6-32 位", trigger: "blur" },
    ],
};

async function loadList() {
    loading.value = true;
    try {
        const data = await pageUsers(query);
        rows.value = data.list;
        total.value = data.total;
    } finally {
        loading.value = false;
    }
}

async function loadRoles() {
    if (roleList.value.length === 0) {
        roleList.value = await roleOptions();
    }
}

function handleSearch() {
    query.pageNum = 1;
    void loadList();
}

function handleReset() {
    query.username = "";
    query.phone = "";
    query.status = undefined;
    handleSearch();
}

async function openCreate() {
    editingId.value = null;
    formTitle.value = "新增账号";
    await loadRoles();
    formVisible.value = true;
}

async function openEdit(row: SysUser) {
    editingId.value = row.id;
    formTitle.value = "编辑账号";
    await loadRoles();
    form.username = row.username;
    form.nickname = row.nickname ?? "";
    form.phone = row.phone ?? "";
    form.email = row.email ?? "";
    form.status = row.status;
    form.remark = row.remark ?? "";
    form.password = "";
    form.roleIds = await getUserRoleIds(row.id);
    formVisible.value = true;
}

function handleFormClosed() {
    formRef.value?.clearValidate();
    form.username = "";
    form.nickname = "";
    form.phone = "";
    form.email = "";
    form.status = 1;
    form.remark = "";
    form.password = "";
    form.roleIds = [];
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
            await createUser(form);
            ElMessage.success("新增成功");
        } else {
            await updateUser(editingId.value, form);
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

async function handleToggleStatus(row: SysUser) {
    const next = row.status === 1 ? 0 : 1;
    const action = next === 1 ? "启用" : "停用";
    const confirmed = await ElMessageBox.confirm(`确定要${action}账号「${row.username}」吗？`, "提示", {
        type: "warning",
    }).catch(() => false);
    if (!confirmed) {
        return;
    }
    try {
        await changeUserStatus(row.id, next);
        ElMessage.success(`已${action}`);
    } catch {
        // 拦截器已提示，例如"系统管理员账号不可停用"
    }
    await loadList();
}

function openResetPassword(row: SysUser) {
    pwdUserId.value = row.id;
    pwdUsername.value = row.username;
    pwdVisible.value = true;
}

function handlePwdClosed() {
    pwdFormRef.value?.clearValidate();
    pwdForm.password = "";
}

async function handleResetPassword() {
    if (!pwdFormRef.value || pwdUserId.value === null) {
        return;
    }
    const valid = await pwdFormRef.value.validate().catch(() => false);
    if (!valid) {
        return;
    }

    resetting.value = true;
    try {
        await resetUserPassword(pwdUserId.value, pwdForm.password);
        ElMessage.success("密码已重置，该账号可用新密码登录");
        pwdVisible.value = false;
    } catch {
        // 拦截器已提示
    } finally {
        resetting.value = false;
    }
}

async function handleDelete(row: SysUser) {
    const confirmed = await ElMessageBox.confirm(
        `确定要删除账号「${row.username}」吗？此操作不可恢复。`,
        "提示",
        { type: "warning" },
    ).catch(() => false);
    if (!confirmed) {
        return;
    }
    try {
        await deleteUser(row.id);
        ElMessage.success("删除成功");
    } catch {
        // 拦截器已提示，例如"系统管理员不可删除"
    }
    await loadList();
}

onMounted(loadList);
</script>

<style scoped>
.user-page__filter {
    margin-bottom: 16px;
}

.user-page__input {
    width: 180px;
}

.user-page__select {
    width: 130px;
}

.user-page__toolbar {
    margin-bottom: 12px;
}

.user-page__pager {
    margin-top: 16px;
    justify-content: flex-end;
}

.user-page__roles {
    width: 100%;
}

.user-page__muted {
    color: #c0c4cc;
}
</style>
