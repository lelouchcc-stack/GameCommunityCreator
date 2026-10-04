<template>
    <div class="prop-control">
        <el-switch
            v-if="meta.type === 'bool'"
            :model-value="modelValue === true || modelValue === 'true'"
            @update:model-value="(v: any) => emit('update:modelValue', v)"
        />
        <el-select
            v-else-if="meta.type === 'enum'"
            :model-value="modelValue ?? ''"
            clearable
            placeholder="跟随默认"
            style="width: 100%"
            @update:model-value="(v: any) => emit('update:modelValue', v ?? undefined)"
        >
            <el-option v-for="option in meta.options" :key="option" :label="option" :value="option" />
        </el-select>
        <el-input-number
            v-else-if="meta.type === 'number'"
            :model-value="numberValue"
            :min="meta.min"
            :max="meta.max"
            :step="meta.step ?? 1"
            style="width: 100%"
            @update:model-value="(v: any) => emit('update:modelValue', v)"
        />
        <el-color-picker
            v-else-if="meta.type === 'color'"
            :model-value="modelValue || null"
            show-alpha
            @update:model-value="(v: any) => emit('update:modelValue', v ?? undefined)"
        />
        <el-input
            v-else-if="meta.type === 'jsonArray' || meta.type === 'jsonObject'"
            v-model="jsonText"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            placeholder="JSON"
            @blur="commitJson"
        />
        <el-input
            v-else
            :model-value="modelValue ?? ''"
            :maxlength="meta.maxLength"
            :placeholder="placeholder"
            clearable
            @update:model-value="(v: any) => emit('update:modelValue', v)"
        />
    </div>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import type { PropertyMeta } from "../blocks/propertyMeta";

const props = defineProps<{ meta: PropertyMeta; modelValue: any }>();
const emit = defineEmits<{ (e: "update:modelValue", value: any): void }>();

const numberValue = computed(() => {
    if (props.modelValue === "" || props.modelValue === null || props.modelValue === undefined) {
        return undefined;
    }
    return Number(props.modelValue);
});

const placeholder = computed(() => {
    switch (props.meta.type) {
        case "length":
            return "如 16px / 100% / auto";
        case "image":
            return "图片地址（URL 或 / 开头）";
        case "url":
            return "https://... 或 /post/1";
        case "binding":
            return "$ref.节点.端口（阶段3接入）";
        default:
            return "";
    }
});

const jsonText = ref("");
watch(
    () => props.modelValue,
    (value) => {
        jsonText.value = value === undefined || value === null ? "" : JSON.stringify(value, null, 2);
    },
    { immediate: true }
);

function commitJson() {
    const text = jsonText.value.trim();
    if (!text) {
        emit("update:modelValue", props.meta.type === "jsonArray" ? [] : {});
        return;
    }
    try {
        const parsed = JSON.parse(text);
        emit("update:modelValue", parsed);
        jsonText.value = JSON.stringify(parsed, null, 2);
    } catch {
        ElMessage.error("JSON 格式不合法");
        jsonText.value = JSON.stringify(props.modelValue ?? (props.meta.type === "jsonArray" ? [] : {}), null, 2);
    }
}
</script>

<style scoped>
.prop-control {
    width: 100%;
}
</style>
