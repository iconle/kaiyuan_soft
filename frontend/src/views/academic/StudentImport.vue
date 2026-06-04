<template>
  <div class="page-container">
    <div class="page-header">
      <h3>学生名单管理</h3>
    </div>

    <div class="content-card">
      <div class="filter-panel">
        <div class="filter-row">
          <el-select
            v-model="filterMajorId"
            placeholder="按专业筛选"
            clearable
            class="filter-control"
            @change="loadStudents"
          >
            <el-option
              v-for="m in majors"
              :key="m.id"
              :label="m.name"
              :value="m.id"
            />
          </el-select>

          <div class="filter-break"></div>

          <el-input
            v-model="keyword"
            placeholder="搜索学号/姓名"
            clearable
            class="filter-control search-input"
            @keyup.enter="loadStudents"
            @clear="loadStudents"
          />

          <el-button
            type="primary"
            class="search-button"
            @click="loadStudents"
          >
            查询
          </el-button>
        </div>
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
.content-card {
  padding: 20px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.06);
}

.filter-panel {
  margin-bottom: var(--space-4);
  padding: 18px 20px;
  border: 1px solid #eef1f6;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #fafbff 100%);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px 16px;
}

.filter-control {
  width: 240px;
}

.search-input {
  width: 240px;
}

.filter-break {
  flex-basis: 100%;
  height: 0;
}

.search-button {
  height: 42px;
  padding: 0 24px;
  border-radius: 14px;
  font-weight: 500;
}

/* 美化 Element Plus 输入框和下拉框内部样式 */
:deep(.filter-control .el-input__wrapper),
:deep(.filter-control .el-select__wrapper) {
  min-height: 42px;
  border-radius: 14px;
  padding: 0 14px;
  background-color: #fff;
  box-shadow: 0 0 0 1px #e5e7eb inset;
  transition: all 0.2s ease;
}

:deep(.filter-control .el-input__wrapper:hover),
:deep(.filter-control .el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--el-color-primary-light-5) inset;
}

:deep(.filter-control .el-input__wrapper.is-focus),
:deep(.filter-control .el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset;
}

:deep(.filter-control .el-input__inner) {
  font-size: 14px;
  color: #303133;
}

:deep(.filter-control .el-input__inner::placeholder) {
  color: #a8abb2;
}

/* 窄屏时自动铺满 */
@media (max-width: 768px) {
  .filter-control,
  .search-input,
  .search-button {
    width: 100%;
  }
}
.pagination { margin-top: var(--space-4); justify-content: flex-end; }
.import-section { margin-top: 24px; padding-top: var(--space-5); border-top: 1px solid var(--border-default); }
.import-section h4 { margin: 0 0 8px 0; }
.import-hint { color: var(--text-secondary); font-size: 13px; margin: 0 0 8px 0; }
.import-result { margin-left: 12px; color: var(--el-color-success); font-size: 13px; }
</style>
