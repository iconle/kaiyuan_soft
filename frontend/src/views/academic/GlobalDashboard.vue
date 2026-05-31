<template>
  <div class="page-container">
    <div class="page-header">
      <h3>宏观看板</h3>
      <el-select v-model="selectedMajorId" placeholder="选择专业" @change="loadData" style="width:240px">
        <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
      </el-select>
      <el-segmented v-model="selectedRange" :options="rangeOptions" @change="loadData" />
    </div>

    <el-alert v-if="dashboard.allReady" type="success" show-icon :closable="false" style="margin-bottom:16px">
      所有课程已完成课程级计算，可以执行专业级计算。
    </el-alert>
    <el-alert v-else-if="dashboard.totalCount > 0" type="warning" show-icon :closable="false" style="margin-bottom:16px">
      还有 <strong>{{ dashboard.totalCount - dashboard.lockedCount }}</strong> 门课程未完成课程级计算
    </el-alert>

    <el-card header="报表图表总览" style="margin-bottom:16px">
      <div v-if="!hasChartData && !loading" class="empty-chart">暂无图表数据</div>
      <div v-else class="chart-grid" v-loading="loading">
        <div class="chart-panel">
          <div class="chart-title">锁定趋势</div>
          <div ref="lineChartRef" class="chart"></div>
        </div>
        <div class="chart-panel">
          <div class="chart-title">课程状态对比</div>
          <div ref="barChartRef" class="chart"></div>
        </div>
        <div class="chart-panel">
          <div class="chart-title">状态占比</div>
          <div ref="pieChartRef" class="chart"></div>
        </div>
      </div>
    </el-card>

    <el-card header="课程计算状态总览">
      <div v-if="filteredCourseStatuses.length === 0 && !loading" style="text-align:center;padding:40px;color:var(--text-secondary)">
        暂无该时间范围内的课程数据
      </div>
      <template v-else>
      <div style="margin-bottom:12px; color:var(--text-regular)">
        已锁定: <strong>{{ filteredLockedCount }}</strong> / {{ filteredCourseStatuses.length }}
      </div>
      <el-table :data="filteredCourseStatuses" border stripe v-loading="loading" max-height="400"
        style="width:100%" highlight-current-row @row-click="showDetail">
        <el-table-column prop="courseName" label="课程名称" min-width="160" />
        <el-table-column prop="className" label="教学班级" min-width="180" />
        <el-table-column prop="teacherName" label="主讲教师" width="110" />
        <el-table-column prop="semesterName" label="开课学期" min-width="180" />
        <el-table-column prop="lockedAt" label="锁定时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.lockedAt) || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
      </el-table>
      </template>
    </el-card>

    <!-- 达成度详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="`达成度详情 - ${detailClass?.courseName || ''} ${detailClass?.className || ''}`" width="720px">
      <div v-if="detailLoading" style="text-align:center;padding:20px">加载中...</div>
      <div v-else-if="!detailHasData" style="text-align:center;padding:20px;color:var(--text-secondary)">该课程尚未执行课程级计算，无达成度数据</div>
      <div v-else style="display:flex;gap:20px;flex-wrap:wrap">
        <el-card header="课程目标达成度（第一级）" style="flex:1;min-width:300px">
          <el-table :data="detailObjData" border stripe size="small">
            <el-table-column prop="label" label="目标编号" width="100" />
            <el-table-column prop="value" label="达成度" />
          </el-table>
        </el-card>
        <el-card header="课程级指标点达成度（第二级）" style="flex:1;min-width:300px">
          <el-table :data="detailIndData" border stripe size="small">
            <el-table-column prop="label" label="指标点编号" width="120" />
            <el-table-column prop="value" label="达成度" />
          </el-table>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { getDashboard as apiGetDashboard } from '../../api/director'
import { listMajors } from '../../api/admin'
import { getCourseComputeResults } from '../../api/teacher'
import StatusTag from '../../components/StatusTag.vue'

const rangeOptions = [
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '本学期', value: 'semester' }
]

const statusLabels = {
  LOCKED: '已锁定',
  IMPORTED: '已导入',
  EMPTY: '未导入',
  DRAFT: '草稿'
}

const majors = ref([])
const selectedMajorId = ref(null)
const selectedRange = ref('semester')
const loading = ref(false)
const lineChartRef = ref(null)
const barChartRef = ref(null)
const pieChartRef = ref(null)
const chartInstances = []

const dashboard = reactive({
  allReady: false, lockedCount: 0, totalCount: 0,
  incompleteClassIds: [], courseStatuses: []
})

// Detail dialog
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailClass = ref(null)
const detailObjData = ref([])
const detailIndData = ref([])
const detailHasData = computed(() => detailObjData.value.length > 0 || detailIndData.value.length > 0)

const filteredCourseStatuses = computed(() => {
  const rows = dashboard.courseStatuses || []
  if (selectedRange.value === 'semester') return rows
  return rows.filter(row => isInSelectedRange(row.lockedAt))
})

const filteredLockedCount = computed(() =>
  filteredCourseStatuses.value.filter(row => row.status === 'LOCKED').length
)

const hasChartData = computed(() => filteredCourseStatuses.value.length > 0)

const statusSummary = computed(() => {
  const summary = new Map()
  for (const row of filteredCourseStatuses.value) {
    const label = statusLabels[row.status] || row.status || '未知'
    summary.set(label, (summary.get(label) || 0) + 1)
  }
  return Array.from(summary, ([name, value]) => ({ name, value }))
})

