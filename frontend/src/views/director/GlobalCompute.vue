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
        <el-table-column prop="achievement" label="达成度 G_k">
          <template #default="{ row }">
            <el-tag :type="getAchievementType(row.achievement)" effect="plain">
              {{ row.achievement.toFixed(3) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span :style="{ color: getStatusColor(row.achievement) }">
              {{ getStatusText(row.achievement) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:12px; color:var(--text-secondary); font-size:13px">
        计算时间：{{ calcTime }}
      </div>
      <div style="margin-top:12px">
        <el-button @click="downloadExcel">导出穿透式 Excel 台账</el-button>
      </div>
    </el-card>

    <!-- Charts Section -->
    <el-card v-if="hasResults" header="达成度可视化分析" style="margin-top:16px">
      <!-- Chart Type Selector -->
      <div style="margin-bottom:16px; display:flex; gap:12px; align-items:center">
        <span>图表类型：</span>
        <el-radio-group v-model="chartType" @change="renderCurrentChart">
          <el-radio-button value="radar">雷达图</el-radio-button>
          <el-radio-button value="bar">柱状图</el-radio-button>
          <el-radio-button value="line">趋势图</el-radio-button>
        </el-radio-group>
        <el-button v-if="chartType !== 'radar'" @click="toggleSort" size="small" style="margin-left:auto">
          {{ sortAsc ? '降序排列' : '升序排列' }}
        </el-button>
      </div>

      <!-- Chart Container -->
      <div ref="chartRef" style="width:100%;height:450px"></div>

      <!-- Statistics Summary -->
      <div style="margin-top:16px; padding:12px; background:#f5f7fa; border-radius:4px">
        <div style="display:flex; justify-content:space-around; flex-wrap:wrap; gap:16px">
          <div style="text-align:center">
            <div style="font-size:24px; font-weight:bold; color:#67C23A">{{ stats.avg.toFixed(3) }}</div>
            <div style="color:#909399; font-size:12px">平均达成度</div>
          </div>
          <div style="text-align:center">
            <div style="font-size:24px; font-weight:bold; color:#409EFF">{{ stats.max.toFixed(3) }}</div>
            <div style="color:#909399; font-size:12px">最高达成度</div>
          </div>
          <div style="text-align:center">
            <div style="font-size:24px; font-weight:bold; color:#E6A23C">{{ stats.min.toFixed(3) }}</div>
            <div style="color:#909399; font-size:12px">最低达成度</div>
          </div>
          <div style="text-align:center">
            <div style="font-size:24px; font-weight:bold" :style="{ color: stats.weakCount > 0 ? '#F56C6C' : '#67C23A' }">
              {{ stats.weakCount }}
            </div>
            <div style="color:#909399; font-size:12px">低于0.7的指标点</div>
          </div>
        </div>
      </div>
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
const chartType = ref('radar')
const chartInstance = ref(null)
const sortAsc = ref(false)

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
      renderCurrentChart()
    }
  } catch { /* ignore */ }
}

function onMajorChange() {
  results.value = {}
  calcTime.value = ''
  chartType.value = 'radar'
  sortAsc.value = false
  if (chartInstance.value) {
    chartInstance.value.dispose()
    chartInstance.value = null
  }
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

// 统计数据
const stats = computed(() => {
  const values = Object.values(results.value).map(Number)
  if (values.length === 0) return { avg: 0, max: 0, min: 0, weakCount: 0 }
  return {
    avg: values.reduce((a, b) => a + b, 0) / values.length,
    max: Math.max(...values),
    min: Math.min(...values),
    weakCount: values.filter(v => v < 0.7).length
  }
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
    renderCurrentChart()
  } catch { /* handled */ }
  finally { computing.value = false }
}

async function renderChart() {
  await renderCurrentChart()
}

async function renderCurrentChart() {
  if (!chartRef.value) return
  try {
    const res = await getRadarData(selectedMajorId.value, 1)
    const radarData = res.data?.radarData || {}

    // 销毁旧图表
    if (chartInstance.value) {
      chartInstance.value.dispose()
    }

    const chart = echarts.init(chartRef.value)
    chartInstance.value = chart

    const indicators = Object.keys(radarData)
    const values = Object.values(radarData)

    if (chartType.value === 'radar') {
      renderRadarChart(chart, indicators, values)
    } else if (chartType.value === 'bar') {
      renderBarChart(chart, indicators, values)
    } else if (chartType.value === 'line') {
      renderLineChart(chart, indicators, values)
    }
  } catch { /* handled */ }
}

function renderRadarChart(chart, indicators, values) {
  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${params.name}<br/>${indicators.map((ind, i) =>
          `${ind}: ${values[i].toFixed(3)}`
        ).join('<br/>')}`
      }
    },
    legend: {
      data: ['达成度'],
      bottom: 10
    },
    radar: {
      indicator: indicators.map(k => ({ name: k, max: 1 })),
      radius: '65%',
      axisName: {
        color: '#333',
        fontSize: 13
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(64, 158, 255, 0.05)', 'rgba(64, 158, 255, 0.1)']
        }
      }
    },
    series: [{
      type: 'radar',
      name: '达成度',
      data: [{
        value: values,
        name: '达成度',
        itemStyle: { color: '#409EFF' },
        areaStyle: {
          color: 'rgba(64, 158, 255, 0.3)'
        },
        lineStyle: {
          color: '#409EFF',
          width: 2
        }
      }]
    }]
  }, true)
}

function renderBarChart(chart, indicators, values) {
  // 排序数据
  const sortedData = indicators.map((ind, i) => ({ name: ind, value: values[i] }))
  sortedData.sort((a, b) => sortAsc.value ? a.value - b.value : b.value - a.value)

  const sortedIndicators = sortedData.map(d => d.name)
  const sortedValues = sortedData.map(d => d.value)

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const val = params[0].value
        return `${params[0].name}<br/>达成度: ${val.toFixed(3)}<br/>状态: ${getStatusText(val)}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: sortedIndicators,
      axisLabel: {
        rotate: 45,
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      max: 1,
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: [{
      type: 'bar',
      data: sortedValues.map((val, i) => ({
        value: val,
        itemStyle: {
          color: getBarColor(val)
        }
      })),
      barWidth: '60%',
      label: {
        show: true,
        position: 'top',
        formatter: (params) => params.value.toFixed(3),
        fontSize: 11
      }
    }]
  }, true)
}

function renderLineChart(chart, indicators, values) {
  // 排序数据
  const sortedData = indicators.map((ind, i) => ({ name: ind, value: values[i] }))
  sortedData.sort((a, b) => sortAsc.value ? a.value - b.value : b.value - a.value)

  const sortedIndicators = sortedData.map(d => d.name)
  const sortedValues = sortedData.map(d => d.value)

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const val = params[0].value
        return `${params[0].name}<br/>达成度: ${val.toFixed(3)}<br/>状态: ${getStatusText(val)}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: sortedIndicators,
      axisLabel: {
        rotate: 45,
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      max: 1
    },
    series: [{
      type: 'line',
      data: sortedValues,
      smooth: true,
      lineStyle: {
        color: '#409EFF',
        width: 3
      },
      itemStyle: {
        color: '#409EFF'
      },
      areaStyle: {
        color: 'rgba(64, 158, 255, 0.2)'
      },
      markLine: {
        data: [
          { yAxis: 0.7, name: '达标线', lineStyle: { color: '#E6A23C', type: 'dashed' } },
          { yAxis: 0.65, name: '预警线', lineStyle: { color: '#F56C6C', type: 'dashed' } }
        ],
        label: {
          formatter: '{b}: {c}'
        }
      }
    }]
  }, true)
}

function toggleSort() {
  sortAsc.value = !sortAsc.value
  renderCurrentChart()
}

function getAchievementType(value) {
  if (value >= 0.7) return 'success'
  if (value >= 0.65) return 'warning'
  return 'danger'
}

function getStatusColor(value) {
  if (value >= 0.7) return '#67C23A'
  if (value >= 0.65) return '#E6A23C'
  return '#F56C6C'
}

function getStatusText(value) {
  if (value >= 0.7) return '达标'
  if (value >= 0.65) return '预警'
  return '不达标'
}

function getBarColor(value) {
  if (value >= 0.7) return '#67C23A'
  if (value >= 0.65) return '#E6A23C'
  return '#F56C6C'
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
