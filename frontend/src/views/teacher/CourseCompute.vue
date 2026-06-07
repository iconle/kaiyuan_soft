<template>
  <div class="page-container">
    <div class="page-header">
      <h3>课程级计算</h3>
      <el-button type="primary" @click="handleCompute" :disabled="computing || status !== 'IMPORTED'"
        :loading="computing">
        一键计算
      </el-button>
      <StatusTag v-if="status" :status="status" />
    </div>

    <el-alert v-if="status === 'LOCKED'" type="success" show-icon :closable="false" style="margin-bottom:16px">
      课程级计算已完成，成绩单已锁定。计算时间：{{ results.calcTime || '-' }}
      <div style="margin-top:8px;font-size:13px">
        如需勘误成绩，请点击下方按钮提交勘误申请，由教务管理员或系统管理员审批解锁。
        <el-button v-if="!hasPendingRequest" size="small" type="warning" style="margin-left:8px"
          @click="showRequestDialog"> 提交勘误申请</el-button>
      </div>
    </el-alert>
    <el-alert v-else-if="status === 'IMPORTED'" type="warning" show-icon :closable="false" style="margin-bottom:16px">
      成绩已导入，点击「一键计算」执行课程级达成度计算（目标级 + 课程级）。计算完成后成绩单将锁定。
    </el-alert>
    <el-alert v-else type="info" show-icon :closable="false" style="margin-bottom:16px">
      请先在「成绩导入」中导入学生成绩数据。
    </el-alert>

    <!-- My unlock requests status -->
    <el-card v-if="myRequests.length > 0" header="我的勘误申请" style="margin-bottom:16px">
      <el-table :data="myRequests" border stripe size="small">
        <el-table-column prop="id" label="工单ID" width="65" />
        <el-table-column prop="reason" label="勘误原因" min-width="140" />
        <el-table-column prop="createdAt" label="提交时间" width="155" />
        <el-table-column label="状态" width="170">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger"
              @click="handleCancelRequest(row)">撤销</el-button>
            <span v-else style="color:var(--text-secondary)">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="requestDialogVisible" title="提交勘误申请" width="500px">
      <el-form label-width="80px">
        <el-form-item label="勘误原因">
          <el-input v-model="unlockReason" type="textarea" :rows="3"
            placeholder="请说明需要修改哪些成绩项以及原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="requestDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRequestUnlock" :loading="requesting">提交申请</el-button>
      </template>
    </el-dialog>

    <div v-if="hasResults" style="display:flex; gap: 20px; flex-wrap: wrap;">
      <!-- Objective achievements -->
      <el-card header="课程目标达成度（第一级）" style="flex:1; min-width: 360px;">
        <el-table :data="objectiveData" border stripe size="small">
          <el-table-column prop="objectiveNo" label="目标编号" width="100" />
          <el-table-column prop="achievement" label="达成度" />
        </el-table>
      </el-card>

      <!-- Course indicator achievements -->
      <el-card header="课程级指标点达成度（第二级）" style="flex:1; min-width: 360px;">
        <el-table :data="indicatorData" border stripe size="small">
          <el-table-column prop="indicatorId" label="指标点编号" width="110" />
          <el-table-column prop="achievement" label="达成度" />
        </el-table>
      </el-card>
    </div>

    <!-- Charts Section -->
    <template v-if="hasResults">
      <!-- Objective Chart -->
      <el-card header="课程目标达成度可视化分析（第一级）" style="margin-top:16px">
        <div style="margin-bottom:16px; display:flex; gap:12px; align-items:center">
          <span>图表类型：</span>
          <el-radio-group v-model="objectiveChartType" @change="renderObjectiveChart">
            <el-radio-button value="radar">雷达图</el-radio-button>
            <el-radio-button value="bar">柱状图</el-radio-button>
            <el-radio-button value="line">趋势图</el-radio-button>
          </el-radio-group>
          <el-button v-if="objectiveChartType !== 'radar'" @click="toggleSort" size="small" style="margin-left:auto">
            {{ sortAsc ? '降序排列' : '升序排列' }}
          </el-button>
        </div>
        <div ref="objectiveChartRef" style="width:100%;height:400px"></div>
        <div style="margin-top:16px; padding:12px; background:#f5f7fa; border-radius:4px">
          <div style="display:flex; justify-content:space-around; flex-wrap:wrap; gap:16px">
            <div style="text-align:center">
              <div style="font-size:24px; font-weight:bold; color:#67C23A">{{ objectiveStats.avg.toFixed(3) }}</div>
              <div style="color:#909399; font-size:12px">平均达成度</div>
            </div>
            <div style="text-align:center">
              <div style="font-size:24px; font-weight:bold; color:#409EFF">{{ objectiveStats.max.toFixed(3) }}</div>
              <div style="color:#909399; font-size:12px">最高达成度</div>
            </div>
            <div style="text-align:center">
              <div style="font-size:24px; font-weight:bold; color:#E6A23C">{{ objectiveStats.min.toFixed(3) }}</div>
              <div style="color:#909399; font-size:12px">最低达成度</div>
            </div>
            <div style="text-align:center">
              <div style="font-size:24px; font-weight:bold" :style="{ color: objectiveStats.weakCount > 0 ? '#F56C6C' : '#67C23A' }">
                {{ objectiveStats.weakCount }}
              </div>
              <div style="color:#909399; font-size:12px">低于0.7的目标数</div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- Indicator Chart -->
      <el-card header="课程级指标点达成度可视化分析（第二级）" style="margin-top:16px">
        <div style="margin-bottom:16px; display:flex; gap:12px; align-items:center">
          <span>图表类型：</span>
          <el-radio-group v-model="indicatorChartType" @change="renderIndicatorChart">
            <el-radio-button value="radar">雷达图</el-radio-button>
            <el-radio-button value="bar">柱状图</el-radio-button>
            <el-radio-button value="line">趋势图</el-radio-button>
          </el-radio-group>
          <el-button v-if="indicatorChartType !== 'radar'" @click="toggleSort" size="small" style="margin-left:auto">
            {{ sortAsc ? '降序排列' : '升序排列' }}
          </el-button>
        </div>
        <div ref="indicatorChartRef" style="width:100%;height:400px"></div>
        <div style="margin-top:16px; padding:12px; background:#f5f7fa; border-radius:4px">
          <div style="display:flex; justify-content:space-around; flex-wrap:wrap; gap:16px">
            <div style="text-align:center">
              <div style="font-size:24px; font-weight:bold; color:#67C23A">{{ indicatorStats.avg.toFixed(3) }}</div>
              <div style="color:#909399; font-size:12px">平均达成度</div>
            </div>
            <div style="text-align:center">
              <div style="font-size:24px; font-weight:bold; color:#409EFF">{{ indicatorStats.max.toFixed(3) }}</div>
              <div style="color:#909399; font-size:12px">最高达成度</div>
            </div>
            <div style="text-align:center">
              <div style="font-size:24px; font-weight:bold; color:#E6A23C">{{ indicatorStats.min.toFixed(3) }}</div>
              <div style="color:#909399; font-size:12px">最低达成度</div>
            </div>
            <div style="text-align:center">
              <div style="font-size:24px; font-weight:bold" :style="{ color: indicatorStats.weakCount > 0 ? '#F56C6C' : '#67C23A' }">
                {{ indicatorStats.weakCount }}
              </div>
              <div style="color:#909399; font-size:12px">低于0.7的指标点</div>
            </div>
          </div>
        </div>
      </el-card>
    </template>

    <div v-if="hasResults" style="margin-top: 16px; display: flex; gap: 8px;">
      <el-button @click="downloadPdf">导出 PDF 报告</el-button>
      <el-button @click="downloadExcel">导出 Excel 报告</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import {
  getScoreStatus, triggerCourseCompute, getCourseComputeResults,
  downloadCoursePdf, downloadCourseExcel
} from '../../api/teacher'
import { useUserStore } from '../../stores/user'
import StatusTag from '../../components/StatusTag.vue'
import * as echarts from 'echarts'

