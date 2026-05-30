<template>
  <div class="page-container">
    <div class="page-header">
      <h3>教学班级管理</h3>
      <el-button type="primary" @click="showDialog()">新增教学班级</el-button>
    </div>

    <div class="content-card">
      <div class="filter-bar">
        <el-select v-model="filterCourseId" placeholder="按课程筛选" clearable @change="loadData" style="width:200px">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="filterSemesterId" placeholder="按学期筛选" clearable @change="loadData" style="width:220px;margin-left:8px">
          <el-option v-for="s in semesters" :key="s.id" :label="s.label" :value="s.id" />
        </el-select>
      </div>

      <el-table class="wide-class-table" :data="classes" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="className" label="班级名称" min-width="260" class-name="nowrap-column" />
        <el-table-column prop="courseName" label="所属课程" min-width="260" class-name="nowrap-column" />
        <el-table-column prop="teacherName" label="主讲教师" min-width="130" class-name="nowrap-column" />
        <el-table-column label="学生管理" min-width="120" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="manageStudents(row)">管理学生</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text @click="showDialog(row)">编辑</el-button>
            <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          v-model:current-page="page" :page-size="size" @current-change="loadData" />
      </div>
    </div>

    <!-- Class edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑教学班级' : '新增教学班级'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="班级名称" required><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="所属课程" required><el-select v-model="form.courseId" style="width:100%"><el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="主讲教师" required><el-select v-model="form.teacherId" style="width:100%"><el-option v-for="t in teachers" :key="t.id" :label="`${t.realName} (${t.username})`" :value="t.id" /></el-select></el-form-item>
        <el-form-item label="开课学期" required><el-select v-model="form.semesterId" style="width:100%"><el-option v-for="s in semesters" :key="s.id" :label="s.label" :value="s.id" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template>
    </el-dialog>

    <!-- Student management dialog -->
    <el-dialog v-model="studentDialogVisible" :title="`学生管理 - ${currentClass?.className || ''}`" width="800px" append-to-body>
      <el-tabs v-model="studentTab">
        <el-tab-pane label="添加学生" name="add">
          <div class="student-filter-bar">
            <el-select v-model="stuFilterCollegeId" placeholder="学院" clearable style="width:150px" @change="loadAvailable">
              <el-option v-for="c in stuColleges" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-select v-model="stuFilterMajorId" placeholder="专业" clearable style="width:150px" @change="loadAvailable">
              <el-option v-for="m in stuMajors" :key="m.id" :label="m.name" :value="m.id" />
            </el-select>
            <el-select v-model="stuFilterAdminClassId" placeholder="行政班级" clearable style="width:170px" @change="loadAvailable">
              <el-option v-for="c in stuAdminClasses" :key="c.id" :label="c.className" :value="c.id" />
            </el-select>
            <el-select v-model="stuFilterYear" placeholder="入学年份" clearable style="width:110px" @change="loadAvailable">
              <el-option v-for="y in [2021,2022,2023,2024,2025,2026]" :key="y" :label="String(y)" :value="y" />
            </el-select>
            <el-input v-model="stuFilterKeyword" placeholder="学号/姓名" clearable style="width:140px" @input="onStuSearch" />
          </div>
          <div style="margin:8px 0">
            <el-button type="primary" size="small" :disabled="selectedStuIds.length === 0" @click="handleBatchAdd">批量添加 ({{ selectedStuIds.length }})</el-button>
            <el-button size="small" @click="selectAll">全选</el-button>
          </div>
          <el-table :data="availableStudents" ref="availTable" border size="small" max-height="280"
            @selection-change="onSelectionChange">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="studentNo" label="学号" width="130" />
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="collegeName" label="学院" width="140" />
            <el-table-column prop="majorName" label="专业" width="150" />
            <el-table-column prop="enrollmentYear" label="入学年份" width="80" />
            <el-table-column prop="adminClassName" label="行政班级" width="150" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="已分配学生" name="list">
          <el-table :data="classStudents" border size="small" max-height="360">
            <el-table-column prop="studentNo" label="学号" width="130" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }"><el-button size="small" type="danger" @click="handleRemoveStudent(row)">移除</el-button></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTeachingClasses, createTeachingClass, updateTeachingClass, deleteTeachingClass, listUsers } from '../../api/admin'
import { listCourses, listSemesters } from '../../api/academic'
import request from '../../utils/request'

const loading = ref(false)
const classes = ref([])
const courses = ref([])
const teachers = ref([])
const semesters = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filterCourseId = ref(null)
const filterSemesterId = ref(null)

const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive({ className: '', courseId: null, teacherId: null, semesterId: null })

