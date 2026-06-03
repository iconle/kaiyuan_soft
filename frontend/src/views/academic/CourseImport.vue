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
            <el-button size="small" @click="showCourseDialog(row)">编辑</el-button>
            <el-button size="small" @click="showClasses(row)">教学班级</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" background layout="total, prev, pager, next"
                     :total="total" :page-size="search.size" v-model:current-page="search.page"
                     @current-change="loadCourses" />
    </div>

    <!-- Course edit dialog -->
    <el-dialog v-model="courseDialogVisible" :title="editingCourse ? '编辑课程' : '新增课程'" width="480px">
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
    <el-dialog v-model="classDialogVisible" :title="`教学班级 - ${currentCourse?.name || ''}`" width="700px">
      <el-table :data="classList" border size="small">
        <el-table-column prop="className" label="班级名称" width="180" />
        <el-table-column prop="teacherName" label="主讲教师" width="120" />
        <el-table-column label="学生" width="100">
          <template #default="{ row }"><el-button size="small" text type="primary" @click="viewClassStudents(row)">查看学生</el-button></template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="studentDialogVisible" :title="`学生名单 - ${currentClass?.className || ''}`" width="500px" append-to-body>
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
</style>
