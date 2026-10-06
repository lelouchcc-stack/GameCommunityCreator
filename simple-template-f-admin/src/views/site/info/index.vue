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
                        placeholder="显示在浏览器标题、页头与页脚"
                    />
                </el-form-item>
                <el-form-item label="游戏名称" prop="gameName">
                    <el-input
                        v-model.trim="form.gameName"
                        maxlength="30"
                        show-word-limit
                        placeholder="显示在页头副标题与首页游戏简介区块"
                    />
                </el-form-item>
                <el-form-item label="站点简介" prop="siteIntro">
                    <el-input
                        v-model="form.siteIntro"
                        type="textarea"
                        :rows="3"
                        maxlength="200"
                        show-word-limit
                        placeholder="显示在首页游戏简介区块与页脚"
                    />
                </el-form-item>
                <el-form-item label="站点 Logo" prop="logo">
                    <ImageUpload v-model="form.logo" />
                    <div class="site-info__tip">不上传也可以：页头会用站点名称首字生成徽标兜底</div>
                </el-form-item>
                <el-form-item label="版权文案" prop="copyright">
                    <el-input
                        v-model="form.copyright"
                        maxlength="100"
                        show-word-limit
                        placeholder="留空时按「站点名称 + 年份」自动生成"
                    />
                </el-form-item>
                <el-form-item label="主题主色" prop="primaryColor">
                    <el-color-picker v-model="form.primaryColor" />
                    <span class="site-info__color">{{ form.primaryColor }}</span>
                </el-form-item>
                <el-form-item label="默认深色模式" prop="darkMode">
                    <el-switch v-model="form.darkMode" />
                </el-form-item>

                <el-divider content-position="left">导航菜单</el-divider>
                <el-form-item label=" " class="site-info__nav-item">
                    <div class="site-info__nav">
                        <div class="site-info__nav-hint">
                            页头与页脚共用的导航。地址填 <code>/</code> 开头的站内路径或 <code>http(s)</code> 外链；
                            全部删空则回落到内置的「首页 / 攻略与资料 / 社区板块 / 公告」。
                        </div>
                        <div
                            v-for="(item, index) in navItems"
                            :key="index"
                            class="site-info__nav-row"
                        >
                            <el-input v-model.trim="item.label" maxlength="12" placeholder="名称" style="width: 180px" />
                            <el-input v-model.trim="item.url" maxlength="100" placeholder="/guide" style="width: 280px" />
                            <el-button size="small" :disabled="index === 0" @click="moveNav(index, -1)">上移</el-button>
                            <el-button size="small" :disabled="index === navItems.length - 1" @click="moveNav(index, 1)">
                                下移
                            </el-button>
                            <el-button size="small" type="danger" plain @click="removeNav(index)">删除</el-button>
                        </div>
                        <el-button size="small" :disabled="navItems.length >= 8" @click="addNav">
                            + 新增导航项
                        </el-button>
                    </div>
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
import ImageUpload from "../../../components/ImageUpload.vue";
import { getSiteConfig, updateSiteConfig } from "../../../api/site";
import useSiteStore from "../../../store/siteStore";
import type { SiteConfig, SiteNavItem } from "../../../types/site";

const siteStore = useSiteStore();
const loading = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();

const form = reactive<SiteConfig>({
    siteName: "",
    gameName: "",
    siteIntro: "",
    logo: "",
    navItems: [],
    copyright: "",
    primaryColor: "#5B5FE9",
    darkMode: false,
});

/** 导航菜单单独拿一层引用：用 v-for 直接改 form.navItems 的元素在 reactive 里不总是可靠 */
const navItems = ref<SiteNavItem[]>([]);

const rules: FormRules = {
    siteName: [{ required: true, message: "请输入站点名称", trigger: "blur" }],
    primaryColor: [{ required: true, message: "请选择主题主色", trigger: "change" }],
};

async function loadConfig() {
    loading.value = true;
    try {
        const data = await getSiteConfig();
        Object.assign(form, data);
        navItems.value = (data.navItems ?? []).map((item) => ({ ...item }));
    } finally {
        loading.value = false;
    }
}

function addNav() {
    navItems.value.push({ label: "", url: "/" });
}

function removeNav(index: number) {
    navItems.value.splice(index, 1);
}

function moveNav(index: number, delta: number) {
    const next = index + delta;
    const item = navItems.value[index];
    if (next < 0 || next >= navItems.value.length || !item) {
        return;
    }
    navItems.value.splice(index, 1);
    navItems.value.splice(next, 0, item);
}

/** 空行是"还没填完"，提交前拦住并给出明确提示，避免存进一条点不动的导航 */
function validateNav(): boolean {
    for (const item of navItems.value) {
        if (!item.label.trim()) {
            ElMessage.warning("导航菜单里有项目还没填名称");
            return false;
        }
        if (!/^(\/|https?:\/\/)/.test(item.url.trim())) {
            ElMessage.warning(`导航「${item.label}」的地址要以 / 开头或填 http(s) 外链`);
            return false;
        }
    }
    return true;
}

async function handleSave() {
    if (!formRef.value) {
        return;
    }
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid || !validateNav()) {
        return;
    }

    saving.value = true;
    try {
        // 导航项单独维护了一份副本，提交前合并回表单再发出去
        form.navItems = navItems.value.map((item) => ({ label: item.label.trim(), url: item.url.trim() }));
        // 用接口返回的最新配置做预览，而不是用表单值：保证预览与真正落库的内容一致
        const saved = await updateSiteConfig({ ...form, navItems: form.navItems });
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
    max-width: 720px;
}

.site-info__color {
    margin-left: 12px;
    color: #909399;
    font-family: monospace;
}

.site-info__tip {
    width: 100%;
    margin-top: 4px;
    color: #909399;
    font-size: 12px;
    line-height: 1.6;
}

.site-info__nav {
    width: 100%;
}

.site-info__nav-hint {
    margin-bottom: 8px;
    color: #909399;
    font-size: 12px;
    line-height: 1.7;
}

.site-info__nav-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    margin-bottom: 8px;
}
</style>
