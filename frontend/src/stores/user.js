import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const storedUserId = localStorage.getItem('userId')
  const userId = ref(storedUserId ? Number(storedUserId) : null)
  const username = ref(localStorage.getItem('username') || '')
  const realName = ref(localStorage.getItem('realName') || '')
  const roleCode = ref(localStorage.getItem('roleCode') || '')
  const roleName = ref(localStorage.getItem('roleName') || '')

  function setUserInfo(data) {
    userId.value = data.userId
    username.value = data.username
    realName.value = data.realName
    roleCode.value = data.roleCode
    roleName.value = data.roleName
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('username', data.username)
    localStorage.setItem('realName', data.realName)
    localStorage.setItem('roleCode', data.roleCode)
    localStorage.setItem('roleName', data.roleName)
  }

  function setLogin(data) {
    token.value = data.token
    localStorage.setItem('token', data.token)
    setUserInfo(data)
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    realName.value = ''
    roleCode.value = ''
    roleName.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('realName')
    localStorage.removeItem('roleCode')
    localStorage.removeItem('roleName')
  }

  return { token, userId, username, realName, roleCode, roleName, setLogin, setUserInfo, logout }
})
