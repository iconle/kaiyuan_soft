<template>
  <AcademicGlobalDashboard
    title="专业宏观看板"
    description="查看负责专业的课程计算进度与专业级计算准备情况"
    :load-dashboard="getDashboard"
  />
</template>

<script setup>
import { getDashboard } from '../../api/director'
import AcademicGlobalDashboard from '../academic/GlobalDashboard.vue'
</script>
