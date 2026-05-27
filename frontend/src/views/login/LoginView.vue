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
  password: ''
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
  background: #f0f2f5;
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.login-title {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.login-subtitle {
  text-align: center;
  font-size: 13px;
  color: #909399;
  margin-bottom: 32px;
}

.login-btn {
  width: 100%;
}
</style>