const studentDialogVisible = ref(false)
const currentClass = ref(null)
const classStudents = ref([])
const availableStudents = ref([])
const selectedStuIds = ref([])
const studentTab = ref('add')
const stuColleges = ref([])
const stuMajors = ref([])
const stuAdminClasses = ref([])
const stuFilterCollegeId = ref(null)
const stuFilterMajorId = ref(null)
const stuFilterAdminClassId = ref(null)
const stuFilterYear = ref(null)
const stuFilterKeyword = ref('')
let stuSearchTimer = null

onMounted(async () => {
  const [cRes, sRes, uRes] = await Promise.all([
    listCourses({ page: 1, size: 999 }),
    listSemesters(),
    listUsers({ page: 1, size: 999 })
  ])
  courses.value = cRes.data?.records || []
  semesters.value = sRes.data || []
  teachers.value = (uRes.data?.records || []).filter(u => u.roleCode === 'TEACHER')
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await listTeachingClasses({
      page: page.value, size: size.value,
      courseId: filterCourseId.value || undefined,
      semesterId: filterSemesterId.value || undefined
    })
    classes.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function showDialog(row) {
  editing.value = row || null
  Object.assign(form, { className: row?.className || '', courseId: row?.courseId || null, teacherId: row?.teacherId || null, semesterId: row?.semesterId || null })
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (editing.value) { await updateTeachingClass(editing.value.id, { ...form }) }
    else { await createTeachingClass({ ...form }) }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } catch { /* handled */ }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除班级「${row.className}」？`, '提示', { type: 'warning' })
  await deleteTeachingClass(row.id)
  ElMessage.success('已删除')
  loadData()
}

async function manageStudents(cls) {
  currentClass.value = cls
  studentTab.value = 'add'
  selectedStuIds.value = []
  // Load class students
  const clsRes = await request.get(`/api/teaching-classes/${cls.id}/students`)
  classStudents.value = clsRes.data || []
  // Load filters
  const [cRes, mRes, aRes] = await Promise.all([
    request.get('/api/dict/colleges'),
    request.get('/api/dict/majors', { params: { page: 1, size: 999 } }),
    request.get('/api/dict/admin-classes', { params: { page: 1, size: 999 } })
  ])
  stuColleges.value = cRes.data || []
  stuMajors.value = mRes.data?.records || []
  stuAdminClasses.value = aRes.data?.records || []
  loadAvailable()
  studentDialogVisible.value = true
}

function onStuSearch() {
  clearTimeout(stuSearchTimer)
  stuSearchTimer = setTimeout(() => loadAvailable(), 300)
}

async function loadAvailable() {
  const existingIds = new Set(classStudents.value.map(s => s.id))
  const params = { page: 1, size: 9999 }
  if (stuFilterCollegeId.value) params.collegeId = stuFilterCollegeId.value
  if (stuFilterMajorId.value) params.majorId = stuFilterMajorId.value
  if (stuFilterAdminClassId.value) params.adminClassId = stuFilterAdminClassId.value
  if (stuFilterYear.value) params.enrollmentYear = stuFilterYear.value
  if (stuFilterKeyword.value) params.keyword = stuFilterKeyword.value
  const res = await request.get('/api/students', { params })
  const all = res.data?.records || []
  availableStudents.value = all.filter(s => !existingIds.has(s.id))
  selectedStuIds.value = []
}

function onSelectionChange(rows) {
  selectedStuIds.value = rows.map(r => r.id)
}

function selectAll() {
  const table = document.querySelector('.el-table__body-wrapper table')
  // Toggle all via the table ref
  if (selectedStuIds.value.length === availableStudents.value.length) {
    selectedStuIds.value = []
  } else {
    selectedStuIds.value = availableStudents.value.map(s => s.id)
  }
}

async function handleBatchAdd() {
  if (selectedStuIds.value.length === 0 || !currentClass.value) return
  let added = 0
  for (const sid of selectedStuIds.value) {
    try {
      await request.post(`/api/teaching-classes/${currentClass.value.id}/students/${sid}`)
      added++
    } catch { /* skip duplicates */ }
  }
  ElMessage.success(`已添加 ${added} 名学生`)
  const clsRes = await request.get(`/api/teaching-classes/${currentClass.value.id}/students`)
  classStudents.value = clsRes.data || []
  loadAvailable()
}

async function handleRemoveStudent(row) {
  await ElMessageBox.confirm(`确定移除「${row.name}」？`, '提示', { type: 'warning' })
  await request.delete(`/api/teaching-classes/${currentClass.value.id}/students/${row.id}`)
  ElMessage.success('已移除')
  manageStudents(currentClass.value)
}
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header { display: flex; align-items: center; gap: var(--space-4); margin-bottom: var(--space-4); }
.page-header h3 { margin: 0; font-size: var(--text-lg); }
.filter-bar { margin-bottom: var(--space-4); display: flex; }
.pagination-wrap { margin-top: var(--space-4); display: flex; justify-content: flex-end; }
.wide-class-table { width: 100%; }
:deep(.nowrap-column .cell) { white-space: nowrap; }
</style>
