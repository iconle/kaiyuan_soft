<template>
  <div class="page-container">
    <div class="page-header">
      <h3>课程目标设定</h3>
      <el-button type="primary" @click="showDialog()">新增目标</el-button>
    </div>

    <div class="content-card">
      <el-table :data="objectives" border stripe v-loading="loading">
        <el-table-column prop="objNo" label="编号" width="80" />
        <el-table-column prop="dimension" label="维度" width="80" />
        <el-table-column prop="description" label="目标描述" />

        <el-table-column
          label="操作"
          width="180"
          align="center"
          class-name="operation-column"
        >
          <template #default="{ row }">
            <div class="operation-actions">
              <el-button
                size="small"
                class="action-btn edit-btn"
                @click="showDialog(row)"
              >
                编辑
              </el-button>

              <el-button
                size="small"
                type="danger"
                class="action-btn delete-btn"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>

      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑目标' : '新增目标'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编号" required>
          <el-input v-model="form.objNo" placeholder="如 1-1" />
        </el-form-item>
        <el-form-item label="维度">
          <el-select v-model="form.dimension" style="width: 100%">
            <el-option value="知识" />
            <el-option value="能力" />
            <el-option value="价值" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" required>
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listObjectives, createObjective, updateObjective, deleteObjective } from '../../api/teacher'

const route = useRoute()
const classId = ref(route.params.classId || route.query.classId)
const loading = ref(false)
const objectives = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive({ objNo: '', dimension: '', description: '' })

onMounted(() => { if (classId.value) loadObjectives() })

async function loadObjectives() {
  loading.value = true
  try {
    const res = await listObjectives(classId.value)
    objectives.value = res.data || []
  } finally {
    loading.value = false
  }
}

function showDialog(row) {
  editing.value = row || null
  Object.assign(form, { objNo: row?.objNo || '', dimension: row?.dimension || '', description: row?.description || '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (editing.value) {
    await updateObjective(editing.value.id, { ...form, outlineId: editing.value.outlineId })
  } else {
    await createObjective(classId.value, form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadObjectives()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除目标「${row.objNo}」？相关权重和考核点将一并删除`, '提示', { type: 'warning' })
  await deleteObjective(classId.value, row.id)
  ElMessage.success('已删除')
  loadObjectives()
}
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header { display: flex; align-items: center; gap: var(--space-4); margin-bottom: var(--space-4); }
.page-header h3 { margin: 0; font-size: var(--text-lg); }
/* 操作列按钮：对齐学生管理、教学班级管理风格 */
:deep(.operation-column .cell) {
  display: flex;
  justify-content: center;
  align-items: center;
}

.operation-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  white-space: nowrap;
}

.action-btn {
  height: 25px;
  padding: 0 18px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

:deep(.action-btn.el-button) {
  margin-left: 0;
}

.edit-btn {
  color: #7e57c2;
  border-color: #d8c9f3;
  background-color: #f6f0ff;
}

.edit-btn:hover {
  color: #6f42c1;
  border-color: #b79dea;
  background-color: #efe6ff;
}

.delete-btn {
  color: #fff;
  border-color: #ef9aa0;
  background-color: #ef9aa0;
}

.delete-btn:hover {
  color: #fff;
  border-color: #e78087;
  background-color: #e78087;
}
</style>
