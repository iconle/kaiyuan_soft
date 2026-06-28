-- 为 major_achievement 表新增 grade 字段
-- 用于区分不同年级的专业级计算结果
ALTER TABLE `major_achievement`
  ADD COLUMN `grade` int(0) NULL COMMENT '年级（入学年份）'
  AFTER `semester_id`;

-- 添加索引
ALTER TABLE `major_achievement`
  ADD INDEX `idx_grade` (`grade`);
