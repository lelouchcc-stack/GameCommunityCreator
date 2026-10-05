<template>
    <div class="dimension-page">
        <el-card class="dimension-page__filter" shadow="never">
            <el-form inline @submit.prevent>
                <el-form-item label="目标板块">
                    <el-select
                        v-model="boardId"
                        class="dimension-page__board"
                        placeholder="请选择板块"
                        @change="handleBoardChange"
                    >
                        <el-option
                            v-for="board in boards"
                            :key="board.id"
                            :label="board.name"
                            :value="board.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-alert
                        type="info"
                        :closable="false"
                        show-icon
                        title="版本与进度阶段按板块分别配置；板块关闭后不可配置。"
                    />
                </el-form-item>
            </el-form>
        </el-card>

        <div v-if="!boardId" class="dimension-page__empty">
            <el-empty description="请选择一个板块后配置内容维度" :image-size="80" />
        </div>

        <div v-else class="dimension-page__columns">
            <!-- 游戏版本 -->
            <el-card shadow="never">
                <template #header>
                    <div class="dimension-page__card-header">
                        <span>游戏版本</span>
                        <el-button type="primary" size="small" @click="openVersionCreate">新增版本</el-button>
                    </div>
                </template>

                <el-table v-loading="versionLoading" :data="versions" border>
                    <el-table-column prop="versionNo" label="版本号" width="110" />
                    <el-table-column prop="versionName" label="版本名" min-width="110" show-overflow-tooltip />
                    <el-table-column label="当前" width="80" align="center">
                        <template #default="{ row }">
                            <el-tag v-if="row.isCurrent" type="success" size="small">当前</el-tag>
                            <span v-else class="dimension-page__muted">—</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="80" align="center">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                                {{ row.status === 1 ? "正常" : "停用" }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="releaseTime" label="发布时间" width="160" />
                    <el-table-column label="操作" width="240" fixed="right">
                        <template #default="{ row }">
                            <el-button
                                v-if="!row.isCurrent"
                                link
                                type="primary"
                                :disabled="row.status !== 1"
                                @click="handleSetCurrent(row)"
                            >
                                设为当前
                            </el-button>
                            <el-button link type="primary" @click="openVersionEdit(row)">编辑</el-button>
                            <el-button link type="primary" @click="handleVersionToggle(row)">
                                {{ row.status === 1 ? "停用" : "启用" }}
                            </el-button>
                            <el-button link type="danger" @click="handleVersionDelete(row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>

            <!-- 进度阶段 -->
            <el-card shadow="never">
                <template #header>
                    <div class="dimension-page__card-header">
                        <span>进度阶段</span>
                        <el-button type="primary" size="small" @click="openStageCreate">新增阶段</el-button>
                    </div>
                </template>

                <el-table v-loading="stageLoading" :data="stages" border>
                    <el-table-column prop="sort" label="排序" width="70" align="center" />
                    <el-table-column prop="stageName" label="阶段名" min-width="110" show-overflow-tooltip />
                    <el-table-column prop="description" label="说明" min-width="140" show-overflow-tooltip />
                    <el-table-column label="状态" width="80" align="center">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                                {{ row.status === 1 ? "正常" : "停用" }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="260" fixed="right">
                        <template #default="{ row, $index }">
                            <el-button link type="primary" :disabled="$index === 0" @click="moveStage($index, -1)">
                                上移
                            </el-button>
                            <el-button
                                link
                                type="primary"
                                :disabled="$index === stages.length - 1"
                                @click="moveStage($index, 1)"
                            >
                                下移
                            </el-button>
                            <el-button link type="primary" @click="openStageEdit(row)">编辑</el-button>
                            <el-button link type="primary" @click="handleStageToggle(row)">
                                {{ row.status === 1 ? "停用" : "启用" }}
                            </el-button>
                            <el-button link type="danger" @click="handleStageDelete(row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>
        </div>

        <!-- 版本弹窗 -->
        <el-dialog
            v-model="versionDialogVisible"
            :title="versionDialogTitle"
            width="520px"
            @closed="resetVersionForm"
        >
            <el-form ref="versionFormRef" :model="versionForm" :rules="versionRules" label-width="90px">
                <el-form-item label="版本号" prop="versionNo">
                    <el-input v-model.trim="versionForm.versionNo" maxlength="50" placeholder="例如：1.3.0" />
                </el-form-item>
                <el-form-item label="版本名" prop="versionName">
                    <el-input v-model.trim="versionForm.versionName" maxlength="50" placeholder="例如：星海回响" />
                </el-form-item>
                <el-form-item label="发布时间" prop="releaseTime">
                    <el-date-picker
                        v-model="versionForm.releaseTime"
                        type="datetime"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        placeholder="选择发布时间"
                        style="width: 100%"
                    />
                </el-form-item>
                <el-form-item label="更新说明" prop="updateNote">
                    <el-input v-model="versionForm.updateNote" type="textarea" :rows="3" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="versionDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="versionSubmitting" @click="submitVersion">确定</el-button>
            </template>
        </el-dialog>

        <!-- 阶段弹窗 -->
        <el-dialog
            v-model="stageDialogVisible"
            :title="stageDialogTitle"
            width="480px"
            @closed="resetStageForm"
        >
            <el-form ref="stageFormRef" :model="stageForm" :rules="stageRules" label-width="90px">
                <el-form-item label="阶段名" prop="stageName">
                    <el-input v-model.trim="stageForm.stageName" maxlength="50" placeholder="例如：新手期" />
                </el-form-item>
                <el-form-item label="阶段说明" prop="description">
                    <el-input v-model="stageForm.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
                </el-form-item>
                <el-form-item label="排序" prop="sort">
                    <el-input-number v-model="stageForm.sort" :min="0" :max="9999" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="stageDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="stageSubmitting" @click="submitStage">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { listBoards, type BoardItem } from "../../../api/board";
import {
    changeStageStatus,
    changeVersionStatus,
    createStage,
    createVersion,
    deleteStage,
    deleteVersion,
    listStages,
    listVersions,
    reorderStages,
    setCurrentVersion,
    updateStage,
    updateVersion,
} from "../../../api/dimension";
import type {
    BoardVersion,
    BoardVersionForm,
    ProgressStage,
    ProgressStageForm,
} from "../../../types/dimension";

const boards = ref<BoardItem[]>([]);
const boardId = ref<number | null>(null);

const versions = ref<BoardVersion[]>([]);
const stages = ref<ProgressStage[]>([]);
const versionLoading = ref(false);
const stageLoading = ref(false);

/* ---------------- 板块切换 ---------------- */

onMounted(async () => {
    try {
        boards.value = await listBoards();
    } catch {
        boards.value = [];
    }
    if (boards.value.length) {
        boardId.value = boards.value[0]!.id;
        await loadAll();
    }
});

function handleBoardChange() {
    void loadAll();
}

async function loadAll() {
    await Promise.all([loadVersions(), loadStages()]);
}

async function loadVersions() {
    if (boardId.value === null) {
        return;
    }
    versionLoading.value = true;
    try {
        versions.value = await listVersions(boardId.value);
    } finally {
        versionLoading.value = false;
    }
}

async function loadStages() {
    if (boardId.value === null) {
        return;
    }
    stageLoading.value = true;
    try {
        stages.value = await listStages(boardId.value);
    } finally {
        stageLoading.value = false;
    }
}

/* ---------------- 版本 ---------------- */

const versionDialogVisible = ref(false);
const versionDialogTitle = ref("新增版本");
const versionEditingId = ref<number | null>(null);
const versionFormRef = ref<FormInstance>();
const versionSubmitting = ref(false);
const versionForm = reactive<BoardVersionForm>({
    versionNo: "",
    versionName: "",
    releaseTime: "",
    updateNote: "",
});

const versionRules: FormRules = {
    versionNo: [{ required: true, message: "请输入版本号", trigger: "blur" }],
};

function openVersionCreate() {
    versionEditingId.value = null;
    versionDialogTitle.value = "新增版本";
    versionDialogVisible.value = true;
}

function openVersionEdit(row: BoardVersion) {
    versionEditingId.value = row.id;
    versionDialogTitle.value = "编辑版本";
    versionForm.versionNo = row.versionNo;
    versionForm.versionName = row.versionName ?? "";
    versionForm.releaseTime = row.releaseTime ?? "";
    versionForm.updateNote = row.updateNote ?? "";
    versionDialogVisible.value = true;
}

function resetVersionForm() {
    versionFormRef.value?.clearValidate();
    versionForm.versionNo = "";
    versionForm.versionName = "";
    versionForm.releaseTime = "";
    versionForm.updateNote = "";
}

async function submitVersion() {
    if (boardId.value === null || !versionFormRef.value) {
        return;
    }
    const valid = await versionFormRef.value.validate().catch(() => false);
    if (!valid) {
        return;
    }
    versionSubmitting.value = true;
    try {
        if (versionEditingId.value === null) {
            await createVersion(boardId.value, versionForm);
            ElMessage.success("新增成功");
        } else {
            await updateVersion(boardId.value, versionEditingId.value, versionForm);
            ElMessage.success("保存成功");
        }
        versionDialogVisible.value = false;
        await loadVersions();
    } catch {
        // 拦截器已提示
    } finally {
        versionSubmitting.value = false;
    }
}

async function handleSetCurrent(row: BoardVersion) {
    if (boardId.value === null) {
        return;
    }
    const confirmed = await ElMessageBox.confirm(
        `确定把「${row.versionNo}」设为当前版本吗？旧版本帖子会标记为版本过期。`,
        "提示",
        { type: "warning" },
    ).catch(() => false);
    if (!confirmed) {
        return;
    }
    await setCurrentVersion(boardId.value, row.id);
    ElMessage.success("已切换当前版本");
    await loadVersions();
}

async function handleVersionToggle(row: BoardVersion) {
    if (boardId.value === null) {
        return;
    }
    const next = row.status === 1 ? 0 : 1;
    try {
        await changeVersionStatus(boardId.value, row.id, next);
        ElMessage.success(next === 1 ? "已启用" : "已停用");
    } catch {
        // 拦截器已提示，例如"当前版本不能停用"
    }
    await loadVersions();
}

async function handleVersionDelete(row: BoardVersion) {
    if (boardId.value === null) {
        return;
    }
    const confirmed = await ElMessageBox.confirm(
        `确定删除版本「${row.versionNo}」吗？被帖子引用的版本会被拒绝。`,
        "提示",
        { type: "warning" },
    ).catch(() => false);
    if (!confirmed) {
        return;
    }
    try {
        await deleteVersion(boardId.value, row.id);
        ElMessage.success("删除成功");
    } catch {
        // 拦截器已提示
    }
    await loadVersions();
}

/* ---------------- 进度阶段 ---------------- */

const stageDialogVisible = ref(false);
const stageDialogTitle = ref("新增阶段");
const stageEditingId = ref<number | null>(null);
const stageFormRef = ref<FormInstance>();
const stageSubmitting = ref(false);
const stageForm = reactive<ProgressStageForm>({ stageName: "", description: "", sort: 0 });

const stageRules: FormRules = {
    stageName: [{ required: true, message: "请输入阶段名", trigger: "blur" }],
};

function openStageCreate() {
    stageEditingId.value = null;
    stageDialogTitle.value = "新增阶段";
    stageForm.sort = stages.value.length;
    stageDialogVisible.value = true;
}

function openStageEdit(row: ProgressStage) {
    stageEditingId.value = row.id;
    stageDialogTitle.value = "编辑阶段";
    stageForm.stageName = row.stageName;
    stageForm.description = row.description ?? "";
    stageForm.sort = row.sort;
    stageDialogVisible.value = true;
}

function resetStageForm() {
    stageFormRef.value?.clearValidate();
    stageForm.stageName = "";
    stageForm.description = "";
    stageForm.sort = 0;
}

async function submitStage() {
    if (boardId.value === null || !stageFormRef.value) {
        return;
    }
    const valid = await stageFormRef.value.validate().catch(() => false);
    if (!valid) {
        return;
    }
    stageSubmitting.value = true;
    try {
        if (stageEditingId.value === null) {
            await createStage(boardId.value, stageForm);
            ElMessage.success("新增成功");
        } else {
            await updateStage(boardId.value, stageEditingId.value, stageForm);
            ElMessage.success("保存成功");
        }
        stageDialogVisible.value = false;
        await loadStages();
    } catch {
        // 拦截器已提示
    } finally {
        stageSubmitting.value = false;
    }
}

/** 上移/下移：交换相邻两项后，把整组新顺序提交给后端 */
async function moveStage(index: number, delta: number) {
    if (boardId.value === null) {
        return;
    }
    const target = index + delta;
    const current = stages.value[index];
    const neighbor = stages.value[target];
    if (!current || !neighbor) {
        return;
    }
    const reordered = [...stages.value];
    reordered.splice(index, 1);
    reordered.splice(target, 0, current);
    const items = reordered.map((stage, i) => ({ id: stage.id, sort: i }));
    try {
        await reorderStages(boardId.value, items);
        await loadStages();
    } catch {
        // 拦截器已提示
    }
}

async function handleStageToggle(row: ProgressStage) {
    if (boardId.value === null) {
        return;
    }
    const next = row.status === 1 ? 0 : 1;
    try {
        await changeStageStatus(boardId.value, row.id, next);
        ElMessage.success(next === 1 ? "已启用" : "已停用");
    } catch {
        // 拦截器已提示
    }
    await loadStages();
}

async function handleStageDelete(row: ProgressStage) {
    if (boardId.value === null) {
        return;
    }
    const confirmed = await ElMessageBox.confirm(
        `确定删除阶段「${row.stageName}」吗？被帖子或玩家进度引用的阶段会被拒绝。`,
        "提示",
        { type: "warning" },
    ).catch(() => false);
    if (!confirmed) {
        return;
    }
    try {
        await deleteStage(boardId.value, row.id);
        ElMessage.success("删除成功");
    } catch {
        // 拦截器已提示
    }
    await loadStages();
}
</script>

<style scoped>
.dimension-page__filter {
    margin-bottom: 16px;
}

.dimension-page__board {
    width: 220px;
}

.dimension-page__columns {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    gap: 16px;
}

.dimension-page__card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.dimension-page__empty {
    padding: 48px 0;
}

.dimension-page__muted {
    color: #c0c4cc;
}
</style>
