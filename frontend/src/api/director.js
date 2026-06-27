import request from '../utils/request'

// 毕业要求管理
export function listGradReqs(majorId) {
  return request.get('/api/grad-req', { params: { majorId } })
}

export function createGradReq(data) {
  return request.post('/api/grad-req', data)
}

export function updateGradReq(id, data) {
  return request.put(`/api/grad-req/${id}`, data)
}

export function deleteGradReq(id) {
  return request.delete(`/api/grad-req/${id}`)
}

export function addIndicator(gradReqId, data) {
  return request.post(`/api/grad-req/${gradReqId}/indicators`, data)
}

export function updateIndicator(id, data) {
  return request.put(`/api/grad-req/indicators/${id}`, data)
}

export function deleteIndicator(id) {
  return request.delete(`/api/grad-req/indicators/${id}`)
}

// 指标点批量导入
export function downloadIndicatorTemplate(majorId) {
  return request.get('/api/grad-req/indicator-template', {
    params: { majorId },
    responseType: 'blob'
  })
}

export function importIndicators(majorId, file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/grad-req/indicators/import', form, {
    params: { majorId },
    skipErrorMessage: true
  })
}

// 宏观支撑矩阵
export function getMacroMatrix(majorId) {
  return request.get('/api/macro-matrix', { params: { majorId } })
}

export function updateMacroMatrix(data) {
  return request.put('/api/macro-matrix', data)
}

export function getSupportedIndicators(courseId) {
  return request.get(`/api/macro-matrix/course/${courseId}/supported-indicators`)
}

// 课程支撑矩阵批量导入
export function downloadMacroMatrixTemplate(majorId) {
  return request.get('/api/macro-matrix/template', {
    params: { majorId },
    responseType: 'blob'
  })
}

export function importMacroMatrix(majorId, file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/macro-matrix/import', form, {
    params: { majorId },
    skipErrorMessage: true
  })
}

// 全局计算
export function getDashboard(majorId) {
  return request.get('/api/global/dashboard', { params: { majorId } })
}

export function triggerGlobalCompute(majorId, semesterId, operator) {
  return request.post('/api/global/compute', null, { params: { majorId, semesterId, operator } })
}

export function getGlobalResults(majorId, semesterId) {
  return request.get('/api/global/results', { params: { majorId, semesterId } })
}

export function listMajorPersonalAchievements(majorId, semesterId, indicatorId) {
  return request.get('/api/global/personal-achievements', {
    params: { majorId, semesterId, indicatorId }
  })
}

// 报表
export function getMajorReport(majorId, semesterId) {
  return request.get('/api/reports/major', { params: { majorId, semesterId } })
}

export function getRadarData(majorId, semesterId) {
  return request.get('/api/reports/major/radar', { params: { majorId, semesterId } })
}

export function downloadMajorExcel(majorId, semesterId) {
  return request.get('/api/reports/major/excel', {
    params: { majorId, semesterId },
    responseType: 'blob'
  })
}
