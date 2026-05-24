<template>
  <div class="page-container">
    <div class="page-header">
      <h3>用户管理</h3>
      <el-button type="primary" @click="showCreateDialog">新增用户</el-button>
    </div>

    <div class="search-bar">
      <el-input v-model="search.keyword" placeholder="搜索用户名/姓名" clearable
                style="width: 200px" @clear="loadUsers" @keyup.enter="loadUsers" />
      <el-select v-model="search.roleId" placeholder="角色筛选" clearable
                 style="width: 160px; margin-left: 12px" @change="loadUsers">
        <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
      </el-select>
      <el-button type="primary" @click="loadUsers" style="margin-left: 12px">查询</el-button>
    </div>

    <el-table :data="users" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag>{{ getRoleName(row.roleId) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" fixed="right" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button size="small" @click="handleResetPwd(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="handleDisable(row)"
                     :disabled="row.status === 0">禁用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pagination" background layout="total, prev, pager, next"
                   :total="total" :page-size="search.size" v-model:current-page="search.page"
                   @current-change="loadUsers" />

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属学院">
          <el-select v-model="form.collegeId" clearable placeholder="可选" style="width: 100%">
            <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, createUser, updateUser, disableUser, resetPassword, listRoles } from '../../api/admin'
import { listColleges } from '../../api/admin'

const loading = ref(false)
const users = ref([])
const roles = ref([])
const colleges = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const editingId = ref(null)

const search = reactive({ page: 1, size: 10, keyword: '', roleId: null })

const form = reactive({
  username: '', password: '', realName: '', roleId: null, collegeId: null
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadColleges(), loadUsers()])
})

async function loadUsers() {
  loading.value = true
  try {
    const res = await listUsers(search)
    users.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  const res = await listRoles()
  roles.value = res.data
}

async function loadColleges() {
  const res = await listColleges()
  colleges.value = res.data
}

function getRoleName(roleId) {
  return roles.value.find(r => r.id === roleId)?.roleName || '-'
}

function showCreateDialog() {
  isEdit.value = false
  editingId.value = null
  Object.assign(form, { username: '', password: '', realName: '', roleId: null, collegeId: null })
  dialogVisible.value = true
}

function showEditDialog(row) {
  isEdit.value = true
  editingId.value = row.id
  Object.assign(form, { username: row.username, password: '', realName: row.realName, roleId: row.roleId, collegeId: row.collegeId })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  if (isEdit.value) {
    await updateUser(editingId.value, { realName: form.realName, roleId: form.roleId, collegeId: form.collegeId })
    ElMessage.success('更新成功')
  } else {
    await createUser(form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  loadUsers()
}

async function handleDisable(row) {
  await ElMessageBox.confirm(`确定禁用用户 ${row.realName}？`, '提示', { type: 'warning' })
  await disableUser(row.id)
  ElMessage.success('已禁用')
  loadUsers()
}

async function handleResetPwd(row) {
  await ElMessageBox.confirm(`确定重置 ${row.realName} 的密码为 123456？`, '提示', { type: 'warning' })
  await resetPassword(row.id, '123456')
  ElMessage.success('密码已重置为 123456')
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h3 {
  margin: 0;
  font-size: 18px;
}

.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
