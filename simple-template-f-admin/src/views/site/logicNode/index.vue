<template>
    <div class="logic-node">
        <el-card shadow="never">
            <template #header>
                <div class="logic-node__header">
                    <span>原子库</span>
                    <span class="logic-node__hint">
                        逻辑图上能用的节点类型。原子由代码实现（封装后端接口或前端动作），管理端只读；
                        连线时按端口类型匹配，图只能编排这些能力，不能发明新能力
                    </span>
                </div>
            </template>

            <div class="logic-node__filter">
                <el-select v-model="kindFilter" placeholder="全部分组" clearable style="width: 160px">
                    <el-option v-for="(label, key) in KIND_LABELS" :key="key" :label="label" :value="key" />
                </el-select>
                <el-input v-model="keyword" placeholder="按名称 / 标识 / 接口搜索" clearable style="width: 280px" />
                <span class="logic-node__count">{{ filtered.length }} / {{ rows.length }} 个原子</span>
            </div>

            <el-table v-loading="loading" :data="filtered" border row-key="id">
                <el-table-column label="原子" min-width="210">
                    <template #default="{ row }">
                        <div class="logic-node__name">{{ row.typeName }}</div>
                        <el-tag size="small" type="info">{{ row.typeCode }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="分组" width="90" align="center">
                    <template #default="{ row }">{{ row.category || row.kind }}</template>
                </el-table-column>
                <el-table-column label="类型" width="80" align="center">
                    <template #default="{ row }">
                        <el-tag size="small" :type="row.runner === 'api' ? 'success' : 'warning'">
                            {{ row.runner === "api" ? "接口" : "前端" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="对应接口 / 说明" min-width="200">
                    <template #default="{ row }">
                        <span v-if="row.sourceApi" class="logic-node__api">{{ row.sourceApi }}</span>
                        <span v-else class="logic-node__muted">{{ row.remark || "—" }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="输入端口" min-width="220">
                    <template #default="{ row }">
                        <el-tag
                            v-for="port in row.inputs"
                            :key="port.key"
                            size="small"
                            class="logic-node__port"
                            :type="port.type === 'signal' ? 'info' : ''"
                        >
                            {{ port.key }}:{{ port.type }}{{ port.required ? "*" : "" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="输出端口" min-width="200">
                    <template #default="{ row }">
                        <el-tag v-for="port in row.outputs" :key="port.key" size="small" class="logic-node__port">
                            {{ port.key }}:{{ port.type }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="90" align="center">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <p class="logic-node__legend">端口写法 key:type，带 * 表示必填；signal 是控制流端口，其余是数据端口</p>
        </el-card>

        <el-dialog v-model="detailVisible" :title="current?.typeName" width="640px">
            <el-descriptions v-if="current" :column="2" border size="small">
                <el-descriptions-item label="标识">{{ current.typeCode }}</el-descriptions-item>
                <el-descriptions-item label="分组">{{ current.category || current.kind }}</el-descriptions-item>
                <el-descriptions-item label="执行方">
                    {{ current.runner === "api" ? "调后端接口" : "前端本地动作" }}
                </el-descriptions-item>
                <el-descriptions-item label="接口">{{ current.sourceApi || "—" }}</el-descriptions-item>
                <el-descriptions-item label="排序">{{ current.sort }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                    {{ current.status === 1 ? "启用" : "停用" }}
                </el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{ current.remark || "—" }}</el-descriptions-item>
            </el-descriptions>
            <el-divider content-position="left">端口</el-divider>
            <el-table :data="portRows" border size="small">
                <el-table-column prop="direction" label="方向" width="80" />
                <el-table-column prop="key" label="端口" width="140" />
                <el-table-column prop="type" label="类型" width="120" />
                <el-table-column label="必填" width="70" align="center">
                    <template #default="{ row }">{{ row.required ? "是" : "-" }}</template>
                </el-table-column>
                <el-table-column label="可选值 / 说明">
                    <template #default="{ row }">{{ row.options?.length ? row.options.join(" / ") : row.note || "—" }}</template>
                </el-table-column>
            </el-table>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from "vue";
import { listLogicNodes } from "../../../api/uiLogic";
import type { LogicNode, LogicPort } from "../../../types/uiLogic";

const KIND_LABELS: Record<string, string> = {
    trigger: "触发",
    source: "数据源",
    operator: "算子",
    condition: "条件",
    action: "动作",
    ui: "界面动作",
};

const loading = ref(false);
const rows = ref<LogicNode[]>([]);
const kindFilter = ref("");
const keyword = ref("");

const detailVisible = ref(false);
const current = ref<LogicNode | null>(null);

const filtered = computed(() => {
    const text = keyword.value.trim().toLowerCase();
    return rows.value.filter((row) => {
        if (kindFilter.value && row.kind !== kindFilter.value) {
            return false;
        }
        if (!text) {
            return true;
        }
        return [row.typeCode, row.typeName, row.sourceApi ?? "", row.remark ?? ""]
            .some((field) => field.toLowerCase().includes(text));
    });
});

interface PortRow extends LogicPort {
    direction: string
}

const portRows = computed<PortRow[]>(() => {
    const node = current.value;
    if (!node) {
        return [];
    }
    return [
        ...node.inputs.map((port) => ({ ...port, direction: "输入" })),
        ...node.outputs.map((port) => ({ ...port, direction: "输出" })),
        ...node.config.map((port) => ({ ...port, direction: "设置项" })),
    ];
});

function openDetail(row: LogicNode) {
    current.value = row;
    detailVisible.value = true;
}

onMounted(async () => {
    loading.value = true;
    try {
        rows.value = await listLogicNodes();
    } finally {
        loading.value = false;
    }
});
</script>

<style scoped>
.logic-node__header {
    display: flex;
    align-items: center;
    gap: 12px;
}

.logic-node__header > span:first-child {
    font-weight: 600;
}

.logic-node__hint {
    flex: 1;
    font-size: 12px;
    color: #909399;
}

.logic-node__filter {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
}

.logic-node__count {
    font-size: 12px;
    color: #909399;
}

.logic-node__name {
    font-weight: 600;
}

.logic-node__api {
    font-family: monospace;
    font-size: 12px;
}

.logic-node__muted {
    font-size: 12px;
    color: #909399;
}

.logic-node__port {
    margin: 0 4px 4px 0;
    font-family: monospace;
}

.logic-node__legend {
    margin: 10px 0 0;
    font-size: 12px;
    color: #909399;
}
</style>
