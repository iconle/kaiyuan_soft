<template>
  <div class="page-container">
    <div class="page-header">
      <h3>成绩导入与管理</h3>
      <el-select v-model="selectedAssessmentId" placeholder="选择考核点" style="width:260px" @change="loadAll">
        <el-option v-for="ap in assessments" :key="ap.id" :label="`${ap.name} → ${assessmentObjLabel(ap)}`" :value="ap.id" />
      </el-select>
      <el-button type="primary" @click="downloadTemplate" :disabled="!selectedAssessmentId">下载模板</el-button>
      <el-upload
        :show-file-list="false"
        :before-upload="beforeUpload"
        :http-request="uploadFile"
        accept=".xlsx"
        :disabled="status === 'LOCKED'"
      >
        <el-button type="primary" plain class="import-action-btn" :loading="importing" :disabled="status === 'LOCKED'">导入成绩</el-button>
      </el-upload>
      <StatusTag v-if="status" :status="status" />
      <el-button
        v-if="status === 'LOCKED' && !hasPendingRequest"
        type="warning"
        plain
        @click="showRequestDialog"
      >
        申请成绩勘误
      </el-button>
    </div>

    <el-alert v-if="status === 'LOCKED'" type="warning" show-icon :closable="false" style="margin-bottom:16px">
      成绩单已锁定，无法继续导入或修改。如发现成绩录入有误，请点击「申请成绩勘误」提交说明，审批解锁后再修改。
    </el-alert>
    <el-alert v-else-if="!loading && assessments.length === 0" type="info" show-icon :closable="false" style="margin-bottom:16px">
      暂无考核点数据，请先在「考核点设置」中创建考核点。
    </el-alert>
    <el-alert v-else-if="!loading && assessments.length > 0 && selectedAssessmentId && questions.length === 0" type="info" show-icon :closable="false" style="margin-bottom:16px">
      该考核点尚未设置题目，成绩将按考核点整体录入。可在「题目设置」中细分为多个题目。
    </el-alert>

    <div v-if="selectedAssessmentId" class="content-card">
      <el-empty v-if="!loading && scoreRows.length === 0" description="暂无学生数据，请先为学生选课" />
      <div class="section-title">考核点: {{ currentAssessment?.name || '' }}
        <span style="font-size:13px;color:var(--text-secondary);margin-left:8px">满分: {{ currentAssessment?.maxScore || '-' }}</span>
      </div>

      <div style="overflow-x: auto; max-width: 100%;">
      <el-table
        :data="scoreRows"
        border
        stripe
        size="small"
        v-loading="loading"
        v-if="questions.length > 0"
        class="score-import-table"
      >
        <el-table-column prop="studentNo" label="学号" width="120" fixed />
        <el-table-column prop="studentName" label="姓名" width="100" fixed />
        <el-table-column v-for="q in questions" :key="q.id" :label="q.name" min-width="110">
          <template #header>
            <div>{{ q.name }}</div>
            <div style="font-size:11px;color:var(--text-secondary)">满分:{{ q.maxScore }}→{{ questionObjLabel(q) }}</div>
          </template>
          <template #default="{ row }">
            <el-input-number
              v-if="status !== 'LOCKED'"
              :model-value="getQScore(row.studentId, q.id)"
              @update:model-value="val => setQScore(row.studentId, q.id, val)"
              :min="0"
              :max="q.maxScore"
              :precision="1"
              size="small"
              controls-position="right"
              style="width:100%"
              class="score-number-input"
            />
            <span v-else>{{ getQScore(row.studentId, q.id) ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总成绩" width="90">
          <template #default="{ row }">
            <strong>{{ calcTotal(row.studentId) }}</strong>
          </template>
        </el-table-column>
      </el-table>

      <!-- No questions: show assessment-level score input -->
      <el-table :data="scoreRows" border stripe size="small" v-loading="loading" v-else-if="scoreRows.length > 0">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column :label="currentAssessment?.name || '成绩'" min-width="150">
          <template #header><div>{{ currentAssessment?.name || '成绩' }}</div><div style="font-size:11px;color:var(--text-secondary)">满分:{{ currentAssessment?.maxScore || '-' }}</div></template>
          <template #default="{ row }">
            <el-input-number v-if="status !== 'LOCKED'" :model-value="getAScore(row.studentId)"
              @update:model-value="val => setAScore(row.studentId, val)" :min="0" :max="currentAssessment?.maxScore"
              :precision="1" size="small" controls-position="right" style="width:100%" />
            <span v-else>{{ getAScore(row.studentId) ?? '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
      </div>

      <div style="margin-top:12px" v-if="hasEdits && status !== 'LOCKED'">
        <el-button type="warning" @click="saveAll">保存修改 ({{ editCount }})</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getScores, getScoreStatus, downloadScoreTemplate, listAssessments, uploadScores
} from '../../api/teacher'
import StatusTag from '../../components/StatusTag.vue'
import request from '../../utils/request'
import { validateExcelFile, showExcelImportError } from '../../utils/excelImport'
import { buildClassFilename, downloadBlob, ensureDownloadBlob, showDownloadError } from '../../utils/downloadFile'

const route = useRoute()
const classId = ref(route.params.classId)
const loading = ref(false)
const status = ref('')
const assessments = ref([])
const allObjectives = ref([])
const selectedAssessmentId = ref(null)
const questions = ref([])
const scoreRows = ref([])
const edits = ref({})
const importing = ref(false)

onMounted(async () => { if (classId.value) await loadAll() })

async function loadAll() {
  loading.value = true
  try {
    const [scoreRes, statusRes, assessRes, objRes] = await Promise.all([
      getScores(classId.value).catch(() => ({ data: { rows: [], status: '' } })),
      getScoreStatus(classId.value).catch(() => ({ data: { status: '' } })),
      listAssessments(classId.value).catch(() => ({ data: [] })),
      request.get(`/api/classes/${classId.value}/objectives`).catch(() => ({ data: [] }))
    ])
    assessments.value = assessRes.data || []
    allObjectives.value = objRes.data || []
    scoreRows.value = scoreRes.data?.rows || []
    status.value = statusRes.data?.status || scoreRes.data?.status || ''
    if (assessments.value.length > 0 && !selectedAssessmentId.value) selectedAssessmentId.value = assessments.value[0].id
    if (selectedAssessmentId.value) await loadQuestions()
  } catch (err) {
    ElMessage.error('加载数据失败，请刷新页面重试')
  } finally { loading.value = false }
}

async function loadQuestions() {
  if (!selectedAssessmentId.value) return
  try {
    const res = await request.get(`/api/assessments/${selectedAssessmentId.value}/questions`)
      .catch(() => ({ data: [] }))
    questions.value = res.data || []
    const sRes = await getScores(classId.value)
      .catch(() => ({ data: { rows: [] } }))
    scoreRows.value = sRes.data?.rows || []
    edits.value = {}
  } catch { questions.value = [] }
}

const currentAssessment = computed(() => assessments.value.find(a => (a.id || a.assessmentId) === selectedAssessmentId.value))

function assessmentObjLabel(ap) {
  const ids = ap.objectiveIds || (ap.objectiveId ? [ap.objectiveId] : [])
  return ids.map(id => allObjectives.value.find(o => o.id === id)?.objNo || id).join(',')
}

function questionObjLabel(q) {
  return (q.objectiveIds || []).map(id => allObjectives.value.find(o => o.id === id)?.objNo || id).join(',')
}

function getQScore(studentId, questionId) {
  const k = `q_${studentId}_${questionId}`
  if (k in edits.value) return edits.value[k]
  const row = scoreRows.value.find(r => r.studentId === studentId)
  const cell = row?.cells?.find(c => c.assessmentId === selectedAssessmentId.value)
  return cell?.questionScores?.[questionId] ?? null
}

function getAScore(studentId) {
  const k = `a_${studentId}_${selectedAssessmentId.value}`
  if (k in edits.value) return edits.value[k]
  const row = scoreRows.value.find(r => r.studentId === studentId)
  const cell = row?.cells?.find(c => c.assessmentId === selectedAssessmentId.value)
  return cell?.score ?? null
}

function setQScore(studentId, questionId, val) {
  const k = `q_${studentId}_${questionId}`
  edits.value = { ...edits.value, [k]: val ?? 0 }
}

function setAScore(studentId, val) {
  const k = `a_${studentId}_${selectedAssessmentId.value}`
  edits.value = { ...edits.value, [k]: val ?? 0 }
}

function calcTotal(studentId) {
  if (questions.value.length === 0) return getAScore(studentId) ?? '-'
  let sum = 0
  for (const q of questions.value) {
    const s = getQScore(studentId, q.id)
    if (s != null) sum += Number(s)
  }
  return sum || '-'
}

const editCount = computed(() => Object.keys(edits.value).length)
const hasEdits = computed(() => editCount.value > 0)

async function saveAll() {
  let saved = 0
  let failed = 0
  for (const [key, score] of Object.entries(edits.value)) {
    if (key.startsWith('q_')) {
      const [, studentId, questionId] = key.split('_')
      try {
        await request.put(`/api/classes/${classId.value}/scores`, { studentId: Number(studentId), assessmentId: selectedAssessmentId.value, questionId: Number(questionId), score })
        saved++
      } catch { failed++ }
    } else if (key.startsWith('a_')) {
      const [, studentId, assessId] = key.split('_')
      try {
        await request.put(`/api/classes/${classId.value}/scores`, { studentId: Number(studentId), assessmentId: Number(assessId), score })
        saved++
      } catch { failed++ }
    }
  }
  if (saved > 0) {
    const msg = failed > 0 ? `已保存 ${saved} 条，失败 ${failed} 条` : `已保存 ${saved} 条`
    ElMessage.success(msg)
    edits.value = {}
    await loadQuestions()
  } else if (failed > 0) {
    ElMessage.error('保存失败，请重试')
  }
}

async function downloadTemplate() {
  try {
    const assessmentName = currentAssessment.value?.name || '成绩'
    downloadBlob(await ensureDownloadBlob(await downloadScoreTemplate(classId.value)), buildClassFilename(classId.value, `${assessmentName}成绩模板`, 'xlsx'))
  } catch (error) {
    showDownloadError(error)
  }
}

function beforeUpload(file) {
  return validateExcelFile(file)
}

async function uploadFile({ file }) {
  importing.value = true
  const form = new FormData()
  form.append('file', file)
  try {
    await uploadScores(classId.value, form)
    ElMessage.success('成绩导入成功')
    // Wait a bit before reloading to ensure backend has processed
    await new Promise(resolve => setTimeout(resolve, 300))
    await loadAll().catch(() => {
      ElMessage.warning('成绩导入成功，但刷新数据失败，请手动刷新页面')
    })
  } catch (error) {
    showExcelImportError(error)
  } finally { importing.value = false }
}

function escapeHtml(value) {
  return String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header { display: flex; align-items: center; gap: var(--space-4); margin-bottom: var(--space-4); flex-wrap: wrap; }
.page-header h3 { margin: 0; font-size: var(--text-lg); }
.section-title { font-size: 15px; font-weight: var(--font-semibold); margin-bottom: 10px; color: var(--text-primary); }

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

/* 成绩输入框上下调节按钮：浅紫色 */
:deep(.score-import-table .score-number-input .el-input-number__increase),
:deep(.score-import-table .score-number-input .el-input-number__decrease) {
  background-color: #ede7ff;
  border-color: #d8c9f3;
  color: #7e57c2;
}

:deep(.score-import-table .score-number-input .el-input-number__increase:hover),
:deep(.score-import-table .score-number-input .el-input-number__decrease:hover) {
  background-color: #e3d8ff;
  border-color: #c7b3f5;
  color: #6f42c1;
}
</style>
