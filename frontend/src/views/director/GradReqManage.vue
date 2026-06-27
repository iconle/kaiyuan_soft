<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h3>毕业要求管理</h3>

        <el-select
          v-model="currentMajorId"
          placeholder="选择专业"
          class="major-select"
          @change="loadData"
        >
          <el-option
            v-for="m in majors"
            :key="m.id"
            :label="m.name"
            :value="m.id"
          />
        </el-select>

        <el-button
          type="primary"
          class="primary-pill-btn"
          @click="showReqDialog()"
          :disabled="!currentMajorId"
        >
          新增毕业要求
        </el-button>

        <el-button
          class="primary-pill-btn template-pill-btn"
          :loading="downloading"
          :disabled="!currentMajorId"
          @click="handleDownloadTemplate"
        >
          下载模板
        </el-button>

        <el-button
          type="primary"
          class="primary-pill-btn"
          :disabled="!currentMajorId"
          @click="openImportDialog"
        >
          导入指标点
        </el-button>
      </div>
    </div>

    <el-empty v-if="!currentMajorId" description="请先选择专业" />



    <div v-else class="content-card grad-req-card">
      <el-collapse
        v-model="expandedReqs"
        v-loading="loading"
        class="grad-req-collapse"
      >
        <el-collapse-item
          v-for="req in requirements"
          :key="req.id"
          :name="req.id"
          class="grad-req-item"
        >
          <template #title>
            <div class="req-title">
              <div class="req-main">
                <el-tag type="info" size="small" class="req-tag">
                  要求{{ req.reqNo }}
                </el-tag>
                <span class="req-name">{{ req.title }}</span>
              </div>

              <div class="req-actions">
                <el-button
                  size="small"
                  class="req-action-btn req-edit-btn"
                  @click.stop="showReqDialog(req)"
                >
                  编辑
                </el-button>

                <el-button
                  size="small"
                  type="danger"
                  class="req-action-btn req-delete-btn"
                  @click.stop="handleDeleteReq(req)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </template>


          <div class="indicator-section">
            <div class="section-header">
              <span>指标点列表</span>
              <el-button
                size="small"
                type="primary"
                class="add-indicator-btn"
                @click="showIndicatorDialog(req.id)"
              >
                新增指标点
              </el-button>
            </div>

            <el-table
              :data="req.indicators || []"
              border
              size="small"
              class="indicator-table"
            >
              <el-table-column
                prop="indicatorNo"
                label="编号"
                width="100"
                align="center"
              />

              <el-table-column
                prop="content"
                label="描述"
                min-width="300"
              />

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
                      class="indicator-action-btn indicator-edit-btn"
                      @click="showIndicatorDialog(req.id, row)"
                    >
                      编辑
                    </el-button>

                    <el-button
                      size="small"
                      type="danger"
                      class="indicator-action-btn indicator-delete-btn"
                      @click="handleDeleteIndicator(row.id)"
                    >
                      删除
                    </el-button>
                  </div>
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
        <el-form-item label="编号">
          <el-tag v-if="!editingIndicator" type="info">自动生成</el-tag>
          <el-tag v-else type="info">{{ indicatorForm.indicatorNo }}</el-tag>
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

    <!-- 指标点导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入指标点" width="520px">
      <div class="import-tip">
        请先点击「下载模板」获取标准模板，按格式填写后再上传。<br />
        仅支持 .xlsx 文件，<b>表头不可修改</b>，否则将无法导入。
      </div>
      <el-upload
        :show-file-list="false"
        :before-upload="beforeUpload"
        :http-request="customUpload"
        accept=".xlsx"
        :disabled="importing"
      >
        <el-button type="primary" :loading="importing">选择文件并导入</el-button>
        <template #tip>
          <div class="upload-tip">仅支持 .xlsx 格式，校验通过后才会写入数据库。</div>
        </template>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listGradReqs, createGradReq, updateGradReq, deleteGradReq, addIndicator, updateIndicator, deleteIndicator, downloadIndicatorTemplate, importIndicators } from '../../api/director'
import { listMajors } from '../../api/admin'
import { validateExcelFile, showExcelImportError } from '../../utils/excelImport'
import { buildDatedFilename, downloadBlob, ensureDownloadBlob, showDownloadError } from '../../utils/downloadFile'

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

const importing = ref(false)
const downloading = ref(false)
const importDialogVisible = ref(false)
const currentMajorName = computed(() => majors.value.find(item => item.id === currentMajorId.value)?.name || `专业${currentMajorId.value}`)

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

function openImportDialog() {
  if (!currentMajorId.value) { ElMessage.warning('请先选择专业'); return }
  importDialogVisible.value = true
}

async function handleDownloadTemplate() {
  if (!currentMajorId.value) { ElMessage.warning('请先选择专业'); return }
  downloading.value = true
  try {
    const blob = await downloadIndicatorTemplate(currentMajorId.value)
    downloadBlob(await ensureDownloadBlob(blob), buildDatedFilename([currentMajorName.value, '指标点导入模板'], 'xlsx'))
    ElMessage.success('模板已开始下载')
  } catch (error) {
    showDownloadError(error)
  } finally {
    downloading.value = false
  }
}

function beforeUpload(file) {
  return validateExcelFile(file)
}

