<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h3>个人达成度</h3>
        <p>按当前课程成绩与课程目标权重，查看每位学生的达成情况。</p>
      </div>
      <el-tooltip
        :disabled="exportReady"
        content="暂无个人达成度数据，请先完成课程级计算后再导出"
        placement="bottom"
      >
        <span>
          <el-button
            type="primary"
            :icon="Download"
            :loading="exporting"
            :disabled="!exportReady"
            @click="handleExport"
          >
            导出 Excel
          </el-button>
        </span>
      </el-tooltip>
    </div>

    <div class="summary-band">
      <div class="summary-item">
        <span>学生数</span>
        <strong>{{ rows.length }}</strong>
      </div>
      <div class="summary-item">
        <span>平均达成度</span>
        <strong>{{ averageAchievement.toFixed(4) }}</strong>
      </div>
      <div class="summary-item">
        <span>达标人数</span>
        <strong class="success">{{ achievedCount }}</strong>
      </div>
      <div class="summary-item">
        <span>未达标人数</span>
        <strong :class="{ danger: unachievedCount > 0 }">{{ unachievedCount }}</strong>
      </div>
    </div>

    <div class="table-toolbar">
      <el-input
        v-model="keyword"
        :prefix-icon="Search"
        clearable
        placeholder="搜索学号或姓名"
        class="search-input"
      />
    </div>

    <el-table
      v-loading="loading"
      :data="filteredRows"
      border
      stripe
      class="personal-achievement-table"
    >
      <template #empty>
        <el-empty :description="tableEmptyText" :image-size="96">
          <el-button
            v-if="loadError"
            type="primary"
            link
            class="personal-empty-action"
            @click="loadRows"
          >
            重新加载
          </el-button>
          <el-button
            v-else-if="keyword.trim()"
            type="primary"
            link
            class="personal-empty-action"
            @click="keyword = ''"
          >
            清空搜索条件
          </el-button>
          <el-button
            v-else-if="!loadError"
            type="primary"
            link
            class="personal-empty-action"
            @click="goCourseCompute"
          >
            去课程级计算
          </el-button>
        </el-empty>
      </template>

      <el-table-column
        prop="studentNo"
        label="学号"
        min-width="150"
        align="center"
      />

      <el-table-column
        prop="studentName"
        label="姓名"
        min-width="120"
        align="center"
      />

      <el-table-column
        label="综合达成度"
        width="190"
        align="center"
        header-align="center"
        class-name="achievement-column"
      >
        <template #default="{ row }">
          <div class="achievement-cell">
            <span class="achievement-value">
              {{ formatAchievement(row.overallAchievement) }}
            </span>
            <el-progress
              :percentage="achievementPercent(row.overallAchievement)"
              :stroke-width="7"
              :show-text="false"
              class="achievement-progress-mini"
            />
          </div>
        </template>
      </el-table-column>

      <el-table-column
        label="状态"
        width="130"
        align="center"
        class-name="personal-status-column"
      >
        <template #default="{ row }">
          <el-tag
            :type="statusType(row.overallAchievement)"
            effect="light"
            class="personal-status-tag"
          >
            {{ statusText(row.overallAchievement) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        label="操作"
        width="150"
        align="center"
        class-name="personal-operation-column"
      >
        <template #default="{ row }">
          <div class="personal-operation-actions">
            <el-button
              size="small"
              class="detail-action-btn"
              @click="openDetail(row)"
            >
              查看详情
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="detailVisible"
      :title="detailTitle"
      width="720px"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <div class="detail-summary">
          <span>综合达成度</span>
          <strong>{{ formatAchievement(detail.overallAchievement) }}</strong>
          <el-tag :type="statusType(detail.overallAchievement)">
            {{ statusText(detail.overallAchievement) }}
          </el-tag>
        </div>

        <el-tabs>
          <el-tab-pane label="课程目标">
            <el-table
              :data="objectiveRows"
              border
              size="small"
              empty-text="暂无课程目标达成度明细，请先完成课程级计算"
            >
              <el-table-column prop="label" label="课程目标" />
              <el-table-column label="达成度" width="150" align="center">
                <template #default="{ row }">{{ formatAchievement(row.value) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="statusType(row.value)" size="small">{{ statusText(row.value) }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="指标点">
            <el-table
              :data="indicatorRows"
              border
              size="small"
              empty-text="暂无指标点达成度明细，请先完成课程级计算"
            >
              <el-table-column prop="label" label="指标点" />
              <el-table-column label="达成度" width="150" align="center">
                <template #default="{ row }">{{ formatAchievement(row.value) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="statusType(row.value)" size="small">{{ statusText(row.value) }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Download, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  downloadPersonalAchievements,
  getPersonalAchievement,
  listPersonalAchievements
} from '../../api/teacher'
import { buildClassFilename, downloadBlob } from '../../utils/downloadFile'

const route = useRoute()
const router = useRouter()
const classId = computed(() => route.params.classId)
const resolveClassName = inject('resolveClassName', () => '')
const loading = ref(false)
const exporting = ref(false)
const keyword = ref('')
const rows = ref([])
const loadError = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = reactive({
  studentNo: '',
  studentName: '',
  overallAchievement: 0,
  objectiveAchievements: {},
  indicatorAchievements: {},
  objectiveLabels: {},
  indicatorLabels: {}
})

const filteredRows = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return rows.value
  return rows.value.filter(row =>
    String(row.studentNo || '').toLowerCase().includes(query) ||
    String(row.studentName || '').toLowerCase().includes(query)
  )
})

const averageAchievement = computed(() => {
  if (rows.value.length === 0) return 0
  return rows.value.reduce((sum, row) => sum + Number(row.overallAchievement || 0), 0) / rows.value.length
})
const exportReady = computed(() => rows.value.length > 0 && !loadError.value)

