<template>
  <div class="page-container">
    <div class="page-header">
      <h3>宏观支撑矩阵配置</h3>
      <div class="header-actions">
        <el-select v-model="currentMajorId" placeholder="选择专业" class="major-select" @change="loadData">
          <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
        </el-select>
        <span class="hint" v-if="indicators.length > 0">所有指标点列合计必须为 1.00</span>
        <el-button v-if="indicators.length > 0" @click="handleDownloadTemplate" :loading="downloading">下载模板</el-button>
        <el-button v-if="indicators.length > 0" @click="openImportDialog">导入课程支撑</el-button>
        <!-- <el-button v-if="indicators.length > 0" @click="handleAddRow">添加课程支撑</el-button> -->
        <el-button v-if="indicators.length > 0" type="primary" @click="handleSubmit" :disabled="!allColumnsValid">提交生效</el-button>
      </div>
    </div>

    <el-empty v-if="!currentMajorId" description="请先选择专业" />
    <div v-else v-loading="loading" class="content-card">
      <div class="matrix-wrapper" v-if="indicators.length > 0">
        <el-table class="matrix-table" :data="matrixRows" border size="small" :cell-class-name="cellClassName"
                  show-summary :summary-method="getSummaries"
                  :style="{ minWidth: `max(100%, ${180 + indicators.length * 150}px)` }"
                  @cell-mouse-enter="highlightRC" @cell-mouse-leave="clearHighlight">
          <el-table-column prop="courseName" label="课程" min-width="180" class-name="nowrap-column" />
          <el-table-column v-for="ind in indicators" :key="ind.id" :label="`${ind.indicatorNo}`"
                           min-width="140" align="center">
            <template #header>
              <div class="indicator-header">{{ ind.indicatorNo }}</div>
            </template>
            <template #default="{ row }">
              <div class="macro-cell">
                <el-checkbox
                  :model-value="row.cells[ind.id] !== undefined"
                  @change="(val) => onToggleSupport(row.courseId, ind.id, val)"
                />
                <el-input-number
                  v-if="row.cells[ind.id] !== undefined"
                  v-model="row.cells[ind.id].weight"
                  :min="0"
                  :max="1"
                  :step="0.05"
                  :precision="2"
                  size="small"
                  class="macro-weight-input"
                  style="width: 110px"
                  @change="onWeightChange(row.courseId, ind.id, row.cells[ind.id].weight)"
                />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else description="请先配置毕业要求和指标点" />
    </div>

    <!-- 添加支撑关系对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加课程支撑" width="480px">
      <el-form label-width="80px">
        <el-form-item label="课程">
          <el-select v-model="addForm.courseId" filterable placeholder="选择课程" style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="`${c.code || ''} ${c.name}`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="支撑强度">
          <el-select v-model="addForm.supportLevel" style="width: 100%">
            <el-option value="H" label="H (高支撑)" />
            <el-option value="M" label="M (中等支撑)" />
            <el-option value="L" label="L (低支撑)" />
          </el-select>
        </el-form-item>
        <el-form-item label="指标点">
          <el-select v-model="addForm.indicatorIds" multiple placeholder="选择支撑的指标点" style="width: 100%">
            <el-option v-for="ind in indicators" :key="ind.id" :label="`${ind.indicatorNo} ${ind.content?.substring(0,20)}`" :value="ind.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 导入课程支撑对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入课程支撑" width="520px">
      <div class="import-tip">
        请先点击「下载模板」获取标准模板，按格式填写后再上传。<br />
        首列为「课程名称」，其余各列为指标点编号，单元格填写支撑权重（0~1）。<b>表头不可修改</b>。
      </div>
      <el-upload
        :show-file-list="false"
        :before-upload="beforeUpload"
        :http-request="customUpload"
        accept=".xlsx"
        :disabled="importing"
      >
        <el-button type="primary" :loading="importing">选择文件并导入</el-button>
        <template #tip>
          <div class="upload-tip">仅支持 .xlsx 格式；导入为合并更新，不会删除已有支撑，核对无误后请点击「提交生效」保存。</div>
        </template>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted,h } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMacroMatrix, updateMacroMatrix, downloadMacroMatrixTemplate, importMacroMatrix } from '../../api/director'
