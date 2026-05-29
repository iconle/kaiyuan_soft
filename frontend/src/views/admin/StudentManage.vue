<template>
  <div class="page-container">
    <div class="page-header">
      <h3>学生管理</h3>
      <el-button type="primary" @click="showDialog()">新增学生</el-button>
    </div>

    <div class="content-card">
      <div class="filter-bar">
        <el-select v-model="filterCollegeId" placeholder="按学院筛选" clearable @change="loadData" style="width:200px">
          <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="filterMajorId" placeholder="按专业筛选" clearable @change="loadData" style="width:200px;margin-left:8px">
          <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
        </el-select>
        <el-select v-model="filterAdminClassId" placeholder="按行政班级筛选" clearable @change="loadData" style="width:200px;margin-left:8px">
          <el-option v-for="c in adminClasses" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
        <el-select v-model="filterEnrollmentYear" placeholder="按入学年份筛选" clearable @change="loadData" style="width:140px;margin-left:8px">
          <el-option v-for="y in enrollmentYears" :key="y" :label="String(y)" :value="y" />
        </el-select>
        <el-input v-model="filterKeyword" placeholder="搜索学号/姓名" clearable @input="onSearch" style="width:180px;margin-left:8px" />
      </div>

      <el-table :data="students" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="55" />
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="collegeName" label="学院" width="140" />
        <el-table-column prop="majorName" label="专业" width="160" />
        <el-table-column prop="enrollmentYear" label="入学年份" width="85" />
        <el-table-column prop="adminClassName" label="行政班级" min-width="160" />
        <el-table-column label="操作" width="140">
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑学生' : '新增学生'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="学号" required>
          <el-input v-model="form.studentNo" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="学院">
          <el-select v-model="form.collegeId" clearable style="width:100%" @change="onCollegeChange">
            <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-select v-model="form.majorId" clearable style="width:100%">
            <el-option v-for="m in filteredMajors" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入学年份">
          <el-input-number v-model="form.enrollmentYear" :min="2010" :max="2030" style="width:100%" />
        </el-form-item>
        <el-form-item label="行政班级">
          <el-select v-model="form.adminClassId" clearable style="width:100%">
            <el-option v-for="c in adminClasses" :key="c.id" :label="c.className" :value="c.id" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listStudents, createStudent, updateStudent, deleteStudent, listColleges, listMajors, listAdminClasses } from '../../api/admin'

const loading = ref(false)
const students = ref([])
const colleges = ref([])
const majors = ref([])
const adminClasses = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filterCollegeId = ref(null)
const filterMajorId = ref(null)
const filterAdminClassId = ref(null)
const filterEnrollmentYear = ref(null)
const filterKeyword = ref('')

const enrollmentYears = [2021, 2022, 2023, 2024, 2025, 2026]

const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive({ studentNo: '', name: '', collegeId: null, majorId: null, enrollmentYear: 2024, adminClassId: null })

let searchTimer = null

const filteredMajors = computed(() => {
  if (!form.collegeId) return majors.value
  return majors.value.filter(m => m.collegeId === form.collegeId)
})

onMounted(async () => {
  const [cRes, mRes, aRes] = await Promise.all([
    listColleges(),
    listMajors(),
    listAdminClasses({ page: 1, size: 999 })
  ])
  colleges.value = cRes.data || []
  majors.value = mRes.data?.records || []
  adminClasses.value = aRes.data?.records || []
  loadData()
})

function onCollegeChange() {
  form.majorId = null
}

function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadData(), 300)
}

async function loadData() {
  loading.value = true
  try {
    const res = await listStudents({
      page: page.value, size: size.value,
      keyword: filterKeyword.value || undefined,
      collegeId: filterCollegeId.value || undefined,
      majorId: filterMajorId.value || undefined,
      adminClassId: filterAdminClassId.value || undefined,
      enrollmentYear: filterEnrollmentYear.value || undefined
    })
    students.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function showDialog(row) {
  editing.value = row || null
  Object.assign(form, {
    studentNo: row?.studentNo || '',
    name: row?.name || '',
    collegeId: row?.collegeId || null,
    majorId: row?.majorId || null,
    enrollmentYear: row?.enrollmentYear || 2024,
    adminClassId: row?.adminClassId || null
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (editing.value) {
      await updateStudent(editing.value.id, { ...form })
    } else {
      await createStudent({ ...form })
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } catch { /* handled */ }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除学生「${row.name}(${row.studentNo})」？`, '提示', { type: 'warning' })
  await deleteStudent(row.id)
  ElMessage.success('已删除')
  loadData()
}
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header { display: flex; align-items: center; gap: var(--space-4); margin-bottom: var(--space-4); }
.page-header h3 { margin: 0; font-size: var(--text-lg); }
.filter-bar { margin-bottom: var(--space-4); display: flex; align-items: center; flex-wrap: wrap; gap: 8px 0; }
.pagination-wrap { margin-top: var(--space-4); display: flex; justify-content: flex-end; }
</style>
