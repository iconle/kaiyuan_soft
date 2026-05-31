# 系统权限拆分：Admin 仅保留用户管理 + 字典管理

## Context

当前系统管理员（ADMIN）拥有 5 个菜单项：用户管理、学生管理、班级管理、数据字典、成绩解锁。Issue 要求 Admin 仅保留**用户管理**和**字典管理**两个模块，其余功能全部移交教务管理员（ACADEMIC），并确保 Admin 无法对教学管理页面执行任何写操作。

## 变更概要

| 功能 | 当前归属 | 变更后 |
|------|---------|--------|
| 用户管理 | ADMIN | ADMIN（不变） |
| 字典管理（学院/专业/学期） | ADMIN | ADMIN（不变） |
| 学生管理 | ADMIN + ACADEMIC | ACADEMIC only |
| 班级管理（行政班级） | ADMIN only | ACADEMIC only |
| 成绩解锁（含勘误工单） | ADMIN + ACADEMIC | ACADEMIC only |

---

## 1. 前端 - 路由权限变更

**文件：** `frontend/src/router/index.js`

| 路由 | 当前 meta.roles | 变更后 |
|------|----------------|--------|
| `/admin/students` | `['ADMIN', 'ACADEMIC']` | `['ACADEMIC']` |
| `/admin/classes` | `['ADMIN']` | 改路径为 `/academic/classes`，roles 改为 `['ACADEMIC']` |
| `/admin/score-unlock` | `['ADMIN', 'ACADEMIC']` | `['ACADEMIC']` |

保持不变的路由：
- `/admin/users` → `['ADMIN']`
- `/admin/dict` → `['ADMIN']`

---

## 2. 前端 - 侧边栏菜单变更

**文件：** `frontend/src/layouts/AdminLayout.vue`

**Admin 菜单（"系统管理"子菜单）：** 删除学生管理、班级管理、成绩解锁三个 `el-menu-item`，仅保留：
- 用户管理 `/admin/users`
- 数据字典 `/admin/dict`

**ACADEMIC 菜单（"教务管理"子菜单）：** 新增班级管理菜单项：
- 班级管理 `/academic/classes`（新增，使用 OfficeBuilding 图标）

其余 ACADEMIC 菜单项不变（课程体系、学生管理、教学班级、宏观看板、专业级计算、成绩解锁）。

---

## 3. 前端 - ScoreUnlock.vue 角色逻辑简化

**文件：** `frontend/src/views/admin/ScoreUnlock.vue`

当前该组件有两套基于角色的 UI 分支（ADMIN 做紧急解锁和最终解锁，ACADEMIC 做初审同意）。变更后 ACADEMIC 承担全部流程，需调整：

1. **Tab 1 紧急解锁按钮**：`v-if="row.status === 'LOCKED' && isAdmin"` → `v-if="row.status === 'LOCKED' && isAcademic"`
2. **删除 "需管理员操作" 提示文字**（line 28）：ACADEMIC 自己就能操作，无需此提示
3. **Tab 2 勘误申请操作列**：
   - PENDING 状态：保持 ACADEMIC 的"同意/拒绝"按钮不变
   - APPROVED 状态：`v-else-if="isAdmin && row.status === 'APPROVED'"` → `v-else-if="isAcademic && row.status === 'APPROVED'"`，ACADEMIC 可看到"解锁/拒绝"
   - 删除 "等待教务审核" tag 分支（line 69-71，原 ADMIN 查看 PENDING 时的提示）
4. **handleApprove 确认对话框文字**：删除"同意后将转交管理员最终审批解锁"，改为"同意后可进行解锁操作"
5. **handleApprove 成功提示**：`已同意，已转交管理员审批` → `已同意，可在勘误申请中解锁`
6. **handleUnlock 确认对话框标题**：`管理员解锁` → `解锁确认`

---

## 4. 后端 - StudentController 权限变更

**文件：** `backend/src/main/java/com/obe/platform/modulea/controller/StudentController.java`

| 端点 | 当前 | 变更后 |
|------|------|--------|
| `GET /api/students` | `hasAnyRole('ADMIN','ACADEMIC')` | `hasRole('ACADEMIC')` |
| `POST /api/students` | `hasRole('ADMIN')` | `hasRole('ACADEMIC')` |
| `PUT /api/students/{id}` | `hasRole('ADMIN')` | `hasRole('ACADEMIC')` |
| `DELETE /api/students/{id}` | `hasRole('ADMIN')` | `hasRole('ACADEMIC')` |
| `POST /api/students/import` | `hasRole('ACADEMIC')` | 不变 |

---

## 5. 后端 - DictController 行政班级部分权限变更

**文件：** `backend/src/main/java/com/obe/platform/modulea/controller/DictController.java`

字典管理部分（学院/专业/学期 CRUD）保持 ADMIN 不变。仅行政班级部分移交 ACADEMIC：

