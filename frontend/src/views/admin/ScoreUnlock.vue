<template>
  <div class="page-container">
    <div class="page-header">
      <h3>成绩解锁管理（勘误工单）</h3>
      <el-select v-model="filterGrade" placeholder="目标年级" clearable style="width:150px" @change="loadSheets">
        <el-option v-for="g in gradeOptions" :key="g" :label="`${g} 级`" :value="g" />
      </el-select>
    </div>

    <div class="content-card">
      <el-tabs v-model="activeTab">
        <!-- Tab 1: Score sheet status + direct unlock -->
        <el-tab-pane label="成绩单状态" name="sheets">
          <el-table :data="filteredSheets" border stripe v-loading="loadingSheets">
            <el-table-column prop="id" label="成绩单ID" width="90" />
            <el-table-column prop="classId" label="班级ID" width="80" />
            <el-table-column prop="className" label="班级名称" min-width="180" show-overflow-tooltip />
            <el-table-column label="目标年级" width="100" align="center">
              <template #default="{ row }">
                {{ row.grade ? `${row.grade} 级` : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'LOCKED' ? 'success' : row.status === 'IMPORTED' ? 'warning' : 'info'" size="small">
                  {{ row.status === 'LOCKED' ? '已锁定' : row.status === 'IMPORTED' ? '已导入' : '未录入' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lockedAt" label="锁定时间" width="220" class-name="time-column" />
            <el-table-column
              label="操作"
              width="140"
              align="center"
              class-name="unlock-operation-column"
            >
              <template #default="{ row }">
                <div class="unlock-operation-actions">
                  <el-button
                    v-if="row.status === 'LOCKED'"
                    size="small"
                    type="danger"
                    class="unlock-action-btn danger-action-btn"
                    @click="handleDirectUnlock(row)"
                  >
                    紧急解锁
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 2: Unlock requests from teachers -->
        <el-tab-pane label="勘误申请" name="requests">
          <el-alert v-if="requestError" type="warning" show-icon :closable="false" style="margin-bottom:12px">
            勘误工单功能需要执行数据库迁移脚本：docs/database/migrate_unlock_request.sql
          </el-alert>
          <el-alert v-else-if="requests.length === 0" type="info" show-icon :closable="false" style="margin-bottom:12px">
            暂无勘误申请。教师锁定成绩后可在课程级计算页面提交勘误申请。
          </el-alert>
          <el-table v-if="!requestError" :data="requests" border stripe v-loading="loadingRequests">
            <el-table-column prop="id" label="工单ID" width="80" />
            <el-table-column prop="className" label="班级" min-width="160" show-overflow-tooltip />
            <el-table-column prop="requesterName" label="申请人" width="120" />
            <el-table-column prop="reason" label="勘误原因" min-width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'UNLOCKED' ? 'success' : row.status === 'APPROVED' ? '' : 'danger'" size="small">
                  {{ row.status === 'PENDING' ? '待审批' : row.status === 'UNLOCKED' ? '已解锁' : row.status === 'APPROVED' ? '已同意(待解锁)' : '已拒绝' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="申请时间" width="220" class-name="time-column" />
            <el-table-column
              label="操作"
              width="180"
              align="center"
              class-name="unlock-operation-column"
            >
              <template #default="{ row }">
                <div class="unlock-operation-actions">
                  <template v-if="row.status === 'PENDING'">
                    <el-button
                      size="small"
                      type="success"
                      class="unlock-action-btn agree-action-btn"
                      @click="handleApprove(row)"
                    >
                      同意
                    </el-button>
                    <el-button
                      size="small"
                      type="danger"
                      class="unlock-action-btn reject-action-btn"
                      @click="handleReject(row)"
                    >
                      拒绝
                    </el-button>
                  </template>

                  <template v-else-if="row.status === 'APPROVED'">
                    <el-button
                      size="small"
                      type="success"
                      class="unlock-action-btn agree-action-btn"
                      @click="handleUnlock(row)"
                    >
                      解锁
                    </el-button>
                    <el-button
                      size="small"
                      type="danger"
                      class="unlock-action-btn reject-action-btn"
                      @click="handleReject(row)"
                    >
                      拒绝
                    </el-button>
                  </template>

                  <template v-else-if="row.status === 'UNLOCKED'">
                    <el-tag size="small" type="success">已解锁</el-tag>
                  </template>

                  <span v-else class="operation-placeholder">已处理</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { unlockScoreSheet } from '../../api/admin'
const activeTab = ref('sheets')
const loadingSheets = ref(false)
const loadingRequests = ref(false)
const sheets = ref([])
const requests = ref([])
const requestError = ref(false)
const filterGrade = ref(null)
const gradeOptions = [2020, 2021, 2022, 2023, 2024, 2025, 2026, 2027, 2028, 2029]

const filteredSheets = computed(() => {
  if (!filterGrade.value) return sheets.value
  return sheets.value.filter(s => s.grade === filterGrade.value)
})

onMounted(() => {
  loadSheets()
  loadRequests()
})

async function loadSheets() {
  loadingSheets.value = true
  try {
    const res = await request.get('/api/admin/scores')
    sheets.value = res.data || []
  } finally { loadingSheets.value = false }
}

async function loadRequests() {
  loadingRequests.value = true
  try {
    const res = await request.get('/api/admin/unlock-requests')
    requests.value = res.data || []
    requestError.value = false
  } catch {
    requestError.value = true
    requests.value = []
  } finally { loadingRequests.value = false }
}

async function handleDirectUnlock(row) {
  await ElMessageBox.confirm(
    `【紧急操作】确定直接解锁「${row.className}」的成绩单？此操作将跳过勘误工单审批流程，清除已有计算结果。建议通过「勘误申请」Tab 按流程处理。`,
    '紧急解锁确认', { type: 'warning', confirmButtonText: '确认解锁', confirmButtonClass: 'el-button--danger' }
  )
  try {
    await unlockScoreSheet(row.id)
    ElMessage.success('已解锁，教师可重新修改成绩')
    loadSheets()
  } catch { ElMessage.error('解锁失败') }
}

async function handleApprove(row) {
  await ElMessageBox.confirm(
    `同意「${row.requesterName}」的勘误申请？同意后可进行解锁操作。`,
    '审核确认', { type: 'warning' }
  )
  try {
    await request.post(`/api/admin/unlock-requests/${row.id}/approve`)
    ElMessage.success('已同意，可进行解锁操作')
    loadRequests()
  } catch { ElMessage.error('操作失败') }
}

async function handleUnlock(row) {
  await ElMessageBox.confirm(
    `确认解锁「${row.requesterName}」的勘误申请？解锁后教师可重新修改成绩。`,
    '解锁确认', { type: 'warning' }
  )
  try {
    await request.post(`/api/admin/unlock-requests/${row.id}/unlock`)
    ElMessage.success('已解锁，教师可重新修改成绩')
    loadSheets()
    loadRequests()
  } catch { ElMessage.error('操作失败') }
}

async function handleReject(row) {
  await ElMessageBox.confirm(
    `拒绝「${row.requesterName}」的勘误申请？`,
    '确认拒绝', { type: 'warning' }
  )
  try {
    await request.post(`/api/admin/unlock-requests/${row.id}/reject`)
    ElMessage.success('已拒绝')
    loadRequests()
  } catch { ElMessage.error('操作失败') }
}
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header { display: flex; align-items: center; gap: var(--space-4); margin-bottom: var(--space-4); }
.page-header h3 { margin: 0; font-size: var(--text-lg); }
:deep(.time-column .cell) { white-space: nowrap; }
/* 成绩解锁页面：操作列居中 */
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
  gap: 8px;
  white-space: nowrap;
}

.unlock-action-btn {
  height: 28px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
}

:deep(.unlock-action-btn.el-button) {
  margin-left: 0;
}

.agree-action-btn {
  color: #fff;
  border-color: #67c23a;
  background-color: #67c23a;
}

.agree-action-btn:hover {
  color: #fff;
  border-color: #5daf34;
  background-color: #5daf34;
}

.reject-action-btn,
.danger-action-btn {
  color: #fff;
  border-color: #ef9aa0;
  background-color: #ef9aa0;
}

.reject-action-btn:hover,
.danger-action-btn:hover {
  color: #fff;
  border-color: #e78087;
  background-color: #e78087;
}

.operation-placeholder {
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
</style>
