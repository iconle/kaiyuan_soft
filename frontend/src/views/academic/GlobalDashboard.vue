<template>
  <div class="page-container">
    <div class="dashboard-toolbar">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
      <div class="toolbar-actions">
        <el-select v-model="selectedMajorId" placeholder="选择专业" @change="loadData" class="major-select">
          <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
        </el-select>
        <el-select v-model="selectedGrade" placeholder="目标年级" clearable @change="loadData" style="width:150px">
          <el-option v-for="g in gradeOptions" :key="g" :label="`${g} 级`" :value="g" />
        </el-select>
      </div>
    </div>

    <el-alert v-if="dashboard.allReady" type="success" show-icon :closable="false" class="status-alert">
      所有课程已完成课程级计算，可以执行专业级计算。
    </el-alert>
    <el-alert v-else-if="dashboard.totalCount > 0" type="warning" show-icon :closable="false" class="status-alert">
      还有 <strong>{{ dashboard.totalCount - dashboard.lockedCount }}</strong> 门课程未完成课程级计算
    </el-alert>

    <section class="metric-grid">
      <div class="metric-card">
        <span>课程总数</span>
        <strong>{{ filteredCourseStatuses.length }}</strong>
        <small>当前专业课程</small>
      </div>
      <div class="metric-card is-success">
        <span>已锁定</span>
        <strong>{{ filteredLockedCount }}</strong>
        <small>可参与专业级计算</small>
      </div>
      <div class="metric-card is-warning">
        <span>未完成</span>
        <strong>{{ unfinishedCount }}</strong>
        <small>仍需导入或锁定</small>
      </div>
      <div class="metric-card is-primary">
        <span>完成率</span>
        <strong>{{ completionRate }}%</strong>
        <small>{{ filteredLockedCount }} / {{ filteredCourseStatuses.length }}</small>
      </div>
    </section>

    <section class="chart-board" v-loading="loading">
      <div v-if="!hasChartData && !loading" class="empty-chart">暂无图表数据</div>
      <template v-else>
        <div class="chart-card trend-card">
          <div class="chart-card-header">
            <div>
              <h4>锁定趋势</h4>
              <p>按课程顺序展示累计锁定进度</p>
            </div>
            <span>{{ trendTotal }} 门</span>
          </div>
          <div ref="lineChartRef" class="chart chart-lg"></div>
        </div>

        <div class="chart-card status-card">
          <div class="chart-card-header">
            <div>
              <h4>状态占比</h4>
              <p>当前专业课程状态分布</p>
            </div>
          </div>

          <div class="pie-panel">
            <div class="pie-wrap">
              <div ref="pieChartRef" class="chart chart-pie"></div>
              <div class="completion-center">
                <strong>{{ completionRate }}%</strong>
                <span>完成率</span>
              </div>
            </div>
          </div>
        </div>

        <div class="chart-card compare-card">
          <div class="chart-card-header">
            <div>
              <h4>课程状态对比</h4>
              <p>按课程聚合已锁定与未完成数量</p>
            </div>
          </div>
          <div ref="barChartRef" class="chart chart-md"></div>
        </div>
      </template>
    </section>

    <el-card class="table-card" header="课程计算状态总览">
      <div v-if="filteredCourseStatuses.length === 0 && !loading" class="empty-table">
        暂无该专业课程数据
      </div>
      <template v-else>
        <div class="table-summary">
          已锁定 <strong>{{ filteredLockedCount }}</strong> / {{ filteredCourseStatuses.length }}
        </div>
        <el-table :data="filteredCourseStatuses" border stripe v-loading="loading" max-height="400"
          style="width:100%" highlight-current-row @row-click="showDetail">
          <el-table-column prop="courseName" label="课程名称" min-width="160" />
          <el-table-column prop="className" label="教学班级" min-width="180" />
          <el-table-column prop="teacherName" label="主讲教师" width="110" />
          <el-table-column prop="semesterName" label="开课学期" min-width="180" />
          <el-table-column prop="lockedAt" label="锁定时间" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.lockedAt) }}
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

    <el-dialog v-model="detailVisible" :title="`达成度详情 - ${detailClass?.courseName || ''} ${detailClass?.className || ''}`" width="min(720px, 96vw)">
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
import { getDashboard as getAcademicDashboard } from '../../api/academic'
import { listMajors } from '../../api/admin'
import { getCourseComputeResults } from '../../api/teacher'
import StatusTag from '../../components/StatusTag.vue'

