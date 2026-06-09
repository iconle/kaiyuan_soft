import request from '../utils/request'

// 课程目标
export function listObjectives(classId) {
  return request.get(`/api/classes/${classId}/objectives`)
}

export function createObjective(classId, data) {
  return request.post(`/api/classes/${classId}/objectives`, data)
}

export function updateObjective(id, data) {
  return request.put(`/api/classes/0/objectives/${id}`, data)
}

export function deleteObjective(classId, id) {
  return request.delete(`/api/classes/${classId}/objectives/${id}`)
}

// 内部权重
export function getWeights(classId) {
  return request.get(`/api/classes/${classId}/weights`)
}

export function updateWeights(classId, data) {
  return request.put(`/api/classes/${classId}/weights`, data)
}

export function getSupportedIndicators(classId) {
  return request.get(`/api/classes/${classId}/weights/supported-indicators`)
}

// 考核点
export function listAssessments(classId) {
  return request.get(`/api/classes/${classId}/assessments`)
}

export function createAssessment(classId, data) {
  return request.post(`/api/classes/${classId}/assessments`, data)
}

export function updateAssessment(id, data) {
  return request.put(`/api/classes/0/assessments/${id}`, data)
}

export function deleteAssessment(classId, id) {
  return request.delete(`/api/classes/${classId}/assessments/${id}`)
}

// 成绩模板
export function downloadScoreTemplate(classId) {
  return request.get(`/api/classes/${classId}/score-template`, { responseType: 'blob' })
}

// 成绩录入与管理
export function uploadScores(classId, formData) {
  return request.post(`/api/classes/${classId}/scores/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getScores(classId) {
  return request.get(`/api/classes/${classId}/scores`)
}

export function updateScore(classId, data) {
  return request.put(`/api/classes/${classId}/scores`, data)
}

export function getScoreStatus(classId) {
  return request.get(`/api/classes/${classId}/score-status`)
}

// 课程级计算
export function triggerCourseCompute(classId, operator) {
  return request.post(`/api/classes/${classId}/compute?operator=${operator}`)
}

export function getCourseComputeResults(classId) {
  return request.get(`/api/classes/${classId}/compute/results`)
}

// 个人达成度
export function listPersonalAchievements(classId) {
  return request.get(`/api/classes/${classId}/personal-achievements`)
}

export function getPersonalAchievement(classId, studentId) {
  return request.get(`/api/classes/${classId}/personal-achievements/${studentId}`)
}

export function downloadPersonalAchievements(classId) {
  return request.get(`/api/classes/${classId}/personal-achievements/excel`, {
    responseType: 'blob'
  })
}

// 报表
export function getCourseReport(classId) {
  return request.get(`/api/reports/course/${classId}`)
}

export function downloadCoursePdf(classId) {
  return request.get(`/api/reports/course/${classId}/pdf`, { responseType: 'blob' })
}

export function downloadCourseExcel(classId) {
  return request.get(`/api/reports/course/${classId}/excel`, { responseType: 'blob' })
}

// 字典（复用 admin 接口）
export { listColleges, listMajors, listSemesters } from './admin'
