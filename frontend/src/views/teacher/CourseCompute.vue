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
    <el-card
      v-if="myRequests.length > 0"
      header="我的勘误申请"
      class="my-unlock-card"
    >
      <el-table
        :data="myRequests"
        border
        stripe
        size="small"
        class="my-unlock-table"
      >
        <el-table-column
          prop="id"
          label="工单ID"
          width="70"
          align="center"
        />

        <el-table-column
          prop="reason"
          label="勘误原因"
          min-width="220"
          show-overflow-tooltip
        />

        <el-table-column
          prop="createdAt"
          label="提交时间"
          width="300"
          align="center"
          class-name="unlock-time-column"
        />

        <el-table-column
          label="状态"
          width="210"
          align="center"
          class-name="unlock-status-column"
        >
          <template #default="{ row }">
            <el-tag
              :type="statusTagType(row.status)"
              size="small"
              effect="light"
              class="unlock-status-tag"
            >
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          label="操作"
          width="120"
          align="center"
          class-name="unlock-operation-column"
        >
          <template #default="{ row }">
            <div class="unlock-operation-actions">
              <el-button
                v-if="row.status === 'PENDING'"
                size="small"
                type="danger"
                class="unlock-cancel-btn"
                @click="handleCancelRequest(row)"
              >
                撤销
              </el-button>
              <span v-else class="operation-placeholder">-</span>
            </div>
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
    <div v-if="hasResults" class="achievement-overview">
      <!-- Objective achievements -->
      <el-card class="achievement-panel" shadow="hover">
        <template #header>
          <div class="achievement-panel-header">
            <div>
              <div class="achievement-title">课程目标达成度（第一级）</div>
              <div class="achievement-subtitle">按课程目标展示达成情况</div>
            </div>
            <el-tag type="success" effect="light" class="achievement-count-tag">
              共 {{ objectiveData.length }} 项
            </el-tag>
          </div>
        </template>

        <div class="achievement-summary">
          <div class="summary-item">
            <div class="summary-label">平均值</div>
            <div class="summary-value primary">
              {{ formatAchievement(objectiveStats.avg) }}
            </div>
          </div>
          <div class="summary-item">
            <div class="summary-label">最高值</div>
            <div class="summary-value success">
              {{ formatAchievement(objectiveStats.max) }}
            </div>
          </div>
          <div class="summary-item">
            <div class="summary-label">最低值</div>
            <div class="summary-value warning">
              {{ formatAchievement(objectiveStats.min) }}
            </div>
          </div>
        </div>

        <div class="achievement-list">
          <div
            v-for="item in objectiveData"
            :key="item.objectiveNo"
            class="achievement-item"
          >
            <div class="achievement-item-top">
              <div class="achievement-name">
                {{ item.objectiveNo }}
              </div>
              <el-tooltip :content="item.description" placement="top" :show-after="300" effect="light">
                <span class="achievement-desc">{{ item.description }}</span>
              </el-tooltip>

              <div class="achievement-value">
                {{ formatAchievement(item.achievement) }}
              </div>

              <el-tag
                size="small"
                effect="light"
                :type="achievementTagType(item.achievement)"
                class="achievement-status-tag"
              >
                {{ achievementTagText(item.achievement) }}
              </el-tag>

              <el-button
                size="small"
                type="primary"
                link
                class="personal-detail-btn"
                @click.stop="openPersonalDialog('objective', item)"
              >
                学生明细
              </el-button>
            </div>

            <el-progress
              :percentage="achievementPercent(item.achievement)"
              :stroke-width="10"
              :show-text="false"
              class="achievement-progress"
            />
          </div>
        </div>
      </el-card>

      <!-- Course indicator achievements -->
      <el-card class="achievement-panel" shadow="hover">
        <template #header>
          <div class="achievement-panel-header">
            <div>
              <div class="achievement-title">课程级指标点达成度（第二级）</div>
              <div class="achievement-subtitle">按指标点展示达成情况</div>
            </div>
            <el-tag type="success" effect="light" class="achievement-count-tag">
              共 {{ indicatorData.length }} 项
            </el-tag>
          </div>
        </template>

        <div class="achievement-summary">
          <div class="summary-item">
            <div class="summary-label">平均值</div>
            <div class="summary-value primary">
              {{ formatAchievement(indicatorStats.avg) }}
            </div>
          </div>
          <div class="summary-item">
            <div class="summary-label">最高值</div>
            <div class="summary-value success">
              {{ formatAchievement(indicatorStats.max) }}
            </div>
          </div>
          <div class="summary-item">
            <div class="summary-label">最低值</div>
            <div class="summary-value warning">
              {{ formatAchievement(indicatorStats.min) }}
            </div>
          </div>
        </div>

        <div class="achievement-list">
          <div
            v-for="item in indicatorData"
            :key="item.indicatorId"
            class="achievement-item"
          >
            <div class="achievement-item-top">
              <div class="achievement-name">
                {{ item.indicatorNo }}
              </div>
              <el-tooltip :content="item.description" placement="top" :show-after="300" effect="light">
                <span class="achievement-desc">{{ item.description }}</span>
              </el-tooltip>

              <div class="achievement-value">
                {{ formatAchievement(item.achievement) }}
              </div>

              <el-tag
                size="small"
                effect="light"
                :type="achievementTagType(item.achievement)"
                class="achievement-status-tag"
              >
                {{ achievementTagText(item.achievement) }}
              </el-tag>

              <el-button
                size="small"
                type="primary"
                link
                class="personal-detail-btn"
                @click.stop="openPersonalDialog('indicator', item)"
              >
                学生明细
              </el-button>
            </div>

            <el-progress
              :percentage="achievementPercent(item.achievement)"
              :stroke-width="10"
              :show-text="false"
              class="achievement-progress"
            />
          </div>
        </div>
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
              <div style="font-size:24px; font-weight:bold; color:#67C23A">{{ formatAchievement(objectiveStats.avg) }}</div>
              <div style="color:#909399; font-size:12px">平均达成度</div>
            </div>
            <div style="text-align:center">
              <div style="font-size:24px; font-weight:bold; color:#409EFF">{{ formatAchievement(objectiveStats.max) }}</div>
              <div style="color:#909399; font-size:12px">最高达成度</div>
            </div>
            <div style="text-align:center">
              <div style="font-size:24px; font-weight:bold; color:#E6A23C">{{ formatAchievement(objectiveStats.min) }}</div>
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
              <div style="font-size:24px; font-weight:bold; color:#67C23A">{{ formatAchievement(indicatorStats.avg) }}</div>
              <div style="color:#909399; font-size:12px">平均达成度</div>
            </div>
            <div style="text-align:center">
              <div style="font-size:24px; font-weight:bold; color:#409EFF">{{ formatAchievement(indicatorStats.max) }}</div>
              <div style="color:#909399; font-size:12px">最高达成度</div>
            </div>
            <div style="text-align:center">
              <div style="font-size:24px; font-weight:bold; color:#E6A23C">{{ formatAchievement(indicatorStats.min) }}</div>
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
      <el-button
        :loading="pdfDownloading"
        :disabled="pdfDownloading"
        @click="downloadPdf"
      >
        导出 PDF 报告
      </el-button>
      <el-button
        :loading="excelDownloading"
        :disabled="excelDownloading"
        @click="downloadExcel"
      >
        导出 Excel 报告
      </el-button>
      <el-button
        :loading="personalDownloading"
        :disabled="personalDownloading"
        @click="downloadPersonalAchievementExcel"
      >
        导出学生个人达成度
      </el-button>
    </div>

    <el-dialog
      v-model="personalDialogVisible"
      width="760px"
    >
      <template #header>
        <div>
          <div style="font-size:16px;font-weight:700">{{ personalDialogTitle }}</div>
          <div v-if="personalDialogDesc" style="margin-top:6px;font-size:13px;color:#606266;line-height:1.5">
            {{ personalDialogDesc }}
          </div>
        </div>
      </template>
      <el-table
        v-loading="personalLoading"
        :data="personalRows"
        border
        stripe
        size="small"
      >
        <el-table-column prop="studentNo" label="学号" width="180" align="center" />
        <el-table-column prop="studentName" label="姓名" min-width="160" align="center" />
        <el-table-column label="个人达成度" width="160" align="center">
          <template #default="{ row }">
            {{ formatAchievement(row.achievement) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag
              size="small"
              effect="light"
              :type="achievementTagType(row.achievement)"
            >
              {{ achievementTagText(row.achievement) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, inject, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import {
  getScoreStatus, triggerCourseCompute, getCourseComputeResults,
  downloadCoursePdf, downloadCourseExcel, downloadPersonalAchievements,
  listObjectivePersonalAchievements, listIndicatorPersonalAchievements
} from '../../api/teacher'
import { useUserStore } from '../../stores/user'
import StatusTag from '../../components/StatusTag.vue'
import * as echarts from 'echarts'
import { downloadBlob } from '../../utils/downloadFile'

const route = useRoute()
const userStore = useUserStore()
const classId = ref(route.params.classId)
const resolveClassName = inject('resolveClassName', () => '')
const status = ref('')
const computing = ref(false)
const requesting = ref(false)
const pdfDownloading = ref(false)
const excelDownloading = ref(false)
const personalDownloading = ref(false)
const requestDialogVisible = ref(false)
const unlockReason = ref('')
const myRequests = ref([])
const results = reactive({ objectiveAchievements: {}, courseAchievements: {}, calcTime: null, objectiveLabels: {}, indicatorLabels: {}, objectiveContents: {}, indicatorContents: {} })

// Chart related refs
const objectiveChartRef = ref(null)
const indicatorChartRef = ref(null)
const objectiveChartType = ref('radar')
const indicatorChartType = ref('radar')
const objectiveChartInstance = ref(null)
const indicatorChartInstance = ref(null)
const sortAsc = ref(false)
const personalDialogVisible = ref(false)
const personalDialogTitle = ref('')
const personalDialogDesc = ref('')
const personalLoading = ref(false)
const personalRows = ref([])

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
  const contents = results.objectiveContents || {}
  return Object.entries(results.objectiveAchievements || {}).map(([id, val]) => ({
    objectiveId: id,
    objectiveNo: labels[id] || `目标${id}`,
    achievement: val,
    description: contents[id] || ''
  }))
})

const indicatorData = computed(() => {
  const labels = results.indicatorLabels || {}
  const contents = results.indicatorContents || {}
  const data = Object.entries(results.courseAchievements || {}).map(([id, val]) => ({
    indicatorId: id,
    indicatorNo: labels[id] || `指标点${id}`,
    achievement: val,
    description: contents[id] || ''
  }))
  data.sort((a, b) => {
    const [a1, a2] = (a.indicatorNo || '').split('-').map(Number)
    const [b1, b2] = (b.indicatorNo || '').split('-').map(Number)
    if (!isNaN(a1) && !isNaN(b1)) return a1 - b1 || (a2 || 0) - (b2 || 0)
    return (a.indicatorNo || '').localeCompare(b.indicatorNo || '')
  })
  return data
})
function formatAchievement(value) {
  const num = Number(value)
  if (Number.isNaN(num)) return '-'
  return num.toFixed(4)
}

function achievementPercent(value) {
  const num = Number(value)
  if (Number.isNaN(num)) return 0
  return Math.max(0, Math.min(100, Number((num * 100).toFixed(1))))
}

function achievementTagType(value) {
  const num = Number(value)
  if (Number.isNaN(num)) return 'info'
  return num >= 0.7 ? 'success' : 'warning'
}

function achievementTagText(value) {
  const num = Number(value)
  if (Number.isNaN(num)) return '暂无数据'
  return num >= 0.7 ? '达标' : '需关注'
}
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

async function openPersonalDialog(type, item) {
  personalDialogVisible.value = true
  personalLoading.value = true
  personalRows.value = []
  personalDialogTitle.value = type === 'objective'
    ? `${item.objectiveNo} 学生个人达成度`
    : `${item.indicatorNo} 学生个人达成度`
  personalDialogDesc.value = item.description || ''
  try {
    const res = type === 'objective'
      ? await listObjectivePersonalAchievements(classId.value, item.objectiveId)
      : await listIndicatorPersonalAchievements(classId.value, item.indicatorId)
    personalRows.value = res.data || []
  } catch { /* handled */ }
  finally { personalLoading.value = false }
}

async function downloadPdf() {
  if (pdfDownloading.value) return
  pdfDownloading.value = true
  try {
    const blob = await downloadCoursePdf(classId.value)
    downloadBlob(blob, `课程达成度报告_${resolveClassName(classId.value)}.pdf`)
  } catch { /* 拦截器已提示错误 */ }
  finally { pdfDownloading.value = false }
}

async function downloadExcel() {
  if (excelDownloading.value) return
  excelDownloading.value = true
  try {
    const blob = await downloadCourseExcel(classId.value)
    downloadBlob(blob, `课程达成度报告_${resolveClassName(classId.value)}.xlsx`)
  } catch { /* 拦截器已提示错误 */ }
  finally { excelDownloading.value = false }
}

async function downloadPersonalAchievementExcel() {
  if (personalDownloading.value) return
  personalDownloading.value = true
  try {
    const blob = await downloadPersonalAchievements(classId.value)
    downloadBlob(blob, `学生个人达成度_${resolveClassName(classId.value)}.xlsx`)
  } catch { /* 拦截器已提示错误 */ }
  finally { personalDownloading.value = false }
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
          `${d.name}: ${d.value.toFixed(4)}`
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
          `${d.name}: ${d.value.toFixed(4)}`
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
        return `${params[0].name}<br/>达成度: ${val.toFixed(4)}<br/>状态: ${getStatusText(val)}`
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
        formatter: (params) => params.value.toFixed(4),
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
        return `${params[0].name}<br/>达成度: ${val.toFixed(4)}<br/>状态: ${getStatusText(val)}`
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
        formatter: (params) => params.value.toFixed(4),
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
        return `${params[0].name}<br/>达成度: ${val.toFixed(4)}<br/>状态: ${getStatusText(val)}`
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
        return `${params[0].name}<br/>达成度: ${val.toFixed(4)}<br/>状态: ${getStatusText(val)}`
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
/* 我的勘误申请表格优化 */
:deep(.my-unlock-table .el-table__header th) {
  height: 42px;
  background-color: #f7f7fa;
  color: #606266;
  font-weight: 700;
}

:deep(.my-unlock-table .el-table__row td) {
  height: 56px;
}

/* 提交时间一行显示 */
:deep(.unlock-time-column .cell) {
  white-space: nowrap;
  text-align: center;
}

/* 状态列居中 */
:deep(.unlock-status-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 状态标签更明显 */
.unlock-status-tag {
  min-width: 120px;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

/* 操作列居中 */
:deep(.unlock-operation-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.unlock-operation-actions {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 撤销按钮美化 */
.unlock-cancel-btn {
  height: 28px;
  padding: 0 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  border-color: #ef9aa0;
  background-color: #ef9aa0;
}

.unlock-cancel-btn:hover {
  color: #fff;
  border-color: #e78087;
  background-color: #e78087;
}

:deep(.unlock-cancel-btn.el-button) {
  margin-left: 0;
}

/* 无操作占位符居中 */
.operation-placeholder {
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.my-unlock-card {
  width: 100%;
  margin-bottom: 20px;
}
/* 课程达成度结果展示优化 */
.achievement-overview {
  display: grid;
  grid-template-columns: repeat(2, minmax(360px, 1fr));
  gap: 20px;
  margin-top: 16px;
}

.achievement-panel {
  border-radius: 18px;
  overflow: hidden;
}

.achievement-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.achievement-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
}

.achievement-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.achievement-count-tag {
  border-radius: 999px;
  font-weight: 600;
}

.achievement-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  padding: 12px 10px;
  border-radius: 14px;
  background: linear-gradient(135deg, #fbf8ff 0%, #ffffff 100%);
  border: 1px solid #f0eafd;
  text-align: center;
}

.summary-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.summary-value {
  font-size: 20px;
  font-weight: 800;
}

.summary-value.primary {
  color: #7e57c2;
}

.summary-value.success {
  color: #67c23a;
}

.summary-value.warning {
  color: #e6a23c;
}

.achievement-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.achievement-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(90deg, #fbf8ff 0%, #ffffff 100%);
  border: 1px solid #f0eafd;
  transition: all 0.2s ease;
}

.achievement-item:hover {
  box-shadow: 0 8px 20px rgba(126, 87, 194, 0.08);
  transform: translateY(-1px);
}

.achievement-item-top {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  gap: 12px;
}

.achievement-name {
  min-width: 64px;
  font-size: 15px;
  font-weight: 700;
  color: #303133;
}

.achievement-desc {
  flex: 1;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 280px;
  cursor: default;
}

.achievement-value {
  font-size: 18px;
  font-weight: 800;
  color: #7e57c2;
}

.achievement-status-tag {
  border-radius: 999px;
  font-weight: 600;
  min-width: 64px;
  text-align: center;
}

.personal-detail-btn {
  margin-left: auto;
  flex-shrink: 0;
}

.achievement-progress {
  width: 100%;
}

:deep(.achievement-progress .el-progress-bar__outer) {
  border-radius: 999px;
  background-color: #f1eef8;
}

:deep(.achievement-progress .el-progress-bar__inner) {
  border-radius: 999px;
}

@media (max-width: 1200px) {
  .achievement-overview {
    grid-template-columns: 1fr;
  }
}
</style>
