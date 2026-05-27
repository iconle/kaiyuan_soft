# OBE达成度计算平台 - 数据库ER图设计 v1

## 实体列表（共18个核心实体）

| 实体 | 说明 |
|------|------|
| course | 课程库 |
| course_outline | 课程大纲 |
| course_objective | 课程目标 |
| assessment_point | 考核点 |
| assessment_question | 考核点题目 |
| teaching_class | 教学班级 |
| student | 学生 |
| score_sheet | 成绩单 |
| student_score | 学生成绩明细 |
| grad_requirement | 毕业要求 |
| indicator | 毕业要求指标点 |
| objective_indicator_weight | 课程目标-指标点权重 |
| macro_support_matrix | 宏观支撑矩阵 |
| obj_achievement | 目标级达成度（第一级） |
| course_achievement | 课程级达成度（第二级） |
| major_achievement | 专业级达成度（第三级） |
| sys_user | 系统用户 |
| score_unlock_request | 成绩勘误工单 |

## 多对多关系拆分（中间表）

| 关系 | 中间表 |
|------|--------|
| 考核点 ⇄ 课程目标 | assessment_objective |
| 题目 ⇄ 课程目标 | question_objective |
| 教学班级 ⇄ 学生 | class_student |
| 课程目标 ⇄ 指标点 | objective_indicator_weight |
| 课程 ⇄ 指标点 | macro_support_matrix |

## 业务闭环
毕业要求 → 指标点 → 宏观支撑矩阵 → 课程 → 教学班 → 大纲 → 课程目标 →
考核点 → 题目 → 学生成绩 → 目标达成度(第一级) → 课程达成度(第二级) → 专业达成度(第三级)
### 三层达成度计算
1. **第一级**：基于学生成绩计算每个课程目标的达成度（`obj_achievement`）
2. **第二级**：根据目标达成度与权重计算课程对指标点的支撑达成度（`course_achievement`）
3. **第三级**：聚合所有相关课程，计算专业级指标点达成度（`major_achievement`）

## 外键约束
- 所有外键均使用 `ON DELETE RESTRICT ON UPDATE RESTRICT`，保证数据完整性
- 成绩单锁定后触发达成度计算，需通过工单流程申请解锁