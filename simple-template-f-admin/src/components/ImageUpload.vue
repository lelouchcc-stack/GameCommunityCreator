<template>
    <div class="image-upload">
        <el-upload
            class="image-upload__drop"
            :show-file-list="false"
            :http-request="handleUpload"
            :disabled="uploading"
            accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
        >
            <div class="image-upload__box" :class="{ 'is-empty': !previewUrl }">
                <img v-if="previewUrl" class="image-upload__img" :src="previewUrl" alt="" />
                <div v-else class="image-upload__hint">
                    <el-icon><Plus /></el-icon>
                    <span>{{ uploading ? "上传中…" : "点击上传图片" }}</span>
                </div>
            </div>
        </el-upload>

        <div class="image-upload__meta">
            <el-input
                :model-value="modelValue ?? ''"
                size="small"
                clearable
                placeholder="或直接填图片地址"
                @update:model-value="(v: any) => emit('update:modelValue', v ?? '')"
            />
            <el-button
                v-if="modelValue"
                link
                type="danger"
                size="small"
                @click="emit('update:modelValue', '')"
            >
                移除
            </el-button>
        </div>
        <div class="image-upload__tip">支持 jpg / png / gif / webp / bmp，单张不超过 5MB</div>
    </div>
</template>

<script lang="ts" setup>
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import type { UploadRequestOptions } from "element-plus";
import { uploadImage } from "../api/upload";
import { resolveAssetUrl } from "../utils/asset";

const props = defineProps<{ modelValue?: string | null }>();
const emit = defineEmits<{ (e: "update:modelValue", value: string): void }>();

const uploading = ref(false);
/** 绑定的值是 key 或完整地址，预览前统一解析成可访问地址 */
const previewUrl = computed(() => resolveAssetUrl(props.modelValue));

async function handleUpload(options: UploadRequestOptions) {
    uploading.value = true;
    try {
        const result = await uploadImage(options.file);
        emit("update:modelValue", result.key);
        ElMessage.success("图片已上传");
    } catch {
        // 失败原因由请求拦截器统一提示
    } finally {
        uploading.value = false;
    }
}
</script>

<style scoped>
.image-upload {
    width: 100%;
}

.image-upload__drop :deep(.el-upload) {
    width: 100%;
}

.image-upload__box {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 110px;
    overflow: hidden;
    border: 1px dashed var(--el-border-color);
    border-radius: 6px;
    background: var(--el-fill-color-lighter);
    transition: border-color 0.2s;
}

.image-upload__box:hover {
    border-color: var(--el-color-primary);
}

.image-upload__img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
}

.image-upload__hint {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.image-upload__meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
}

.image-upload__tip {
    margin-top: 4px;
    color: var(--el-text-color-placeholder);
    font-size: 12px;
}
</style>