const route = useRoute()
const userStore = useUserStore()
const classId = ref(route.params.classId)
const status = ref('')
const computing = ref(false)
const requesting = ref(false)
const requestDialogVisible = ref(false)
const unlockReason = ref('')
const myRequests = ref([])
const results = reactive({ objectiveAchievements: {}, courseAchievements: {}, calcTime: null, objectiveLabels: {}, indicatorLabels: {} })

// Chart related refs
const objectiveChartRef = ref(null)
const indicatorChartRef = ref(null)
const objectiveChartType = ref('radar')
const indicatorChartType = ref('radar')
const objectiveChartInstance = ref(null)
const indicatorChartInstance = ref(null)
const sortAsc = ref(false)

const hasPendingRequest = computed(() => myRequests.value.some(r => r.status === 'PENDING'))

onMounted(() => { if (classId.value) loadData() })

async function loadData() {
  try {
    const sRes = await getScoreStatus(classId.value)
    status.value = sRes.data?.status || ''
    if (status.value === 'LOCKED') {
      const rRes = await getCourseComputeResults(classId.value)
      Object.assign(results, rRes.data || {})
      await nextTick()
      renderCharts()
    }
    loadMyRequests()
  } catch { /* handled */ }
}

async function loadMyRequests() {
  try {
    const res = await request.get(`/api/classes/${classId.value}/my-unlock-requests`)
    myRequests.value = res.data || []
  } catch { myRequests.value = [] }
}

