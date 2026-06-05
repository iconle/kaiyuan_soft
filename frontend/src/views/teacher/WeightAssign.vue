<template>
  <div class="page-container">
    <div class="page-header">
      <h3>内部权重分配</h3>
      <el-button
        class="save-weight-btn"
        type="primary"
        round
        :disabled="!allValid || saving"
        :loading="saving"
        @click="handleSave"
      >
        保存权重
      </el-button>
    </div>

    <div class="content-card">
      <el-alert
        v-if="!allValid"
        class="weight-warning-alert"
        type="error"
        :closable="false"
        title="存在指标点权重合计不为 1.00，请调整后再保存"
      />

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
          <el-input-number
            v-model="row.weights[ind.id]"
            class="weight-input"
            :min="0"
            :max="1"
            :step="0.1"
            :precision="4"
            size="small"
            controls-position="right"
          />
        </template>
      </el-table-column>
    </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getWeights, updateWeights, getSupportedIndicators, listObjectives } from '../../api/teacher'

const route = useRoute()
const classId = ref(route.params.classId || route.query.classId)
const loading = ref(false)
const objectives = ref([])
const saving = ref(false)
const supportedIndicators = ref([])
const weightData = ref([]) // flat list from API
const weightMatrix = ref([]) // computed matrix rows

const colSums = computed(() => {
  const sums = {}
  supportedIndicators.value.forEach(ind => {
    let sum = 0
    weightMatrix.value.forEach(row => { sum += (row.weights[ind.id] || 0) })
    sums[ind.id] = { sum, valid: Math.abs(sum - 1.0) < 0.0001 }
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
    if (!ind) return '-'

    const col = colSums.value[ind.id]
    const sumText = col?.sum?.toFixed(2) || '0.00'
    const valid = col?.valid

    return h(
      'span',
      {
        class: ['col-sum', valid ? 'valid' : 'invalid']
      },
      sumText
    )
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
  if (!allValid.value) {
    ElMessage.error('权重校验未通过')
    return
  }

  saving.value = true
  try {
    const flatWeights = []
    weightMatrix.value.forEach(row => {
      supportedIndicators.value.forEach(ind => {
        if (row.weights[ind.id] > 0) {
          flatWeights.push({
            objectiveId: row.objectiveId,
            indicatorId: ind.id,
            weight: row.weights[ind.id]
          })
        }
      })
    })

    await updateWeights(classId.value, flatWeights)
    ElMessage.success('权重已保存')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page-container {  padding: var(--space-5);}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  margin-bottom: var(--space-4);
}

.page-header h3 {
  margin: 0;
  font-size: var(--text-lg);
}

.content-card {
  position: relative;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(158, 137, 205, 0.12);
  box-shadow: 0 18px 40px rgba(31, 41, 55, 0.08);
  overflow: hidden;
}

.content-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 3px;
  background: linear-gradient(90deg, #9e89cd, #f2a7b3, #9ad3bc);
}

.content-card > * {
  position: relative;
  z-index: 1;
}

.empty-hint {
  margin-top: 40px;
}

.weight-matrix-scroll {
  overflow-x: auto;
  max-width: 100%;
  border-radius: 14px;
}

.weight-matrix-table {
  width: 100%;
}

:deep(.weight-matrix-table) {
  width: 100%;
  border-radius: 14px;
  overflow: hidden;
}

:deep(.weight-matrix-table .el-table__inner-wrapper) {
  border-radius: 14px;
}

:deep(.weight-matrix-table .el-table__header-wrapper),
:deep(.weight-matrix-table .el-table__body-wrapper),
:deep(.weight-matrix-table .el-table__footer-wrapper) {
  width: 100%;
}

:deep(.weight-matrix-table .el-table__header th) {
  background: rgba(248, 247, 252, 0.96);
  color: var(--text-secondary);
  font-weight: 700;
}

:deep(.weight-matrix-table .el-table__body tr:hover > td) {
  background: rgba(158, 137, 205, 0.08) !important;
}

:deep(.weight-matrix-table .el-table__footer-wrapper td) {
  color: var(--el-color-success);
  font-weight: var(--font-medium);
  text-align: center;
  background: rgba(248, 247, 252, 0.98);
}

:deep(.weight-matrix-table .el-table__footer-wrapper td:first-child) {
  color: var(--text-primary);
}

.col-sum {
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
}

:deep(.weight-matrix-table .el-table__footer-wrapper .col-sum) {
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
}

:deep(.weight-matrix-table .el-table__footer-wrapper .col-sum.valid) {
  color: var(--el-color-success) !important;
}

:deep(.weight-matrix-table .el-table__footer-wrapper .col-sum.invalid) {
  color: var(--el-color-danger) !important;
}

.save-weight-btn {
  min-width: 104px;
  height: 32px;
  padding: 0 20px;
  border: none;
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #9e89cd, #8f7bc7);
  box-shadow: 0 6px 14px rgba(128, 107, 191, 0.22);
  transition: all 0.2s ease;
}

.save-weight-btn:not(.is-disabled):hover {
  transform: translateY(-1px);
  background: linear-gradient(135deg, #a895d4, #806bbf);
  box-shadow: 0 8px 18px rgba(128, 107, 191, 0.28);
}

.save-weight-btn.is-disabled {
  opacity: 0.55;
  box-shadow: none;
}

:deep(.weight-input) {
  width: 140px;
}

:deep(.weight-input .el-input__wrapper) {
  border-radius: 999px;
  box-shadow: 0 0 0 1px rgba(128, 107, 191, 0.16) inset;
}

:deep(.weight-input .el-input__inner) {
  font-weight: 500;
  text-align: center;
}

:deep(.weight-input .el-input-number__increase),
:deep(.weight-input .el-input-number__decrease) {
  width: 28px;
  background: rgba(128, 107, 191, 0.24);
  color: #5b43a3;
  border-color: rgba(128, 107, 191, 0.45);
  font-weight: 600;
}

:deep(.weight-input .el-input-number__increase:hover),
:deep(.weight-input .el-input-number__decrease:hover) {
  background: rgba(128, 107, 191, 0.36);
  color: #4c358f;
  border-color: rgba(128, 107, 191, 0.6);
}

:deep(.weight-warning-alert) {
  margin-bottom: 16px;
  border-radius: 16px;
  background-color: rgba(245, 108, 108, 0.12) !important;
  border: 1px solid rgba(245, 108, 108, 0.28);
}

:deep(.weight-warning-alert .el-alert__title) {
  color: #d93025 !important;
  font-weight: 500;
}

:deep(.weight-warning-alert .el-alert__icon) {
  color: #d93025 !important;
}
</style>