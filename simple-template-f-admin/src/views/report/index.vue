<template>
    <div class="report-page">
        <el-card class="report-page__filter" shadow="never">
            <el-form :model="query" inline @submit.prevent>
                <el-form-item label="状态">
                    <el-select v-model="query.status" class="report-page__select" placeholder="全部" clearable>
                        <el-option label="待处理" :value="0" />
                        <el-option label="已处理" :value="1" />
                        <el-option label="已驳回" :value="2" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <el-card shadow="never">
            <el-table v-loading="loading" :data="rows" border>
                <el-table-column prop="postTitle" label="被举报帖子" min-width="200" show-overflow-tooltip />
                <el-table-column prop="reporterName" label="举报人" width="110" show-overflow-tooltip />
                <el-table-column prop="reason" label="举报原因" width="120" show-overflow-tooltip />
                <el-table-column prop="description" label="补充说明" min-width="180" show-overflow-tooltip />
                <el-table-column label="帖子状态" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag :type="postStatusTag(row.postStatus)" size="small">
                            {{ postStatusText(row.postStatus) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="举报状态" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="handleUserName" label="处理人" width="110" show-overflow-tooltip />
                <el-table-column prop="handleTime" label="处理时间" width="170" />
                <el-table-column label="操作" width="180" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                        <el-button
                            v-if="row.status === 0"
                            link
                            type="primary"
                            @click="openHandle(row, 1)"
                        >
                            处理
                        </el-button>
                        <el-button v-if="row.status === 0" link type="danger" @click="openHandle(row, 2)">
                            驳回
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-pagination
                v-model:current-page="query.pageNum"
                v-model:page-size="query.pageSize"
                class="report-page__pager"
                background
                layout="total, sizes, prev, pager, next"
                :total="total"
                :page-sizes="[10, 20, 50]"
                @current-change="loadList"
                @size-change="handleSearch"
            />
        </el-card>

        <el-dialog v-model="detailVisible" title="举报详情" width="680px">
            <el-descriptions :column="1" border>
                <el-descriptions-item label="被举报帖子">{{ detail?.postTitle ?? "—" }}</el-descriptions-item>
                <el-descriptions-item label="帖子当前状态">
                    <el-tag :type="postStatusTag(detail?.postStatus ?? null)" size="small">
                        {{ postStatusText(detail?.postStatus ?? null) }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="帖子正文">
                    <pre class="report-page__pre">{{ detail?.postContent || "（无正文）" }}</pre>
                </el-descriptions-item>
                <el-descriptions-item label="举报人 / 原因">
                    {{ detail?.reporterName ?? "—" }} / {{ detail?.reason ?? "—" }}
                </el-descriptions-item>
                <el-descriptions-item label="补充说明">{{ detail?.description || "—" }}</el-descriptions-item>
                <el-descriptions-item label="举报时间">{{ detail?.createTime ?? "—" }}</el-descriptions-item>
                <el-descriptions-item label="处理结果">
                    <el-tag :type="statusTag(detail?.status ?? 0)" size="small">
                        {{ statusText(detail?.status ?? 0) }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item v-if="detail?.handleUserName" label="处理人 / 时间">
                    {{ detail.handleUserName }} / {{ detail.handleTime }}
                </el-descriptions-item>
                <el-descriptions-item v-if="detail?.handleRemark" label="处理说明">
                    <pre class="report-page__pre">{{ detail.handleRemark }}</pre>
                </el-descriptions-item>
            </el-descriptions>
        </el-dialog>

        <el-dialog v-model="handleVisible" :title="handleTitle" width="520px" @closed="handleDialogClosed">
            <el-form label-width="110px">
                <el-form-item label="处理结果">
                    <el-radio-group v-model="handleForm.status">
                        <el-radio :value="1">已处理</el-radio>
                        <el-radio :value="2">已驳回</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item v-if="handleForm.status === 1" label="同时下架帖子">
                    <el-switch v-model="handleForm.offlinePost" />
                    <span class="report-page__tip">开启后会一并把被举报的帖子置为已下架</span>
                </el-form-item>
                <el-form-item :label="handleForm.status === 2 ? '驳回说明' : '处理说明'">
                    <el-input
                        v-model="handleForm.remark"
                        type="textarea"
                        :rows="3"
                        maxlength="500"
                        show-word-limit
                        :placeholder="remarkPlaceholder"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="handleVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { handleReport, pageReports } from "../../api/report";
import type { ReportItem, ReportQuery } from "../../types/report";

const STATUS_PENDING = 0;
const STATUS_HANDLED = 1;

const loading = ref(false);
const submitting = ref(false);
const rows = ref<ReportItem[]>([]);
const total = ref(0);

const detailVisible = ref(false);
const detail = ref<ReportItem | null>(null);

const handleVisible = ref(false);
const handleId = ref<number | null>(null);
const handleForm = reactive({ status: STATUS_HANDLED, remark: "", offlinePost: false });

const query = reactive<ReportQuery>({ pageNum: 1, pageSize: 10, status: STATUS_PENDING });

const handleTitle = computed(() => (handleForm.status === 2 ? "驳回举报" : "处理举报"));

const remarkPlaceholder = computed(() => {
    if (handleForm.status === 2) {
        return "驳回必须填写说明，会记录在举报单上";
    }
    return handleForm.offlinePost ? "要同时下架帖子，说明必填（会写进帖子的下架原因）" : "可选";
});

function statusText(status: number) {
    if (status === STATUS_PENDING) {
        return "待处理";
    }
    return status === STATUS_HANDLED ? "已处理" : "已驳回";
}

function statusTag(status: number): "warning" | "success" | "info" {
    if (status === STATUS_PENDING) {
        return "warning";
    }
    return status === STATUS_HANDLED ? "success" : "info";
}

function postStatusText(status: number | null) {
    if (status === null) {
        return "已删除";
    }
    if (status === 0) {
        return "待审核";
    }
    return status === 1 ? "已发布" : "已下架";
}

function postStatusTag(status: number | null): "warning" | "success" | "info" | "danger" {
    if (status === null) {
        return "danger";
    }
    if (status === 0) {
        return "warning";
    }
    return status === 1 ? "success" : "info";
}

async function loadList() {
    loading.value = true;
    try {
        const data = await pageReports(query);
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
    query.status = STATUS_PENDING;
    handleSearch();
}

function openDetail(row: ReportItem) {
    detail.value = row;
    detailVisible.value = true;
}

function openHandle(row: ReportItem, status: number) {
    handleId.value = row.id;
    handleForm.status = status;
    handleForm.remark = "";
    handleForm.offlinePost = false;
    handleVisible.value = true;
}

function handleDialogClosed() {
    handleForm.remark = "";
    handleForm.offlinePost = false;
}

async function handleSubmit() {
    if (handleId.value === null) {
        return;
    }
    if (handleForm.status === 2 && !handleForm.remark.trim()) {
        ElMessage.warning("驳回举报必须填写驳回说明");
        return;
    }
    if (handleForm.offlinePost && !handleForm.remark.trim()) {
        ElMessage.warning("同时下架帖子时必须填写处理说明");
        return;
    }

    submitting.value = true;
    try {
        await handleReport(
            handleId.value,
            handleForm.status,
            handleForm.remark.trim(),
            handleForm.status === 1 && handleForm.offlinePost,
        );
        ElMessage.success(handleForm.status === 2 ? "已驳回" : "已处理");
        handleVisible.value = false;
    } catch {
        // 拦截器已提示，例如"该举报已被处理"
    } finally {
        submitting.value = false;
    }
    await loadList();
}

onMounted(loadList);
</script>

<style scoped>
.report-page__filter {
    margin-bottom: 16px;
}

.report-page__select {
    width: 140px;
}

.report-page__pager {
    margin-top: 16px;
    justify-content: flex-end;
}

.report-page__pre {
    max-height: 220px;
    margin: 0;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-all;
    font-size: 13px;
}

.report-page__tip {
    margin-left: 12px;
    color: #909399;
    font-size: 13px;
}
</style>