function statusTagType(s) {
  return s === 'UNLOCKED' ? 'success' : s === 'PENDING' ? 'warning' : s === 'APPROVED' ? '' : 'danger'
}

function statusLabel(s) {
  return s === 'PENDING' ? '待教务审核' : s === 'APPROVED' ? '已同意，等待管理员解锁' : s === 'UNLOCKED' ? '已解锁（成绩可修改）' : '已拒绝'
}

async function handleCancelRequest(row) {
  await ElMessageBox.confirm('确定撤销该勘误申请？', '确认撤销', { type: 'warning' })
  try {
    await request.post(`/api/classes/${classId.value}/cancel-unlock-request/${row.id}`)
    ElMessage.success('申请已撤销')
    loadMyRequests()
  } catch { /* handled */ }
}

const hasResults = computed(() =>
  Object.keys(results.objectiveAchievements || {}).length > 0 ||
  Object.keys(results.courseAchievements || {}).length > 0
)

const objectiveData = computed(() => {
  const labels = results.objectiveLabels || {}
  return Object.entries(results.objectiveAchievements || {}).map(([id, val]) => ({
    objectiveNo: labels[id] || `目标${id}`, achievement: val
  }))
})

const indicatorData = computed(() => {
  const labels = results.indicatorLabels || {}
  return Object.entries(results.courseAchievements || {}).map(([id, val]) => ({
    indicatorId: labels[id] || `指标点${id}`, achievement: val
  }))
})

function showRequestDialog() {
  unlockReason.value = ''
  requestDialogVisible.value = true
}

async function handleRequestUnlock() {
  if (!unlockReason.value.trim()) {
    ElMessage.warning('请填写勘误原因')
    return
  }
  requesting.value = true
  try {
    await request.post(`/api/classes/${classId.value}/request-unlock`, {
      reason: unlockReason.value
    })
    ElMessage.success('勘误申请已提交，请等待教务管理员或系统管理员审批')
    requestDialogVisible.value = false
    loadMyRequests()
  } catch { /* handled */ }
  finally { requesting.value = false }
}

async function handleCompute() {
  computing.value = true
  try {
    const userId = userStore.userId || 1
    await triggerCourseCompute(classId.value, userId)
    ElMessage.success('课程级计算完成，成绩单已锁定')
    loadData()
  } catch { /* handled */ }
  finally { computing.value = false }
}

async function downloadPdf() {
  const blob = await downloadCoursePdf(classId.value)
  downloadBlob(blob, `课程达成度报告_${classId.value}.pdf`)
}

async function downloadExcel() {
  const blob = await downloadCourseExcel(classId.value)
  downloadBlob(blob, `课程达成度报告_${classId.value}.xlsx`)
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = filename; a.click()
  URL.revokeObjectURL(url)
}

// Chart rendering functions
async function renderCharts() {
  await nextTick()
  renderObjectiveChart()
  renderIndicatorChart()
}

function renderObjectiveChart() {
  if (!objectiveChartRef.value) return
  if (objectiveChartInstance.value) {
    objectiveChartInstance.value.dispose()
  }

  const labels = results.objectiveLabels || {}
  const data = Object.entries(results.objectiveAchievements || {}).map(([id, val]) => ({
    name: labels[id] || `目标${id}`,
    value: val
  }))

  if (data.length === 0) return

  const chart = echarts.init(objectiveChartRef.value)
  objectiveChartInstance.value = chart

  if (objectiveChartType.value === 'radar') {
    renderObjectiveRadar(chart, data)
  } else if (objectiveChartType.value === 'bar') {
    renderObjectiveBar(chart, data)
  } else if (objectiveChartType.value === 'line') {
    renderObjectiveLine(chart, data)
  }
}

