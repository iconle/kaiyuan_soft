<template>
  <div class="page-container">
    <div class="page-header">
      <h3>学生名单管理</h3>
    </div>

    <div class="content-card">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索学号/姓名" clearable style="width: 200px"
                  @keyup.enter="loadStudents" @clear="loadStudents" />
        <el-select v-model="filterMajorId" placeholder="专业筛选" clearable style="width: 180px; margin-left: 12px"
                   @change="loadStudents">
          <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
        </el-select>
        <el-button type="primary" @click="loadStudents" style="margin-left: 12px">查询</el-button>
      </div>

      <el-table :data="students" border stripe v-loading="loading">
        <el-table-column prop="studentNo" label="学号" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="enrollmentYear" label="入学年份" width="100" />
        <el-table-column label="专业" width="160">
          <template #default="{ row }">
            {{ getMajorName(row.majorId) }}
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" background layout="total, prev, pager, next"
                     :total="total" :page-size="search.size" v-model:current-page="search.page"
                     @current-change="loadStudents" />
    </div>

    <!-- 批量导入 -->
    <div class="import-section">
      <h4>批量导入学生</h4>
      <p class="import-hint">格式：每行一条，包含学号、姓名、入学年份、专业ID（逗号分隔）</p>
      <el-input v-model="importText" type="textarea" :rows="6" placeholder="2024001,张三,2024,1&#10;2024002,李四,2024,1" />
      <el-button type="primary" @click="handleImport" :loading="importing" style="margin-top: 8px">导入</el-button>
      <span v-if="importResult" class="import-result">{{ importResult }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listStudents, importStudents } from '../../api/academic'
import { listMajors } from '../../api/admin'

const loading = ref(false)
const students = ref([])
const majors = ref([])
const total = ref(0)
const keyword = ref('')
const filterMajorId = ref(null)
const search = reactive({ page: 1, size: 15 })

const importText = ref('')
const importing = ref(false)
const importResult = ref('')

onMounted(async () => {
  const res = await listMajors({ page: 1, size: 100 })
  majors.value = res.data.records
  loadStudents()
})

async function loadStudents() {
  loading.value = true
  try {
    const res = await listStudents({
      page: search.page, size: search.size,
      keyword: keyword.value || undefined,
      majorId: filterMajorId.value || undefined
    })
    students.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function getMajorName(majorId) {
  return majors.value.find(m => m.id === majorId)?.name || '-'
}

async function handleImport() {
  if (!importText.value.trim()) { ElMessage.warning('请输入导入数据'); return }
  importing.value = true
  importResult.value = ''
  try {
    const lines = importText.value.trim().split('\n').filter(l => l.trim())
    const data = lines.map(line => {
      const [studentNo, name, enrollmentYear, majorId] = line.split(',').map(s => s.trim())
      return { studentNo, name, enrollmentYear: enrollmentYear ? parseInt(enrollmentYear) : null, majorId: majorId ? parseInt(majorId) : null }
    })
    const res = await importStudents(data)
    importResult.value = `成功导入 ${res.data} 条记录`
    ElMessage.success('导入完成')
    loadStudents()
  } catch (e) {
    importResult.value = '导入失败'
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.page-container { padding: var(--space-5); }
.page-header h3 { margin: 0 0 var(--space-4) 0; font-size: var(--text-lg); }
.search-bar { display: flex; align-items: center; margin-bottom: var(--space-4); }
.pagination { margin-top: var(--space-4); justify-content: flex-end; }
.import-section { margin-top: 24px; padding-top: var(--space-5); border-top: 1px solid var(--border-default); }
.import-section h4 { margin: 0 0 8px 0; }
.import-hint { color: var(--text-secondary); font-size: 13px; margin: 0 0 8px 0; }
.import-result { margin-left: 12px; color: var(--el-color-success); font-size: 13px; }
</style>
