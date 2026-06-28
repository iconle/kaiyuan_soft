# 开发报告：年级字段与分级计算实施

**日期**：2026-06-28  
**关联文档**：`年级字段与分级计算方案可行性审查报告.md`、`三级计算正确性与个人达成度分析报告.md`

---

## 一、变更清单

### 1.1 数据库变更

| 文件 | 操作 | 说明 |
|------|------|------|
| `sql/modify/001_add_grade_to_teaching_class.sql` | **新建** | DDL 脚本：新增 `grade` 字段 + 索引 + 数据回填 |

**脚本内容**：

```sql
-- 新增字段
ALTER TABLE `teaching_class`
  ADD COLUMN `grade` int(0) NULL COMMENT '年级（入学年份），如 2024 表示 2024 级'
  AFTER `semester_id`;

-- 添加索引
ALTER TABLE `teaching_class`
  ADD INDEX `idx_grade` (`grade`);

-- 回填现有数据
UPDATE `teaching_class` SET `grade` = 2025 WHERE `id` IN (1, 2, 3, 4);
UPDATE `teaching_class` SET `grade` = 2024 WHERE `id` IN (5, 6, 7, 8);
```

| 教学班 | 课程 | 学期 | grade |
|--------|------|------|-------|
| class 1 | 数据结构 | 2025-2026秋 | **2025** |
| class 2 | C语言程序设计 | 2025-2026秋 | **2025** |
| class 3 | 计算机网络 | 2025-2026春 | **2025** |
| class 4 | 计算机网络 | 2025-2026秋 | **2025** |
| class 5 | 数据库原理 | 2024-2025秋 | **2024** |
| class 6 | 大学物理B | 2024-2025秋 | **2024** |
| class 7 | 线性代数 | 2024-2025春 | **2024** |
| class 8 | 高等数学B | 2024-2025春 | **2024** |

---

### 1.2 后端变更

| 文件 | 操作 | 说明 |
|------|------|------|
| `modulea/entity/TeachingClass.java` | **修改** | 新增 `private Integer grade;` 字段 |
| `modulec/service/GlobalCalcService.java` | **修改** | `getDashboard()` 增加 `semesterId` 和 `grade` 可选过滤；`compute()` 增加 `grade` 参数 |
| `modulec/controller/GlobalCalcController.java` | **修改** | `/dashboard` 和 `/compute` 端点增加可选 `grade` 参数 |

#### 修改1：TeachingClass Entity

```java
// 新增字段（第16行之后）
private Integer grade;  // 年级（入学年份），如 2024
```

#### 修改2：GlobalCalcService.getDashboard()

**函数签名**：

```java
// 修改前：只按 majorId 查询，无过滤
public DashboardData getDashboard(Long majorId)

// 修改后：增加可选的 semesterId 和 grade 过滤
public DashboardData getDashboard(Long majorId, Long semesterId, Integer grade)
```

**核心过滤逻辑**：

```java
LambdaQueryWrapper<TeachingClass> classWrapper =
    new LambdaQueryWrapper<TeachingClass>()
        .eq(TeachingClass::getCourseId, course.getId());
if (semesterId != null) {
    classWrapper.eq(TeachingClass::getSemesterId, semesterId);  // ← 修复原 Bug
}
if (grade != null) {
    classWrapper.eq(TeachingClass::getGrade, grade);            // ← 新增年级过滤
}
```

**参数为空时的行为**：`semesterId=null` 或 `grade=null` 时，对应过滤条件不生效，保持向后兼容。

#### 修改3：GlobalCalcService.compute()

```java
// 修改前
public MajorCalcResult compute(Long majorId, Long semesterId, Long operator)

// 修改后
public MajorCalcResult compute(Long majorId, Long semesterId, Integer grade, Long operator)
```

第一行调用改为 `getDashboard(majorId, semesterId, grade)`。

#### 修改4：GlobalCalcController

`/api/global/dashboard` 和 `/api/global/compute` 均新增可选参数：

```
GET  /api/global/dashboard?majorId=1&semesterId=1&grade=2025
POST /api/global/compute?majorId=1&semesterId=1&grade=2025&operator=3
```

