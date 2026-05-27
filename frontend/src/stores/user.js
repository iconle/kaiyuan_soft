import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(null)
  const username = ref('')
  const realName = ref('')
  const roleCode = ref('')
  const roleName = ref('')

  function setLogin(data) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    realName.value = data.realName
    roleCode.value = data.roleCode
    roleName.value = data.roleName
    localStorage.setItem('token', data.token)
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    realName.value = ''
    roleCode.value = ''
    roleName.value = ''
    localStorage.removeItem('token')
  }

  return { token, userId, username, realName, roleCode, roleName, setLogin, logout }
})