const props = defineProps({
  title: {
    type: String,
    default: '宏观看板'
  },
  description: {
    type: String,
    default: '查看当前专业课程计算进度与达成度准备情况'
  },
  loadDashboard: {
    type: Function,
    default: getAcademicDashboard
  }
})

const statusLabels = {
  LOCKED: '已锁定',
  IMPORTED: '已导入',
  EMPTY: '未导入',
  DRAFT: '草稿'
}

const chartColors = {
  locked: '#67C23A',
  unfinished: '#E6A23C',
  empty: '#909399',
  primary: '#8f7cc3',
  line: '#409EFF'
}

const majors = ref([])
const selectedMajorId = ref(null)
const selectedGrade = ref(null)
const gradeOptions = [2020, 2021, 2022, 2023, 2024, 2025, 2026, 2027, 2028, 2029]
const loading = ref(false)
const lineChartRef = ref(null)
const barChartRef = ref(null)
const pieChartRef = ref(null)
const chartInstances = []

const dashboard = reactive({
  allReady: false, lockedCount: 0, totalCount: 0,
  incompleteClassIds: [], courseStatuses: []
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailClass = ref(null)
const detailObjData = ref([])
const detailIndData = ref([])
const detailHasData = computed(() => detailObjData.value.length > 0 || detailIndData.value.length > 0)

const filteredCourseStatuses = computed(() => {
  return dashboard.courseStatuses || []
})

const filteredLockedCount = computed(() =>
  filteredCourseStatuses.value.filter(row => row.status === 'LOCKED').length
)

const unfinishedCount = computed(() =>
  Math.max(filteredCourseStatuses.value.length - filteredLockedCount.value, 0)
)

const completionRate = computed(() => {
  if (filteredCourseStatuses.value.length === 0) return 0
  return Math.round((filteredLockedCount.value / filteredCourseStatuses.value.length) * 100)
})

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
  let cumulative = 0
  return filteredCourseStatuses.value.map((row, index) => {
    if (row.status === 'LOCKED') cumulative += 1
    return {
      label: row.courseName || `课程${index + 1}`,
      cumulative
    }
  })
})

const trendTotal = computed(() =>
  trendData.value.length ? trendData.value[trendData.value.length - 1].cumulative : 0
)

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
    const res = await listMajors({ page: 1, size: 100 })
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
    const res = await props.loadDashboard(selectedMajorId.value, selectedGrade.value)
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
  renderPieChart()
  renderBarChart()
}

function renderLineChart() {
  const chart = getChart(lineChartRef.value)
  if (!chart) return
  const labels = trendData.value.map(item => item.label)
  const values = trendData.value.map(item => item.cumulative)
  chart.setOption({
    color: [chartColors.line],
    tooltip: {
      trigger: 'axis',
      formatter: params => {
        const item = params[0]
        return `${item.axisValue}<br/>累计锁定：${item.data} 门`
      }
    },
    grid: {
      left: 42,
      right: 56,
      top: 28,
      bottom: 44,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels.length ? labels : ['暂无'],
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: {
        color: '#606266',
        fontSize: 12,
        margin: 10,
        interval: 0
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eef1f6' } },
      axisLabel: { color: '#606266' }
    },
    series: [{
      name: '累计锁定',
      type: 'line',
      smooth: true,
      symbolSize: 7,
      data: values.length ? values : [0],
      lineStyle: { width: 3 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(64, 158, 255, 0.22)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.02)' }
          ]
        }
      }
    }]
  })
}

