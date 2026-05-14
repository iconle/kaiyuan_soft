<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-number">8</div>
            <div class="stat-label">毕业要求</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-number">24</div>
            <div class="stat-label">核心课程</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-number">182</div>
            <div class="stat-label">在读学生</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-number">85%</div>
            <div class="stat-label">平均达成度</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <div class="card-title">计算进度</div>
          </template>
          <div class="progress-list">
            <div class="progress-item">
              <span>数据结构课程</span>
              <el-progress :percentage="100" />
            </div>
            <div class="progress-item">
              <span>离散数学课程</span>
              <el-progress :percentage="85" />
            </div>
            <div class="progress-item">
              <span>操作系统课程</span>
              <el-progress :percentage="60" />
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <div class="card-title">最近活动</div>
          </template>
          <el-timeline>
            <el-timeline-item timestamp="2026-05-14 10:30" placement="top">
              <p>已锁定《数据结构》课程成绩</p>
            </el-timeline-item>
            <el-timeline-item timestamp="2026-05-13 15:45" placement="top">
              <p>导入了100名学生数据</p>
            </el-timeline-item>
            <el-timeline-item timestamp="2026-05-12 09:20" placement="top">
              <p>配置了支撑矩阵权重</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.15);
}

.stat-content {
  padding: 20px 0;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #282c34;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 10px;
}

.card-title {
  font-weight: bold;
  color: #282c34;
}

.progress-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.progress-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.progress-item span {
  font-size: 12px;
  color: #606266;
}
</style>

