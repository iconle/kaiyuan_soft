<template>
  <div class="page-container">
    <div class="page-header">
      <h3>学生管理</h3>
      <el-button @click="handleDownloadTemplate">下载模板</el-button>
      <el-button type="primary" @click="showImportDialog">导入 Excel</el-button>
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
        <el-table-column prop="collegeName" label="学院" width="220" class-name="nowrap-column" />
        <el-table-column prop="majorName" label="专业" width="240" class-name="nowrap-column" />
        <el-table-column prop="enrollmentYear" label="入学年份" width="85" />
        <el-table-column prop="adminClassName" label="行政班级" min-width="160" />
        <el-table-column
          label="操作"
          width="170"
          align="center"
          class-name="operation-column"
        >
          <template #default="{ row }">
            <div class="operation-actions">
              <el-button
                size="small"
                class="action-btn edit-btn"
                @click="showDialog(row)"
              >
                编辑
              </el-button>

              <el-button
                size="small"
                type="danger"
                class="action-btn delete-btn"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
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

    <!-- 导入 Excel 对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入学生名单" width="480px">
      <div class="import-tip">
        请先下载模板，按模板格式填写学生信息后上传。
      </div>
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx"
        :on-change="handleFileChange"
        :on-exceed="handleExceed"
        drag
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            只支持 .xlsx 格式，单次最多导入 1000 条记录
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing">确定导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { listStudents, createStudent, updateStudent, deleteStudent, listColleges, listMajors, listAdminClasses, downloadStudentTemplate, importStudentExcel } from '../../api/admin'

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

const importDialogVisible = ref(false)
const importing = ref(false)
const uploadRef = ref(null)
const selectedFile = ref(null)

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

function showImportDialog() {
  selectedFile.value = null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  importDialogVisible.value = true
}

function handleFileChange(file) {
  selectedFile.value = file.raw
}

function handleExceed() {
  ElMessage.warning('只能上传一个文件')
}

async function handleDownloadTemplate() {
  try {
    const res = await downloadStudentTemplate()
    const url = URL.createObjectURL(new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
    const link = document.createElement('a')
    link.href = url
    link.download = '学生名单导入模板.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载模板失败')
  }
}

async function handleImport() {
  if (!selectedFile.value) {
    ElMessage.warning('请选择要导入的文件')
    return
  }

  importing.value = true
  try {
    const res = await importStudentExcel(selectedFile.value)
    ElMessage.success(`成功导入 ${res.data} 条学生记录`)
    importDialogVisible.value = false
    loadData()
  } catch (e) {
    const errorMsg = e.response?.data?.msg || e.message || '导入失败'
    ElMessage.error(errorMsg)
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header { display: flex; align-items: center; gap: var(--space-4); margin-bottom: var(--space-4); }
.page-header h3 { margin: 0; font-size: var(--text-lg); }
.filter-bar { margin-bottom: var(--space-4); display: flex; align-items: center; flex-wrap: wrap; gap: 8px 0; }
.pagination-wrap { margin-top: var(--space-4); display: flex; justify-content: flex-end; }

:deep(.nowrap-column .cell) {
  white-space: nowrap;
}
:deep(.operation-column .cell) {
  display: flex;
  justify-content: center;
  align-items: center;
}

.operation-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  white-space: nowrap;
}

.action-btn {
  height: 25px;
  padding: 0 18px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

:deep(.action-btn.el-button) {
  margin-left: 0;
}

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
.delete-btn {
  color: #fff;
  border-color: #ef9aa0;
  background-color: #ef9aa0;
}

.delete-btn:hover {
  color: #fff;
  border-color: #e78087;
  background-color: #e78087;
}

.import-tip {
  margin-bottom: 16px;
  padding: 10px 14px;
  border-radius: 8px;
  background: rgba(128, 107, 191, 0.08);
  color: #806bbf;
  font-size: 13px;
}

:deep(.el-upload-dragger) {
  border: 2px dashed #d8c9f3;
  border-radius: 12px;
  background: #fafbff;
}

:deep(.el-upload-dragger:hover) {
  border-color: #806bbf;
  background: #f6f0ff;
}
</style>
