# 面向专业认证的毕业要求达成度统一计算平台

> 基于 OBE（Outcome-Based Education，成果导向教育）理念，为本科专业认证提供从"底层考核点题目得分"到"顶层毕业要求达成度"的全自动化三级计算引擎。

[![Java](https://img.shields.io/badge/Java-17+-brightgreen.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4+-brightgreen.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 项目简介

本平台旨在解决高校专业认证工作中毕业要求达成度计算繁杂、易错、难以追溯的痛点问题。通过构建精确的三级计算引擎，实现从课程考核得分到专业级达成度的自动化流转，彻底取代传统手工 Excel 计算模式。

### 核心价值

- **精确计算**：三级联动计算引擎，权重归一化自动校验，确保计算结果准确可靠
- **流程可控**：两阶段触发机制，成绩锁定/解锁流程，勘误工单审批，全流程可追溯
- **数据可视**：雷达图展示达成度分布，穿透式台账支持逐层溯源
- **报告一键生成**：支持 PDF/Excel 多格式导出，满足认证材料申报需求

---

## 技术架构

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         用户层                                │
│  系统管理员  |  教务管理员  |  专业负责人  |  主讲教师         │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      接入层 (Nginx)                           │
│              反向代理 | 静态资源 | 跨域处理                   │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────────────┐   ┌────────────────┐   ┌────────────────┐
│   前端服务     │   │   后端服务     │   │   数据库服务   │
│               │   │                │   │                │
│  Vue 3 SPA    │◄──┤  Spring Boot   │◄──┤   MySQL 8.0    │
│  Element Plus │   │  RESTful API   │   │  关系型存储    │
│  Pinia        │   │  JWT + RBAC    │   │                │
│  ECharts      │   │  计算引擎      │   │                │
└───────────────┘   └────────────────┘   └────────────────┘
```

### 技术栈清单

| 分类 | 技术 | 版本 | 说明 |
|:----:|:-----|:----:|:-----|
| **前端** | Vue.js | 3.4+ | 渐进式响应式框架 |
| | Element Plus | 2.7+ | Vue 3 UI 组件库 |
| | Pinia | 2.x | 状态管理 |
| | Vite | 5.x | 下一代前端构建工具 |
| | ECharts | 5.5 | 数据可视化图表库 |
| | Axios | 1.x | HTTP 请求库 |
| **后端** | Spring Boot | 3.2.5 | Java 应用框架 |
| | JDK | 17 | Java 运行环境 |
| | MyBatis-Plus | 3.5.6 | 持久层增强框架 |
| | Spring Security | 6.x | 认证与授权框架 |
| | JJWT | 0.12.5 | JWT 令牌处理 |
| | EasyExcel | 3.3.4 | Excel 读写库 |
| | PDFBox | 3.0.1 | PDF 文档生成 |
| | SpringDoc | 2.5.0 | API 文档生成 |
| **数据库** | MySQL | 8.0+ | 关系型数据库 |
| **部署** | Nginx | Latest | 反向代理服务器 |
| | Docker Compose | Latest | 容器编排工具 |

---

## 核心功能

### 四角色 RBAC 权限体系

| 角色 | 职责范围 | 核心功能 |
|:----:|:---------|:---------|
| **系统管理员** | 全局配置与用户管理 | • 用户账号管理<br>• 学生信息管理<br>• 字典配置管理<br>• 行政班级管理<br>• 全局成绩解锁 |
| **教务管理员** | 课程体系与教学管理 | • 课程导入与维护<br>• 教学班级管理<br>• 宏观看板统计<br>• 专业级计算触发<br>• 成绩解锁审批 |
| **专业负责人** | 毕业要求与支撑矩阵 | • 毕业要求管理<br>• 指标点管理<br>• 宏观支撑矩阵配置<br>• 宏观看板查看<br>• 专业级计算触发 |
| **主讲教师** | 课程实施与成绩管理 | • 课程大纲编制<br>• 课程目标与权重设置<br>• 考核点与题目管理<br>• 成绩录入与导入<br>• 课程级计算触发<br>• 勘误工单提交 |

### 三级达成度计算引擎

```
┌─────────────────────────────────────────────────────────────────┐
│                    第三级：专业级达成度                           │
│              G_k = Σ(E_k × W_c)  [宏观总支撑权重 W]                │
├─────────────────────────────────────────────────────────────────┤
│                    第二级：课程级达成度                           │
│              E_k = Σ(C̄_j × w_jk)  [大纲内部贡献权重 w]             │
├─────────────────────────────────────────────────────────────────┤
│                    第一级：课程目标级达成度                       │
│              C_ij = Σ(题目得分) / Σ(题目满分)                     │
└─────────────────────────────────────────────────────────────────┘
```

#### 计算特性

- **考核点题目细分**：一个考核点可拆分为多道题目，每题单独绑定目标、单独打分
- **两级权重归一化**：微观权重（大纲内）$w$ 与宏观权重（总矩阵）$W$ 均需 Σ = 1.0，系统实时校验
- **两阶段触发机制**：
  - 阶段一：主讲教师触发课程级计算，计算后课程成绩锁定
  - 阶段二：专业负责人/教务管理员触发专业级计算，汇总所有课程数据
- **成绩勘误工单**：教师提交勘误申请 → 教务审核 → 管理员解锁 → 修正后重新计算

---

## 项目结构

```
kaiyuan_soft/
├── docs/                          # 项目文档
│   ├── design/                    # 系统架构设计文档
│   ├── database/                  # 数据库脚本
│   ├── analyse/                   # 核心计算分析文档
│   ├── material/                  # 软件需求规格说明书
│   ├── verification/              # 计算验证报告
│   ├── deploy/                    # 项目部署文档
│   └── report/                    # 开发报告
│
├── frontend/                      # Vue 3 前端应用
│   ├── src/
│   │   ├── api/                   # API 请求封装（按角色拆分）
│   │   ├── components/            # 公共组件（StatusTag、表格组件等）
│   │   ├── layouts/               # 页面布局（AdminLayout 侧边栏 + 路由视图）
│   │   ├── router/                # 路由配置 + 守卫
│   │   ├── stores/                # Pinia 状态管理（用户信息）
│   │   ├── utils/                 # 工具类（request.js Axios 封装）
│   │   └── views/                 # 页面视图（按角色组织）
│   │       ├── admin/             # 系统管理员视图
│   │       ├── academic/          # 教务管理员视图
│   │       ├── director/          # 专业负责人视图
│   │       └── teacher/           # 主讲教师视图
│   ├── package.json
│   └── vite.config.js
│
├── backend/                       # Spring Boot 3 后端服务
│   └── src/main/java/com/obe/platform/
│       ├── common/                # 通用组件（Result、PageResult、BizException）
│       ├── config/                # 配置类（Security、JWT、CORS、MyBatis-Plus）
│       ├── security/              # 安全认证（JWT 认证与 RBAC 鉴权）
│       ├── engine/               # 三级计算引擎（Level1/2/3 + WeightValidator）
│       ├── modulea/              # 基础数据 + 宏观矩阵 + 教学班级
│       ├── moduleb/              # 课程大纲 + 考核点 + 题目
│       ├── modulec/              # 成绩录入 + 课程/专业级计算 + 勘误工单
│       ├── moduled/              # 报表导出（PDF/Excel/穿透式台账）
│       ├── controller/           # REST API 控制器
│       ├── service/              # 业务逻辑层
│       ├── mapper/               # MyBatis 数据访问层
│       └── entity/               # 实体类
│   └── pom.xml
│
├── sql/                           # 数据库初始化脚本
│   └── obe_platform.sql          # 完整数据库结构与初始数据
│
├── nginx/                         # Nginx 配置文件
│   └── obe-platform.conf         # 反向代理配置
│
├── docker-compose.yml             # Docker Compose 编排文件
├── README.md                      # 项目说明文档
└── 审计报告.md                    # 系统审计报告
```

---

## 快速开始

### 环境要求

| 软件 | 版本要求 | 说明 |
|:----|:---------|:-----|
| JDK | 17+ | 后端运行环境 |
| Node.js | 18+ | 前端开发环境 |
| MySQL | 8.0+ | 数据库服务 |
| Maven | 3.9+ | 后端构建工具 |

### 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE obe_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入数据结构
mysql -u root -p obe_platform < sql/obe_platform.sql
```

### 后端启动

```bash
# 进入后端目录
cd backend

# Maven 编译打包
mvn clean package -DskipTests

# 运行服务
java -jar target/obe-platform-1.0.0.jar

# 或使用 Spring Boot Maven 插件
mvn spring-boot:run
```

后端服务默认运行在 `http://localhost:8080`

API 文档访问：`http://localhost:8080/swagger-ui.html`

### 前端启动

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 生产构建
npm run build
```

前端开发服务默认运行于 `http://localhost:5173`

### Docker 部署（推荐）

```bash
# 一键启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

### 默认账号

| 角色 | 账号 | 密码 |
|:----|:----|:----|
| 系统管理员 | admin | 123456 |
| 教务管理员 | academic01 | 123456 |
| 专业负责人 | director01 | 123456 |
| 主讲教师 | teacher_wang | 123456 |
| 主讲教师 | teacher_zhao | 123456 |

> ⚠️ **安全警告**
>
> 以上账号仅用于本地开发与功能演示。生产环境部署时**必须**：
>
> 1. 修改所有默认账号密码（建议 12 位以上，含大小写字母 + 数字 + 符号）
> 2. 删除测试角色账号（如 `teacher_wang` / `teacher_zhao`）
> 3. 通过 SQL 直接更新数据库中默认账号的 BCrypt 密码哈希，例如：
>    ```sql
>    -- 生成新的 BCrypt 哈希（在 Spring Shell / 任何支持 BCrypt 的工具中）
>    -- 然后 update sys_user set password = '<新哈希>' where username = 'admin';
>    ```
> 4. 限制数据库与后端 API 仅在内网或 VPN 后访问

---

## 在线访问

- **校内访问**：http://10.65.199.3:5173 （仅校园网内可访问）
- **公网 Demo**：暂未提供，可参考 `docs/deploy/项目部署文档.md` 自行部署
- **测试账号**：同上（统一密码：123456）

---

## 文档导航

| 文档 | 说明 | 链接 |
|:----|:-----|:----|
| 软件需求规格说明书 | 业务边界、角色权限、功能需求 | [查看](docs/material/软件需求规格说明书.md) |
| 系统架构设计 | 总体架构、前后端设计、数据库、API、安全 | [查看](docs/pic/系统架构设计.md) |
| 核心计算分析 | 三级计算公式推演、两阶段触发机制 | [查看](docs/analyse/核心计算分析.md) |
| 项目部署文档 | 传统部署 + Docker Compose 部署 | [查看](docs/deploy/项目部署文档.md) |
| 计算验证报告 | 多次全量数据手工验算报告 | [查看](docs/verification/) |
| 审计报告 | 系统功能审计与验收报告 | [查看](审计报告.md) |

---

## 计算公式说明

### 第一级：课程目标达成度

对于第 $i$ 个学生、第 $j$ 个课程目标：

$$C_{ij} = \frac{\sum_{k=1}^{n} s_{ijk}}{\sum_{k=1}^{n} f_{ijk}}$$

其中：
- $s_{ijk}$：学生 $i$ 在目标 $j$ 对应题目 $k$ 的得分
- $f_{ijk}$：题目 $k$ 的满分

### 第二级：课程达成度

$$E_k = \sum_{j=1}^{m} \bar{C}_j \times w_{jk}$$

其中：
- $\bar{C}_j$：课程目标 $j$ 的班级平均达成度
- $w_{jk}$：目标 $j$ 对毕业要求 $k$ 的内部贡献权重

### 第三级：专业级达成度

$$G_k = \sum_{c=1}^{p} E_c^{(k)} \times W_c^{(k)}$$

其中：
- $E_c^{(k)}$：课程 $c$ 对毕业要求 $k$ 的达成度
- $W_c^{(k)}$：课程 $c$ 对毕业要求 $k$ 的宏观支撑权重

---

## 开发规范

### 代码风格

- 后端：遵循阿里巴巴 Java 开发规范
- 前端：遵循 Vue 3 风格指南
- Git 提交：遵循 Conventional Commits 规范

### 分支策略

- `main`：生产环境分支
- `develop`：开发环境分支
- `feature/*`：功能开发分支
- `bugfix/*`：缺陷修复分支

---

## 许可证

MIT License

本项目仅供学习与教学研究使用。

---

## 联系方式

如有问题或建议，欢迎通过以下方式联系：

- 提交 Issue
- 发起 Pull Request

---

**感谢使用面向专业认证的毕业要求达成度统一计算平台！**