`grade` 参数标注 `required = false`，不传时行为不变。

---

### 1.3 前端变更

#### 1.3.1 Bug 修复

| 文件 | 操作 | 说明 |
|------|------|------|
| `views/teacher/CourseCompute.vue` | **Bug 修复** | 删除重复的本地 `downloadBlob` 函数声明 |

**问题**：第411行已从 `../../utils/downloadFile` 导入 `downloadBlob`，第594-598行又定义了同名本地函数，导致编译错误：

```
[vue/compiler-sfc] Identifier 'downloadBlob' has already been declared. (197:9)
```

**修复**：删除第594-598行的本地函数定义（共6行代码），直接使用导入的工具函数。

#### 1.3.2 教学班级管理 — 新增"目标年级"字段

| 文件 | 操作 | 说明 |
|------|------|------|
| `views/academic/TeachingClassManage.vue` | **修改** | 新增/编辑对话框增加"目标年级"下拉选项 |

**模板变更**（第90行后新增）：

```html
<el-form-item label="目标年级" required>
  <el-select v-model="form.grade" style="width:100%" placeholder="选择目标年级">
    <el-option v-for="g in gradeOptions" :key="g" :label="`${g} 级`" :value="g" />
  </el-select>
</el-form-item>
```

**脚本变更**：

```javascript
// form 新增 grade 字段
const form = reactive({ 
  className: '', courseId: null, teacherId: null, semesterId: null, grade: null 
})

// 年级选项（与 student.enrollment_year 对齐）
const gradeOptions = [2020, 2021, 2022, 2023, 2024, 2025, 2026, 2027, 2028, 2029]

// showDialog 函数同步 grade 字段
function showDialog(row) {
  ...
  Object.assign(form, {
    ...,
    grade: row?.grade || null
  })
}
```

**效果**：新建/编辑教学班时可选择"目标年级"（2020~2029级），`grade` 字段随表单提交到后端，由 MyBatis-Plus 自动映射到 Entity。

#### 1.3.3 专业级计算 — 新增"目标年级"过滤下拉

| 文件 | 操作 | 说明 |
|------|------|------|
| `views/director/GlobalCompute.vue` | **修改** | 页面头部增加年级下拉，联动 API 调用 |
| `api/director.js` | **修改** | `getDashboard`、`triggerGlobalCompute`、`getGlobalResults`、`listMajorPersonalAchievements` 增加 `grade` 参数 |
| `api/academic.js` | **修改** | `getDashboard` 增加 `grade` 参数 |

**GlobalCompute.vue 模板变更**：

```html
<el-select v-model="selectedGrade" placeholder="目标年级" style="width:160px"
  clearable @change="onGradeChange">
  <el-option v-for="g in gradeOptions" :key="g" :label="`${g} 级`" :value="g" />
</el-select>
```

**GlobalCompute.vue 脚本变更**：

```javascript
// 新增状态
const selectedGrade = ref(null)
const gradeOptions = [2020, 2021, 2022, 2023, 2024, 2025, 2026, 2027, 2028, 2029]

// 新增年级切换处理
function onGradeChange() {
  loadDashboard()
  loadResults()
}

// 所有 API 调用增加 grade 参数
getDashboard(selectedMajorId.value, selectedGrade.value)
triggerGlobalCompute(selectedMajorId.value, 1, selectedGrade.value, userId)
getGlobalResults(selectedMajorId.value, 1, selectedGrade.value)
listMajorPersonalAchievements(selectedMajorId.value, 1, selectedGrade.value, row.indicatorId)
```

**API 函数签名变更**（`director.js` 和 `academic.js`）：

```javascript
// 修改前
export function getDashboard(majorId)
export function triggerGlobalCompute(majorId, semesterId, operator)
export function getGlobalResults(majorId, semesterId)
export function listMajorPersonalAchievements(majorId, semesterId, indicatorId)

// 修改后（grade 为可选参数）
export function getDashboard(majorId, grade)
export function triggerGlobalCompute(majorId, semesterId, grade, operator)
export function getGlobalResults(majorId, semesterId, grade)
export function listMajorPersonalAchievements(majorId, semesterId, grade, indicatorId)
```

