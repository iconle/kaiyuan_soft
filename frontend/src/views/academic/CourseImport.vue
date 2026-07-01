<template>
  <div class="page-container">
    <div class="page-header">
      <h3>课程体系管理</h3>
      <el-select v-model="filterMajorId" placeholder="专业筛选" clearable style="width:200px" @change="loadCourses">
        <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索课程名/代码" clearable style="width:200px"
                @keyup.enter="loadCourses" @clear="loadCourses" />
      <el-button @click="loadCourses">查询</el-button>
      <el-button type="primary" @click="showCourseDialog()">新增课程</el-button>
    </div>

    <div class="content-card">
      <el-table :data="courses" border stripe v-loading="loading">
        <el-table-column prop="code" label="课程代码" width="110" />
        <el-table-column
          prop="name"
          label="课程名称"
          min-width="240"
          show-overflow-tooltip
        />
        <el-table-column prop="credit" label="学分" width="70" />
        <el-table-column prop="hoursTheory" label="理论学时" width="85" />
        <el-table-column prop="hoursExperiment" label="实验学时" width="85" />
        <el-table-column
          prop="category"
          label="类别"
          width="110"
          align="center"
          class-name="course-category-column"
        />
        <el-table-column
          label="操作"
          width="240"
          align="center"
          class-name="course-action-column"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              class="edit-course-btn"
              @click="showCourseDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              class="class-manage-btn"
              @click="showClasses(row)"
            >
              教学班级
            </el-button>
            <el-button size="small" type="danger" class="delete-course-btn" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" background layout="total, prev, pager, next"
                     :total="total" :page-size="search.size" v-model:current-page="search.page"
                     @current-change="loadCourses" />
    </div>

    <!-- Course edit dialog -->
    <el-dialog v-model="courseDialogVisible" :title="editingCourse ? '编辑课程' : '新增课程'" width="min(480px, 92vw)">
      <el-form :model="courseForm" label-width="90px">
        <el-form-item label="课程代码"><el-input v-model="courseForm.code" /></el-form-item>
        <el-form-item label="课程名称" required><el-input v-model="courseForm.name" /></el-form-item>
        <el-form-item label="学分"><el-input-number v-model="courseForm.credit" :min="0" :step="0.5" :precision="1" style="width:100%" /></el-form-item>
        <el-form-item label="理论学时"><el-input-number v-model="courseForm.hoursTheory" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="实验学时"><el-input-number v-model="courseForm.hoursExperiment" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="类别"><el-select v-model="courseForm.category" style="width:100%"><el-option label="必修" value="必修" /><el-option label="选修" value="选修" /></el-select></el-form-item>
        <el-form-item label="所属专业"><el-select v-model="courseForm.majorId" style="width:100%"><el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="courseDialogVisible = false">取消</el-button><el-button type="primary" @click="handleCourseSubmit">确定</el-button></template>
    </el-dialog>
    <!-- Class viewer dialog (read-only) -->
    <el-dialog
      v-model="classDialogVisible"
      class="teaching-class-dialog"
      :title="`教学班级 - ${currentCourse?.name || ''}`"
      width="min(620px, 92vw)"
    >
      <div class="class-dialog-tip">
        当前课程已关联的教学班级如下，可点击“查看学生”查看班级学生名单。
      </div>

      <el-table
        :data="classList"
        class="class-list-table"
        border
        size="default"
        empty-text="暂无教学班级"
      >
        <el-table-column prop="className" label="班级名称" min-width="220" />
        <el-table-column prop="teacherName" label="主讲教师" min-width="150" align="center" />
        <el-table-column label="学生" width="130" align="center">
          <template #default="{ row }">
            <el-button
              size="small"
              class="student-view-btn"
              @click="viewClassStudents(row)"
            >
              查看学生
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="studentDialogVisible" :title="`学生名单 - ${currentClass?.className || ''}`" width="min(500px, 92vw)" append-to-body>
      <el-table :data="students" border size="small" max-height="360">
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="name" label="姓名" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCourses, deleteCourse, listClasses } from '../../api/academic'
import { listMajors } from '../../api/admin'
import request from '../../utils/request'

const loading = ref(false)
const courses = ref([])
const majors = ref([])
const total = ref(0)
const keyword = ref('')
const filterMajorId = ref(null)
const search = reactive({ page: 1, size: 15 })

const courseDialogVisible = ref(false)
const editingCourse = ref(null)
const courseForm = reactive({ code: '', name: '', credit: null, hoursTheory: null, hoursExperiment: null, category: '必修', majorId: null })

const classDialogVisible = ref(false)
const currentCourse = ref(null)
const classList = ref([])
const studentDialogVisible = ref(false)
const currentClass = ref(null)
const students = ref([])

onMounted(async () => {
  const res = await listMajors({ page: 1, size: 100 })
  majors.value = res.data?.records || []
  loadCourses()
})

