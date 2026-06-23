<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { getCurrentUser } from './api/auth'
import { useUserStore } from './stores/user'

const userStore = useUserStore()

onMounted(async () => {
  if (!userStore.token) return
  try {
    const res = await getCurrentUser()
    userStore.setUserInfo(res.data)
  } catch (e) {
    const code = e.response?.data?.code || e.response?.status
    if (code === 401 || code === 403) {
      userStore.logout()
    }
  }
})
</script>