function renderIndicatorChart() {
  if (!indicatorChartRef.value) return
  if (indicatorChartInstance.value) {
    indicatorChartInstance.value.dispose()
  }

  const labels = results.indicatorLabels || {}
  const data = Object.entries(results.courseAchievements || {}).map(([id, val]) => ({
    name: labels[id] || `指标点${id}`,
    value: val
  }))

  if (data.length === 0) return

  const chart = echarts.init(indicatorChartRef.value)
  indicatorChartInstance.value = chart

  if (indicatorChartType.value === 'radar') {
    renderIndicatorRadar(chart, data)
  } else if (indicatorChartType.value === 'bar') {
    renderIndicatorBar(chart, data)
  } else if (indicatorChartType.value === 'line') {
    renderIndicatorLine(chart, data)
  }
}

function renderObjectiveRadar(chart, data) {
  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${params.name}<br/>${data.map(d =>
          `${d.name}: ${d.value.toFixed(3)}`
        ).join('<br/>')}`
      }
    },
    legend: {
      data: ['达成度'],
      bottom: 10
    },
    radar: {
      indicator: data.map(d => ({ name: d.name, max: 1 })),
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
        value: data.map(d => d.value),
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

function renderIndicatorRadar(chart, data) {
  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${params.name}<br/>${data.map(d =>
          `${d.name}: ${d.value.toFixed(3)}`
        ).join('<br/>')}`
      }
    },
    legend: {
      data: ['达成度'],
      bottom: 10
    },
    radar: {
      indicator: data.map(d => ({ name: d.name, max: 1 })),
      radius: '65%',
      axisName: {
        color: '#333',
        fontSize: 13
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(103, 194, 58, 0.05)', 'rgba(103, 194, 58, 0.1)']
        }
      }
    },
    series: [{
      type: 'radar',
      name: '达成度',
      data: [{
        value: data.map(d => d.value),
        name: '达成度',
        itemStyle: { color: '#67C23A' },
        areaStyle: {
          color: 'rgba(103, 194, 58, 0.3)'
        },
        lineStyle: {
          color: '#67C23A',
          width: 2
        }
      }]
    }]
  }, true)
}

function renderObjectiveBar(chart, data) {
  const sortedData = [...data].sort((a, b) => sortAsc.value ? a.value - b.value : b.value - a.value)

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
      data: sortedData.map(d => d.name),
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
      type: 'bar',
      data: sortedData.map(d => ({
        value: d.value,
        itemStyle: {
          color: getBarColor(d.value)
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

function renderIndicatorBar(chart, data) {
  const sortedData = [...data].sort((a, b) => sortAsc.value ? a.value - b.value : b.value - a.value)

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
      data: sortedData.map(d => d.name),
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
      type: 'bar',
      data: sortedData.map(d => ({
        value: d.value,
        itemStyle: {
          color: getBarColor(d.value)
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

function renderObjectiveLine(chart, data) {
  const sortedData = [...data].sort((a, b) => sortAsc.value ? a.value - b.value : b.value - a.value)

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
      data: sortedData.map(d => d.name),
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
      data: sortedData.map(d => d.value),
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

function renderIndicatorLine(chart, data) {
  const sortedData = [...data].sort((a, b) => sortAsc.value ? a.value - b.value : b.value - a.value)

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
      data: sortedData.map(d => d.name),
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
      data: sortedData.map(d => d.value),
      smooth: true,
      lineStyle: {
        color: '#67C23A',
        width: 3
      },
      itemStyle: {
        color: '#67C23A'
      },
      areaStyle: {
        color: 'rgba(103, 194, 58, 0.2)'
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

// Helper functions
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

function toggleSort() {
  sortAsc.value = !sortAsc.value
  renderCharts()
}

// Stats computed properties
const objectiveStats = computed(() => {
  const values = Object.values(results.objectiveAchievements || {}).map(Number)
  if (values.length === 0) return { avg: 0, max: 0, min: 0, weakCount: 0 }
  return {
    avg: values.reduce((a, b) => a + b, 0) / values.length,
    max: Math.max(...values),
    min: Math.min(...values),
    weakCount: values.filter(v => v < 0.7).length
  }
})

const indicatorStats = computed(() => {
  const values = Object.values(results.courseAchievements || {}).map(Number)
  if (values.length === 0) return { avg: 0, max: 0, min: 0, weakCount: 0 }
  return {
    avg: values.reduce((a, b) => a + b, 0) / values.length,
    max: Math.max(...values),
    min: Math.min(...values),
    weakCount: values.filter(v => v < 0.7).length
  }
})
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header { display: flex; align-items: center; gap: var(--space-4); margin-bottom: var(--space-4); }
.page-header h3 { margin: 0; font-size: var(--text-lg); }
</style>