async function loadCourses() {
  loading.value = true
  try {
    const res = await listCourses({ page: search.page, size: search.size, majorId: filterMajorId.value || undefined, keyword: keyword.value || undefined })
    courses.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function showCourseDialog(row) {
  editingCourse.value = row || null
  Object.assign(courseForm, { code: row?.code || '', name: row?.name || '', credit: row?.credit || null, hoursTheory: row?.hoursTheory || null, hoursExperiment: row?.hoursExperiment || null, category: row?.category || '必修', majorId: row?.majorId || null })
  courseDialogVisible.value = true
}

async function handleCourseSubmit() {
  try {
    if (editingCourse.value) {
      await request.put(`/api/courses/${editingCourse.value.id}`, { ...courseForm })
    } else {
      await request.post('/api/courses', { ...courseForm })
    }
    ElMessage.success('操作成功')
    courseDialogVisible.value = false
    loadCourses()
  } catch { /* handled */ }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除课程「${row.name}」？`, '提示', { type: 'warning' })
  await deleteCourse(row.id)
  ElMessage.success('已删除')
  loadCourses()
}

async function showClasses(course) {
  currentCourse.value = course
  const res = await listClasses(course.id)
  classList.value = res.data || []
  classDialogVisible.value = true
}

async function viewClassStudents(cls) {
  currentClass.value = cls
  const res = await request.get(`/api/teaching-classes/${cls.id}/students`)
  students.value = res.data || []
  studentDialogVisible.value = true
}
</script>

<style scoped>
.page-container {
  padding: var(--space-6);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: var(--space-5);
  flex-wrap: wrap;
}

.page-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  padding-left: 14px;
  position: relative;
}

.page-header h3::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  width: 4px;
  height: 20px;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--brand-400), var(--peach-400));
}

:deep(.course-category-column .cell) {
  white-space: nowrap;
  display: flex;
  justify-content: center;
  align-items: center;
}

:deep(.course-action-column .cell) {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

:deep(.el-table .cell) {
  line-height: 1.5;
}

:deep(.el-table tbody td) {
  padding: 12px 10px !important;
}

.pagination {
  margin-top: var(--space-4);
  justify-content: flex-end;
}
.class-manage-btn {
  color: #d97706b1 !important;
  background: rgba(245, 158, 11, 0.10) !important;
  border-color: rgba(245, 158, 11, 0.28) !important;
  border-radius: 999px;
  font-weight: 500;
}

.class-manage-btn:hover {
  color: #ffffff !important;
  background: linear-gradient(135deg, #fbbf24, #f0ac35cc) !important;
  border-color: #ecc47f !important;
  box-shadow: 0 6px 14px rgba(245, 158, 11, 0.24);
}
.edit-course-btn {
  color: #806bbf !important;
  background: rgba(128, 107, 191, 0.10) !important;
  border-color: rgba(128, 107, 191, 0.24) !important;
  border-radius: 999px;
  font-weight: 500;
}

.edit-course-btn:hover {
  color: #ffffff !important;
  background: linear-gradient(135deg, #9e89cd, #806bbf) !important;
  border-color: #806bbf !important;
  box-shadow: 0 6px 14px rgba(128, 107, 191, 0.24);
}
.delete-course-btn {
  color: #ffffff !important;
  background: linear-gradient(135deg, #ef9aa0, #e78087) !important;
  border-color: #ef9aa0 !important;
  border-radius: 999px;
  font-weight: 500;
}

.delete-course-btn:hover {
  color: #ffffff !important;
  background: linear-gradient(135deg, #e78087, #d9676e) !important;
  border-color: #e78087 !important;
  box-shadow: 0 6px 14px rgba(231, 128, 135, 0.28);
}
:deep(.teaching-class-dialog) {
  border-radius: 26px;
  overflow: hidden;
  box-shadow:
    0 24px 60px rgba(31, 41, 55, 0.16),
    0 10px 28px rgba(128, 107, 191, 0.14);
}

:deep(.teaching-class-dialog .el-dialog__header) {
  margin: 0;
  padding: 24px 28px 14px;
  background:
    radial-gradient(circle at 12% 20%, rgba(158, 137, 205, 0.12), transparent 32%),
    linear-gradient(135deg, #ffffff 0%, #faf8ff 100%);
}

:deep(.teaching-class-dialog .el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: #2f2f3a;
}

:deep(.teaching-class-dialog .el-dialog__headerbtn) {
  top: 22px;
  right: 24px;
}

:deep(.teaching-class-dialog .el-dialog__close) {
  color: #9e89cd;
  font-size: 20px;
}

:deep(.teaching-class-dialog .el-dialog__body) {
  padding: 8px 28px 28px;
}

.class-dialog-tip {
  margin-bottom: 14px;
  padding: 10px 14px;
  border-radius: 14px;
  color: #806bbf;
  font-size: 13px;
  background: rgba(128, 107, 191, 0.08);
  border: 1px solid rgba(128, 107, 191, 0.14);
}

.class-list-table {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.class-list-table .el-table__header th) {
  background: #f7f6fa;
  color: #5f5f6b;
  font-weight: 700;
}

:deep(.class-list-table .el-table__body td) {
  height: 54px;
}

:deep(.class-list-table .el-table__body tr:hover > td) {
  background: rgba(158, 137, 205, 0.08) !important;
}

.student-view-btn {
  color: #806bbf !important;
  background: rgba(128, 107, 191, 0.10) !important;
  border-color: rgba(128, 107, 191, 0.24) !important;
  border-radius: 999px;
  font-weight: 500;
}

.student-view-btn:hover {
  color: #ffffff !important;
  background: linear-gradient(135deg, #9e89cd, #806bbf) !important;
  border-color: #806bbf !important;
  box-shadow: 0 6px 14px rgba(128, 107, 191, 0.24);
}
</style>
