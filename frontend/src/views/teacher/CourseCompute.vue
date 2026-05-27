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
          @click="showRequestDialog">提交勘误申请</el-button>
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
        <el-table-column label="操作" width="70">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger"
              @click="handleCancelRequest(row)">撤销</el-button>
            <span v-else style="color:#909399">-</span>
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

    <div v-if="hasResults" style="margin-top: 16px; display: flex; gap: 8px;">
      <el-button @click="downloadPdf">导出 PDF 报告</el-button>
      <el-button @click="downloadExcel">导出 Excel 报告</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import {
  getScoreStatus, triggerCourseCompute, getCourseComputeResults,
  downloadCoursePdf, downloadCourseExcel
} from '../../api/teacher'
import { useUserStore } from '../../stores/user'
import StatusTag from '../../components/StatusTag.vue'

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

const hasPendingRequest = computed(() => myRequests.value.some(r => r.status === 'PENDING'))

onMounted(() => { if (classId.value) loadData() })

async function loadData() {
  try {
    const sRes = await getScoreStatus(classId.value)
    status.value = sRes.data?.status || ''
    if (status.value === 'LOCKED') {
      const rRes = await getCourseComputeResults(classId.value)
      Object.assign(results, rRes.data || {})
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
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.page-header h3 { margin: 0; font-size: 18px; }
</style>
