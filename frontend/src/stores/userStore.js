import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userName = ref('')
  const roles = ref([])
  const permissions = ref([])

  const isAuthenticated = computed(() => !!token.value)

  const setUser = (user) => {
    token.value = user.token
    userName.value = user.userName
    roles.value = user.roles || []
    permissions.value = user.permissions || []
  }

  const logout = () => {
    token.value = ''
    userName.value = ''
    roles.value = []
    permissions.value = []
  }

  return {
    token,
    userName,
    roles,
    permissions,
    isAuthenticated,
    setUser,
    logout
  }
}, {
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'user_store',
        storage: localStorage,
        paths: ['token', 'userName', 'roles']
      }
    ]
  }
})
