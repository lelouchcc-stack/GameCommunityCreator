<template>
    <div class="board-page">
        <el-card class="board-page__filter" shadow="never">
            <el-form :model="query" inline @submit.prevent>
                <el-form-item label="板块名称">
                    <el-input
                        v-model.trim="query.name"
                        class="board-page__input"
                        placeholder="请输入板块名称"
                        clearable
                        @keyup.enter="handleSearch"
                    />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="query.status" class="board-page__select" placeholder="全部" clearable>
                        <el-option label="开放" :value="1" />
                        <el-option label="关闭" :value="0" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <el-card shadow="never">
            <div class="board-page__toolbar">
                <el-button type="primary" @click="openCreate">新增板块</el-button>
            </div>

            <el-table v-loading="loading" :data="rows" border>
                <el-table-column prop="name" label="板块名称" min-width="140" show-overflow-tooltip />
                <el-table-column prop="intro" label="简介" min-width="220" show-overflow-tooltip />
                <el-table-column label="当前版本" width="110" align="center">
                    <template #default="{ row }">
                        <span v-if="row.currentVersionNo">{{ row.currentVersionNo }}</span>
                        <span v-else class="board-page__muted">未设置</span>
                    </template>
                </el-table-column>
                <el-table-column prop="sort" label="排序" width="80" align="center" />
                <el-table-column label="状态" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                            {{ row.status === 1 ? "开放" : "关闭" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="170" />
                <el-table-column label="操作" width="200" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="primary" @click="handleToggleStatus(row)">
                            {{ row.status === 1 ? "关闭" : "开放" }}
                        </el-button>
                        <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-pagination
                v-model:current-page="query.pageNum"
                v-model:page-size="query.pageSize"
                class="board-page__pager"
                background
                layout="total, sizes, prev, pager, next"
                :total="total"
                :page-sizes="[10, 20, 50]"
                @current-change="loadList"
                @size-change="handleSearch"
            />
        </el-card>

        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @closed="handleDialogClosed">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="板块名称" prop="name">
                    <el-input v-model.trim="form.name" maxlength="50" show-word-limit placeholder="例如：示例游戏" />
                </el-form-item>
                <el-form-item label="简介" prop="intro">
                    <el-input v-model="form.intro" type="textarea" :rows="3" maxlength="500" show-word-limit />
                </el-form-item>
                <el-form-item label="封面" prop="cover">
                    <ImageUpload v-model="form.cover" />
                </el-form-item>
                <el-form-item label="排序" prop="sort">
                    <el-input-number v-model="form.sort" :min="0" :max="9999" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import ImageUpload from "../../../components/ImageUpload.vue";
import {
    changeBoardStatus,
    createBoard,
    deleteBoard,
    pageBoards,
    updateBoard,
} from "../../../api/board";
import type { Board, BoardForm, BoardQuery } from "../../../types/board";

const loading = ref(false);
const submitting = ref(false);
const rows = ref<Board[]>([]);
const total = ref(0);

const query = reactive<BoardQuery>({ pageNum: 1, pageSize: 10, name: "", status: undefined });

const dialogVisible = ref(false);
const dialogTitle = ref("新增板块");
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<BoardForm>({ name: "", intro: "", cover: "", sort: 0 });

const rules: FormRules = {
    name: [{ required: true, message: "请输入板块名称", trigger: "blur" }],
};

async function loadList() {
    loading.value = true;
    try {
        const data = await pageBoards(query);
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
    query.name = "";
    query.status = undefined;
    handleSearch();
}

function openCreate() {
    editingId.value = null;
    dialogTitle.value = "新增板块";
    dialogVisible.value = true;
}

function openEdit(row: Board) {
    editingId.value = row.id;
    dialogTitle.value = "编辑板块";
    form.name = row.name;
    form.intro = row.intro ?? "";
    form.cover = row.cover ?? "";
    form.sort = row.sort;
    dialogVisible.value = true;
}

function handleDialogClosed() {
    formRef.value?.clearValidate();
    form.name = "";
    form.intro = "";
    form.cover = "";
    form.sort = 0;
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
            await createBoard(form);
            ElMessage.success("新增成功");
        } else {
            await updateBoard(editingId.value, form);
            ElMessage.success("保存成功");
        }
        dialogVisible.value = false;
        await loadList();
    } catch {
        // 失败原因由请求拦截器统一提示
    } finally {
        submitting.value = false;
    }
}

async function handleToggleStatus(row: Board) {
    const next = row.status === 1 ? 0 : 1;
    const action = next === 1 ? "开放" : "关闭";
    const confirmed = await ElMessageBox.confirm(`确定要${action}板块「${row.name}」吗？`, "提示", {
        type: "warning",
    }).catch(() => false);
    if (!confirmed) {
        return;
    }
    await changeBoardStatus(row.id, next);
    ElMessage.success(`已${action}`);
    await loadList();
}

async function handleDelete(row: Board) {
    const confirmed = await ElMessageBox.confirm(
        `确定要删除板块「${row.name}」吗？板块下存在帖子时会被拒绝。`,
        "提示",
        { type: "warning" },
    ).catch(() => false);
    if (!confirmed) {
        return;
    }
    try {
        await deleteBoard(row.id);
        ElMessage.success("删除成功");
    } catch {
        // 拦截器已提示具体原因，例如"该板块下还有 6 篇帖子，无法删除"
    }
    await loadList();
}

onMounted(loadList);
</script>

<style scoped>
.board-page__filter {
    margin-bottom: 16px;
}

.board-page__input {
    width: 200px;
}

.board-page__select {
    width: 140px;
}

.board-page__toolbar {
    margin-bottom: 12px;
}

.board-page__pager {
    margin-top: 16px;
    justify-content: flex-end;
}

.board-page__muted {
    color: #c0c4cc;
}
</style>
