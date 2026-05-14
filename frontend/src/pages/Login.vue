<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-title">毕业要求达成度统一计算平台</div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" placeholder="请输入密码" type="password" />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option label="系统管理员" value="admin" />
            <el-option label="教务管理员" value="teach_admin" />
            <el-option label="专业负责人" value="prof_lead" />
            <el-option label="课程教师" value="teacher" />
          </el-select>
        </el-form-item>

        <el-button type="primary" native-type="submit" :loading="loading" class="login-btn">
          登录
        </el-button>
      </el-form>

      <div class="login-footer">
        <p>演示账号：admin / 123456</p>
        <p>© 2026 开元学堂 All Rights Reserved</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = ref({
  username: 'admin',
  password: '123456',
  role: 'admin'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true

  try {
    // 模拟登录 - 直接使用Mock数据
    if (form.value.username === 'admin' && form.value.password === '123456') {
      const mockUser = {
        token: 'mock-token-' + Date.now(),
        userName: form.value.username,
        roles: [form.value.role],
        permissions: getPermissionsByRole(form.value.role)
      }

      // 存储用户信息
      userStore.setUser(mockUser)
      ElMessage.success('登录成功！')

      // 直接跳转到仪表板
      router.push('/dashboard')
    } else {
      ElMessage.error('用户名或密码错误')
    }
  } catch (error) {
    ElMessage.error('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 根据角色返回权限
const getPermissionsByRole = (role) => {
  const permissions = {
    admin: ['view_dashboard', 'admin', 'data_manage', 'teach', 'calc'],
    teach_admin: ['view_dashboard', 'data_manage', 'calc'],
    prof_lead: ['view_dashboard', 'data_manage', 'calc'],
    teacher: ['view_dashboard', 'teach', 'calc']
  }
  return permissions[role] || []
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.login-title {
  text-align: center;
  font-size: 20px;
  font-weight: bold;
  color: #282c34;
}

.login-btn {
  width: 100%;
  margin-top: 20px;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  color: #909399;
  font-size: 12px;
}

.login-footer p {
  margin: 5px 0;
}
</style>