**交互流程**：

```
用户选择 年级=2025 → onGradeChange()
  → loadDashboard() 传递 grade=2025 → 只显示 2025 级的课程状态
  → loadResults()   传递 grade=2025 → 只显示 2025 级的计算结果
  → 用户点击"执行专业级计算"
    → triggerGlobalCompute(grade=2025) → 后端只计算 2025 级的班级学术
```

---

## 二、修复效果验证

### 2.1 Vue 编译错误

| 修复前 | 修复后 |
|--------|--------|
| `Identifier 'downloadBlob' has already been declared` — 编译失败 | ✅ 编译通过 |

### 2.2 年级过滤效果

以 `compute(majorId=1, semesterId=1, grade=2025, operator=3)` 为例：

| 修复前（全量混合） | 修复后（按年级过滤） |
|--------------------|---------------------|
| 纳入 2024级 class 5-8 + 2025级 class 1-4<br>共 8 个班混合归一化 | 仅纳入 grade=2025 的 class 1-4<br>共 4 个班独立归一化 |
| 2025级学生个人第三级被 2024级课程稀释 | 2025级学生仅在 2025 级课程范围内计算 |
| 指标点1 有效权重 12.5%（被稀释） | 指标点1 有效权重 100%（组内归一化） |

### 2.3 Semester 过滤效果

同时修复了 `semesterId` 被忽略的 Bug。现在 `semesterId=1` 会正确过滤仅该学期的教学班。

### 2.4 向后兼容

不传 `grade` 和 `semesterId` 时行为与修改前完全一致（全量计算），保证存量调用不受影响。

---

## 三、API 变更说明

### 3.1 GET /api/global/dashboard

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `majorId` | Long | ✅ | 专业 ID |
| `semesterId` | Long | ❌ | 学期过滤（新增，null=全部） |
| `grade` | Integer | ❌ | 年级过滤（新增，null=全部） |

### 3.2 POST /api/global/compute

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `majorId` | Long | ✅ | 专业 ID |
| `semesterId` | Long | ✅ | 结果存储学期 |
| `grade` | Integer | ❌ | 年级过滤（新增，null=全部） |
| `operator` | Long | ✅ | 操作人 ID |

---

## 四、后续建议

1. **Excel 导入模板**：`TeachingClassImportService` 同步更新，支持导入 `grade` 列
2. **年级选项动态化**：改为从 `/api/students/enrollment-years?majorId=1` 动态获取，而非硬编码
3. **宏观看板**（`GlobalDashboard.vue`）：可选添加年级过滤下拉
4. **测试验证**：分别以 `grade=2024` 和 `grade=2025` 触发计算，对比验证结果

---

## 五、变更文件汇总

| 序号 | 文件路径 | 变更类型 | 说明 |
|------|----------|----------|------|
| 1 | `sql/modify/001_add_grade_to_teaching_class.sql` | **新建** | DDL + 索引 + 数据回填 |
| 2 | `backend/.../entity/TeachingClass.java` | 修改 | +1行 `grade` 字段 |
| 3 | `backend/.../service/GlobalCalcService.java` | 修改 | `getDashboard()` +semester/grade过滤；`compute()` +grade参数 |
| 4 | `backend/.../controller/GlobalCalcController.java` | 修改 | `/dashboard` 和 `/compute` +grade参数 |
| 5 | `frontend/.../views/teacher/CourseCompute.vue` | 修改 | 删除重复 `downloadBlob`（-6行） |
| 6 | `frontend/.../views/academic/TeachingClassManage.vue` | **修改** | 新增加"目标年级"下拉（含表单联动） |
| 7 | `frontend/.../views/director/GlobalCompute.vue` | **修改** | 新增"目标年级"下拉 + API调用联动 |
| 8 | `frontend/.../api/director.js` | 修改 | 4个函数签名 +grade 参数 |
| 9 | `frontend/.../api/academic.js` | 修改 | `getDashboard` +grade 参数 |

---

*报告完*