const achievedCount = computed(() =>
  rows.value.filter(row => Number(row.overallAchievement) >= 0.7).length
)
const unachievedCount = computed(() => rows.value.length - achievedCount.value)
const detailTitle = computed(() =>
  `${detail.studentName || '学生'}（${detail.studentNo || '-'}）`
)
const tableEmptyText = computed(() =>
  loadError.value
    ? loadError.value
    : keyword.value.trim()
    ? '未找到匹配的学生，请调整搜索条件'
    : '暂无个人达成度数据，请先完成成绩导入和课程级计算'
)

const objectiveRows = computed(() => toDetailRows(
  detail.objectiveAchievements,
  detail.objectiveLabels,
  '课程目标'
))
const indicatorRows = computed(() => toDetailRows(
  detail.indicatorAchievements,
  detail.indicatorLabels,
  '指标点'
))

onMounted(loadRows)
watch(classId, loadRows)

async function loadRows() {
  if (!classId.value) return
  loading.value = true
  loadError.value = ''
  try {
    const response = await listPersonalAchievements(classId.value)
    rows.value = response.data || []
  } catch {
    rows.value = []
    loadError.value = '个人达成度数据加载失败，请刷新页面后重试'
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const response = await getPersonalAchievement(classId.value, row.studentId)
    Object.assign(detail, response.data || {})
  } finally {
    detailLoading.value = false
  }
}

async function handleExport() {
  exporting.value = true
  try {
    downloadBlob(await downloadPersonalAchievements(classId.value), buildClassFilename(resolveClassName(classId.value), '个人达成度', 'xlsx'))
    ElMessage.success('个人达成度 Excel 已导出')
  } finally {
    exporting.value = false
  }
}

function goCourseCompute() {
  router.push(`/teacher/${classId.value}/compute`)
}

function toDetailRows(values = {}, labels = {}, fallback) {
  return Object.entries(values || {}).map(([id, value]) => ({
    id,
    label: labels?.[id] || `${fallback}${id}`,
    value: Number(value || 0)
  }))
}

function formatAchievement(value) {
  return Number(value || 0).toFixed(4)
}
function achievementPercent(value) {
  const num = Number(value || 0)
  if (Number.isNaN(num)) return 0
  return Math.max(0, Math.min(100, Number((num * 100).toFixed(1))))
}
function statusType(value) {
  const achievement = Number(value || 0)
  if (achievement >= 0.7) return 'success'
  if (achievement >= 0.65) return 'warning'
  return 'danger'
}

function statusText(value) {
  const achievement = Number(value || 0)
  if (achievement >= 0.7) return '达标'
  if (achievement >= 0.65) return '预警'
  return '未达标'
}
</script>

<style scoped>
.page-container {
  padding: var(--space-5);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-header h3 {
  margin: 0;
  font-size: 20px;
  color: var(--text-primary);
}

.page-header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.summary-band {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  border: 1px solid var(--gray-150);
  border-radius: 6px;
  background: #fff;
  margin-bottom: 16px;
}

.summary-item {
  min-height: 84px;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-right: 1px solid var(--gray-150);
}

.summary-item:last-child {
  border-right: 0;
}

.summary-item span {
  font-size: 13px;
  color: var(--text-secondary);
}

.summary-item strong {
  margin-top: 6px;
  font-size: 24px;
  color: var(--text-primary);
}

.summary-item strong.success {
  color: #67c23a;
}

.summary-item strong.danger {
  color: #f56c6c;
}

.table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.search-input {
  width: 260px;
}

.achievement-value {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.detail-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: #f7f8fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.detail-summary span {
  color: var(--text-secondary);
}

.detail-summary strong {
  font-size: 22px;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 800px) {
  .summary-band {
    grid-template-columns: repeat(2, 1fr);
  }

  .summary-item:nth-child(2) {
    border-right: 0;
  }

  .summary-item:nth-child(-n + 2) {
    border-bottom: 1px solid var(--gray-150);
  }
}
/* 个人达成度表格优化 */
.personal-achievement-table {
  width: 100%;
  border-radius: 14px;
  overflow: hidden;
}

:deep(.personal-achievement-table .el-table__header th) {
  height: 44px;
  background-color: #f7f7fa;
  color: #606266;
  font-weight: 700;
}

:deep(.personal-achievement-table .el-table__row td) {
  height: 58px;
}

.personal-empty-action {
  margin-top: 4px;
}

/* 综合达成度：数值 + 小进度条 */
.achievement-cell {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.achievement-value {
  min-width: 48px;
  font-weight: 800;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.achievement-progress-mini {
  width: 82px;
}

:deep(.achievement-progress-mini .el-progress-bar__outer) {
  border-radius: 999px;
  background-color: #f1eef8;
}

:deep(.achievement-progress-mini .el-progress-bar__inner) {
  border-radius: 999px;
  background-color: #9b87c9;
}

/* 状态列居中并突出显示 */
:deep(.personal-status-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.personal-status-tag {
  min-width: 64px;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

/* 操作列居中 */
:deep(.personal-operation-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.personal-operation-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

/* 查看详情按钮：紫色胶囊按钮 */
.detail-action-btn {
  height: 30px;
  padding: 0 18px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  border-color: #9b87c9;
  background-color: #9b87c9;
  box-shadow: 0 6px 14px rgba(126, 87, 194, 0.18);
}

.detail-action-btn:hover {
  color: #fff;
  border-color: #8c76c2;
  background-color: #8c76c2;
}

:deep(.detail-action-btn.el-button) {
  margin-left: 0;
}
/* 综合达成度列居中 */
:deep(.achievement-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 综合达成度内容整体居中 */
.achievement-cell {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
</style>