import { listGradReqs } from '../../api/director'
import { listMajors } from '../../api/admin'
import { listCourses } from '../../api/academic'
import { buildDatedFilename, downloadBlob, ensureDownloadBlob, showDownloadError } from '../../utils/downloadFile'

const loading = ref(false)
const majors = ref([])
const courses = ref([])
const currentMajorId = ref(null)
const indicators = ref([])
const matrixData = ref([]) // raw MacroSupportMatrix[]
const hoverRow = ref(null)
const hoverColLabel = ref(null)
const addDialogVisible = ref(false)
const addForm = reactive({ courseId: null, supportLevel: 'H', indicatorIds: [] })
const downloading = ref(false)
const importing = ref(false)
const importDialogVisible = ref(false)
const currentMajorName = computed(() => majors.value.find(item => item.id === currentMajorId.value)?.name || `专业${currentMajorId.value}`)

const matrixRows = computed(() => {
  const courseMap = new Map()
  matrixData.value.forEach(m => {
    if (!courseMap.has(m.courseId)) {
      courseMap.set(m.courseId, { courseId: m.courseId, courseName: courses.value.find(c => c.id === m.courseId)?.name || `课程${m.courseId}`, cells: {} })
    }
    courseMap.get(m.courseId).cells[m.indicatorId] = { weight: m.weight || 0, supportLevel: m.supportLevel }
  })
  return Array.from(courseMap.values())
})

const columnSums = computed(() => {
  const sums = {}
  indicators.value.forEach(ind => {
    let sum = 0
    matrixData.value.forEach(m => {
      if (m.indicatorId === ind.id && m.weight) sum += m.weight
    })
    sums[ind.id] = { sum, valid: Math.abs(sum - 1.0) < 0.011 }
  })
  return sums
})

const allColumnsValid = computed(() => {
  return indicators.value.every(ind => columnSums.value[ind.id]?.valid)
})

function getSummaries({ columns }) {
  return columns.map((column, index) => {
    if (index === 0) return '列合计'

    const ind = indicators.value[index - 1]
    if (!ind) return '-'

    const sumText = columnSums.value[ind.id]?.sum?.toFixed(2) || '0.00'
    const isValid = sumText === '1.00'

    return h(
      'span',
      {
        class: ['macro-col-sum', isValid ? 'is-valid' : 'is-invalid']
      },
      sumText
    )
  })
}

onMounted(async () => {
  const [majorRes, courseRes] = await Promise.all([listMajors({ page: 1, size: 100 }), listCourses({ page: 1, size: 200 })])
  majors.value = majorRes.data.records
  courses.value = courseRes.data.records
})

async function loadData() {
  if (!currentMajorId.value) return
  loading.value = true
  try {
    const [gradRes, matrixRes] = await Promise.all([listGradReqs(currentMajorId.value), getMacroMatrix(currentMajorId.value)])
    indicators.value = (gradRes.data || []).flatMap(r => r.indicators || [])
    matrixData.value = matrixRes.data || []
  } finally {
    loading.value = false
  }
}

function onWeightChange(courseId, indicatorId, weight) {
  const item = matrixData.value.find(m => m.courseId === courseId && m.indicatorId === indicatorId)
  if (item) item.weight = weight
}

// 新勾选一个支撑关系时使用的默认权重
const DEFAULT_SUPPORT_WEIGHT = 0.1