function renderBarChart() {
  const chart = getChart(barChartRef.value)
  if (!chart) return
  const labels = courseCompareData.value.map(item => item.courseName)
  chart.setOption({
    color: [chartColors.locked, chartColors.unfinished],
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: '#606266' } },
    grid: { left: 42, right: 24, top: 44, bottom: labels.length > 4 ? 72 : 44 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { interval: 0, rotate: labels.length > 4 ? 24 : 0, color: '#606266' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eef1f6' } },
      axisLabel: { color: '#606266' }
    },
    series: [
      {
        name: '已锁定',
        type: 'bar',
        stack: 'status',
        barMaxWidth: 42,
        data: courseCompareData.value.map(item => item.locked)
      },
      {
        name: '未完成',
        type: 'bar',
        stack: 'status',
        barMaxWidth: 42,
        data: courseCompareData.value.map(item => item.unfinished)
      }
    ]
  })
}

function renderPieChart() {
  const chart = getChart(pieChartRef.value)
  if (!chart) return
  chart.setOption({
    color: [chartColors.locked, chartColors.unfinished, chartColors.empty, chartColors.primary],
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 门 ({d}%)' },
    legend: {
      bottom: 0,
      left: 'center',
      itemWidth: 18,
      itemHeight: 10,
      icon: 'roundRect',
      itemGap: 16,
      textStyle: {
        color: '#606266',
        fontSize: 13
      }
    },
    series: [{
      name: '状态占比',
      type: 'pie',
      radius: ['48%', '68%'],
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 3
      },
      label: {
        show: true,
        color: '#606266',
        fontSize: 12,
        formatter: '{b}'
      },
      labelLine: {
        show: true,
        length: 10,
        length2: 8,
        lineStyle: {
          width: 1.5
        }
      },
      emphasis: {
        scale: true,
        scaleSize: 6,
        itemStyle: {
          shadowBlur: 14,
          shadowColor: 'rgba(0, 0, 0, 0.12)'
        }
      },
      data: statusSummary.value
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
.page-container {
  padding: var(--space-5);
  background: #f7f8fb;
}

.dashboard-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
}

.dashboard-toolbar h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 700;
}

.dashboard-toolbar p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.major-select {
  width: 260px;
}

.status-alert {
  margin-bottom: 16px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.metric-card {
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.04);
}

.metric-card span,
.metric-card small {
  display: block;
  color: var(--text-secondary);
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin: 8px 0 4px;
  color: var(--text-primary);
  font-size: 30px;
  line-height: 1.1;
}

.metric-card.is-success strong { color: #67C23A; }
.metric-card.is-warning strong { color: #E6A23C; }
.metric-card.is-primary strong { color: #8f7cc3; }

.chart-board {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.chart-card {
  min-width: 0;
  padding: 18px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.04);
}

.trend-card {
  min-height: 390px;
}

.compare-card {
  grid-column: 1 / -1;
}

.chart-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.chart-card-header h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
}

.chart-card-header p {
  margin: 5px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
}

.chart-card-header span {
  color: #409EFF;
  font-size: 22px;
  font-weight: 700;
}

.chart {
  width: 100%;
}

.chart-lg {
  height: 320px;
}

.chart-md {
  height: 300px;
}

/* 状态占比卡片美化 */
.status-card {
  overflow: hidden;
}

.pie-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 8px;
}

.pie-wrap {
  position: relative;
  width: 100%;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-pie {
  width: 270px;
  height: 270px;
}

.completion-center {
  position: absolute;
  top: 46%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 104px;
  height: 104px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: inset 0 0 0 1px #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.completion-center strong {
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
  color: #303133;
}

.completion-center span {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.empty-chart,
.empty-table {
  text-align: center;
  padding: 56px 0;
  color: var(--text-secondary);
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.table-card {
  border-radius: 8px;
}

.table-summary {
  margin-bottom: 12px;
  color: var(--text-regular);
}

@media (max-width: 1180px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }

  .chart-board {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-toolbar {
    display: block;
  }

  .toolbar-actions {
    justify-content: flex-start;
    margin-top: 12px;
  }

  .major-select {
    width: 100%;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}
.pie-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 8px;
}
</style>
