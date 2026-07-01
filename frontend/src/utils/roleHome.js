export function getHomePath(roleCode, options = {}) {
  const activeClassId = options.activeClassId ?? localStorage.getItem('activeClassId')

  switch (roleCode) {
    case 'ADMIN':
      return '/admin/users'
    case 'ACADEMIC':
      return '/academic/courses'
    case 'DIRECTOR':
      return '/director/grad-req'
    case 'TEACHER':
      return activeClassId ? `/teacher/${activeClassId}/objectives` : null
    default:
      return null
  }
}

export function getTeacherHomePath(classId) {
  return classId ? `/teacher/${classId}/objectives` : null
}