function onToggleSupport(courseId, indicatorId, checked) {
  const idx = matrixData.value.findIndex(m => m.courseId === courseId && m.indicatorId === indicatorId)
  if (checked && idx === -1) {
    // 勾选 = 建立支撑关系，出现可调整的数字
    matrixData.value.push({ courseId, indicatorId, supportLevel: 'M', weight: DEFAULT_SUPPORT_WEIGHT })
  } else if (!checked && idx !== -1) {
    // 取消勾选 = 移除支撑关系
    matrixData.value.splice(idx, 1)
  }
}

async function handleSubmit() {
  if (!allColumnsValid.value) {
    ElMessage.error('存在指标点权重合计不为 1.00，请检查')
    return
  }
  await updateMacroMatrix(matrixData.value.map(m => ({
    courseId: m.courseId, indicatorId: m.indicatorId, supportLevel: m.supportLevel, weight: m.weight
  })))
  ElMessage.success('矩阵已保存')
}

function handleAddRow() {
  Object.assign(addForm, { courseId: null, supportLevel: 'H', indicatorIds: [] })
  addDialogVisible.value = true
}

async function handleAddSubmit() {
  const newEntries = addForm.indicatorIds.map(indId => ({
    courseId: addForm.courseId, indicatorId: indId, supportLevel: addForm.supportLevel, weight: 0
  }))
  matrixData.value.push(...newEntries)
  addDialogVisible.value = false
}

function openImportDialog() {
  if (!currentMajorId.value) { ElMessage.warning('请先选择专业'); return }
  importDialogVisible.value = true
}

async function handleDownloadTemplate() {
  if (!currentMajorId.value) { ElMessage.warning('请先选择专业'); return }
  downloading.value = true
  try {
    const blob = await downloadMacroMatrixTemplate(currentMajorId.value)
    downloadBlob(await ensureDownloadBlob(blob), buildDatedFilename([currentMajorName.value, '课程支撑矩阵导入模板'], 'xlsx'))
    ElMessage.success('模板已开始下载')
  } catch (error) {
    showDownloadError(error)
  } finally {
    downloading.value = false
  }
}

