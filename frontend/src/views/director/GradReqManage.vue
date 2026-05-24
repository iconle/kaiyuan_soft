<template>
  <div class="page-container">
    <div class="page-header">
      <h3>毕业要求管理</h3>
      <el-select v-model="currentMajorId" placeholder="选择专业" style="width: 220px"
                 @change="loadData">
        <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
      </el-select>
      <el-button type="primary" @click="showReqDialog()" :disabled="!currentMajorId">新增毕业要求</el-button>
    </div>

    <el-empty v-if="!currentMajorId" description="请先选择专业" />
    <div v-else>
      <el-collapse v-model="expandedReqs" v-loading="loading">
        <el-collapse-item v-for="req in requirements" :key="req.id" :name="req.id">
          <template #title>
            <div class="req-title">
              <el-tag type="info" size="small">要求{{ req.reqNo }}</el-tag>
              <span class="req-name">{{ req.title }}</span>
              <el-button size="small" text @click.stop="showReqDialog(req)">编辑</el-button>
              <el-button size="small" text type="danger" @click.stop="handleDeleteReq(req)">删除</el-button>
            </div>
          </template>
          <div class="indicator-section">
            <div class="section-header">
              <span>指标点列表</span>
              <el-button size="small" type="primary" @click="showIndicatorDialog(req.id)">新增指标点</el-button>
            </div>
            <el-table :data="req.indicators || []" border size="small">
              <el-table-column prop="indicatorNo" label="编号" width="80" />
              <el-table-column prop="content" label="描述" />
              <el-table-column label="操作" width="140">
                <template #default="{ row }">
                  <el-button size="small" text @click="showIndicatorDialog(req.id, row)">编辑</el-button>
                  <el-button size="small" text type="danger" @click="handleDeleteIndicator(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <!-- 毕业要求对话框 -->
    <el-dialog v-model="reqDialogVisible" :title="editingReq ? '编辑毕业要求' : '新增毕业要求'" width="560px">
      <el-form :model="reqForm" label-width="100px">
        <el-form-item label="编号" required>
          <el-input-number v-model="reqForm.reqNo" :min="1" :max="8" />
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="reqForm.title" />
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input v-model="reqForm.content" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reqDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReqSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 指标点对话框 -->
    <el-dialog v-model="indicatorDialogVisible" :title="editingIndicator ? '编辑指标点' : '新增指标点'" width="480px">
      <el-form :model="indicatorForm" label-width="80px">
        <el-form-item label="编号" required>
          <el-input v-model="indicatorForm.indicatorNo" placeholder="如 3-1" />
        </el-form-item>
        <el-form-item label="描述" required>
          <el-input v-model="indicatorForm.content" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="indicatorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleIndicatorSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listGradReqs, createGradReq, updateGradReq, deleteGradReq, addIndicator, updateIndicator, deleteIndicator } from '../../api/director'
import { listMajors } from '../../api/admin'

const loading = ref(false)
const requirements = ref([])
const expandedReqs = ref([])
const majors = ref([])
const currentMajorId = ref(null)

const reqDialogVisible = ref(false)
const editingReq = ref(null)
const reqForm = reactive({ reqNo: 1, title: '', content: '' })

const indicatorDialogVisible = ref(false)
const editingIndicator = ref(null)
const currentGradReqId = ref(null)
const indicatorForm = reactive({ indicatorNo: '', content: '' })

onMounted(async () => {
  const res = await listMajors({ page: 1, size: 100 })
  majors.value = res.data.records
})

async function loadData() {
  if (!currentMajorId.value) return
  loading.value = true
  try {
    const res = await listGradReqs(currentMajorId.value)
    requirements.value = res.data || []
    expandedReqs.value = requirements.value.map(r => r.id)
  } finally {
    loading.value = false
  }
}

function showReqDialog(req) {
  editingReq.value = req || null
  Object.assign(reqForm, { reqNo: req?.reqNo || 1, title: req?.title || '', content: req?.content || '' })
  reqDialogVisible.value = true
}

async function handleReqSubmit() {
  if (editingReq.value) {
    await updateGradReq(editingReq.value.id, { ...reqForm, majorId: currentMajorId.value })
  } else {
    await createGradReq({ ...reqForm, majorId: currentMajorId.value })
  }
  ElMessage.success('操作成功')
  reqDialogVisible.value = false
  loadData()
}

async function handleDeleteReq(req) {
  await ElMessageBox.confirm(`确定删除毕业要求「${req.title}」及其所有指标点？`, '提示', { type: 'warning' })
  await deleteGradReq(req.id)
  ElMessage.success('已删除')
  loadData()
}

function showIndicatorDialog(gradReqId, indicator) {
  currentGradReqId.value = gradReqId
  editingIndicator.value = indicator || null
  Object.assign(indicatorForm, { indicatorNo: indicator?.indicatorNo || '', content: indicator?.content || '' })
  indicatorDialogVisible.value = true
}

async function handleIndicatorSubmit() {
  if (editingIndicator.value) {
    await updateIndicator(editingIndicator.value.id, indicatorForm)
  } else {
    await addIndicator(currentGradReqId.value, indicatorForm)
  }
  ElMessage.success('操作成功')
  indicatorDialogVisible.value = false
  loadData()
}

async function handleDeleteIndicator(id) {
  await ElMessageBox.confirm('确定删除该指标点？', '提示', { type: 'warning' })
  await deleteIndicator(id)
  ElMessage.success('已删除')
  loadData()
}
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
.page-header h3 { margin: 0; font-size: 18px; }
.req-title { display: flex; align-items: center; gap: 8px; }
.req-name { font-weight: 500; }
.indicator-section { padding: 0 12px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-weight: 500; }
</style>
