<template>
  <el-tag :type="tagType" size="small">{{ label }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true }
})

const tagType = computed(() => {
  switch (props.status) {
    case 'LOCKED': return 'success'
    case 'IMPORTED': return 'warning'
    case 'EMPTY': return 'info'
    case 'DRAFT': return ''
    default: return 'info'
  }
})

const label = computed(() => {
  switch (props.status) {
    case 'LOCKED': return '已锁定'
    case 'IMPORTED': return '已导入'
    case 'EMPTY': return '未录入'
    case 'DRAFT': return '草稿'
    default: return props.status
  }
})
</script>
