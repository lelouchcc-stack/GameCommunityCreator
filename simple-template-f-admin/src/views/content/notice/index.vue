<template>
    <div class="notice-page">
        <el-card class="notice-page__filter" shadow="never">
            <el-form :model="query" inline @submit.prevent>
                <el-form-item label="状态">
                    <el-select v-model="query.status" class="notice-page__small" placeholder="全部" clearable>
                        <el-option label="已发布" :value="1" />
                        <el-option label="草稿" :value="0" />
                    </el-select>
                </el-form-item>
                <el-form-item label="类型">
                    <el-select v-model="query.type" class="notice-page__small" placeholder="全部" clearable>
                        <el-option label="社区公告" :value="1" />
                        <el-option label="版本更新说明" :value="2" />
                    </el-select>
                </el-form-item>
                <el-form-item label="关键字">
                    <el-input
                        v-model.trim="query.keyword"
                        class="notice-page__input"
                        placeholder="按标题或正文搜索"
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
            <div class="notice-page__toolbar">
                <el-button type="primary" @click="openCreate">新增公告</el-button>
                <span class="notice-page__hint">
                    保存即生效：已发布的公告会立刻出现在用户端的公告页与首页公告栏，不需要额外的发布动作
                </span>
            </div>

            <el-table v-loading="loading" :data="rows" border>
                <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
                <el-table-column label="类型" width="130" align="center">
                    <template #default="{ row }">
                        <el-tag size="small" :type="row.type === 2 ? 'warning' : 'primary'">
                            {{ row.type === 2 ? "版本更新说明" : "社区公告" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="正文" min-width="200" show-overflow-tooltip>
                    <template #default="{ row }">
                        <span v-if="row.content">{{ excerpt(row.content) }}</span>
                        <span v-else class="notice-page__muted">（空）</span>
                    </template>
                </el-table-column>
                <el-table-column prop="sort" label="排序" width="80" align="center" />
                <el-table-column label="状态" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                            {{ row.status === 1 ? "已发布" : "草稿" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="updateTime" label="更新时间" width="170" />
                <el-table-column label="操作" width="220" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="primary" @click="handleToggleStatus(row)">
                            {{ row.status === 1 ? "转草稿" : "发布" }}
                        </el-button>
                        <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-pagination
                v-model:current-page="query.pageNum"
                v-model:page-size="query.pageSize"
                class="notice-page__pager"
                background
                layout="total, sizes, prev, pager, next"
                :total="total"
                :page-sizes="[10, 20, 50]"
                @current-change="loadList"
                @size-change="handleSearch"
            />
        </el-card>

        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" top="6vh" @closed="handleDialogClosed">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="标题" prop="title">
                    <el-input v-model.trim="form.title" maxlength="100" show-word-limit placeholder="例如：1.3.0 版本更新说明" />
                </el-form-item>
                <el-form-item label="类型" prop="type">
                    <el-radio-group v-model="form.type">
                        <el-radio :value="1">社区公告</el-radio>
                        <el-radio :value="2">版本更新说明</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="正文" prop="content">
                    <el-input
                        v-model="form.content"
                        type="textarea"
                        :rows="8"
                        maxlength="20000"
                        show-word-limit
                        placeholder="支持换行；用户端按纯文本原样展示，不解析 HTML"
                    />
                </el-form-item>
                <el-form-item label="排序" prop="sort">
                    <el-input-number v-model="form.sort" :min="0" :max="9999" />
                    <span class="notice-page__tip">越小越靠前（首页公告栏与公告列表都按它排）</span>
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="form.status">
                        <el-radio :value="1">直接发布</el-radio>
                        <el-radio :value="0">存为草稿</el-radio>
                    </el-radio-group>
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
/**
 * 公告管理。公告此前只有用户端只读接口，发公告只能改数据库，这里补上后台维护入口。
 * 保存即生效：已发布的公告立刻出现在用户端公告页与首页公告栏（草稿不下发）。
 */
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import {
    changeNoticeStatus,
    createNotice,
    deleteNotice,
    pageNoticesAdmin,
    updateNotice,
} from "../../../api/notice";
import type { Notice, NoticeForm, NoticeQuery } from "../../../types/notice";

const loading = ref(false);
const submitting = ref(false);
const rows = ref<Notice[]>([]);
const total = ref(0);

const query = reactive<NoticeQuery>({
    pageNum: 1,
    pageSize: 10,
    status: undefined,
    type: undefined,
    keyword: "",
});

const dialogVisible = ref(false);
const dialogTitle = ref("新增公告");
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<NoticeForm>({ title: "", content: "", type: 1, sort: 0, status: 1 });

const rules: FormRules = {
    title: [{ required: true, message: "请输入公告标题", trigger: "blur" }],
    type: [{ required: true, message: "请选择公告类型", trigger: "change" }],
};

onMounted(loadList);

async function loadList() {
    loading.value = true;
    try {
        const data = await pageNoticesAdmin(query);
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
    query.status = undefined;
    query.type = undefined;
    query.keyword = "";
    handleSearch();
}

function openCreate() {
    editingId.value = null;
    dialogTitle.value = "新增公告";
    dialogVisible.value = true;
}

function openEdit(row: Notice) {
    editingId.value = row.id;
    dialogTitle.value = "编辑公告";
    form.title = row.title;
    form.content = row.content ?? "";
    form.type = row.type;
    form.sort = row.sort ?? 0;
    form.status = row.status ?? 1;
    dialogVisible.value = true;
}

function handleDialogClosed() {
    formRef.value?.clearValidate();
    form.title = "";
    form.content = "";
    form.type = 1;
    form.sort = 0;
    form.status = 1;
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
            await createNotice({ ...form });
            ElMessage.success(form.status === 1 ? "公告已发布" : "草稿已保存");
        } else {
            await updateNotice(editingId.value, { ...form });
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

async function handleToggleStatus(row: Notice) {
    const next = row.status === 1 ? 0 : 1;
    try {
        await changeNoticeStatus(row.id, next);
        ElMessage.success(next === 1 ? "已发布，用户端刷新即可见" : "已转为草稿，用户端不再展示");
    } catch {
        // 拦截器已提示
    }
    await loadList();
}

async function handleDelete(row: Notice) {
    const confirmed = await ElMessageBox.confirm(
        `确定删除公告「${row.title}」吗？删除后用户端立即不再展示。`,
        "提示",
        { type: "warning" },
    ).catch(() => false);
    if (!confirmed) {
        return;
    }
    try {
        await deleteNotice(row.id);
        ElMessage.success("删除成功");
    } catch {
        // 拦截器已提示
    }
    await loadList();
}

/** 表格里只给一句摘要，正文可能很长 */
function excerpt(content: string) {
    const flat = content.replace(/\s+/g, " ").trim();
    return flat.length > 60 ? `${flat.slice(0, 60)}…` : flat;
}
</script>

<style scoped>
.notice-page__filter {
    margin-bottom: 16px;
}

.notice-page__input {
    width: 200px;
}

.notice-page__small {
    width: 140px;
}

.notice-page__toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
}

.notice-page__hint {
    color: #909399;
    font-size: 12px;
}

.notice-page__tip {
    margin-left: 12px;
    color: #909399;
    font-size: 12px;
}

.notice-page__muted {
    color: #c0c4cc;
}

.notice-page__pager {
    margin-top: 16px;
    justify-content: flex-end;
}
</style>
