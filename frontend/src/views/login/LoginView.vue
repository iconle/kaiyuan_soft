<template>
  <div class="login-container">
    <div class="login-bg-orb login-bg-orb-left"></div>
    <div class="login-bg-orb login-bg-orb-right"></div>

    <div class="login-card">
      <div class="login-logo">
        <img src="../../assets/zjnu.png" alt="平台标识" />
      </div>

      <h2 class="login-title">OBE 达成度计算平台</h2>
      <p class="login-subtitle">面向专业认证的毕业要求达成度统一计算平台</p>

      <el-form
        ref="formRef"
        class="login-form"
        :model="form"
        :rules="rules"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            prefix-icon="User"
            size="large"
            class="login-input"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            show-password
            class="login-input"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleLogin"
            size="large"
            class="login-btn"
          >
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
import { getHomePath, getTeacherHomePath } from '../../utils/roleHome'

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



async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form)
    userStore.setLogin(res.data)
    localStorage.setItem('roleCode', res.data.roleCode)
    // 清理上一用户残留的班级选择,避免跨用户数据泄露
    localStorage.removeItem('activeClassId')
    ElMessage.success('登录成功')

    // TEACHER 角色需要先拿到自己的班级列表才能确定首页,避免误跳到不属于自己的班级
    if (res.data.roleCode === 'TEACHER') {
      try {
        const { default: request } = await import('../../utils/request')
        const classRes = await request.get('/api/teaching-classes/my-classes')
        const classes = classRes.data || []
        if (classes.length > 0) {
          const firstId = String(classes[0].id)
          localStorage.setItem('activeClassId', firstId)
          router.push(getTeacherHomePath(firstId))
          return
        }
        ElMessage.warning('您当前未任教任何教学班级,请联系教务管理员分配班级')
      } catch {
        // 接口失败时 fallback 到默认 route
      }
    }
    router.push(getHomePath(res.data.roleCode) || '/login')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  position: relative;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 28%, rgba(158, 137, 205, 0.16), transparent 32%),
    radial-gradient(circle at 82% 42%, rgba(242, 167, 179, 0.14), transparent 30%),
    radial-gradient(circle at 50% 82%, rgba(154, 211, 188, 0.10), transparent 34%),
    linear-gradient(135deg, #fbfafc 0%, #f7f5fb 48%, #ffffff 100%);
}

.login-bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(8px);
  opacity: 0.55;
  pointer-events: none;
}

.login-bg-orb-left {
  width: 260px;
  height: 260px;
  left: 12%;
  top: 22%;
  background: rgba(158, 137, 205, 0.16);
}

.login-bg-orb-right {
  width: 320px;
  height: 320px;
  right: 10%;
  bottom: 18%;
  background: rgba(242, 167, 179, 0.13);
}

.login-card {
  position: relative;
  z-index: 1;
  width: 440px;
  padding: 42px 46px 40px;
  box-sizing: border-box;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.90);
  border: 1px solid rgba(158, 137, 205, 0.16);
  box-shadow:
    0 22px 56px rgba(31, 41, 55, 0.11),
    0 8px 24px rgba(128, 107, 191, 0.09);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  transition: all 0.28s ease;
}

.login-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 34px 78px rgba(31, 41, 55, 0.14),
    0 14px 34px rgba(128, 107, 191, 0.14);
}

.login-logo {
  width: 72px;
  height: 72px;
  margin: 0 auto 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid rgba(158, 137, 205, 0.18);
  box-shadow: 0 12px 28px rgba(128, 107, 191, 0.18);
}

.login-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transform: scale(1.00);
}

.login-title {
  margin: 0;
  text-align: center;
  font-size: 34px;
  font-weight: 800;
  letter-spacing: 1px;
  color: transparent;
  background: linear-gradient(90deg, #3b256f 0%, #6f55b5 50%, #9e89cd 100%);
  -webkit-background-clip: text;
  background-clip: text;
}

.login-subtitle {
  margin: 14px 0 42px;
  text-align: center;
  font-size: 15px;
  color: #777084;
  line-height: 1.7;
  letter-spacing: 0.2px;
}

.login-form {
  width: 100%;
}

:deep(.login-input .el-input__wrapper) {
  min-height: 50px;
  border-radius: 16px;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 0 0 1px rgba(158, 137, 205, 0.16) inset;
  transition: all 0.2s ease;
}

:deep(.login-input .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(158, 137, 205, 0.32) inset;
}

:deep(.login-input .el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px rgba(128, 107, 191, 0.58) inset,
    0 8px 18px rgba(128, 107, 191, 0.12);
}

:deep(.login-input .el-input__inner) {
  font-size: 16px;
  color: var(--text-primary);
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-btn {
  width: 100%;
  height: 52px;
  margin-top: 8px;
  border: none;
  border-radius: 18px;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 8px;
  color: #ffffff;
  background: linear-gradient(135deg, #9e89cd, #806bbf) !important;
  box-shadow: 0 14px 28px rgba(128, 107, 191, 0.28);
  transition: all 0.2s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  background: linear-gradient(135deg, #a895d4, #735ab8) !important;
  box-shadow: 0 18px 34px rgba(128, 107, 191, 0.36) !important;
}

@media (max-width: 640px) {
  .login-card {
    width: calc(100% - 40px);
    padding: 46px 30px 42px;
  }

  .login-title {
    font-size: 28px;
  }

  .login-subtitle {
    font-size: 14px;
  }
}
</style>
