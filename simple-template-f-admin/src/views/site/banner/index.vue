<template>
    <div class="banner-page">
        <el-card shadow="never">
            <template #header>
                <div class="banner-page__header">
                    <span>首页轮播图</span>
                    <span class="banner-page__hint">
                        展示中的轮播图会按顺序出现在用户端首页；点「上移 / 下移」即时保存顺序
                    </span>
                </div>
            </template>

            <div class="banner-page__toolbar">
                <el-button type="primary" @click="openCreate">新增轮播图</el-button>
            </div>

            <el-table v-loading="loading || sorting" :data="rows" border>
                <el-table-column label="预览" width="140" align="center">
                    <template #default="{ row }">
                        <img
                            v-if="previewOf(row)"
                            class="banner-page__thumb"
                            :src="previewOf(row)"
                            alt=""
                        />
                        <span v-else class="banner-page__muted">未设置</span>
                    </template>
                </el-table-column>
                <el-table-column label="标题" min-width="180">
                    <template #default="{ row }">
                        <div v-if="row.title">{{ row.title }}</div>
                        <span v-else class="banner-page__muted">未填写</span>
                        <div v-if="row.subtitle" class="banner-page__sub">{{ row.subtitle }}</div>
                    </template>
                </el-table-column>
                <el-table-column label="跳转" min-width="140">
                    <template #default="{ row }">
                        <span v-if="row.linkUrl">{{ row.linkUrl }}</span>
                        <span v-else class="banner-page__muted">不可点</span>
                    </template>
                </el-table-column>
                <el-table-column label="顺序" width="170" align="center">
                    <template #default="{ $index }">
                        <span class="banner-page__sort">{{ $index + 1 }}</span>
                        <el-button link type="primary" :disabled="$index === 0" @click="move($index, -1)">
                            上移
                        </el-button>
                        <el-button
                            link
                            type="primary"
                            :disabled="$index === rows.length - 1"
                            @click="move($index, 1)"
                        >
                            下移
                        </el-button>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                            {{ row.status === 1 ? "展示" : "隐藏" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="180" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="primary" @click="handleToggleStatus(row)">
                            {{ row.status === 1 ? "隐藏" : "展示" }}
                        </el-button>
                        <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <p v-if="!loading && !rows.length" class="banner-page__empty">
                还没有轮播图，点「新增轮播图」上传第一张
            </p>
        </el-card>

        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" @closed="handleDialogClosed">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="轮播图" prop="image">
                    <ImageUpload v-model="form.image" />
                </el-form-item>
                <el-form-item label="标题" prop="title">
                    <el-input v-model.trim="form.title" maxlength="50" show-word-limit placeholder="例如：1.3.0 版本已上线" />
                </el-form-item>
                <el-form-item label="副标题" prop="subtitle">
                    <el-input v-model.trim="form.subtitle" maxlength="100" show-word-limit placeholder="一句话说明" />
                </el-form-item>
                <el-form-item label="跳转地址" prop="linkUrl">
                    <el-input v-model.trim="form.linkUrl" maxlength="255" placeholder="留空表示图上不可点，例如 /board/1" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import ImageUpload from "../../../components/ImageUpload.vue";
import { resolveAssetUrl } from "../../../utils/asset";
import {
    changeBannerStatus,
    createBanner,
    deleteBanner,
    listBanners,
    reorderBanners,
    updateBanner,
} from "../../../api/banner";
import type { BannerSlide, BannerSlideForm } from "../../../types/banner";

const loading = ref(false);
const submitting = ref(false);
const sorting = ref(false);
const rows = ref<BannerSlide[]>([]);

const dialogVisible = ref(false);
const dialogTitle = ref("新增轮播图");
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<BannerSlideForm>({ title: "", subtitle: "", image: "", linkUrl: "" });

const rules: FormRules = {
    image: [{ required: true, message: "请上传轮播图或填写图片地址", trigger: "change" }],
};

function previewOf(row: BannerSlide) {
    return resolveAssetUrl(row.image);
}

async function loadList() {
    loading.value = true;
    try {
        rows.value = await listBanners();
    } finally {
        loading.value = false;
    }
}

function openCreate() {
    editingId.value = null;
    dialogTitle.value = "新增轮播图";
    dialogVisible.value = true;
}

function openEdit(row: BannerSlide) {
    editingId.value = row.id;
    dialogTitle.value = "编辑轮播图";
    form.title = row.title ?? "";
    form.subtitle = row.subtitle ?? "";
    form.image = row.image ?? "";
    form.linkUrl = row.linkUrl ?? "";
    dialogVisible.value = true;
}

function handleDialogClosed() {
    formRef.value?.clearValidate();
    form.title = "";
    form.subtitle = "";
    form.image = "";
    form.linkUrl = "";
}

async function handleSubmit() {
    if (!formRef.value) {
        return;
    }
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid) {
        return;
    }

    submitting.value = true;
    try {
        if (editingId.value === null) {
            await createBanner(form);
            ElMessage.success("新增成功");
        } else {
            await updateBanner(editingId.value, form);
            ElMessage.success("保存成功");
        }
        dialogVisible.value = false;
        await loadList();
    } catch {
        // 失败原因由请求拦截器统一提示
    } finally {
        submitting.value = false;
    }
}

/** 上移/下移：本地换位后把新顺序整体下发，避免手工填 sort 填重 */
async function move(index: number, offset: number) {
    const target = index + offset;
    if (target < 0 || target >= rows.value.length) {
        return;
    }
    const next = [...rows.value];
    const [moved] = next.splice(index, 1);
    if (!moved) {
        return;
    }
    next.splice(target, 0, moved);
    rows.value = next;

    sorting.value = true;
    try {
        await reorderBanners(next.map((row) => row.id));
        ElMessage.success("顺序已保存");
    } catch {
        // 保存失败回读一次，避免界面顺序与库里不一致
    } finally {
        sorting.value = false;
        await loadList();
    }
}

async function handleToggleStatus(row: BannerSlide) {
    const next = row.status === 1 ? 0 : 1;
    await changeBannerStatus(row.id, next);
    ElMessage.success(next === 1 ? "已设为展示" : "已隐藏");
    await loadList();
}

async function handleDelete(row: BannerSlide) {
    const confirmed = await ElMessageBox.confirm(
        `确定要删除轮播图「${row.title || "未命名"}」吗？`,
        "提示",
        { type: "warning" },
    ).catch(() => false);
    if (!confirmed) {
        return;
    }
    await deleteBanner(row.id);
    ElMessage.success("删除成功");
    await loadList();
}

onMounted(loadList);
</script>

<style scoped>
.banner-page__header {
    display: flex;
    align-items: baseline;
    gap: 12px;
}

.banner-page__hint {
    font-size: 12px;
    color: #909399;
}

.banner-page__toolbar {
    margin-bottom: 12px;
}

.banner-page__thumb {
    width: 108px;
    height: 60px;
    border-radius: 4px;
    object-fit: cover;
}

.banner-page__sub {
    margin-top: 4px;
    color: #909399;
    font-size: 12px;
}

.banner-page__sort {
    display: inline-block;
    width: 24px;
    color: #909399;
}

.banner-page__muted {
    color: #c0c4cc;
}

.banner-page__empty {
    margin: 12px 0 0;
    color: #909399;
    font-size: 13px;
    text-align: center;
}
</style>
