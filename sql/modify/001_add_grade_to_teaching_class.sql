-- ============================================================
-- 变更：teaching_class 表新增 grade（年级）字段
-- 用途：区分不同年级（如 2024 级、2025 级）的教学班，
--       支持第三级计算按年级分别执行
-- 日期：2026-06-28
-- ============================================================

-- 1. 新增 grade 字段
ALTER TABLE `teaching_class`
  ADD COLUMN `grade` int(0) NULL COMMENT '年级（入学年份），如 2024 表示 2024 级'
  AFTER `semester_id`;

-- 2. 添加索引
ALTER TABLE `teaching_class`
  ADD INDEX `idx_grade` (`grade`);

-- 3. 回填现有数据（基于 class_name 中的年份后缀推断）
--    class_name 格式如 "数据结构202501" → 2025 级
--    class_name 格式如 "数据库原理202401" → 2024 级
UPDATE `teaching_class` SET `grade` = 2025 WHERE `id` IN (1, 2, 3, 4);
UPDATE `teaching_class` SET `grade` = 2024 WHERE `id` IN (5, 6, 7, 8);
