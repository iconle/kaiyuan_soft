<template>
  <div class="page-container">
    <div class="page-header">
      <h3>考核点细分与映射</h3>
      <el-button type="primary" @click="showDialog()">新增考核点</el-button>
      <span v-if="weightSum !== null" :style="{color: weightSum === 100 ? '#67c23a' : '#f56c6c', fontSize: '14px', fontWeight:'bold'}">
        总权重: {{ weightSum }}% {{ weightSum === 100 ? '✓' : '✗ 必须等于100%' }}
      </span>
    </div>

    <el-table :data="assessments" border stripe v-loading="loading">
      <el-table-column prop="sortOrder" label="序号" width="60" />
      <el-table-column prop="name" label="考核点名称" width="160" />
      <el-table-column prop="maxScore" label="满分" width="70" />
      <el-table-column prop="weightPercent" label="权重(%)" width="90" />
      <el-table-column label="绑定目标" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="oid in row.objectiveIds" :key="oid" size="small" style="margin-right:4px">{{ getObjNo(oid) }}</el-tag>
          <span v-if="!row.objectiveIds || row.objectiveIds.length === 0" style="color:#909399">未绑定</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" text @click="showDialog(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑考核点' : '新增考核点'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="考核点名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="满分分值" required><el-input-number v-model="form.maxScore" :min="1" :step="10" style="width:100%" /></el-form-item>
        <el-form-item label="权重(%)" required>
          <el-input-number v-model="form.weightPercent" :min="0" :max="100" :precision="2" style="width:100%" placeholder="占总成绩的百分比" />
          <span style="font-size:12px;color:#909399">所有考核点权重之和必须等于100%</span>
        </el-form-item>
        <el-form-item label="绑定课程目标" required>
          <el-select v-model="form.objectiveIds" multiple style="width:100%" placeholder="可多选课程目标">
            <el-option v-for="obj in objectives" :key="obj.id" :label="`${obj.objNo} - ${obj.description?.substring(0,20)}`" :value="obj.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="1" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAssessments, createAssessment, updateAssessment, deleteAssessment, listObjectives } from '../../api/teacher'

const route = useRoute()
const classId = ref(route.params.classId)
const loading = ref(false)
const assessments = ref([])
const objectives = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive({ name: '', maxScore: 100, weightPercent: null, objectiveIds: [], sortOrder: 1 })

const weightSum = computed(() => {
  if (assessments.value.length === 0) return null
  let sum = 0
  for (const a of assessments.value) {
    if (a.weightPercent != null) sum += Number(a.weightPercent)
  }
  return Math.round(sum * 100) / 100
})

onMounted(() => { if (classId.value) loadData() })

async function loadData() {
  loading.value = true
  try {
    const [aRes, oRes] = await Promise.all([listAssessments(classId.value), listObjectives(classId.value)])
    assessments.value = aRes.data || []
    objectives.value = oRes.data || []
  } finally { loading.value = false }
}

function getObjNo(oid) { return objectives.value.find(o => o.id === oid)?.objNo || `目标${oid}` }

function showDialog(row) {
  editing.value = row || null
  Object.assign(form, {
    name: row?.name || '', maxScore: row?.maxScore || 100, weightPercent: row?.weightPercent ?? null,
    objectiveIds: row?.objectiveIds ? [...row.objectiveIds] : [],
    sortOrder: row?.sortOrder || assessments.value.length + 1
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const data = {
    name: form.name, maxScore: form.maxScore,
    weightPercent: form.weightPercent != null ? Number(form.weightPercent) : null,
    objectiveIds: form.objectiveIds || [],
    sortOrder: form.sortOrder
  }
  if (editing.value) {
    await updateAssessment(editing.value.id, { ...data, outlineId: editing.value.outlineId })
  } else {
    await createAssessment(classId.value, data)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除考核点「${row.name}」？`, '提示', { type: 'warning' })
  await deleteAssessment(classId.value, row.id)
  ElMessage.success('已删除')
  loadData()
}
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.page-header h3 { margin: 0; font-size: 18px; }
</style>
