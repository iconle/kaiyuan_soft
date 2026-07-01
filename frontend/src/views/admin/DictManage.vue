<template>
  <div class="page-container">
    <h3 class="page-title">数据字典管理</h3>

    <el-tabs v-model="activeTab">
      <!-- 学院管理 -->
      <el-tab-pane label="学院管理" name="college">
        <div class="content-card">
          <div class="tab-header">
            <el-button type="primary" @click="showCollegeDialog()">新增学院</el-button>
          </div>
          <el-table :data="colleges" border stripe v-loading="collegeLoading">
            <el-table-column type="index" label="ID" width="80" />
            <el-table-column prop="name" label="学院名称" show-overflow-tooltip />



            <el-table-column
              label="操作"
              width="180"
              align="center"
              class-name="dict-operation-column"
            >
              <template #default="{ row }">
                <div class="dict-operation-actions">
                  <el-button
                    size="small"
                    class="dict-action-btn dict-edit-btn"
                    @click="showCollegeDialog(row)"
                  >
                    编辑
                  </el-button>

                  <el-button
                    size="small"
                    type="danger"
                    class="dict-action-btn dict-delete-btn"
                    @click="handleDeleteCollege(row)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>


          </el-table>
        </div>

        <el-dialog v-model="collegeDialogVisible" :title="collegeEditId ? '编辑学院' : '新增学院'" width="400px">
          <el-form label-width="80px">
            <el-form-item label="学院名称">
              <el-input v-model="collegeForm.name" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="collegeDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleCollegeSubmit">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 专业管理 -->
      <el-tab-pane label="专业管理" name="major">
        <div class="content-card">
          <div class="tab-header">
            <el-button type="primary" @click="showMajorDialog()">新增专业</el-button>
          </div>
          <el-table :data="majors" border stripe v-loading="majorLoading">
            <el-table-column type="index" label="ID" width="80" />
            <el-table-column prop="code" label="专业代码" width="140" />
            <el-table-column prop="name" label="专业名称" min-width="160" show-overflow-tooltip />

            <el-table-column
              label="所属学院"
              width="280"
              align="center"
              class-name="major-college-column"
            >
              <template #default="{ row }">
                <span class="college-name-text">
                  {{ getCollegeName(row.collegeId) }}
                </span>
              </template>
            </el-table-column>

            <el-table-column
              label="操作"
              width="140"
              align="center"
              class-name="dict-operation-column"
            >
              <template #default="{ row }">
                <div class="dict-operation-actions">
                  <el-button
                    size="small"
                    class="dict-action-btn dict-edit-btn"
                    @click="showMajorDialog(row)"
                  >
                    编辑
                  </el-button>
                </div>
              </template>
            </el-table-column>

          </el-table>
        </div>

        <el-dialog v-model="majorDialogVisible" :title="majorEditId ? '编辑专业' : '新增专业'" width="480px">
          <el-form label-width="80px">
            <el-form-item label="专业代码">
              <el-input v-model="majorForm.code" />
            </el-form-item>
            <el-form-item label="专业名称">
              <el-input v-model="majorForm.name" />
            </el-form-item>
            <el-form-item label="所属学院">
              <el-select v-model="majorForm.collegeId" clearable style="width: 100%">
                <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="majorDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleMajorSubmit">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 学年学期 -->
      <el-tab-pane label="学年学期" name="semester">
        <div class="content-card">
          <div class="tab-header">
            <el-button type="primary" @click="showSemesterDialog()">新增学期</el-button>
          </div>


          <el-table
            :data="semesters"
            border
            stripe
            v-loading="semesterLoading"
            class="semester-table"
          >
            <el-table-column type="index" label="ID" width="80" align="center" />
            <el-table-column prop="academicYear" label="学年" width="150" align="center" />
            <el-table-column prop="semester" label="学期" width="120" align="center">
              <template #default="{ row }">
                第{{ row.semester }}学期
              </template>
            </el-table-column>
            <el-table-column prop="label" label="显示名" min-width="260" show-overflow-tooltip />
            <el-table-column
              label="操作"
              width="120"
              align="center"
              class-name="semester-operation-column"
            >
              <template #default="{ row }">
                <div class="operation-actions">
                  <el-button
                    size="small"
                    type="danger"
                    class="delete-btn"
                    @click="handleDeleteSemester(row)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>



        </div>

        <el-dialog v-model="semesterDialogVisible" title="新增学期" width="400px">
          <el-form label-width="80px">
            <el-form-item label="学年">
              <el-input v-model="semesterForm.academicYear" placeholder="如 2025-2026" />
            </el-form-item>
            <el-form-item label="学期">
              <el-select v-model="semesterForm.semester" style="width: 100%">
                <el-option :value="1" label="第一学期" />
                <el-option :value="2" label="第二学期" />
              </el-select>
            </el-form-item>
            <el-form-item label="显示名">
              <el-input v-model="semesterForm.label" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="semesterDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSemesterSubmit">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listColleges, createCollege, updateCollege, deleteCollege,
  listMajors, createMajor, updateMajor,
  listSemesters, createSemester, deleteSemester
} from '../../api/admin'

const activeTab = ref('college')

