<template>
    <div class="site-info">
        <el-card shadow="never">
            <template #header>
                <div class="site-info__header">
                    <span>站点信息与主题</span>
                    <span class="site-info__hint">保存后管理端立即应用（保存即预览），用户端刷新后生效</span>
                </div>
            </template>

            <el-form
                ref="formRef"
                :model="form"
                :rules="rules"
                label-width="120px"
                class="site-info__form"
            >
                <el-form-item label="站点名称" prop="siteName">
                    <el-input
                        v-model.trim="form.siteName"
                        maxlength="30"
                        show-word-limit
                        placeholder="显示在浏览器标题与导航栏"
                    />
                </el-form-item>
                <el-form-item label="站点简介" prop="siteIntro">
                    <el-input
                        v-model="form.siteIntro"
                        type="textarea"
                        :rows="3"
                        maxlength="200"
                        show-word-limit
                        placeholder="显示在用户端页脚与关于区块"
                    />
                </el-form-item>
                <el-form-item label="站点 Logo" prop="logo">
                    <el-input v-model.trim="form.logo" placeholder="图片地址（图片上传模块完成前先填 URL）" />
                </el-form-item>
                <el-form-item label="版权文案" prop="copyright">
                    <el-input v-model="form.copyright" maxlength="100" show-word-limit />
                </el-form-item>
                <el-form-item label="主题主色" prop="primaryColor">
                    <el-color-picker v-model="form.primaryColor" />
                    <span class="site-info__color">{{ form.primaryColor }}</span>
                </el-form-item>
                <el-form-item label="默认深色模式" prop="darkMode">
                    <el-switch v-model="form.darkMode" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
                    <el-button @click="loadConfig">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { getSiteConfig, updateSiteConfig } from "../../../api/site";
import useSiteStore from "../../../store/siteStore";
import type { SiteConfig } from "../../../types/site";

const siteStore = useSiteStore();
const loading = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();

const form = reactive<SiteConfig>({
    siteName: "",
    siteIntro: "",
    logo: "",
    copyright: "",
    primaryColor: "#3B82F6",
    darkMode: false,
});

const rules: FormRules = {
    siteName: [{ required: true, message: "请输入站点名称", trigger: "blur" }],
    primaryColor: [{ required: true, message: "请选择主题主色", trigger: "change" }],
};

async function loadConfig() {
    loading.value = true;
    try {
        const data = await getSiteConfig();
        Object.assign(form, data);
    } finally {
        loading.value = false;
    }
}

async function handleSave() {
    if (!formRef.value) {
        return;
    }
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid) {
        return;
    }

    saving.value = true;
    try {
        // 用接口返回的最新配置做预览，而不是用表单值：保证预览与真正落库的内容一致
        const saved = await updateSiteConfig({ ...form });
        siteStore.apply(saved);
        ElMessage.success("保存成功，已即时预览新主题");
    } catch {
        // 失败原因由请求拦截器统一提示，表单不关闭以便修正
    } finally {
        saving.value = false;
    }
}

onMounted(loadConfig);
</script>

<style scoped>
.site-info__header {
    display: flex;
    align-items: baseline;
    gap: 12px;
}

.site-info__hint {
    font-size: 12px;
    color: #909399;
}

.site-info__form {
    max-width: 640px;
}

.site-info__color {
    margin-left: 12px;
    color: #909399;
    font-family: monospace;
}
</style>