const courseCompareData = computed(() => {
  const summary = new Map()
  for (const row of filteredCourseStatuses.value) {
    if (!summary.has(row.courseName)) {
      summary.set(row.courseName, { courseName: row.courseName, locked: 0, unfinished: 0 })
    }
    const item = summary.get(row.courseName)
    if (row.status === 'LOCKED') item.locked += 1
    else item.unfinished += 1
  }
  return Array.from(summary.values())
})

const trendData = computed(() => {
  const summary = new Map()
  for (const row of filteredCourseStatuses.value) {
    if (row.status !== 'LOCKED' || !row.lockedAt) continue
    const label = formatDate(row.lockedAt)
    summary.set(label, (summary.get(label) || 0) + 1)
  }
  return Array.from(summary, ([date, count]) => ({ date, count }))
    .sort((a, b) => a.date.localeCompare(b.date))
})

async function showDetail(row) {
  detailClass.value = row
  detailVisible.value = true
  detailLoading.value = true
  detailObjData.value = []
  detailIndData.value = []
  try {
    const res = await getCourseComputeResults(row.classId)
    const data = res.data || {}
    const objLabels = data.objectiveLabels || {}
    const indLabels = data.indicatorLabels || {}
    detailObjData.value = Object.entries(data.objectiveAchievements || {}).map(([id, val]) => ({
      label: objLabels[id] || `目标${id}`, value: val
    }))
    detailIndData.value = Object.entries(data.courseAchievements || {}).map(([id, val]) => ({
      label: indLabels[id] || `指标点${id}`, value: val
    }))
  } catch { /* no data */ }
  finally { detailLoading.value = false }
}

onMounted(async () => {
  window.addEventListener('resize', resizeCharts)
  try {
    const res = await listMajors()
    majors.value = res.data?.records || []
    if (majors.value.length > 0) {
      selectedMajorId.value = majors.value[0].id
      loadData()
    }
  } catch { /* handled */ }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})

watch(filteredCourseStatuses, () => {
  nextTick(renderCharts)
})

async function loadData() {
  if (!selectedMajorId.value) return
  loading.value = true
  try {
    const res = await apiGetDashboard(selectedMajorId.value)
    Object.assign(dashboard, res.data || {})
    await nextTick()
    renderCharts()
  } catch { /* handled */ }
  finally { loading.value = false }
}

function renderCharts() {
  if (!hasChartData.value) {
    disposeCharts()
    return
  }
  renderLineChart()
  renderBarChart()
  renderPieChart()
}

function renderLineChart() {
  const chart = getChart(lineChartRef.value)
  if (!chart) return
  const labels = trendData.value.map(item => item.date)
  const values = trendData.value.map(item => item.count)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 28, bottom: 32 },
    xAxis: { type: 'category', data: labels.length ? labels : ['暂无'] },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: '锁定课程数',
      type: 'line',
      smooth: true,
      data: values.length ? values : [0],
      areaStyle: { opacity: 0.12 }
    }]
  })
}

function renderBarChart() {
  const chart = getChart(barChartRef.value)
  if (!chart) return
  const labels = courseCompareData.value.map(item => item.courseName)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 36, right: 16, top: 44, bottom: 48 },
    xAxis: { type: 'category', data: labels, axisLabel: { interval: 0, rotate: labels.length > 4 ? 25 : 0 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '已锁定', type: 'bar', stack: 'status', data: courseCompareData.value.map(item => item.locked) },
      { name: '未完成', type: 'bar', stack: 'status', data: courseCompareData.value.map(item => item.unfinished) }
    ]
  })
}

function renderPieChart() {
  const chart = getChart(pieChartRef.value)
  if (!chart) return
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      name: '课程状态',
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      data: statusSummary.value,
      label: { formatter: '{b}: {d}%' }
    }]
  })
}

function getChart(el) {
  if (!el) return null
  let chart = echarts.getInstanceByDom(el)
  if (!chart) {
    chart = echarts.init(el)
    chartInstances.push(chart)
  }
  return chart
}

function resizeCharts() {
  chartInstances.forEach(chart => chart.resize())
}

function disposeCharts() {
  while (chartInstances.length) {
    chartInstances.pop().dispose()
  }
}

function isInSelectedRange(value) {
  if (!value) return false
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return false
  const now = new Date()
  if (selectedRange.value === 'week') {
    const start = startOfWeek(now)
    const end = new Date(start)
    end.setDate(start.getDate() + 7)
    return date >= start && date < end
  }
  if (selectedRange.value === 'month') {
    return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth()
  }
  return true
}

function startOfWeek(date) {
  const start = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const day = start.getDay() || 7
  start.setDate(start.getDate() - day + 1)
  start.setHours(0, 0, 0, 0)
  return start
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${formatDate(value)} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function pad(value) {
  return String(value).padStart(2, '0')
}
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header { display: flex; align-items: center; gap: var(--space-4); margin-bottom: var(--space-4); flex-wrap: wrap; }
.page-header h3 { margin: 0; font-size: var(--text-lg); }
.chart-grid { display: grid; grid-template-columns: repeat(3, minmax(260px, 1fr)); gap: 16px; }
.chart-panel { min-width: 0; }
.chart-title { margin-bottom: 8px; color: var(--text-regular); font-weight: 600; }
.chart { width: 100%; height: 300px; }
.empty-chart { text-align: center; padding: 56px 0; color: var(--text-secondary); }

@media (max-width: 1100px) {
  .chart-grid { grid-template-columns: 1fr; }
}
</style>
