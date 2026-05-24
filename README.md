# 面向专业认证的毕业要求达成度统一计算平台

基于 OBE（Outcome-Based Education）理念，为本科专业认证提供从"底层考核点题目得分"到"顶层毕业要求达成度"的全自动化三级计算引擎。彻底取代繁杂的手工 Excel 计算，实现数据可追溯、权重可校验、报告一键生成。

## 系统架构

采用**前后端分离的单体应用架构**，后端提供 RESTful API，前端为 Vue 3 SPA，通过 Nginx 反向代理统一对外服务。

| 层次 | 技术 | 说明 |
|------|------|------|
| 前端 | Vue 3 + Element Plus + Pinia + Vite + ECharts | 响应式 SPA，高密度数据表格，雷达图 |
| 后端 | Spring Boot 3 (JDK 17) + MyBatis-Plus | RESTful 服务，三级计算引擎 |
| 认证 | Spring Security + JWT + RBAC | 无状态 Token 认证，四角色权限 |
| 数据库 | MySQL 8.0+ | 关系型存储，HikariCP 连接池 |
| 部署 | Nginx + Docker Compose（可选） | 反向代理 / 一键容器编排 |

## 核心功能

### 四角色 RBAC 权限体系

| 角色 | 职责 |
|------|------|
| 系统管理员 | 全局字典配置、用户管理、学生管理、行政班级管理、成绩解锁 |
| 教务管理员 | 课程体系管理、教学班级管理、学生管理、宏观看板、专业级计算、成绩解锁 |
| 专业负责人 | 毕业要求与指标点管理、宏观支撑矩阵配置、宏观看板、专业级计算触发 |
| 主讲教师 | 课程大纲编制（目标/权重/考核点）、题目设置、成绩录入、课程级计算触发、勘误申请 |

### 三级达成度计算引擎

```
┌─────────────────────────────────────────────────────┐
│  第三级：专业级达成度（宏观总支撑权重 W）              │
│  G_k = Σ(E_k × W_c)                                │
├─────────────────────────────────────────────────────┤
│  第二级：课程级达成度（大纲内部贡献权重 w）            │
│  E_k = Σ(C̄_j × w_jk)                              │
├─────────────────────────────────────────────────────┤
│  第一级：课程目标级达成度（基于考核点题目得分）        │
│  C_ij = Σ(题目得分) / Σ(题目满分)                   │
└─────────────────────────────────────────────────────┘
```

- **考核点题目细分**：一个考核点可拆分为多道题目，每题单独绑定目标、单独打分
- **两级权重归一化**：微观权重 $w$ 与宏观权重 $W$ 均需 Σ = 1.0，系统实时校验
- **两阶段触发机制**：阶段一（教师）→ 课程级锁定；阶段二（专业负责人/教务）→ 专业级汇总
- **成绩勘误工单**：教师提交 → 教务审核 → 管理员解锁，全流程可追溯

## 项目结构

```
course-oss/
├── docs/
│   ├── design/              # 系统架构设计文档
│   ├── database/            # 数据库脚本（init + 全量数据）
│   ├── analyse/             # 核心计算分析文档
│   ├── material/            # 软件需求规格说明书
│   ├── verification/        # 计算验证报告
│   ├── deploy/              # 项目部署文档
│   └── report/              # 开发报告
├── frontend/                # Vue 3 前端
│   └── src/
│       ├── api/             # API 请求封装（按角色拆分）
│       ├── components/      # 公共组件（StatusTag 等）
│       ├── layouts/         # AdminLayout（侧边栏 + 路由视图）
│       ├── router/          # 路由配置 + 守卫
│       ├── stores/          # Pinia 状态（用户信息）
│       ├── utils/           # request.js（Axios 封装）
│       └── views/           # 页面视图（按角色组织）
├── backend/                 # Spring Boot 3 后端
│   └── src/main/java/com/obe/platform/
│       ├── common/          # Result, PageResult, BizException
│       ├── config/          # Security, JWT, CORS, MyBatis-Plus
│       ├── security/        # JWT 认证与 RBAC 鉴权
│       ├── engine/          # 三级计算引擎（Level1/2/3 + WeightValidator）
│       ├── modulea/         # 基础数据 + 宏观矩阵 + 教学班级
│       ├── moduleb/         # 课程大纲 + 考核点 + 题目
│       ├── modulec/         # 成绩录入 + 课程/专业级计算 + 勘误工单
│       └── moduled/         # 报表导出（PDF/Excel/穿透式台账）
├── nginx/                   # Nginx 配置文件
├── docker-compose.yml       # Docker 编排文件
└── README.md
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.9+

### 数据库初始化

```bash
mysql -u root -p < sql/obe_platform.sql
```

### 本地开发

```bash
# 后端
cd backend && mvn spring-boot:run

# 前端
cd frontend && npm install && npm run dev
```

访问 `http://localhost:5173`，默认管理员账号 `admin` / `admin123`。

## 文档导航

| 文档 | 说明 |
|------|------|
| [软件需求规格说明书](docs/material/软件需求规格说明书.md) | 业务边界、角色权限、功能需求 |
| [系统架构设计](docs/design/系统架构设计.md) | 总体架构、前后端设计、数据库、API、安全 |
| [核心计算分析](docs/analyse/核心计算分析.md) | 三级计算公式推演、两阶段触发机制 |
| [项目部署文档](docs/deploy/项目部署文档.md) | 传统部署 + Docker Compose 部署 |
| [计算验证报告](docs/verification/) | 多次全量数据手工验算报告 |

## 许可证

本项目仅供学习与教学研究使用。
