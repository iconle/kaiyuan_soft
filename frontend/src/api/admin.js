import request from '../utils/request'

// 用户管理
export function listUsers(params) {
  return request.get('/api/admin/users', { params })
}

export function getUser(id) {
  return request.get(`/api/admin/users/${id}`)
}

export function createUser(data) {
  return request.post('/api/admin/users', data)
}

export function updateUser(id, data) {
  return request.put(`/api/admin/users/${id}`, data)
}

export function disableUser(id) {
  return request.delete(`/api/admin/users/${id}`)
}

export function enableUser(id) {
  return request.put(`/api/admin/users/${id}/enable`)
}

export function deleteUser(id) {
  return request.delete(`/api/admin/users/${id}/remove`)
}

export function resetPassword(id, password) {
  return request.put(`/api/admin/users/${id}/reset-password`, { password })
}

export function listRoles() {
  return request.get('/api/admin/users/roles')
}

// 字典管理
export function listColleges() {
  return request.get('/api/dict/colleges')
}

export function createCollege(name) {
  return request.post('/api/dict/colleges', { name })
}

export function updateCollege(id, name) {
  return request.put(`/api/dict/colleges/${id}`, { name })
}

export function listMajors(params) {
  return request.get('/api/dict/majors', { params })
}

export function createMajor(data) {
  return request.post('/api/dict/majors', data)
}

export function updateMajor(id, data) {
  return request.put(`/api/dict/majors/${id}`, data)
}

export function listSemesters() {
  return request.get('/api/dict/semesters')
}

export function createSemester(data) {
  return request.post('/api/dict/semesters', data)
}

export function deleteSemester(id) {
  return request.delete(`/api/dict/semesters/${id}`)
}

// 学生管理
export function listStudents(params) {
  return request.get('/api/students', { params })
}

export function createStudent(data) {
  return request.post('/api/students', data)
}

export function updateStudent(id, data) {
  return request.put(`/api/students/${id}`, data)
}

export function deleteStudent(id) {
  return request.delete(`/api/students/${id}`)
}

// 成绩管理
export function unlockScoreSheet(sheetId) {
  return request.post(`/api/admin/scores/${sheetId}/unlock`)
}

// 行政班级管理
export function listAdminClasses(params) {
  return request.get('/api/dict/admin-classes', { params })
}

export function createAdminClass(data) {
  return request.post('/api/dict/admin-classes', data)
}

export function updateAdminClass(id, data) {
  return request.put(`/api/dict/admin-classes/${id}`, data)
}

export function deleteAdminClass(id) {
  return request.delete(`/api/dict/admin-classes/${id}`)
}

export function downloadAdminClassTemplate() {
  return request.get('/api/dict/admin-classes/import-template', {
    responseType: 'blob'
  })
}

export function importAdminClassExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/dict/admin-classes/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    skipErrorMessage: true
  })
}

export function getAdminClassStudents(classId) {
  return request.get(`/api/dict/admin-classes/${classId}/students`)
}

export function addAdminClassStudent(classId, studentId) {
  return request.post(`/api/dict/admin-classes/${classId}/students/${studentId}`)
}

export function removeAdminClassStudent(classId, studentId) {
  return request.delete(`/api/dict/admin-classes/${classId}/students/${studentId}`)
}

// 学生导入
export function downloadStudentTemplate() {
  return request.get('/api/students/template', {
    responseType: 'blob'
  })
}

export function importStudentExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/students/import-excel', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 教学班级管理（教务管理员）
export function listTeachingClasses(params) {
  return request.get('/api/teaching-classes', { params })
}

export function createTeachingClass(data) {
  return request.post('/api/teaching-classes', data)
}

export function updateTeachingClass(id, data) {
  return request.put(`/api/teaching-classes/${id}`, data)
}

export function deleteTeachingClass(id) {
  return request.delete(`/api/teaching-classes/${id}`)
}

export function downloadTeachingClassTemplate() {
  return request.get('/api/teaching-classes/import-template', {
    responseType: 'blob'
  })
}

export function importTeachingClassExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/teaching-classes/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    skipErrorMessage: true
  })
}

export function getTeachingClassStudents(classId) {
  return request.get(`/api/teaching-classes/${classId}/students`)
}

export function addTeachingClassStudent(classId, studentId) {
  return request.post(`/api/teaching-classes/${classId}/students/${studentId}`)
}

export function removeTeachingClassStudent(classId, studentId) {
  return request.delete(`/api/teaching-classes/${classId}/students/${studentId}`)
}
