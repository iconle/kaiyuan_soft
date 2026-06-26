<template>
  <div class="page-container">
    <div class="page-header">
      <h3>宏观支撑矩阵配置</h3>
      <el-select v-model="currentMajorId" placeholder="选择专业" style="width:220px" @change="loadData">
        <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
      </el-select>
      <div style="flex:1" />
      <span class="hint" v-if="indicators.length > 0">所有指标点列合计必须为 1.00</span>
      <el-button v-if="indicators.length > 0" @click="handleAddRow">添加课程支撑</el-button>
      <el-button v-if="indicators.length > 0" type="primary" @click="handleSubmit" :disabled="!allColumnsValid">提交生效</el-button>
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
                           min-width="150" align="center">
            <template #header>
              <div class="indicator-header">{{ ind.indicatorNo }}</div>
            </template>
            <template #default="{ row }">
              
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
               
              <span v-else>-</span>
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted,h } from 'vue'
import { ElMessage } from 'element-plus'
import { getMacroMatrix, updateMacroMatrix } from '../../api/director'
import { listGradReqs } from '../../api/director'
import { listMajors } from '../../api/admin'
import { listCourses } from '../../api/academic'

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

function cellClassName({ row, columnIndex }) {
  if (columnIndex === 0) return ''
  const ind = indicators.value[columnIndex - 1]
  const classes = []
  if (ind && row.cells[ind.id] !== undefined && row.cells[ind.id].weight > 0) {
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
.page-header { display: flex; align-items: center; gap: var(--space-4); margin-bottom: var(--space-5); }
.page-header h3 { margin: 0; font-size: var(--text-lg); }
.matrix-wrapper { overflow-x: auto; max-width: 100%; }
.matrix-table { width: 100%; }
:deep(.matrix-table .el-table__row td.highlight-row),
:deep(.matrix-table .el-table__row td.highlight-col) {
  background-color: rgba(128, 107, 191, 0.12) !important;
}
:deep(.matrix-table .el-table__row td.highlight-row.highlight-col) {
  background-color: rgba(128, 107, 191, 0.22) !important;
}
.indicator-header { font-size: var(--text-xs); text-align: center; }
.hint { color: var(--text-secondary); font-size: 13px; }
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
