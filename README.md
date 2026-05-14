# 毕业要求达成度统一计算平台

![Java](https://img.shields.io/badge/Java-17+-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)
![Vue](https://img.shields.io/badge/Vue-3.3+-green.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

## 📋 项目简介

本系统旨在为本科工程教育认证与师范类专业认证提供高精度、可追溯的数据计算支撑。通过结构化的数据流转，实现从"底层考核点原始成绩录入"到"顶层毕业要求达成度输出"的全自动化计算。

### 核心特性

✅ **全自动化计算**: 摒弃手工Excel，三层级自动计算  
✅ **数据可追溯**: 支持穿透式查询，从指标点追溯到具体学生成绩  
✅ **双层权重**: 宏观矩阵与微观大纲相分离，权重灵活配置  
✅ **权重校验**: 实时校验权重总和，确保数据完整性  
✅ **角色权限**: 基于RBAC的四类用户权限管理  
✅ **Excel支持**: 高效导入导出，支持复合表头  

## 🏗️ 系统架构

```
┌─────────────────────────────────────┐
│   Vue 3 SPA (前端)                 │
├─────────────────────────────────────┤
│   Spring Boot 3 REST API (后端)    │
├─────────────────────────────────────┤
│   MyBatis-Plus + JPA (数据访问)     │
├─────────────────────────────────────┤
│   MySQL 8.0 (关系型数据库)          │
└─────────────────────────────────────┘
```

## 📦 项目结构

```
achievement-calc-platform/
├── backend/                 # Java后端项目
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── pom.xml
├── frontend/                # Vue前端项目
│   ├── src/
│   ├── package.json
│   └── vite.config.js
├── docs/                    # 项目文档
│   ├── architecture/        # 架构设计
│   ├── database/            # 数据库设计
│   └── design/              # 设计规范
├── docker-compose.yml       # Docker编排
└── CLAUDE.md               # 项目说明
```

## 🚀 快速开始

### 前置要求

- JDK 17+
- Node.js 16+ & npm
- MySQL 8.0+
- Docker & Docker Compose (可选)

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p < backend/src/main/resources/sql/init_schema.sql

# 导入初始数据
mysql -u root -p achievement_calc < backend/src/main/resources/sql/init_data.sql
```

### 2. 后端启动

```bash
cd backend

# 编译项目
mvn clean package

# 运行应用
mvn spring-boot:run

# 或直接运行JAR
java -jar target/achievement-calc-platform-1.0.0-SNAPSHOT.jar
```

后端API将运行在 `http://localhost:8080/api`

### 3. 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 生产构建
npm run build
```

前端应用将运行在 `http://localhost:5173`

### 4. Docker一键启动 (推荐)

```bash
# 使用docker-compose启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## 📊 核心计算模型

### 三层级达成度计算

#### 第一级：课程目标级达成度
基于考核点得分计算：
```
C_ij = Σ(支撑目标j的考核点实际得分) / Σ(支撑目标j的考核点满分)
班级目标达成度 = 全体学生C_ij的平均值
```

#### 第二级：课程级达成度
结合大纲内部贡献权重 (w)：
```
E_k = Σ(C_j × w_jk)
约束：Σw_jk = 1.0
```

#### 第三级：专业级达成度
结合宏观支撑权重 (W)：
```
G_k = Σ(E_k × W_c)
约束：ΣW_c = 1.0
```

## 👥 用户角色

| 角色 | 职责 | 权限 |
|------|------|------|
| **系统管理员** | 系统初始化、权限管理、字典维护 | 全系统管理 |
| **教务管理员** | 数据导入、班级管理、全局计算 | 数据管理、全局计算 |
| **专业负责人** | 毕业要求定义、支撑矩阵配置 | 专业级数据管理 |
| **课程教师** | 课程大纲、成绩导入、课程计算 | 课程级数据管理 |

## 🔑 主要功能模块

### 模块A：基础数据管理
- [x] 全局数据字典设定
- [x] 毕业要求管理
- [x] 课程库与学生名单导入
- [x] 支撑矩阵配置与权重校验

### 模块B：课程大纲管理
- [x] 课程目标设定
- [x] 内部权重分配与校验
- [x] 考核点细分与映射

### 模块C：成绩处理与计算
- [x] Excel模板动态生成
- [x] 原始成绩导入与验证
- [x] 课程级达成度计算
- [x] 专业级全局计算

### 模块D：报表导出
- [x] 课程级评价报表
- [x] 专业级穿透式台账

## 📚 文档

- [系统架构设计](./docs/architecture/01_system_architecture.md)
- [数据库设计](./docs/database/01_db_design.md)
- [任务分配规划](./docs/design/02_task_allocation.md)
- [需求规格说明书](./作业要求/软件需求规格说明书.md)

## 🛠️ 开发指南

### API文档

后端启动后，可访问Swagger UI进行API文档查看：
```
http://localhost:8080/api/swagger-ui.html
```

### 代码规范

- **后端**: 遵循Google Java Style Guide
- **前端**: 遵循Vue.js官方风格指南
- **数据库**: 使用小写蛇形命名

### Git工作流

```bash
# 创建特性分支
git checkout -b feature/TASK-ID-description

# 提交代码
git add .
git commit -m "[TASK-ID] 功能描述"

# 推送并创建Pull Request
git push origin feature/TASK-ID-description
```

## 🧪 测试

```bash
# 后端单元测试
cd backend
mvn test

# 前端单元测试
cd frontend
npm run test
```

## 📈 性能要求

| 指标 | 目标 | 说明 |
|------|------|------|
| 学生规模 | ≤1000人 | 单专业学生规模 |
| 课程规模 | ≤100门 | 单专业课程规模 |
| 考核点 | ≤1000个 | 所有课程考核点总数 |
| Excel导入 | <10s | 百行级别数据导入 |
| 专业级计算 | <5s | 全局计算执行时间 |
| 页面加载 | <2s | 首屏加载时间 |

## 🔒 安全性

- JWT Token认证与授权
- 基于RBAC的权限管理
- 成绩数据加密存储
- 完整的操作审计日志
- SQL注入防护

## 🌐 部署

### 本地开发环境
见"快速开始"章节

### 云服务器部署

```bash
# 构建Docker镜像
docker build -f backend/Dockerfile -t achievement-calc-backend .
docker build -f frontend/Dockerfile -t achievement-calc-frontend .

# 推送到镜像仓库
docker push achievement-calc-backend:latest
docker push achievement-calc-frontend:latest

# 云服务器上使用docker-compose部署
docker-compose -f docker-compose.prod.yml up -d
```

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交代码 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 📋 许可证

MIT License

## ✉️ 联系方式

**开发团队**: 查琪乐、邱志斌、邱嘉堃、褚茂誉、吴思雨、闫皇冲、李伟鹏、刘辉艺
**开发周期**: 2026年5月-7月  
**最后更新**: 2026年5月14日
