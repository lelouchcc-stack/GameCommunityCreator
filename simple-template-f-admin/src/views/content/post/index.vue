<template>
    <div class="post-page">
        <el-card class="post-page__filter" shadow="never">
            <el-form :model="query" inline @submit.prevent>
                <el-form-item label="状态">
                    <el-select v-model="query.status" class="post-page__select" placeholder="全部" clearable>
                        <el-option label="待审核" :value="0" />
                        <el-option label="已发布" :value="1" />
                        <el-option label="已下架" :value="2" />
                    </el-select>
                </el-form-item>
                <el-form-item label="标题">
                    <el-input
                        v-model.trim="query.keyword"
                        class="post-page__input"
                        placeholder="按标题模糊搜索"
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
            <el-table v-loading="loading" :data="rows" border>
                <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
                <el-table-column prop="boardName" label="板块" width="120" show-overflow-tooltip />
                <el-table-column prop="authorName" label="作者" width="110" show-overflow-tooltip />
                <el-table-column label="关联版本 / 阶段" width="180">
                    <template #default="{ row }">
                        <span>{{ row.versionNo ?? "通用" }}</span>
                        <span v-if="row.stageName" class="post-page__muted"> / {{ row.stageName }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="提交时间" width="170" />
                <el-table-column label="操作" width="230" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                        <el-button
                            v-if="row.status === 0"
                            link
                            type="success"
                            @click="handleApprove(row)"
                        >
                            通过
                        </el-button>
                        <el-button v-if="row.status !== 2" link type="danger" @click="openOffline(row)">
                            下架
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-pagination
                v-model:current-page="query.pageNum"
                v-model:page-size="query.pageSize"
                class="post-page__pager"
                background
                layout="total, sizes, prev, pager, next"
                :total="total"
                :page-sizes="[10, 20, 50]"
                @current-change="loadList"
                @size-change="handleSearch"
            />
        </el-card>

        <el-dialog v-model="detailVisible" title="帖子详情" width="680px">
            <el-descriptions :column="1" border>
                <el-descriptions-item label="标题">{{ detail?.title }}</el-descriptions-item>
                <el-descriptions-item label="板块 / 作者">
                    {{ detail?.boardName ?? "—" }} / {{ detail?.authorName ?? "—" }}
                </el-descriptions-item>
                <el-descriptions-item label="关联版本 / 阶段">
                    {{ detail?.versionNo ?? "通用帖" }}
                    <span v-if="detail?.stageName"> / {{ detail.stageName }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="状态">
                    <el-tag :type="statusTag(detail?.status ?? 0)" size="small">
                        {{ statusText(detail?.status ?? 0) }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="提交时间">{{ detail?.createTime ?? "—" }}</el-descriptions-item>
                <el-descriptions-item label="正文">
                    <pre class="post-page__pre">{{ detail?.content || "（无正文）" }}</pre>
                </el-descriptions-item>
                <el-descriptions-item v-if="detail?.auditRemark" label="审核意见">
                    <pre class="post-page__pre post-page__pre--error">{{ detail.auditRemark }}</pre>
                </el-descriptions-item>
            </el-descriptions>
        </el-dialog>

        <el-dialog v-model="offlineVisible" :title="`下架帖子 - ${offlineTitle}`" width="480px" @closed="reason = ''">
            <el-alert
                class="post-page__hint"
                type="warning"
                :closable="false"
                title="下架原因会记录在帖子上，玩家可以看到，请写清楚理由。"
            />
            <el-input v-model="reason" type="textarea" :rows="3" maxlength="200" show-word-limit />
            <template #footer>
                <el-button @click="offlineVisible = false">取消</el-button>
                <el-button type="danger" :loading="submitting" @click="handleOffline">确定下架</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { auditPost, pagePosts } from "../../../api/post-audit";
import type { PostAuditItem, PostAuditQuery } from "../../../types/post-audit";

const STATUS_PUBLISHED = 1;
const STATUS_OFFLINE = 2;

const loading = ref(false);
const submitting = ref(false);
const rows = ref<PostAuditItem[]>([]);
const total = ref(0);

const detailVisible = ref(false);
const detail = ref<PostAuditItem | null>(null);

const offlineVisible = ref(false);
const offlineId = ref<number | null>(null);
const offlineTitle = ref("");
const reason = ref("");

const query = reactive<PostAuditQuery>({ pageNum: 1, pageSize: 10, status: 0, keyword: "" });

function statusText(status: number) {
    if (status === 0) {
        return "待审核";
    }
    return status === STATUS_PUBLISHED ? "已发布" : "已下架";
}

function statusTag(status: number): "warning" | "success" | "info" {
    if (status === 0) {
        return "warning";
    }
    return status === STATUS_PUBLISHED ? "success" : "info";
}

async function loadList() {
    loading.value = true;
    try {
        const data = await pagePosts(query);
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
    query.status = 0;
    query.keyword = "";
    handleSearch();
}

function openDetail(row: PostAuditItem) {
    detail.value = row;
    detailVisible.value = true;
}

async function handleApprove(row: PostAuditItem) {
    const confirmed = await ElMessageBox.confirm(`确定让「${row.title}」通过审核并发布吗？`, "提示", {
        type: "warning",
    }).catch(() => false);
    if (!confirmed) {
        return;
    }
    try {
        await auditPost(row.id, STATUS_PUBLISHED, "");
        ElMessage.success("已通过审核");
    } catch {
        // 拦截器已提示，例如"该帖子已被处理"
    }
    await loadList();
}

function openOffline(row: PostAuditItem) {
    offlineId.value = row.id;
    offlineTitle.value = row.title;
    offlineVisible.value = true;
}

async function handleOffline() {
    if (offlineId.value === null) {
        return;
    }
    if (!reason.value.trim()) {
        ElMessage.warning("请填写下架原因");
        return;
    }
    submitting.value = true;
    try {
        await auditPost(offlineId.value, STATUS_OFFLINE, reason.value.trim());
        ElMessage.success("已下架");
        offlineVisible.value = false;
    } catch {
        // 拦截器已提示
    } finally {
        submitting.value = false;
    }
    await loadList();
}

onMounted(loadList);
</script>

<style scoped>
.post-page__filter {
    margin-bottom: 16px;
}

.post-page__input {
    width: 200px;
}

.post-page__select {
    width: 140px;
}

.post-page__pager {
    margin-top: 16px;
    justify-content: flex-end;
}

.post-page__hint {
    margin-bottom: 12px;
}

.post-page__pre {
    max-height: 240px;
    margin: 0;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-all;
    font-size: 13px;
}

.post-page__pre--error {
    color: #e6a23c;
}

.post-page__muted {
    color: #909399;
}
</style>
