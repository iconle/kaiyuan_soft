<template>
  <div class="page-container">
    <div class="page-header">
      <h3>宏观看板</h3>
      <el-select v-model="selectedMajorId" placeholder="选择专业" @change="loadData" style="width:240px">
        <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
      </el-select>
    </div>

    <el-alert v-if="dashboard.allReady" type="success" show-icon :closable="false" style="margin-bottom:16px">
      所有课程已完成课程级计算，可以执行专业级计算。
    </el-alert>
    <el-alert v-else-if="dashboard.totalCount > 0" type="warning" show-icon :closable="false" style="margin-bottom:16px">
      还有 <strong>{{ dashboard.totalCount - dashboard.lockedCount }}</strong> 门课程未完成课程级计算
    </el-alert>

    <el-card header="课程计算状态总览">
      <div v-if="dashboard.totalCount === 0 && !loading" style="text-align:center;padding:40px;color:#909399">
        暂无该专业的课程数据，请先在教务管理中导入课程体系并创建教学班级
      </div>
      <template v-else>
      <div style="margin-bottom:12px; color:#606266">
        已锁定: <strong>{{ dashboard.lockedCount }}</strong> / {{ dashboard.totalCount }}
      </div>
      <el-table :data="dashboard.courseStatuses || []" border stripe v-loading="loading" max-height="400"
        style="width:100%" highlight-current-row @row-click="showDetail">
        <el-table-column prop="courseName" label="课程名称" min-width="160" />
        <el-table-column prop="className" label="教学班级" min-width="180" />
        <el-table-column prop="teacherName" label="主讲教师" width="110" />
        <el-table-column prop="semesterName" label="开课学期" min-width="180" />
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
      <div v-else-if="!detailHasData" style="text-align:center;padding:20px;color:#909399">该课程尚未执行课程级计算，无达成度数据</div>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { getDashboard as apiGetDashboard } from '../../api/director'
import { listMajors } from '../../api/admin'
import { getCourseComputeResults } from '../../api/teacher'
import StatusTag from '../../components/StatusTag.vue'

const majors = ref([])
const selectedMajorId = ref(null)
const loading = ref(false)
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
  try {
    const res = await listMajors()
    majors.value = res.data?.records || []
    if (majors.value.length > 0) {
      selectedMajorId.value = majors.value[0].id
      loadData()
    }
  } catch { /* handled */ }
})

async function loadData() {
  if (!selectedMajorId.value) return
  loading.value = true
  try {
    const res = await apiGetDashboard(selectedMajorId.value)
    Object.assign(dashboard, res.data || {})
  } catch { /* handled */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.page-header h3 { margin: 0; font-size: 18px; }
</style>
