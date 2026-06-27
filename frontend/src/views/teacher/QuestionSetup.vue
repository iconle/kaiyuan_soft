<template>
  <div class="page-container">
    <div class="page-header">
      <h3>考核点题目设置</h3>
      <el-select v-model="selectedAssessmentId" placeholder="选择考核点" @change="loadQuestions" style="width:260px">
        <el-option v-for="ap in assessments" :key="ap.id" :label="`${ap.name} (绑定目标: ${(ap.objectiveIds||[]).map(id=>getObjNo(id)).join(',')})`" :value="ap.id" />
      </el-select>
      <el-button @click="downloadTemplate" :loading="downloading" :disabled="!selectedAssessmentId">下载模板</el-button>
      <el-upload :show-file-list="false" :before-upload="beforeUpload" :http-request="uploadFile" accept=".xlsx">
        <el-button type="primary" plain class="import-action-btn" :loading="importing" :disabled="!selectedAssessmentId">导入题目</el-button>
      </el-upload>
      <el-button type="primary" @click="showDialog()" :disabled="!selectedAssessmentId">新增题目</el-button>
      <span v-if="selectedAssessmentId" :style="{color: questionSum === 100 ? 'var(--el-color-success)' : 'var(--el-color-danger)', fontSize:'14px', fontWeight:'bold'}">
        题目总分: {{ questionSum }} {{ questionSum === 100 ? '✓' : '✗ 必须等于100' }}
      </span>
    </div>

    <div class="content-card">
      <el-table :data="questions" border stripe v-loading="loading" v-if="selectedAssessmentId" empty-text="暂无题目，请先新增或导入">
        <el-table-column prop="sortOrder" label="序号" width="60" />
        <el-table-column prop="name" label="题目名称" width="180" />
        <el-table-column prop="maxScore" label="满分" width="80" />
        <el-table-column label="绑定目标" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="oid in (row.objectiveIds||[])" :key="oid" size="small" style="margin-right:4px">{{ getObjNo(oid) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="180"
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
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑题目' : '新增题目'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="题目名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="满分分值" required><el-input-number v-model="form.maxScore" :min="1" :step="5" style="width:100%" /></el-form-item>
        <el-form-item label="绑定目标" required>
          <el-select v-model="form.objectiveIds" multiple style="width:100%" placeholder="选择该题目支撑的目标（仅考核点已绑定的目标）">
            <el-option v-for="obj in parentObjectives" :key="obj.id" :label="`${obj.objNo} - ${obj.description?.substring(0,15)}`" :value="obj.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="1" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAssessments, listObjectives, downloadQuestionTemplate, importQuestions
} from '../../api/teacher'
import request from '../../utils/request'
import { validateExcelFile, showExcelImportError } from '../../utils/excelImport'

const route = useRoute()
const classId = ref(route.params.classId)
const loading = ref(false)
const assessments = ref([])
const allObjectives = ref([])
const selectedAssessmentId = ref(null)
const questions = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive({ name: '', maxScore: 100, objectiveIds: [], sortOrder: 1 })
const importing = ref(false)
const downloading = ref(false)

onMounted(async () => {
  const [aRes, oRes] = await Promise.all([listAssessments(classId.value), listObjectives(classId.value)])
  assessments.value = aRes.data || []
  allObjectives.value = oRes.data || []
  if (assessments.value.length > 0) { selectedAssessmentId.value = assessments.value[0].id; loadQuestions() }
})

function getObjNo(id) { return allObjectives.value.find(o => o.id === id)?.objNo || id }

const parentObjectiveIds = computed(() => {
  const ap = assessments.value.find(a => a.id === selectedAssessmentId.value)
  return ap?.objectiveIds || (ap?.objectiveId ? [ap.objectiveId] : [])
})

const parentObjectives = computed(() => {
  const ids = parentObjectiveIds.value
  return allObjectives.value.filter(o => ids.includes(o.id))
})

const questionSum = computed(() => {
  let sum = 0
  for (const q of questions.value) { if (q.maxScore) sum += Number(q.maxScore) }
  return Math.round(sum * 100) / 100
})

async function loadQuestions() {
  if (!selectedAssessmentId.value) return
  loading.value = true
  try {
    const res = await request.get(`/api/assessments/${selectedAssessmentId.value}/questions`)
    questions.value = res.data || []
  } finally { loading.value = false }
}

function showDialog(row) {
  editing.value = row || null
  Object.assign(form, { name: row?.name || '', maxScore: row?.maxScore || 100, objectiveIds: row?.objectiveIds ? [...row.objectiveIds] : [], sortOrder: row?.sortOrder || questions.value.length + 1 })
  dialogVisible.value = true
}

async function handleSubmit() {
  const existingSum = questions.value.filter(q => q.id !== editing.value?.id).reduce((s, q) => s + Number(q.maxScore || 0), 0)
  const newSum = existingSum + Number(form.maxScore || 0)
  if (newSum > 100) { ElMessage.error(`题目总分(${newSum})超过100，请调整满分值`); return }
  try {
    if (editing.value) {
      await request.put(`/api/assessments/${selectedAssessmentId.value}/questions/${editing.value.id}`, { ...form })
    } else {
      await request.post(`/api/assessments/${selectedAssessmentId.value}/questions`, { ...form })
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadQuestions()
  } catch { /* handled */ }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.name}」？`, '提示', { type: 'warning' })
  await request.delete(`/api/assessments/${selectedAssessmentId.value}/questions/${row.id}`)
  ElMessage.success('已删除')
  loadQuestions()
}

async function downloadTemplate() {
  downloading.value = true
  try {
    saveBlob(await downloadQuestionTemplate(selectedAssessmentId.value), '考核点题目导入模板.xlsx')
  } finally { downloading.value = false }
}

function beforeUpload(file) {
  return validateExcelFile(file)
}

async function uploadFile({ file }) {
  importing.value = true
  try {
    const res = await importQuestions(selectedAssessmentId.value, file)
    ElMessage.success(`成功导入 ${res.data} 个题目`)
    loadQuestions()
  } catch (error) {
    showExcelImportError(error)
  } finally { importing.value = false }
}

function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}

function escapeHtml(value) {
  return String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header { display: flex; align-items: center; gap: var(--space-4); margin-bottom: var(--space-4); flex-wrap: wrap; }
.page-header h3 { margin: 0; font-size: var(--text-lg); }

.import-action-btn {
  color: #ffffff !important;
  background: linear-gradient(135deg, #9e89cd, #806bbf) !important;
  border-color: #806bbf !important;
  box-shadow: 0 8px 18px rgba(128, 107, 191, 0.24);
}

.import-action-btn:hover,
.import-action-btn:focus {
  color: #ffffff !important;
  background: linear-gradient(135deg, #a895d4, #735ab8) !important;
  border-color: #735ab8 !important;
}

:deep(.import-action-btn.is-loading),
:deep(.import-action-btn.is-disabled) {
  color: #ffffff !important;
}

/* 操作列按钮：对齐课程目标、考核点映射等页面风格 */
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
</style>
