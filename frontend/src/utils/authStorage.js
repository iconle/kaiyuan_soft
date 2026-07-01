const AUTH_STORAGE_KEYS = [
  'token',
  'userId',
  'username',
  'realName',
  'roleCode',
  'roleName',
  'activeClassId'
]

export function clearAuthStorage() {
  AUTH_STORAGE_KEYS.forEach(key => {
    localStorage.removeItem(key)
  })
}