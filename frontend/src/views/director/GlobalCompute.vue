<template>
  <div class="page-container">
    <div class="page-header">
      <h3>专业级计算</h3>
      <el-select v-model="selectedMajorId" placeholder="选择专业" style="width:240px"
        @change="onMajorChange">
        <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
      </el-select>
    </div>

    <el-alert v-if="dashboard.allReady" type="success" show-icon :closable="false" style="margin-bottom:16px">
      所有课程已完成课程级计算，可以执行专业级计算。
    </el-alert>
    <el-alert v-else-if="dashboard.totalCount > 0" type="warning" show-icon :closable="false" style="margin-bottom:16px">
      {{ dashboard.totalCount - dashboard.lockedCount }} 门课程尚未完成课程级计算，无法执行专业级计算。
    </el-alert>

    <div style="display:flex; gap:16px; flex-wrap:wrap; margin-bottom:16px">
      <el-card header="计算状态" style="flex:1; min-width:300px">
        <div style="font-size:36px; text-align:center; color:var(--action-primary)">
          {{ dashboard.lockedCount }} / {{ dashboard.totalCount }}
        </div>
        <div style="text-align:center; color:var(--text-secondary); margin-top:8px">已锁定课程 / 总课程数</div>
      </el-card>
      <el-card header="操作" style="flex:1; min-width:300px">
        <el-button type="primary" size="large" :disabled="!dashboard.allReady"
          :loading="computing" @click="handleCompute" style="width:100%">
          {{ computing ? '计算中...' : '执行专业级计算' }}
        </el-button>
      </el-card>
    </div>

    <!-- Results -->
    <el-card v-if="hasResults" header="专业级达成度结果（第三级）">
      <el-table :data="resultData" border stripe size="small">
        <el-table-column prop="indicatorNo" label="指标点" width="120" />
        <el-table-column prop="achievement" label="达成度 G_k" />
      </el-table>
      <div style="margin-top:12px; color:var(--text-secondary); font-size:13px">
        计算时间：{{ calcTime }}
      </div>
      <div style="margin-top:12px">
        <el-button @click="downloadExcel">导出穿透式 Excel 台账</el-button>
      </div>
    </el-card>

    <!-- Radar chart placeholder -->
    <el-card v-if="hasResults" header="达成度雷达图" style="margin-top:16px">
      <div ref="chartRef" style="width:100%;height:400px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getDashboard, triggerGlobalCompute, getGlobalResults,
  getRadarData, downloadMajorExcel
} from '../../api/director'
import { listMajors } from '../../api/admin'
import { listGradReqs } from '../../api/director'
import { useUserStore } from '../../stores/user'
import * as echarts from 'echarts'

const userStore = useUserStore()
const majors = ref([])
const selectedMajorId = ref(null)
const computing = ref(false)
const chartRef = ref(null)
const indicatorLabels = ref({})

const dashboard = reactive({
  allReady: false, lockedCount: 0, totalCount: 0,
  incompleteClassIds: [], courseStatuses: []
})

const results = ref({})
const calcTime = ref('')

onMounted(async () => {
  try {
    const mRes = await listMajors()
    majors.value = mRes.data?.records || []
    if (majors.value.length > 0) {
      selectedMajorId.value = majors.value[0].id
      loadDashboard()
      loadIndicatorLabels()
      loadResults()
    }
  } catch { /* handled */ }
})

async function loadIndicatorLabels() {
  try {
    const res = await listGradReqs(selectedMajorId.value)
    const allIndicators = (res.data || []).flatMap(r => r.indicators || [])
    for (const ind of allIndicators) {
      indicatorLabels.value[ind.id] = ind.indicatorNo
    }
  } catch { /* ignore */ }
}

async function loadResults() {
  if (!selectedMajorId.value) return
  try {
    const res = await getGlobalResults(selectedMajorId.value, 1)
    results.value = res.data || {}
    if (Object.keys(results.value).length > 0) {
      await nextTick()
      renderChart()
    }
  } catch { /* ignore */ }
}

function onMajorChange() {
  results.value = {}
  calcTime.value = ''
  loadDashboard()
  loadIndicatorLabels()
  loadResults()
}

async function loadDashboard() {
  if (!selectedMajorId.value) return
  try {
    const res = await getDashboard(selectedMajorId.value)
    Object.assign(dashboard, res.data || {})
  } catch { /* handled */ }
}

const hasResults = computed(() => Object.keys(results.value).length > 0)

const resultData = computed(() => {
  return Object.entries(results.value).map(([id, val]) => ({
    indicatorNo: indicatorLabels.value[id] || id, achievement: val
  }))
})

async function handleCompute() {
  if (!dashboard.allReady) return
  computing.value = true
  try {
    const userId = userStore.userId || 1
    const res = await triggerGlobalCompute(selectedMajorId.value, 1, userId)
    results.value = res.data?.achievements || {}
    calcTime.value = res.data?.calcTime || ''
    ElMessage.success('专业级计算完成')
    await nextTick()
    renderChart()
  } catch { /* handled */ }
  finally { computing.value = false }
}

async function renderChart() {
  if (!chartRef.value) return
  try {
    const res = await getRadarData(selectedMajorId.value, 1)
    const radarData = res.data?.radarData || {}
    const indicators = Object.keys(radarData)
    const values = Object.values(radarData)

    const chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: {},
      radar: {
        indicator: indicators.map(k => ({ name: k, max: 1 }))
      },
      series: [{
        type: 'radar',
        data: [{ value: values, name: '达成度' }],
        areaStyle: { opacity: 0.3 }
      }]
    })
  } catch { /* handled */ }
}

async function downloadExcel() {
  if (!selectedMajorId.value) return
  const blob = await downloadMajorExcel(selectedMajorId.value, 1)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `专业级达成度台账.xlsx`
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header { display: flex; align-items: center; gap: var(--space-4); margin-bottom: var(--space-4); flex-wrap: wrap; }
.page-header h3 { margin: 0; font-size: var(--text-lg); }
</style>