| 端点 | 当前 | 变更后 |
|------|------|--------|
| `GET /api/dict/admin-classes` | `hasAnyRole('ADMIN','ACADEMIC')` | `hasRole('ACADEMIC')` |
| `POST /api/dict/admin-classes` | `hasRole('ADMIN')` | `hasRole('ACADEMIC')` |
| `PUT /api/dict/admin-classes/{id}` | `hasRole('ADMIN')` | `hasRole('ACADEMIC')` |
| `DELETE /api/dict/admin-classes/{id}` | `hasRole('ADMIN')` | `hasRole('ACADEMIC')` |
| `GET /api/dict/admin-classes/{id}/students` | `hasAnyRole('ADMIN','ACADEMIC')` | `hasRole('ACADEMIC')` |
| `POST /admin-classes/{classId}/students/{studentId}` | `hasRole('ADMIN')` | `hasRole('ACADEMIC')` |
| `DELETE /admin-classes/{classId}/students/{studentId}` | `hasRole('ADMIN')` | `hasRole('ACADEMIC')` |

保持不变：
- `GET /api/dict/colleges` — 无注解（所有认证用户）
- `POST/PUT/DELETE /api/dict/colleges` — `hasRole('ADMIN')`
- `GET /api/dict/majors` — 无注解
- `POST/PUT /api/dict/majors` — `hasRole('ADMIN')`
- `GET /api/dict/semesters` — `hasAnyRole('ADMIN','ACADEMIC','DIRECTOR','TEACHER')`（Admin 字典管理页需要读）
- `POST/DELETE /api/dict/semesters` — `hasRole('ADMIN')`

---

## 6. 后端 - AdminScoreController 权限变更

**文件：** `backend/src/main/java/com/obe/platform/modulec/controller/AdminScoreController.java`

| 端点 | 当前 | 变更后 |
|------|------|--------|
| `POST /api/admin/scores/{sheetId}/unlock` | `hasRole('ADMIN')` | `hasRole('ACADEMIC')` |
| `GET /api/admin/scores` | `hasAnyRole('ADMIN','ACADEMIC')` | `hasRole('ACADEMIC')` |
| `GET /api/admin/unlock-requests` | `hasAnyRole('ADMIN','ACADEMIC')` | `hasRole('ACADEMIC')` |
| `POST /api/admin/unlock-requests/{id}/unlock` | `hasRole('ADMIN')` | `hasRole('ACADEMIC')` |
| `POST /api/admin/unlock-requests/{id}/reject` | `hasAnyRole('ADMIN','ACADEMIC')` | `hasRole('ACADEMIC')` |

保持不变：
- `POST /api/admin/unlock-requests/{id}/approve` — 已经是 `hasRole('ACADEMIC')`

---

## 7. 后端 - UnlockRequestService 清理 ADMIN 逻辑

**文件：** `backend/src/main/java/com/obe/platform/modulec/service/UnlockRequestService.java`

1. **`listRequestsForRole` 方法**（line 57-72）：删除 ADMIN 过滤分支（line 59-61），ACADEMIC 查看全部请求。方法签名可简化，但保持向后兼容可保留 roleCode 参数。
2. **`rejectRequest` 方法**（line 140-156）：删除 ADMIN 拒绝 PENDING 的限制（line 144-146），因为 ADMIN 不再调用此方法，该分支为死代码。
3. **注释更新**：`unlockApprovedRequest` 方法的 Javadoc 从 "Admin: final approval" 改为 "Academic: final approval — unlock the sheet"。

---

## 8. 不需要变更的文件

- `UserController` — 所有端点保持 ADMIN-only（用户管理是 Admin 保留功能）
- `DictManage.vue` — Admin 保留字典管理，页面内容不变
- `UserManage.vue` — Admin 保留用户管理，不变
- `ClassManage.vue` — 视图本身不变，仅路由和菜单归属变更
- `StudentManage.vue` — 视图本身不变，仅路由和菜单归属变更
- `CourseController`, `TeachingClassController`, `GradReqController`, `MacroMatrixController` — 不涉及 ADMIN 角色
- `ScoreController`, `CourseCalcController`, `GlobalCalcController` — 不涉及 ADMIN 角色
- `ReportController` — 不涉及 ADMIN 角色

---

## 验证方案

1. **Admin 登录**：侧边栏仅显示 2 个菜单（用户管理、数据字典）；访问 `/admin/students`、`/academic/classes`、`/admin/score-unlock` 均被路由守卫拦截
2. **Admin API 测试**：使用 Admin JWT 调用 `/api/students POST`、`/api/dict/admin-classes POST`、`/api/admin/scores POST` 等接口，应返回 403 Forbidden
3. **Admin 字典功能**：Admin 可正常 CRUD 学院/专业/学期，`GET /api/dict/semesters` 正常返回
4. **ACADEMIC 登录**：侧边栏显示课程体系、学生管理、教学班级、班级管理、宏观看板、专业级计算、成绩解锁（共 7 项）
5. **ACADEMIC 班级管理**：可正常 CRUD 行政班级及班级学生
6. **ACADEMIC 成绩解锁**：紧急解锁按钮可见可操作；勘误申请可同意也可解锁（全流程）
7. **其他角色不受影响**：DIRECTOR、TEACHER 功能正常
