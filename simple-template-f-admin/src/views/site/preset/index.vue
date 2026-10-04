<template>
    <div class="preset">
        <el-card shadow="never">
            <template #header>
                <div class="preset__header">
                    <span>预设库</span>
                    <span class="preset__hint">
                        预设 = 原子团（一段可复用的子图），页面图里以「预设节点」引用它。
                        内置预设只读，可「复制为自定义」再改；已被页面图引用的预设改动会另起新版本，引用方仍锁旧版本
                    </span>
                    <el-button type="primary" @click="openCreate">新增预设</el-button>
                </div>
            </template>

            <el-table v-loading="loading" :data="rows" border>
                <el-table-column label="预设" min-width="220">
                    <template #default="{ row }">
                        <div class="preset__name">
                            {{ row.presetName }}
                            <el-tag size="small" type="warning">v{{ row.version }}</el-tag>
                        </div>
                        <el-tag size="small" type="info">{{ row.presetCode }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="来源" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag size="small" :type="row.isBuiltin === 1 ? 'warning' : 'success'">
                            {{ row.isBuiltin === 1 ? "内置" : "自定义" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="入参" min-width="160">
                    <template #default="{ row }">
                        <el-tag v-for="port in row.inputs" :key="port.key" size="small" class="preset__port">
                            {{ port.key }}:{{ port.type }}
                        </el-tag>
                        <span v-if="!row.inputs.length" class="preset__muted">—</span>
                    </template>
                </el-table-column>
                <el-table-column label="出参" min-width="160">
                    <template #default="{ row }">
                        <el-tag v-for="port in row.outputs" :key="port.key" size="small" class="preset__port">
                            {{ port.key }}:{{ port.type }}
                        </el-tag>
                        <span v-if="!row.outputs.length" class="preset__muted">—</span>
                    </template>
                </el-table-column>
                <el-table-column label="图" width="80" align="center">
                    <template #default="{ row }">{{ row.graph?.nodes?.length ?? 0 }} 节点</template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                    <template #default="{ row }">
                        <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
                            {{ row.status === 1 ? "启用" : "停用" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="210" align="center">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openDetail(row)">查看图</el-button>
                        <el-button link type="primary" @click="copyToCustom(row)">复制为自定义</el-button>
                        <el-button v-if="row.isBuiltin === 0" link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button v-if="row.isBuiltin === 0" link type="danger" @click="onDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="detailVisible" :title="`${current?.presetName}（${current?.presetCode} v${current?.version}）`" width="720px" top="6vh">
            <el-alert v-if="current?.remark" type="info" :closable="false" :title="current.remark" style="margin-bottom: 12px" />
            <el-table :data="detailNodes" border size="small">
                <el-table-column prop="id" label="节点" width="80" />
                <el-table-column prop="typeCode" label="原子 / 预设" width="180" />
                <el-table-column prop="label" label="说明" width="120" />
                <el-table-column label="设置项">
                    <template #default="{ row }">
                        <span v-if="row.configText" class="preset__config">{{ row.configText }}</span>
                        <span v-else class="preset__muted">—</span>
                    </template>
                </el-table-column>
            </el-table>
            <el-divider content-position="left">连线（{{ current?.graph?.edges?.length ?? 0 }} 条）</el-divider>
            <div class="preset__edges">
                <div v-for="(edge, index) in current?.graph?.edges ?? []" :key="index" class="preset__edge">
                    {{ edge.from?.node }}.{{ edge.from?.port }} → {{ edge.to?.node }}.{{ edge.to?.port }}
                </div>
            </div>
            <el-divider content-position="left">出参来源</el-divider>
            <div class="preset__edges">
                <div v-for="(target, name) in current?.graph?.exports ?? {}" :key="name" class="preset__edge">
                    {{ name }} ← {{ target }}
                </div>
                <span v-if="!Object.keys(current?.graph?.exports ?? {}).length" class="preset__muted">无出参</span>
            </div>
        </el-dialog>

        <el-dialog v-model="dialogVisible" :title="editingId ? '编辑预设' : '新增预设'" width="720px" top="6vh">
            <el-alert
                type="warning"
                :closable="false"
                title="图暂时用 JSON 编辑；连线式画布（FlowEditor）在阶段 3.7 接入后会替换这里"
                style="margin-bottom: 12px"
            />
            <el-form :model="form" label-width="90px">
                <el-form-item label="预设名称" required>
                    <el-input v-model="form.presetName" maxlength="50" placeholder="如 我的登录流程" />
                </el-form-item>
                <el-form-item label="分组">
                    <el-input v-model="form.category" maxlength="20" placeholder="如 member / content / interact" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="form.remark" maxlength="200" />
                </el-form-item>
                <el-form-item label="入参（JSON）">
                    <el-input v-model="inputsText" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" placeholder='[{"key":"postId","type":"number","required":true}]' />
                </el-form-item>
                <el-form-item label="出参（JSON）">
                    <el-input v-model="outputsText" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" placeholder='[{"key":"result","type":"result"}]' />
                </el-form-item>
                <el-form-item label="图（JSON）">
                    <el-input v-model="graphText" type="textarea" :autosize="{ minRows: 8, maxRows: 20 }" />
                </el-form-item>
            </el-form>

            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { createPreset, deletePreset, listPresets, updatePreset } from "../../../api/uiLogic";
import type { LogicGraph, LogicPort, Preset, PresetPayload } from "../../../types/uiLogic";

const loading = ref(false);
const saving = ref(false);
const rows = ref<Preset[]>([]);

const detailVisible = ref(false);
const current = ref<Preset | null>(null);

const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const inputsText = ref("[]");
const outputsText = ref("[]");
const graphText = ref("");

const form = reactive<PresetPayload>(emptyForm());

function emptyForm(): PresetPayload {
    return {
        presetName: "",
        category: null,
        inputsSchema: [],
        outputsSchema: [],
        graphJson: { nodes: [], edges: [], exports: {} },
        status: 1,
        remark: null,
    };
}

const detailNodes = computed(() =>
    (current.value?.graph?.nodes ?? []).map((node: Record<string, any>) => ({
        id: node.id,
        typeCode: node.typeCode === "preset" ? `${node.presetCode}@${node.presetVersion}` : node.typeCode,
        label: node.label ?? "",
        configText: Object.entries(node.config ?? {})
            .map(([key, value]) => `${key}=${value}`)
            .join("，"),
    })),
);

async function loadList() {
    loading.value = true;
    try {
        rows.value = await listPresets();
    } finally {
        loading.value = false;
    }
}

function openDetail(row: Preset) {
    current.value = row;
    detailVisible.value = true;
}

function fillForm(row: Pick<Preset, "presetName" | "category" | "status" | "remark"> & { inputs: LogicPort[]; outputs: LogicPort[]; graph: LogicGraph }) {
    Object.assign(form, {
        presetName: row.presetName,
        category: row.category,
        status: row.status,
        remark: row.remark,
        inputsSchema: row.inputs,
        outputsSchema: row.outputs,
        graphJson: row.graph,
    });
    inputsText.value = JSON.stringify(row.inputs ?? [], null, 2);
    outputsText.value = JSON.stringify(row.outputs ?? [], null, 2);
    graphText.value = JSON.stringify(row.graph ?? { nodes: [], edges: [] }, null, 2);
}

function openCreate() {
    editingId.value = null;
    fillForm({
        presetName: "",
        category: null,
        status: 1,
        remark: null,
        inputs: [],
        outputs: [],
        graph: { nodes: [], edges: [], exports: {} },
    });
    dialogVisible.value = true;
}

function openEdit(row: Preset) {
    editingId.value = row.id;
    fillForm(row);
    dialogVisible.value = true;
}

/** 内置预设只读，复制一份自定义的再改（版本从 1 起，code 由后端生成） */
async function copyToCustom(row: Preset) {
    try {
        await createPreset({
            presetName: `${row.presetName} 副本`,
            category: row.category,
            inputsSchema: row.inputs,
            outputsSchema: row.outputs,
            graphJson: row.graph,
            status: 1,
            remark: `复制自 ${row.presetCode}@${row.version}`,
        });
        ElMessage.success("已复制为自定义预设，可在列表里编辑");
        await loadList();
    } catch {
        // 失败原因由请求拦截器统一提示
    }
}

function parseJson<T>(text: string, label: string): T | null {
    try {
        return JSON.parse(text) as T;
    } catch {
        ElMessage.error(`${label} 不是合法的 JSON`);
        return null;
    }
}

async function handleSave() {
    if (!form.presetName.trim()) {
        ElMessage.error("请输入预设名称");
        return;
    }
    const inputs = parseJson<LogicPort[]>(inputsText.value, "入参");
    const outputs = parseJson<LogicPort[]>(outputsText.value, "出参");
    const graph = parseJson<LogicGraph>(graphText.value, "图");
    if (!inputs || !outputs || !graph) {
        return;
    }
    const payload: PresetPayload = {
        presetName: form.presetName,
        category: form.category,
        inputsSchema: inputs,
        outputsSchema: outputs,
        graphJson: graph,
        status: form.status,
        remark: form.remark,
    };
    saving.value = true;
    try {
        if (editingId.value) {
            await updatePreset(editingId.value, payload);
            ElMessage.success("已保存；若该预设已被页面图引用，会另起一个新版本");
        } else {
            await createPreset(payload);
            ElMessage.success("已创建");
        }
        dialogVisible.value = false;
        await loadList();
    } catch {
        // 图校验不通过时后端会给出具体原因，已由拦截器提示
    } finally {
        saving.value = false;
    }
}

async function onDelete(row: Preset) {
    try {
        await ElMessageBox.confirm(`确定删除预设「${row.presetName}」吗？`, "提示", { type: "warning" });
    } catch {
        return;
    }
    try {
        await deletePreset(row.id);
        ElMessage.success("删除成功");
        await loadList();
    } catch {
        // 内置或被引用会被后端拒绝，原因已由拦截器提示
    }
}

onMounted(loadList);
</script>

<style scoped>
.preset__header {
    display: flex;
    align-items: center;
    gap: 12px;
}

.preset__header > span:first-child {
    font-weight: 600;
}

.preset__hint {
    flex: 1;
    font-size: 12px;
    color: #909399;
}

.preset__name {
    font-weight: 600;
    display: flex;
    align-items: center;
    gap: 6px;
}

.preset__port {
    margin: 0 4px 4px 0;
    font-family: monospace;
}

.preset__config {
    font-family: monospace;
    font-size: 12px;
}

.preset__muted {
    font-size: 12px;
    color: #909399;
}

.preset__edges {
    max-height: 180px;
    overflow: auto;
}

.preset__edge {
    font-family: monospace;
    font-size: 12px;
    line-height: 1.8;
}
</style>