async function customUpload(opt) {
  const file = opt.file
  importing.value = true
  try {
    const res = await importIndicators(currentMajorId.value, file)
    ElMessage.success(`成功导入 ${res.data} 个指标点`)
    importDialogVisible.value = false
    loadData()
  } catch (e) {
    showExcelImportError(e)
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.page-container {
  padding: var(--space-5);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-5);
  flex-wrap: wrap;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.page-header h3 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 700;
}

.major-select {
  width: 240px;
}

:deep(.major-select .el-select__wrapper) {
  min-height: 38px;
  border-radius: 999px;
  box-shadow: 0 0 0 1px #e6e1f0 inset;
}

.primary-pill-btn,
.add-indicator-btn {
  border-radius: 999px;
  font-weight: 600;
  box-shadow: 0 6px 14px rgba(126, 87, 194, 0.18);
}

/* 下载模板：次级描边按钮，与主操作区分 */
.template-pill-btn {
  color: #6f42c1;
  border-color: #d9c4ff;
  background: rgba(246, 240, 255, 0.72);
}

.template-pill-btn:hover {
  color: #fff;
  border-color: #6f42c1;
  background: linear-gradient(135deg, #9e89cd, #6f42c1);
}

.import-tip {
  margin-bottom: 16px;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.7;
  color: #6f42c1;
  background: rgba(128, 107, 191, 0.08);
  border: 1px solid rgba(128, 107, 191, 0.16);
}

.upload-tip {
  margin-top: 8px;
  color: var(--text-secondary, #909399);
  font-size: 12px;
}

.grad-req-card {
  padding: 20px 24px;
  border-radius: 24px;
  overflow: hidden;
}

.grad-req-collapse {
  border-top: none;
  border-bottom: none;
}

:deep(.grad-req-collapse .el-collapse-item__wrap) {
  border-bottom: none;
}

:deep(.grad-req-collapse .el-collapse-item__header) {
  min-height: 66px;
  border-bottom: none;
  background: transparent;
}

.grad-req-item {
  padding: 2px 0 18px;
  border-bottom: 1px solid #f4f1fa;
}

.grad-req-item:last-child {
  border-bottom: none;
}

.req-title {
  width: 100%;
  min-height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 18px 0 14px;
  margin-right: 8px;
  border-radius: 16px;
  background: linear-gradient(90deg, #f6f0ff 0%, #ffffff 62%, #fff7f8 100%);
  border: 1px solid #eee6fb;
  box-shadow: 0 6px 16px rgba(126, 87, 194, 0.08);
  transition: all 0.18s ease;
}
.req-title:hover {
  background: linear-gradient(90deg, #efe6ff 0%, #ffffff 60%, #fff0f2 100%);
  border-color: #dfd2f6;
  box-shadow: 0 8px 20px rgba(126, 87, 194, 0.12);
}
.req-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.req-tag {
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  color: #5aa9f5;
  background: linear-gradient(180deg, #eef8ff 0%, #e3f2ff 100%);
  border-color: #d3ebff;
  font-weight: 700;
}

.req-name {
  font-weight: 700;
  color: #3f3370;
}
.req-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-right: 18px;
  white-space: nowrap;
}

.indicator-section {
  padding: 14px 16px 4px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 700;
  color: #303133;
}

.indicator-table {
  width: 100%;
  border-radius: 14px;
  overflow: hidden;
}

:deep(.indicator-table .el-table__header th) {
  height: 44px;
  background-color: #f7f7fa;
  color: #606266;
  font-weight: 700;
}

:deep(.indicator-table .el-table__row td) {
  height: 52px;
}

:deep(.indicator-table .el-table__row:hover td) {
  background-color: #fbf8ff;
}

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

/* 毕业要求行按钮：用于折叠标题栏，突出一级对象操作 */
.req-action-btn {
  height: 30px;
  padding: 0 18px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 500;
}

:deep(.req-action-btn.el-button) {
  margin-left: 0;
}

.req-edit-btn {
  color: #8b5bd6;
  border-color: #d9c4ff;
  background: rgba(246, 240, 255, 0.72);
}

.req-edit-btn:hover {
  color: #6f42c1;
  border-color: #c6a7ff;
  background: #efe6ff;
}

.req-delete-btn {
  color: #fff;
  border-color: #ef9099;
  background: #ef9099;
}

.req-delete-btn:hover {
  color: #fff;
  border-color: #e78087;
  background: #e78087;
}

/* 指标点列表按钮：用于子表格，颜色和尺寸与毕业要求行区分 */
.indicator-action-btn {
  height: 30px;
  min-width: 72px;
  padding: 0 16px;
  border-radius: 15px;
  font-size: 13px;
  font-weight: 500;
}

:deep(.indicator-action-btn.el-button) {
  margin-left: 0;
}

.indicator-edit-btn {
  color: #5f83d6;
  border-color: #cddafd;
  background: #f2f5ff;
}

.indicator-edit-btn:hover {
  color: #456fd0;
  border-color: #b8c8fb;
  background: #eaf0ff;
}

.indicator-delete-btn {
  color: #e77c84;
  border-color: #f5c5ca;
  background: #fff4f5;
}

.indicator-delete-btn:hover {
  color: #fff;
  border-color: #ee8f97;
  background: #ee8f97;
}
</style>
