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
        <!-- 图片类字段走上传组件；已绑定成 $ref 时仍用文本框显示绑定表达式（由下面的绑定下拉管理） -->
        <ImageUpload
            v-else-if="meta.type === 'image' && !bindingValue"
            :model-value="modelValue ?? ''"
            @update:model-value="(v: any) => emit('update:modelValue', v)"
        />
        <el-input
            v-else
            :model-value="modelValue ?? ''"
            :maxlength="meta.maxLength"
            :placeholder="placeholder"
            clearable
            @update:model-value="(v: any) => emit('update:modelValue', v)"
        />
        <!-- 可绑定的字段多一个下拉：选中即写入 $ref.<节点id>.<端口>（清空则回到字面量） -->
        <el-select
            v-if="bindings"
            class="prop-control__binding"
            :model-value="bindingValue"
            clearable
            filterable
            size="small"
            :placeholder="bindings.length ? '绑定到图节点输出' : '本页还没有逻辑图'"
            @update:model-value="(v: any) => emit('update:modelValue', v ?? '')"
        >
            <el-option v-for="option in bindings" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
    </div>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import ImageUpload from "./ImageUpload.vue";
import type { PropertyMeta } from "../blocks/propertyMeta";

const props = defineProps<{
    meta: PropertyMeta
    modelValue: any
    /** 传了就显示「绑定到图节点输出」下拉；不传表示这个字段不可绑定 */
    bindings?: Array<{ label: string; value: string }>
}>();
const emit = defineEmits<{ (e: "update:modelValue", value: any): void }>();

/** 当前值是不是一条绑定（是则下拉回显它，否则下拉留空、输入框显示字面量） */
const bindingValue = computed(() =>
    typeof props.modelValue === "string" && props.modelValue.startsWith("$ref.") ? props.modelValue : "",
);

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
            return "图片地址或对象键";
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