// ========== 学院 ==========
const colleges = ref([])
const collegeLoading = ref(false)
const collegeDialogVisible = ref(false)
const collegeEditId = ref(null)
const collegeForm = reactive({ name: '' })

async function loadColleges() {
  collegeLoading.value = true
  try {
    const res = await listColleges()
    colleges.value = res.data
  } finally {
    collegeLoading.value = false
  }
}

function showCollegeDialog(row) {
  collegeEditId.value = row?.id || null
  collegeForm.name = row?.name || ''
  collegeDialogVisible.value = true
}

async function handleCollegeSubmit() {
  if (collegeEditId.value) {
    await updateCollege(collegeEditId.value, collegeForm.name)
  } else {
    await createCollege(collegeForm.name)
  }
  ElMessage.success('操作成功')
  collegeDialogVisible.value = false
  loadColleges()
}

async function handleDeleteCollege(row) {
  await ElMessageBox.confirm(`确定删除学院「${row.name}」？`, '提示', { type: 'warning' })
  await deleteCollege(row.id)
  ElMessage.success('已删除')
  loadColleges()
}

// ========== 专业 ==========
const majors = ref([])
const majorLoading = ref(false)
const majorDialogVisible = ref(false)
const majorEditId = ref(null)
const majorForm = reactive({ code: '', name: '', collegeId: null })

async function loadMajors() {
  majorLoading.value = true
  try {
    const res = await listMajors({ page: 1, size: 100 })
    majors.value = res.data.records
  } finally {
    majorLoading.value = false
  }
}

function showMajorDialog(row) {
  majorEditId.value = row?.id || null
  Object.assign(majorForm, { code: row?.code || '', name: row?.name || '', collegeId: row?.collegeId || null })
  majorDialogVisible.value = true
}

async function handleMajorSubmit() {
  if (majorEditId.value) {
    await updateMajor(majorEditId.value, majorForm)
  } else {
    await createMajor({ ...majorForm })
  }
  ElMessage.success('操作成功')
  majorDialogVisible.value = false
  loadMajors()
}

// ========== 学期 ==========
const semesters = ref([])
const semesterLoading = ref(false)
const semesterDialogVisible = ref(false)
const semesterForm = reactive({ academicYear: '', semester: 1, label: '' })

async function loadSemesters() {
  semesterLoading.value = true
  try {
    const res = await listSemesters()
    semesters.value = res.data
  } finally {
    semesterLoading.value = false
  }
}

function showSemesterDialog() {
  Object.assign(semesterForm, { academicYear: '', semester: 1, label: '' })
  semesterDialogVisible.value = true
}

async function handleSemesterSubmit() {
  await createSemester(semesterForm)
  ElMessage.success('新增成功')
  semesterDialogVisible.value = false
  loadSemesters()
}

async function handleDeleteSemester(row) {
  await ElMessageBox.confirm(`确定删除「${row.label}」？`, '提示', { type: 'warning' })
  await deleteSemester(row.id)
  ElMessage.success('已删除')
  loadSemesters()
}

function getCollegeName(collegeId) {
  return colleges.value.find(c => c.id === collegeId)?.name || '-'
}

onMounted(async () => {
  await loadColleges()
  await Promise.all([loadMajors(), loadSemesters()])
})
</script>

<style scoped>
.page-container {
  padding: var(--space-5);
}

.page-title {
  font-size: var(--text-lg);
  margin: 0 0 var(--space-4) 0;
}

.tab-header {
  margin-bottom: var(--space-4);
}
/* 学年学期表格：缩小整体宽度，让操作列往左移 */
.semester-table {
  width: 82%;
  border-radius: 14px;
  overflow: hidden;
}

/* 表头和行高稍微紧凑 */
:deep(.semester-table .el-table__header th) {
  height: 42px;
  background-color: #f7f7fa;
  color: #606266;
  font-weight: 700;
}

:deep(.semester-table .el-table__row td) {
  height: 50px;
}

:deep(.semester-table .cell) {
  font-size: 14px;
}

/* 操作列居中 */
:deep(.semester-operation-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.operation-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

/* 删除按钮样式 */
.delete-btn {
  height: 25px;
  padding: 0 16px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  border-color: #ef9aa0;
  background-color: #ef9aa0;
}

.delete-btn:hover {
  color: #fff;
  border-color: #e78087;
  background-color: #e78087;
}

:deep(.delete-btn.el-button) {
  margin-left: 0;
}
/* 数据字典操作列按钮统一美化 */
:deep(.dict-operation-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.dict-operation-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
  white-space: nowrap;
}

.dict-action-btn {
  height: 25px;
  padding: 0 16px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

:deep(.dict-action-btn.el-button) {
  margin-left: 0;
}

/* 编辑按钮：浅紫色 */
.dict-edit-btn {
  color: #7e57c2;
  border-color: #d8c9f3;
  background-color: #f6f0ff;
}

.dict-edit-btn:hover {
  color: #6f42c1;
  border-color: #b79dea;
  background-color: #efe6ff;
}

/* 删除按钮：粉红色 */
.dict-delete-btn {
  color: #fff;
  border-color: #ef9aa0;
  background-color: #ef9aa0;
}

.dict-delete-btn:hover {
  color: #fff;
  border-color: #e78087;
  background-color: #e78087;
}
</style>
