<template>
    <div class="tag-page">
        <el-card class="tag-page__filter" shadow="never">
            <el-form :model="query" inline @submit.prevent>
                <el-form-item label="所属板块">
                    <el-select v-model="query.boardId" class="tag-page__select" placeholder="全部" clearable>
                        <el-option
                            v-for="board in boards"
                            :key="board.id"
                            :label="board.name"
                            :value="board.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="类型">
                    <el-select v-model="query.tagType" class="tag-page__small" placeholder="全部" clearable>
                        <el-option label="内容类型" :value="1" />
                        <el-option label="主题" :value="2" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="query.status" class="tag-page__small" placeholder="全部" clearable>
                        <el-option label="正常" :value="1" />
                        <el-option label="停用" :value="0" />
                    </el-select>
                </el-form-item>
                <el-form-item label="标签名">
                    <el-input
                        v-model.trim="query.keyword"
                        class="tag-page__input"
                        placeholder="按标签名搜索"
                        clearable
                        @keyup.enter="handleSearch"
                    />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <el-card shadow="never">
            <div class="tag-page__toolbar">
                <el-button type="primary" @click="openCreate">新增标签</el-button>
            </div>

            <el-table v-loading="loading" :data="rows" border>
                <el-table-column prop="tagName" label="标签名" min-width="140" show-overflow-tooltip />
                <el-table-column label="所属板块" width="150" show-overflow-tooltip>
                    <template #default="{ row }">
                        <span v-if="row.boardName">{{ row.boardName }}</span>
                        <el-tag v-else size="small" type="info">全局</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="类型" width="110" align="center">
                    <template #default="{ row }">
                        {{ row.tagType === 1 ? "内容类型" : "主题" }}
                    </template>
                </el-table-column>
                <el-table-column prop="sort" label="排序" width="80" align="center" />
                <el-table-column label="状态" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                            {{ row.status === 1 ? "正常" : "停用" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="170" />
                <el-table-column label="操作" width="200" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="primary" @click="handleToggleStatus(row)">
                            {{ row.status === 1 ? "停用" : "启用" }}
                        </el-button>
                        <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-pagination
                v-model:current-page="query.pageNum"
                v-model:page-size="query.pageSize"
                class="tag-page__pager"
                background
                layout="total, sizes, prev, pager, next"
                :total="total"
                :page-sizes="[10, 20, 50]"
                @current-change="loadList"
                @size-change="handleSearch"
            />
        </el-card>

        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @closed="handleDialogClosed">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="所属板块" prop="boardId">
                    <el-select v-model="form.boardId" placeholder="不选则为全局标签" clearable style="width: 100%">
                        <el-option
                            v-for="board in boards"
                            :key="board.id"
                            :label="board.name"
                            :value="board.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="标签名" prop="tagName">
                    <el-input v-model.trim="form.tagName" maxlength="30" show-word-limit placeholder="例如：攻略" />
                </el-form-item>
                <el-form-item label="类型" prop="tagType">
                    <el-radio-group v-model="form.tagType">
                        <el-radio :value="1">内容类型</el-radio>
                        <el-radio :value="2">主题</el-radio>
                    </el-radio-group>
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
import { listBoards, type BoardItem } from "../../../api/board";
import { changeTagStatus, createTag, deleteTag, pageTags, updateTag } from "../../../api/tag";
import type { Tag, TagForm, TagQuery } from "../../../types/tag";

const boards = ref<BoardItem[]>([]);
const loading = ref(false);
const submitting = ref(false);
const rows = ref<Tag[]>([]);
const total = ref(0);

const query = reactive<TagQuery>({
    pageNum: 1,
    pageSize: 10,
    boardId: undefined,
    tagType: undefined,
    status: undefined,
    keyword: "",
});

const dialogVisible = ref(false);
const dialogTitle = ref("新增标签");
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<TagForm>({ boardId: null, tagName: "", tagType: 1, sort: 0 });

const rules: FormRules = {
    tagName: [{ required: true, message: "请输入标签名", trigger: "blur" }],
    tagType: [{ required: true, message: "请选择标签类型", trigger: "change" }],
};

onMounted(async () => {
    try {
        boards.value = await listBoards();
    } catch {
        boards.value = [];
    }
    await loadList();
});

async function loadList() {
    loading.value = true;
    try {
        const data = await pageTags(query);
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
    query.boardId = undefined;
    query.tagType = undefined;
    query.status = undefined;
    query.keyword = "";
    handleSearch();
}

function openCreate() {
    editingId.value = null;
    dialogTitle.value = "新增标签";
    dialogVisible.value = true;
}

function openEdit(row: Tag) {
    editingId.value = row.id;
    dialogTitle.value = "编辑标签";
    form.boardId = row.boardId;
    form.tagName = row.tagName;
    form.tagType = row.tagType;
    form.sort = row.sort;
    dialogVisible.value = true;
}

function handleDialogClosed() {
    formRef.value?.clearValidate();
    form.boardId = null;
    form.tagName = "";
    form.tagType = 1;
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
            await createTag(form);
            ElMessage.success("新增成功");
        } else {
            await updateTag(editingId.value, form);
            ElMessage.success("保存成功");
        }
        dialogVisible.value = false;
        await loadList();
    } catch {
        // 拦截器已提示，例如"标签名已存在"
    } finally {
        submitting.value = false;
    }
}

async function handleToggleStatus(row: Tag) {
    const next = row.status === 1 ? 0 : 1;
    try {
        await changeTagStatus(row.id, next);
        ElMessage.success(next === 1 ? "已启用" : "已停用");
    } catch {
        // 拦截器已提示
    }
    await loadList();
}

async function handleDelete(row: Tag) {
    const confirmed = await ElMessageBox.confirm(
        `确定删除标签「${row.tagName}」吗？被帖子引用的标签会被拒绝。`,
        "提示",
        { type: "warning" },
    ).catch(() => false);
    if (!confirmed) {
        return;
    }
    try {
        await deleteTag(row.id);
        ElMessage.success("删除成功");
    } catch {
        // 拦截器已提示
    }
    await loadList();
}
</script>

<style scoped>
.tag-page__filter {
    margin-bottom: 16px;
}

.tag-page__input {
    width: 180px;
}

.tag-page__select {
    width: 160px;
}

.tag-page__small {
    width: 120px;
}

.tag-page__toolbar {
    margin-bottom: 12px;
}

.tag-page__pager {
    margin-top: 16px;
    justify-content: flex-end;
}
</style>
