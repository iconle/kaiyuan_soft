import { ElMessage, ElMessageBox } from 'element-plus'

export const DEFAULT_EXCEL_MAX_SIZE_MB = 10

export function validateExcelFile(file, options = {}) {
  const maxSizeMb = options.maxSizeMb || DEFAULT_EXCEL_MAX_SIZE_MB
  const fileName = file?.name || ''
  const isXlsx = fileName.toLowerCase().endsWith('.xlsx')

  if (!file) {
    ElMessage.warning('请选择要导入的 Excel 文件')
    return false
  }

  if (!isXlsx) {
    ElMessage.error('仅支持 .xlsx 格式文件，请先下载模板并按模板填写后再上传')
    return false
  }

  if (file.size > maxSizeMb * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${maxSizeMb}MB，请精简数据后重新上传`)
    return false
  }

  return true
}

export function getExcelUploadTip(options = {}) {
  const maxSizeMb = options.maxSizeMb || DEFAULT_EXCEL_MAX_SIZE_MB
  return `仅支持 .xlsx 格式，文件大小不超过 ${maxSizeMb}MB。请先下载模板，保持表头不变后再导入。`
}

export function showExcelImportError(error, fallback = '导入失败，请检查模板格式后重试') {
  const message = error?.response?.data?.message ||
    error?.response?.data?.msg ||
    error?.message ||
    fallback

  ElMessageBox.alert(escapeHtml(message).replace(/\n/g, '<br>'), '导入失败', {
    confirmButtonText: '我知道了',
    dangerouslyUseHTMLString: true,
    type: 'error'
  })
}

export function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
