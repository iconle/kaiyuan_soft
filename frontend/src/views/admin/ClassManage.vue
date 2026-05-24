<template>
  <div class="page-container">
    <div class="page-header">
      <h3>行政班级管理</h3>
      <el-button type="primary" @click="showDialog()">新增班级</el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="filterMajorId" placeholder="按专业筛选" clearable @change="loadData" style="width:240px">
        <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
      </el-select>
    </div>

    <el-table :data="classes" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="className" label="班级名称" width="240" />
      <el-table-column prop="majorName" label="所属专业" width="200" />
      <el-table-column prop="enrollmentYear" label="入学年份" width="100" />
      <el-table-column prop="studentCount" label="学生人数" width="100" />
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="showStudents(row)">管理学生</el-button>
          <el-button size="small" @click="showDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="page" :page-size="size" @current-change="loadData" />
    </div>

    <!-- Class dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑班级' : '新增班级'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="班级名称" required>
          <el-input v-model="form.className" placeholder="如：计算机科学与技术2501班" />
        </el-form-item>
        <el-form-item label="所属专业" required>
          <el-select v-model="form.majorId" style="width:100%">
            <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入学年份" required>
          <el-input-number v-model="form.enrollmentYear" :min="2010" :max="2030" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Students dialog -->
    <el-dialog v-model="studentDialogVisible" :title="`班级学生 - ${selectedClass?.className || ''}`" width="640px">
      <div style="margin-bottom:12px; display:flex; gap:8px">
        <el-select v-model="addStudentId" placeholder="选择学生" filterable style="flex:1">
          <el-option v-for="s in availableStudents" :key="s.id" :label="`${s.studentNo} ${s.name}`" :value="s.id" />
        </el-select>
        <el-button type="primary" @click="handleAddStudent" :disabled="!addStudentId">添加</el-button>
      </div>
      <el-table :data="classStudents" border stripe size="small" max-height="360">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" text type="danger" @click="handleRemoveStudent(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAdminClasses, createAdminClass, updateAdminClass, deleteAdminClass,
  getAdminClassStudents, addAdminClassStudent, removeAdminClassStudent,
  listMajors
} from '../../api/admin'
import { listStudents } from '../../api/academic'

const loading = ref(false)
const classes = ref([])
const majors = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filterMajorId = ref(null)

const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive({ className: '', majorId: null, enrollmentYear: 2025 })

const studentDialogVisible = ref(false)
const selectedClass = ref(null)
const classStudents = ref([])
const availableStudents = ref([])
const allStudents = ref([])
const addStudentId = ref(null)

onMounted(async () => {
  const mRes = await listMajors()
  majors.value = mRes.data?.records || []
  try {
    const sRes = await listStudents({ page: 1, size: 9999 })
    allStudents.value = sRes.data?.records || []
  } catch { allStudents.value = [] }
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await listAdminClasses({
      page: page.value, size: size.value,
      majorId: filterMajorId.value || undefined
    })
    classes.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function showDialog(row) {
  editing.value = row || null
  Object.assign(form, {
    className: row?.className || '',
    majorId: row?.majorId || null,
    enrollmentYear: row?.enrollmentYear || 2025
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (editing.value) {
      await updateAdminClass(editing.value.id, { ...form })
    } else {
      await createAdminClass({ ...form })
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } catch { /* handled */ }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除班级「${row.className}」？`, '提示', { type: 'warning' })
  await deleteAdminClass(row.id)
  ElMessage.success('已删除')
  loadData()
}

async function showStudents(row) {
  selectedClass.value = row
  const res = await getAdminClassStudents(row.id)
  classStudents.value = res.data || []
  const existingIds = new Set(classStudents.value.map(s => s.id))
  availableStudents.value = allStudents.value.filter(s => !existingIds.has(s.id))
  addStudentId.value = null
  studentDialogVisible.value = true
}

async function handleAddStudent() {
  if (!addStudentId.value || !selectedClass.value) return
  await addAdminClassStudent(selectedClass.value.id, addStudentId.value)
  ElMessage.success('学生已添加')
  showStudents(selectedClass.value)
}

async function handleRemoveStudent(row) {
  await ElMessageBox.confirm(`确定移除「${row.name}」？`, '提示', { type: 'warning' })
  await removeAdminClassStudent(selectedClass.value.id, row.id)
  ElMessage.success('已移除')
  showStudents(selectedClass.value)
}
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.page-header h3 { margin: 0; font-size: 18px; }
.filter-bar { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
