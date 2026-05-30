<template>
  <div class="page-container">
    <div class="page-header">
      <h3>内部权重分配</h3>
      <el-button type="primary" @click="handleSave" :disabled="!allValid">保存权重</el-button>
    </div>

    <div class="content-card">
      <el-alert v-if="!allValid" type="warning" :closable="false" style="margin-bottom: 16px"
                title="存在指标点权重合计不为 1.00，请调整后再保存" />

      <div v-if="supportedIndicators.length === 0" class="empty-hint">
        <el-empty description="本课程尚未在宏观支撑矩阵中配置支撑关系，请先由专业负责人配置宏观矩阵" />
      </div>

      <div v-else class="weight-matrix-scroll">
      <el-table class="weight-matrix-table" :data="weightMatrix" border size="small" v-loading="loading"
        show-summary :summary-method="getSummaries"
        :style="{ minWidth: `max(100%, ${160 + supportedIndicators.length * 180}px)` }">
      <el-table-column prop="objNo" label="课程目标" min-width="160" fixed />
      <el-table-column v-for="ind in supportedIndicators" :key="ind.id" :label="ind.indicatorNo" min-width="180" align="center">
        <template #header>
          <div>{{ ind.indicatorNo }}</div>
        </template>
        <template #default="{ row }">
          <el-input-number v-model="row.weights[ind.id]" :min="0" :max="1" :step="0.1" :precision="4"
                           size="small" controls-position="right" style="width: 120px" />
        </template>
      </el-table-column>
    </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getWeights, updateWeights, getSupportedIndicators, listObjectives } from '../../api/teacher'

const route = useRoute()
const classId = ref(route.params.classId || route.query.classId)
const loading = ref(false)
const objectives = ref([])
const supportedIndicators = ref([])
const weightData = ref([]) // flat list from API
const weightMatrix = ref([]) // computed matrix rows

const colSums = computed(() => {
  const sums = {}
  supportedIndicators.value.forEach(ind => {
    let sum = 0
    weightMatrix.value.forEach(row => { sum += (row.weights[ind.id] || 0) })
    sums[ind.id] = { sum, valid: Math.abs(sum - 1.0) < 0.011 }
  })
  return sums
})

const allValid = computed(() => {
  if (supportedIndicators.value.length === 0) return false
  return Object.values(colSums.value).every(s => s.valid)
})

function getSummaries({ columns }) {
  return columns.map((column, index) => {
    if (index === 0) return '列合计'
    const ind = supportedIndicators.value[index - 1]
    return ind ? (colSums.value[ind.id]?.sum?.toFixed(2) || '0.00') : '-'
  })
}

onMounted(() => { if (classId.value) loadData() })

async function loadData() {
  loading.value = true
  try {
    const [objRes, indRes, weightRes] = await Promise.all([
      listObjectives(classId.value),
      getSupportedIndicators(classId.value),
      getWeights(classId.value)
    ])
    objectives.value = objRes.data || []
    supportedIndicators.value = indRes.data || []
    weightData.value = weightRes.data || []

    // Build matrix: one row per objective
    weightMatrix.value = objectives.value.map(obj => {
      const weights = {}
      supportedIndicators.value.forEach(ind => {
        const existing = weightData.value.find(w => w.objectiveId === obj.id && w.indicatorId === ind.id)
        weights[ind.id] = existing?.weight || 0
      })
      return { objectiveId: obj.id, objNo: obj.objNo, weights }
    })
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!allValid.value) { ElMessage.error('权重校验未通过'); return }
  const flatWeights = []
  weightMatrix.value.forEach(row => {
    supportedIndicators.value.forEach(ind => {
      if (row.weights[ind.id] > 0) {
        flatWeights.push({ objectiveId: row.objectiveId, indicatorId: ind.id, weight: row.weights[ind.id] })
      }
    })
  })
  await updateWeights(classId.value, flatWeights)
  ElMessage.success('权重已保存')
}
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-4); }
.page-header h3 { margin: 0; font-size: var(--text-lg); }
.col-sum { font-size: var(--text-xs); font-weight: var(--font-semibold); }
.col-sum.valid { color: var(--el-color-success); }
.col-sum.invalid { color: var(--el-color-danger); }
.empty-hint { margin-top: 40px; }
.weight-matrix-scroll { overflow-x: auto; max-width: 100%; }
.weight-matrix-table { width: 100%; }
:deep(.el-table__footer-wrapper td) {
  color: var(--el-color-success);
  font-weight: var(--font-medium);
  text-align: center;
}
:deep(.el-table__footer-wrapper td:first-child) {
  color: var(--text-primary);
}
</style>
