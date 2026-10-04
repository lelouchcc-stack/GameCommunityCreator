<template>
    <div class="action-control">
        <el-select
            :model-value="current.action ?? 'none'"
            style="width: 100%"
            @update:model-value="(v: any) => setField('action', v)"
        >
            <el-option v-for="option in ACTIONS" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>

        <el-select
            v-if="needsTarget"
            :model-value="current.target ?? null"
            clearable
            placeholder="选择目标组件"
            style="width: 100%; margin-top: 6px"
            @update:model-value="(v: any) => setField('target', v)"
        >
            <el-option v-for="option in targetOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <div v-if="needsTarget && !targetOptions.length" class="action-control__empty">
            本页还没有可选目标组件
        </div>

        <template v-if="current.action === 'navigate'">
            <el-input
                :model-value="current.url ?? ''"
                placeholder="https://... 或 /post/1"
                style="margin-top: 6px"
                @update:model-value="(v: any) => setField('url', v)"
            />
            <el-checkbox
                :model-value="current.newTab === true"
                style="margin-top: 6px"
                @update:model-value="(v: any) => setField('newTab', v)"
            >
                新窗口打开
            </el-checkbox>
        </template>

        <template v-if="current.action === 'toast'">
            <el-input
                :model-value="current.message ?? ''"
                placeholder="提示文案"
                style="margin-top: 6px"
                @update:model-value="(v: any) => setField('message', v)"
            />
            <el-select
                :model-value="current.toastType ?? 'info'"
                style="width: 100%; margin-top: 6px"
                @update:model-value="(v: any) => setField('toastType', v)"
            >
                <el-option label="信息" value="info" />
                <el-option label="成功" value="success" />
                <el-option label="警告" value="warning" />
                <el-option label="错误" value="error" />
            </el-select>
        </template>
    </div>
</template>

<script lang="ts" setup>
import { computed } from "vue";
import type { BlockAction, BlockActionName } from "../blocks/blockRuntime";

export interface ActionTargetOption {
    value: number
    label: string
    isModal: boolean
}

const props = defineProps<{
    modelValue: BlockAction | null | undefined
    nodeOptions: ActionTargetOption[]
}>();

const emit = defineEmits<{ (e: "update:modelValue", value: BlockAction): void }>();

const ACTIONS: { value: BlockActionName; label: string }[] = [
    { value: "none", label: "无" },
    { value: "openModal", label: "打开弹窗" },
    { value: "closeModal", label: "关闭弹窗" },
    { value: "navigate", label: "跳转页面" },
    { value: "scrollTo", label: "滚动到组件" },
    { value: "toggleVisible", label: "显示/隐藏组件" },
    { value: "toast", label: "弹出提示" },
];

const current = computed<BlockAction>(() => props.modelValue ?? { action: "none" });

const needsTarget = computed(() =>
    ["openModal", "closeModal", "scrollTo", "toggleVisible"].includes(current.value.action ?? "none"),
);

/** 打开/关闭弹窗只列弹窗组件；滚动/显隐列出全部组件 */
const targetOptions = computed(() => {
    const action = current.value.action;
    if (action === "openModal" || action === "closeModal") {
        return props.nodeOptions.filter((option) => option.isModal);
    }
    return props.nodeOptions;
});

function setField<K extends keyof BlockAction>(key: K, value: BlockAction[K]) {
    emit("update:modelValue", { ...current.value, [key]: value });
}
</script>

<style scoped>
.action-control__empty {
    margin-top: 4px;
    font-size: 12px;
    color: #909399;
}
</style>
