  <template>
    <div class="login-page">
        <el-card class="login-card" shadow="always">
            <div class="login-card__header">
                <h1 class="login-card__title">管理后台</h1>
                <p class="login-card__subtitle">请使用管理员账号登录</p>
            </div>

            <el-form
                ref="formRef"
                :model="form"
                :rules="rules"
                label-position="top"
                size="large"
                @submit.prevent="handleSubmit"
            >
                <el-form-item label="账号" prop="username">
                    <el-input
                        v-model.trim="form.username"
                        placeholder="请输入账号"
                        autocomplete="username"
                        clearable
                    />
                </el-form-item>

                <el-form-item label="密码" prop="password">
                    <el-input
                        v-model="form.password"
                        type="password"
                        placeholder="请输入密码"
                        autocomplete="current-password"
                        show-password
                    />
                </el-form-item>

                <el-form-item>
                    <el-button
                        class="login-card__submit"
                        type="primary"
                        native-type="submit"
                        :loading="submitting"
                    >
                        登录
                    </el-button>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { login } from '../api/user'
import type { LoginParams } from '../types/user'
import useUserStore from '../store/userStore'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive<LoginParams>({
    username: '',
    password: '',
})

const rules: FormRules = {
    username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' },
    ],
}

function resolveRedirect(): string {
    const redirect = route.query.redirect
    if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
        return redirect
    }
    return '/'
}

async function handleSubmit() {
    if (!formRef.value || submitting.value) {
        return
    }

    submitting.value = true
    try {
        const valid = await formRef.value.validate().catch(() => false)
        if (!valid) {
            return
        }

        const result = await login({ username: form.username, password: form.password })
        userStore.setToken(result.token)
        userStore.setUserInfo(result.userInfo ?? null)
        ElMessage.success('登录成功')
        await router.replace(resolveRedirect())
    } catch {
        // 失败原因由请求拦截器统一提示，此处只需结束 loading
    } finally {
        submitting.value = false
    }
}
</script>

<style scoped>
.login-page {
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    min-height: 100vh;
    padding: 24px;
    background: linear-gradient(135deg, #1f2a44 0%, #3a4a6b 100%);
}

.login-card {
    width: 100%;
    max-width: 400px;
}

.login-card__header {
    text-align: center;
}

.login-card__title {
    margin: 0;
    font-size: 22px;
    color: #303133;
}

.login-card__subtitle {
    margin: 8px 0 0;
    font-size: 13px;
    color: #909399;
}

.login-card__submit {
    width: 100%;
}
</style>