function beforeUpload(file) {
  const ok = !!file.name && file.name.toLowerCase().endsWith('.xlsx')
  if (!ok) ElMessage.error('仅支持 .xlsx 格式文件，请先下载标准模板')
  return ok
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

async function customUpload(opt) {
  const file = opt.file
  importing.value = true
  try {
    const res = await importMacroMatrix(currentMajorId.value, file)
    const entries = res.data || []
    mergeImported(entries)
    ElMessage.success(`成功导入 ${entries.length} 条支撑关系，请核对后点击「提交生效」保存`)
    importDialogVisible.value = false
  } catch (e) {
    const msg = e && e.message ? e.message : '导入失败'
    // 后端逐行列出问题，换行展示，便于用户对照修改
    ElMessageBox.alert(escapeHtml(msg).replace(/\n/g, '<br/>'), '导入失败', {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '知道了',
      type: 'error'
    })
  } finally {
    importing.value = false
  }
}

// 合并导入的支撑条目：同课程同指标点更新权重，否则新增（不删除已有支撑）
function mergeImported(entries) {
  for (const e of entries) {
    const idx = matrixData.value.findIndex(m => m.courseId === e.courseId && m.indicatorId === e.indicatorId)
    if (idx === -1) {
      matrixData.value.push({
        courseId: e.courseId,
        indicatorId: e.indicatorId,
        supportLevel: e.supportLevel || 'M',
        weight: Number(e.weight)
      })
    } else {
      matrixData.value[idx].weight = Number(e.weight)
      if (e.supportLevel) matrixData.value[idx].supportLevel = e.supportLevel
    }
  }
}

function cellClassName({ row, columnIndex }) {
  if (columnIndex === 0) return ''
  const ind = indicators.value[columnIndex - 1]
  const classes = []
  if (ind && row.cells[ind.id] !== undefined) {
    classes.push('weight-cell')
  }
  if (hoverRow.value && row === hoverRow.value) classes.push('highlight-row')
  if (hoverColLabel.value && ind && hoverColLabel.value === ind.indicatorNo) {
    classes.push('highlight-col')
  }
  return classes.join(' ')
}

function highlightRC(row, column) {
  hoverRow.value = row
  hoverColLabel.value = column?.label || null
}

function clearHighlight() {
  hoverRow.value = null
  hoverColLabel.value = null
}
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
  flex-wrap: wrap;
}
.page-header h3 { margin: 0; font-size: var(--text-lg); }
.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3);
  flex-wrap: wrap;
}
.major-select { width: 220px; }
.matrix-wrapper { overflow-x: auto; max-width: 100%; }
.matrix-table { width: 100%; }
/* 已勾选（存在支撑关系）的单元格：浅紫底色，便于一眼看出支撑分布 */
:deep(.matrix-table .el-table__row td.weight-cell) {
  background-color: rgba(128, 107, 191, 0.06) !important;
}
/* 单元格内部：复选框 + 数字纵向排列（分两行） */
.macro-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
:deep(.matrix-table .el-table__row td.highlight-row),
:deep(.matrix-table .el-table__row td.highlight-col) {
  background-color: rgba(128, 107, 191, 0.12) !important;
}
:deep(.matrix-table .el-table__row td.highlight-row.highlight-col) {
  background-color: rgba(128, 107, 191, 0.22) !important;
}
.indicator-header { font-size: var(--text-xs); text-align: center; }
.hint { color: var(--text-secondary); font-size: 13px; }
.import-tip {
  margin-bottom: 16px;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.7;
  color: #6f42c1;
  background: rgba(128, 107, 191, 0.08);
  border: 1px solid rgba(128, 107, 191, 0.16);
}
.upload-tip {
  margin-top: 8px;
  color: var(--text-secondary, #909399);
  font-size: 12px;
}
:deep(.nowrap-column .cell) { white-space: nowrap; }
/* 列合计行：只控制字体颜色和粗细 */
:deep(.matrix-table .el-table__footer-wrapper td) {
  text-align: center;
  font-weight: var(--font-medium);
}

:deep(.matrix-table .el-table__footer-wrapper td:first-child) {
  color: var(--text-primary);
  font-weight: 600;
}

:deep(.matrix-table .el-table__footer-wrapper .macro-col-sum) {
  font-weight: 600;
}

:deep(.matrix-table .el-table__footer-wrapper .macro-col-sum.is-valid) {
  color: var(--el-color-success);
}

:deep(.matrix-table .el-table__footer-wrapper .macro-col-sum.is-invalid) {
  color: var(--el-color-danger);
  font-weight: 700;
}
/* 宏观矩阵权重输入框：加减按钮浅紫色填充 */
:deep(.matrix-table .macro-weight-input .el-input-number__decrease),
:deep(.matrix-table .macro-weight-input .el-input-number__increase) {
  background: #f1eaff;
  border-color: #d9c8f5;
  color: #7e57c2;
  font-weight: 700;
  transition: all 0.18s ease;
}

:deep(.matrix-table .macro-weight-input .el-input-number__decrease:hover),
:deep(.matrix-table .macro-weight-input .el-input-number__increase:hover) {
  background: #e6dcff;
  border-color: #c7b3f2;
  color: #6f42c1;
}

/* 禁用状态，例如已经到 0 或 1 时，颜色淡一点 */
:deep(.matrix-table .macro-weight-input .el-input-number__decrease.is-disabled),
:deep(.matrix-table .macro-weight-input .el-input-number__increase.is-disabled) {
  background: #f5f2fb;
  border-color: #ebe6f5;
  color: #c3b6de;
}

/* 输入框本体保持干净，只轻微圆角统一 */
:deep(.matrix-table .macro-weight-input .el-input__wrapper) {
  box-shadow: 0 0 0 1px #ebe6f5 inset;
}

:deep(.matrix-table .macro-weight-input .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #d9c8f5 inset;
}
</style>
