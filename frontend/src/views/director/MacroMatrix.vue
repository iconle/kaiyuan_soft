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
    <div v-else v-loading="loading">
      <div class="matrix-wrapper" v-if="indicators.length > 0">
        <el-table :data="matrixRows" border size="small" :cell-class-name="cellClassName"
                  @cell-mouse-enter="highlightRC" @cell-mouse-leave="clearHighlight">
          <el-table-column prop="courseName" label="课程" width="140" />
          <el-table-column v-for="ind in indicators" :key="ind.id" :label="`${ind.indicatorNo}`"
                           width="130" align="center">
            <template #header>
              <div class="indicator-header">{{ ind.indicatorNo }}</div>
            </template>
            <template #default="{ row }">
              <el-input-number v-if="row.cells[ind.id] !== undefined" v-model="row.cells[ind.id].weight"
                               :min="0" :max="1" :step="0.05" :precision="2" size="small"
                               style="width: 110px"
                               @change="onWeightChange(row.courseId, ind.id, row.cells[ind.id].weight)" />
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 列合计行 -->
        <div class="sum-row">
          <span class="sum-label">列合计</span>
          <span v-for="ind in indicators" :key="ind.id" class="sum-cell"
                :class="{ valid: columnSums[ind.id]?.valid, invalid: !columnSums[ind.id]?.valid }">
            {{ columnSums[ind.id]?.sum?.toFixed(2) || '-' }}
          </span>
        </div>

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
import { ref, reactive, computed, onMounted } from 'vue'
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
  if (ind && row.cells[ind.id] !== undefined && row.cells[ind.id].weight > 0) return 'weight-cell'
  return ''
}

function highlightRC() {}
function clearHighlight() {}
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
.page-header h3 { margin: 0; font-size: 18px; }
.matrix-wrapper { overflow-x: auto; max-width: 100%; }
.matrix-wrapper :deep(.el-table) { min-width: 3390px; }
.indicator-header { font-size: 12px; text-align: center; }
.sum-row { display: flex; align-items: center; padding: 8px 0; border-bottom: 1px solid #ebeef5; min-width: 3390px; }
.sum-label { width: 140px; font-weight: 600; font-size: 13px; text-align: center; flex-shrink: 0; }
.sum-cell { width: 130px; text-align: center; font-size: 13px; font-weight: 500; flex-shrink: 0; }
.sum-cell.valid { color: #67c23a; }
.sum-cell.invalid { color: #f56c6c; }
.hint { color: #909399; font-size: 13px; }
</style>
