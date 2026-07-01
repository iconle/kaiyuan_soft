<template>
  <div class="page-container">
    <div class="page-header">
      <h3>用户管理</h3>
      <el-button type="primary" @click="showCreateDialog">新增用户</el-button>
    </div>

    <div class="content-card">
      <div class="search-bar">
        <el-input v-model="search.keyword" placeholder="搜索用户名/姓名" clearable
                  style="width: 200px" @clear="loadUsers" @keyup.enter="loadUsers" />
        <el-select v-model="search.roleId" placeholder="角色筛选" clearable
                   style="width: 160px; margin-left: 12px" @change="loadUsers">
          <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
        </el-select>
        <el-button type="primary" @click="loadUsers" style="margin-left: 12px">查询</el-button>
      </div>

      <el-table
        :data="users"
        :key="users.length"
        border
        stripe
        v-loading="loading"
        class="user-table"
      >
        <el-table-column type="index" label="ID" width="70" :index="indexMethod" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column
          prop="realName"
          label="姓名"
          min-width="120"
          class-name="name-column"
        />
        <el-table-column
          label="角色"
          width="140"
          align="center"
          class-name="role-column"
        >
          <template #default="{ row }">
            <span :class="['role-pill', getRoleClass(row.roleId)]">
              {{ getRoleName(row.roleId) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="120">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          fixed="right"
          width="360"
          align="center"
          class-name="operation-column"
        >
          <template #default="{ row }">
            <div class="operation-actions">
              <el-button
                size="small"
                class="action-btn edit-btn"
                @click="showEditDialog(row)"
              >
                编辑
              </el-button>

              <el-button
                size="small"
                class="action-btn reset-btn"
                @click="handleResetPwd(row)"
              >
                重置密码
              </el-button>

              <el-button
                size="small"
                :type="row.status === 1 ? 'danger' : 'success'"
                :class="[
                  'action-btn',
                  row.status === 1 ? 'disable-btn' : 'enable-btn'
                ]"
                @click="handleToggleStatus(row)"
                :disabled="row.status === 1 && isCurrentUser(row)"
                :title="row.status === 1 && isCurrentUser(row) ? '不能禁用当前登录用户' : ''"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>

              <el-button
                size="small"
                type="danger"
                class="action-btn delete-btn"
                @click="handleDelete(row)"
                :disabled="isCurrentUser(row)"
                :title="isCurrentUser(row) ? '不能删除当前登录用户' : ''"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" background layout="total, prev, pager, next"
                     :total="total" :page-size="search.size" v-model:current-page="search.page"
                     @current-change="loadUsers" />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="min(480px, 92vw)">
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
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, createUser, updateUser, disableUser, enableUser, deleteUser, resetPassword, listRoles } from '../../api/admin'
import { listColleges } from '../../api/admin'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
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

const currentUserId = computed(() => userStore.userId)

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

function indexMethod(i) {
  return (search.page - 1) * search.size + i + 1
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
function getRoleClass(roleId) {
  const roleName = getRoleName(roleId)

  if (roleName.includes('系统管理员')) return 'role-admin'
  if (roleName.includes('教务管理员')) return 'role-academic'
  if (roleName.includes('专业负责人')) return 'role-director'
  if (roleName.includes('主讲教师')) return 'role-teacher'

  return 'role-default'
}
function isCurrentUser(row) {
  return currentUserId.value != null && String(row.id) === String(currentUserId.value)
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.split('T')[0]
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

async function handleToggleStatus(row) {
  if (row.status === 1 && isCurrentUser(row)) {
    ElMessage.warning('不能禁用当前登录用户')
    return
  }
  if (row.status === 1) {
    await ElMessageBox.confirm(`确定禁用用户 ${row.realName}？`, '提示', { type: 'warning' })
    await disableUser(row.id)
    ElMessage.success('已禁用')
  } else {
    await ElMessageBox.confirm(`确定启用用户 ${row.realName}？`, '提示', { type: 'warning' })
    await enableUser(row.id)
    ElMessage.success('已启用')
  }
  loadUsers()
}

async function handleDelete(row) {
  if (isCurrentUser(row)) {
    ElMessage.warning('不能删除当前登录用户')
    return
  }
  await ElMessageBox.confirm(`确定删除用户 ${row.realName}？删除后不可恢复。`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
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
  padding: var(--space-5);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.page-header h3 {
  margin: 0;
  font-size: var(--text-lg);
}

.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: var(--space-4);
}

.pagination {
  margin-top: var(--space-4);
  justify-content: flex-end;
}

:deep(.name-column .cell) {
  line-height: 1.6;
  white-space: nowrap;
  overflow: visible;
}
/* 用户管理表格优化 */
.user-table {
  width: 100%;
  border-radius: 14px;
  overflow: visible;
}

:deep(.user-table .el-table__header th) {
  height: 46px;
  background-color: #f7f7fa;
  color: #606266;
  font-weight: 700;
}

:deep(.user-table .el-table__row td) {
  height: 64px;
}

:deep(.user-table .el-table__row:hover td) {
  background-color: #fbf8ff;
}

/* 角色标签：不同角色不同颜色 */
:deep(.role-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.role-pill {
  min-width: 86px;
  height: 25px;
  padding: 0 14px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  line-height: 30px;
}

.role-admin {
  color: #d36bad;
  background: #f3d3f063;
  border: 1px solid #ffd4fe85;
}

.role-academic {
  color: #2f9e9b;
  background: #eafafa;
  border: 1px solid #c9eeee;
}

.role-director {
  color: #3f7edb;
  background: #eef5ff;
  border: 1px solid #cfe2ff;
}

.role-teacher {
  color: #d58a2f;
  background: #fff6e8;
  border: 1px solid #f4ddb6;
}

.role-default {
  color: #606266;
  background: #f4f4f5;
  border: 1px solid #e4e7ed;
}

/* 操作列居中 */
:deep(.operation-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.operation-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  white-space: nowrap;
}

.action-btn {
  height: 25px;
  padding: 0 16px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

:deep(.action-btn.el-button) {
  margin-left: 0;
}

/* 编辑：紫色 */
.edit-btn {
  color: #7e57c2;
  border-color: #d8c9f3;
  background-color: #f6f0ff;
}

.edit-btn:hover {
  color: #6f42c1;
  border-color: #b79dea;
  background-color: #efe6ff;
}

/* 重置密码：橙色 */
.reset-btn {
  color: #d58a2f;
  border-color: #f2d0a4;
  background-color: #fff7ec;
}

.reset-btn:hover {
  color: #bf7420;
  border-color: #edbd7c;
  background-color: #fff0dc;
}

/* 禁用：粉红色 */
.disable-btn {
  color: #fff;
  border-color: #ef9aa0;
  background-color: #ef9aa0;
}

.disable-btn:hover {
  color: #fff;
  border-color: #e78087;
  background-color: #e78087;
}

.disable-btn.is-disabled,
.disable-btn.is-disabled:hover {
  color: #fff;
  border-color: #f6d4d7;
  background-color: #f6d4d7;
  opacity: 0.65;
}

/* 启用：绿色 */
.enable-btn {
  color: #3f8f5f;
  border-color: #b9dfc7;
  background-color: #f0fbf4;
}

.enable-btn:hover {
  color: #2f7a4e;
  border-color: #91cfa8;
  background-color: #e3f8ea;
}

/* 删除：红色 */
.delete-btn {
  color: #fff;
  border-color: #d95f68;
  background-color: #d95f68;
}

.delete-btn:hover {
  color: #fff;
  border-color: #c84c55;
  background-color: #c84c55;
}
</style>
