import request from '../utils/request'

// 课程管理
export function listCourses(params) {
  return request.get('/api/courses', { params })
}

export function deleteCourse(id) {
  return request.delete(`/api/courses/${id}`)
}

export function listClasses(courseId) {
  return request.get(`/api/courses/${courseId}/classes`)
}

export function createClass(courseId, data) {
  return request.post(`/api/courses/${courseId}/classes`, data)
}

// 学生管理
export function listStudents(params) {
  return request.get('/api/students', { params })
}

export function importStudents(data) {
  return request.post('/api/students/import', data)
}

export function getStudentsByClass(classId) {
  return request.get(`/api/students/class/${classId}`)
}

// 字典（复用 admin 接口）
export { listSemesters } from './admin'

// 宏观看板与报表（与专业负责人共享）
export function getDashboard(majorId) {
  return request.get('/api/global/dashboard', { params: { majorId } })
}

export function getMajorReport(majorId, semesterId) {
  return request.get('/api/reports/major', { params: { majorId, semesterId } })
}

export function downloadMajorExcel(majorId, semesterId) {
  return request.get('/api/reports/major/excel', {
    params: { majorId, semesterId },
    responseType: 'blob'
  })
}

// 导入课程Excel (已有功能，补充参数说明)
export function importCourses(formData) {
  return request.post('/api/courses/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
