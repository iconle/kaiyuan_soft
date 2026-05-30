<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">OBE 达成度计算平台</h2>
      <p class="login-subtitle">面向专业认证的毕业要求达成度统一计算平台</p>

      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码"
                    prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin"
                     size="large" class="login-btn">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../../api/auth'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '123456'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function getHomeRoute(roleCode) {
  switch (roleCode) {
    case 'ADMIN': return '/admin/users'
    case 'ACADEMIC': return '/academic/courses'
    case 'DIRECTOR': return '/director/grad-req'
    case 'TEACHER': return '/teacher/1/objectives'
    default: return '/login'
  }
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form)
    userStore.setLogin(res.data)
    localStorage.setItem('roleCode', res.data.roleCode)
    ElMessage.success('登录成功')
    router.push(getHomeRoute(res.data.roleCode))
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--gray-0);
  background-image:
    radial-gradient(ellipse at 20% 30%, rgba(155, 142, 196, 0.10) 0%, transparent 55%),
    radial-gradient(ellipse at 80% 50%, rgba(240, 181, 164, 0.08) 0%, transparent 55%),
    radial-gradient(ellipse at 50% 80%, rgba(168, 216, 200, 0.06) 0%, transparent 45%);
}

.login-card {
  width: 440px;
  padding: 56px 48px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--gray-150);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.login-card:hover {
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.08),
    0 8px 24px rgba(155, 142, 196, 0.10);
  transform: translateY(-2px);
}

.login-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.login-subtitle {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 40px;
  line-height: 1.6;
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: var(--radius-base);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
}

.login-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(155, 142, 196, 0.45) !important;
}
</style>
