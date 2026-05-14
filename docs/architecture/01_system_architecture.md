# 系统架构设计文档

## 1. 系统架构总体设计

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Layer (浏览器)                      │
├─────────────────────────────────────────────────────────────┤
│  Vue 3 SPA (Frontend at :5173)                               │
│  ├─ Authentication Module                                   │
│  ├─ Dashboard & Monitoring                                  │
│  ├─ Data Management (Courses, Students, Requirements)       │
│  ├─ Matrix Configuration (Support Matrix)                   │
│  ├─ Syllabus & Assessment Management                        │
│  └─ Score Entry & Calculation Trigger                       │
├─────────────────────────────────────────────────────────────┤
│                    API Gateway Layer                         │
│  (Spring Security, CORS, Request/Response Interceptor)      │
├─────────────────────────────────────────────────────────────┤
│              Business Logic Layer (Backend)                  │
│  Spring Boot 3 REST API (at :8080/api)                      │
│  ├─ Authentication Service (JWT)                           │
│  ├─ User & Role Management                                 │
│  ├─ Course Management Service                              │
│  ├─ Requirement Management Service                         │
│  ├─ Student Management Service                             │
│  ├─ Matrix Configuration Service                           │
│  ├─ Calculation Engine (Core Logic)                        │
│  │   ├─ Course Objective Level Calculation                 │
│  │   ├─ Course Level Calculation (with internal weights)   │
│  │   └─ Professional Level Calculation (with matrix)       │
│  ├─ Excel Import/Export Service                            │
│  └─ Report Generation Service                              │
├─────────────────────────────────────────────────────────────┤
│                 Data Access Layer (Repository)              │
│  MyBatis-Plus ORM with Custom Mapper/SQL                    │
├─────────────────────────────────────────────────────────────┤
│                      Database Layer                         │
│  MySQL 8.0+ (achievement_calc database)                     │
│  ├─ Core Tables                                            │
│  ├─ Relation Tables (Matrix)                               │
│  ├─ Calculation Result Tables                              │
│  └─ Audit Logs                                             │
└─────────────────────────────────────────────────────────────┘
```

## 2. 核心模块设计

### 2.1 认证授权模块 (Authentication & Authorization)
- **技术**: Spring Security + JWT
- **功能**: 用户登录、权限验证、角色管理
- **角色**: 系统管理员、教务管理员、专业负责人、课程教师

### 2.2 基础数据管理模块 (Module A)
- **功能**: 系统参数、毕业要求、课程库、学生名单
- **主要操作**: 导入/导出、CRUD

### 2.3 课程大纲与映射模块 (Module B)
- **功能**: 课程目标、内部权重、考核点映射
- **核心**: 权重校验逻辑

### 2.4 成绩处理与计算模块 (Module C)
- **功能**: Excel模板生成、成绩导入、达成度计算
- **核心**: 双层权重计算引擎

### 2.5 报表与数据导出模块 (Module D)
- **功能**: PDF/Excel报表生成、数据透视
- **工具**: Apache POI、iText

## 3. 技术栈

| 层级 | 技术 | 版本 |
|------|-----|------|
| 前端 | Vue | 3.3+ |
| 前端UI | Element Plus | 2.5.0+ |
| 后端框架 | Spring Boot | 3.2.0+ |
| 后端ORM | MyBatis-Plus | 3.5.3+ |
| 数据库 | MySQL | 8.0+ |
| Excel处理 | EasyExcel | 3.3.1+ |
| Java版本 | JDK | 17+ |

## 4. 数据流向

```
学生成绩导入 
    ↓
数据验证与规范化 
    ↓
课程目标级达成度计算 (按考核点)
    ↓
课程级达成度计算 (按内部权重)
    ↓
结果锁定（教师级）
    ↓
专业级达成度汇总 (按支撑矩阵权重)
    ↓
结果持久化与报表生成
```

## 5. 部署架构

```
开发环境: 本地Docker或直连数据库
测试环境: 单服务器部署
演示环境: 云服务器（申请中）
    - 前端: Nginx反向代理 + Vue SPA
    - 后端: Spring Boot应用 + Tomcat
    - 数据库: MySQL 8.0
```

## 6. 安全设计

- **认证**: JWT Token (有效期可配置)
- **授权**: 基于角色的RBAC模型
- **数据安全**: 成绩锁定机制、事务隔离
- **API安全**: CORS、请求签名、速率限制
