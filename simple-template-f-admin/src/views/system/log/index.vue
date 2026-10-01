<template>
    <div class="log-page">
        <el-card class="log-page__filter" shadow="never">
            <el-form :model="query" inline @submit.prevent>
                <el-form-item label="操作人">
                    <el-input
                        v-model.trim="query.username"
                        class="log-page__input"
                        placeholder="后台账号用户名"
                        clearable
                        @keyup.enter="handleSearch"
                    />
                </el-form-item>
                <el-form-item label="模块">
                    <el-input
                        v-model.trim="query.module"
                        class="log-page__input"
                        placeholder="如 板块管理"
                        clearable
                        @keyup.enter="handleSearch"
                    />
                </el-form-item>
                <el-form-item label="时间范围">
                    <el-date-picker
                        v-model="timeRange"
                        type="datetimerange"
                        range-separator="至"
                        start-placeholder="开始时间"
                        end-placeholder="结束时间"
                        value-format="YYYY-MM-DD HH:mm:ss"
                    />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">查询</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <el-card shadow="never">
            <div class="log-page__toolbar">
                <el-button type="danger" plain :disabled="selectedIds.length === 0" @click="handleDeleteBatch">
                    删除选中
                </el-button>
                <span class="log-page__tip">已选 {{ selectedIds.length }} 条</span>
            </div>

            <el-table
                ref="tableRef"
                v-loading="loading"
                :data="rows"
                border
                @selection-change="handleSelectionChange"
            >
                <el-table-column type="selection" width="48" />
                <el-table-column prop="username" label="操作人" width="110" />
                <el-table-column prop="module" label="模块" width="110" />
                <el-table-column prop="operation" label="操作描述" min-width="130" show-overflow-tooltip />
                <el-table-column prop="method" label="请求方法" min-width="180" show-overflow-tooltip />
                <el-table-column prop="ip" label="IP" width="130" />
                <el-table-column label="耗时" width="90" align="right">
                    <template #default="{ row }">{{ row.costTime }} ms</template>
                </el-table-column>
                <el-table-column label="结果" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                            {{ row.status === 1 ? "成功" : "失败" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="操作时间" width="170" />
                <el-table-column label="操作" width="90" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-pagination
                v-model:current-page="query.pageNum"
                v-model:page-size="query.pageSize"
                class="log-page__pager"
                background
                layout="total, sizes, prev, pager, next"
                :total="total"
                :page-sizes="[10, 20, 50]"
                @current-change="loadList"
                @size-change="handleSearch"
            />
        </el-card>

        <el-dialog v-model="detailVisible" title="操作日志详情" width="660px">
            <el-descriptions :column="1" border>
                <el-descriptions-item label="操作人">{{ detail?.username ?? "—" }}</el-descriptions-item>
                <el-descriptions-item label="模块">{{ detail?.module ?? "—" }}</el-descriptions-item>
                <el-descriptions-item label="操作描述">{{ detail?.operation ?? "—" }}</el-descriptions-item>
                <el-descriptions-item label="请求方法">{{ detail?.method ?? "—" }}</el-descriptions-item>
                <el-descriptions-item label="IP">{{ detail?.ip ?? "—" }}</el-descriptions-item>
                <el-descriptions-item label="耗时">{{ detail?.costTime ?? "—" }} ms</el-descriptions-item>
                <el-descriptions-item label="执行结果">
                    <el-tag :type="detail?.status === 1 ? 'success' : 'danger'" size="small">
                        {{ detail?.status === 1 ? "成功" : "失败" }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="操作时间">{{ detail?.createTime ?? "—" }}</el-descriptions-item>
                <el-descriptions-item label="请求参数">
                    <pre class="log-page__pre">{{ detail?.params || "—" }}</pre>
                </el-descriptions-item>
                <el-descriptions-item v-if="detail?.errorMsg" label="错误信息">
                    <pre class="log-page__pre log-page__pre--error">{{ detail.errorMsg }}</pre>
                </el-descriptions-item>
            </el-descriptions>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { deleteOperLogs, pageOperLogs } from "../../../api/oper-log";
import type { OperLogQuery, SysOperLog } from "../../../types/oper-log";

const loading = ref(false);
const rows = ref<SysOperLog[]>([]);
const total = ref(0);
const tableRef = ref();
const selectedIds = ref<number[]>([]);
const timeRange = ref<[string, string] | null>(null);

const detailVisible = ref(false);
const detail = ref<SysOperLog | null>(null);

const query = reactive<OperLogQuery>({
    pageNum: 1,
    pageSize: 10,
    username: "",
    module: "",
    beginTime: undefined,
    endTime: undefined,
});

async function loadList() {
    loading.value = true;
    try {
        const data = await pageOperLogs(query);
        rows.value = data.list;
        total.value = data.total;
    } finally {
        loading.value = false;
    }
}

function handleSearch() {
    query.pageNum = 1;
    query.beginTime = timeRange.value?.[0];
    query.endTime = timeRange.value?.[1];
    void loadList();
}

function handleReset() {
    query.username = "";
    query.module = "";
    timeRange.value = null;
    handleSearch();
}

function handleSelectionChange(selection: SysOperLog[]) {
    selectedIds.value = selection.map((item) => item.id);
}

function openDetail(row: SysOperLog) {
    detail.value = row;
    detailVisible.value = true;
}

async function handleDeleteBatch() {
    const confirmed = await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedIds.value.length} 条日志吗？此操作不可恢复。`,
        "提示",
        { type: "warning" },
    ).catch(() => false);
    if (!confirmed) {
        return;
    }

    try {
        await deleteOperLogs(selectedIds.value);
        ElMessage.success("删除成功");
        tableRef.value?.clearSelection();
    } catch {
        // 失败原因由请求拦截器统一提示
    }
    await loadList();
}

onMounted(loadList);
</script>

<style scoped>
.log-page__filter {
    margin-bottom: 16px;
}

.log-page__input {
    width: 170px;
}

.log-page__toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
}

.log-page__tip {
    color: #909399;
    font-size: 13px;
}

.log-page__pager {
    margin-top: 16px;
    justify-content: flex-end;
}

.log-page__pre {
    max-height: 200px;
    margin: 0;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-all;
    font-size: 12px;
}

.log-page__pre--error {
    color: #f56c6c;
}
</style>
