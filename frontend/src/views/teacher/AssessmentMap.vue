<template>
  <div class="page-container">
    <div class="page-header">
      <h3>考核点细分与映射</h3>
      <el-button @click="downloadTemplate" :loading="downloading">下载模板</el-button>
      <el-upload :show-file-list="false" :before-upload="beforeUpload" :http-request="uploadFile" accept=".xlsx">
        <el-button type="primary" plain class="import-action-btn" :loading="importing">导入考核点</el-button>
      </el-upload>
      <el-button type="primary" @click="showDialog()">新增考核点</el-button>
      <span v-if="weightSum !== null" :style="{color: weightSum === 100 ? 'var(--el-color-success)' : 'var(--el-color-danger)', fontSize: '14px', fontWeight:'bold'}">
        总权重: {{ weightSum }}% {{ weightSum === 100 ? '✓' : '✗ 必须等于100%' }}
      </span>
    </div>

    <div class="content-card">
      <el-table :data="assessments" border stripe v-loading="loading" empty-text="暂无考核点，请先新增或导入">
        <el-table-column prop="sortOrder" label="序号" width="60" />
        <el-table-column
          prop="name"
          label="考核点名称"
          width="160"
          show-overflow-tooltip
        />
        <el-table-column prop="maxScore" label="满分" width="70" />
        <el-table-column prop="weightPercent" label="权重(%)" width="90" />
        <el-table-column label="绑定目标" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="oid in row.objectiveIds" :key="oid" size="small" style="margin-right:4px">{{ getObjNo(oid) }}</el-tag>
            <span v-if="!row.objectiveIds || row.objectiveIds.length === 0" style="color:var(--text-secondary)">未绑定</span>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑考核点' : '新增考核点'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="考核点名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="满分分值" required><el-input-number v-model="form.maxScore" :min="1" :step="10" style="width:100%" /></el-form-item>
        <el-form-item label="权重(%)" required>
          <el-input-number v-model="form.weightPercent" :min="0" :max="100" :precision="2" style="width:100%" placeholder="占总成绩的百分比" />
          <span style="font-size:12px;color:var(--text-secondary)">所有考核点权重之和必须等于100%</span>
        </el-form-item>
        <el-form-item label="绑定课程目标" required>
          <el-select v-model="form.objectiveIds" multiple style="width:100%" placeholder="可多选课程目标">
            <el-option v-for="obj in objectives" :key="obj.id" :label="`${obj.objNo} - ${obj.description?.substring(0,20)}`" :value="obj.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="1" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, inject, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAssessments, createAssessment, updateAssessment, deleteAssessment, listObjectives,
  downloadAssessmentTemplate, importAssessments
} from '../../api/teacher'
import { validateExcelFile, showExcelImportError } from '../../utils/excelImport'
import { buildClassFilename, downloadBlob, ensureDownloadBlob, showDownloadError } from '../../utils/downloadFile'

const route = useRoute()
const classId = ref(route.params.classId)
const resolveClassName = inject('resolveClassName', () => '')
const loading = ref(false)
const assessments = ref([])
const objectives = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive({ name: '', maxScore: 100, weightPercent: null, objectiveIds: [], sortOrder: 1 })
const importing = ref(false)
const downloading = ref(false)

const weightSum = computed(() => {
  if (assessments.value.length === 0) return null
  let sum = 0
  for (const a of assessments.value) {
    if (a.weightPercent != null) sum += Number(a.weightPercent)
  }
  return Math.round(sum * 100) / 100
})

onMounted(() => { if (classId.value) loadData() })

async function loadData() {
  loading.value = true
  try {
    const [aRes, oRes] = await Promise.all([listAssessments(classId.value), listObjectives(classId.value)])
    assessments.value = aRes.data || []
    objectives.value = oRes.data || []
  } finally { loading.value = false }
}

function getObjNo(oid) { return objectives.value.find(o => o.id === oid)?.objNo || `目标${oid}` }

function showDialog(row) {
  editing.value = row || null
  Object.assign(form, {
    name: row?.name || '', maxScore: row?.maxScore || 100, weightPercent: row?.weightPercent ?? null,
    objectiveIds: row?.objectiveIds ? [...row.objectiveIds] : [],
    sortOrder: row?.sortOrder || assessments.value.length + 1
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const data = {
    name: form.name, maxScore: form.maxScore,
    weightPercent: form.weightPercent != null ? Number(form.weightPercent) : null,
    objectiveIds: form.objectiveIds || [],
    sortOrder: form.sortOrder
  }
  if (editing.value) {
    await updateAssessment(editing.value.id, { ...data, outlineId: editing.value.outlineId })
  } else {
    await createAssessment(classId.value, data)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除考核点「${row.name}」？`, '提示', { type: 'warning' })
  await deleteAssessment(classId.value, row.id)
  ElMessage.success('已删除')
  loadData()
}

async function downloadTemplate() {
  downloading.value = true
  try {
    downloadBlob(await ensureDownloadBlob(await downloadAssessmentTemplate(classId.value)), buildClassFilename(resolveClassName(classId.value), '考核点导入模板', 'xlsx'))
  } catch (error) {
    showDownloadError(error)
  } finally { downloading.value = false }
}

function beforeUpload(file) {
  return validateExcelFile(file)
}

async function uploadFile({ file }) {
  importing.value = true
  try {
    const res = await importAssessments(classId.value, file)
    ElMessage.success(`成功导入 ${res.data} 个考核点`)
    loadData()
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

/* 操作列按钮：对齐课程目标、学生管理等页面风格 */
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
