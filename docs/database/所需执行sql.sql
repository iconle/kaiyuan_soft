SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 第一步：清除历史勘误工单（叶子表，直接删除）
-- ============================================
DELETE FROM score_unlock_request;
-- 影响: 5 条记录

-- ============================================
-- 第二步：解锁所有成绩单
-- ============================================
UPDATE score_sheet
SET status    = 'IMPORTED',
    locked_at = NULL,
    locked_by = NULL;
-- 影响: 4 条记录，全部从 LOCKED 回退到 IMPORTED

-- ============================================
-- 第三步：清除三级达成度计算结果
-- ============================================
DELETE FROM obj_achievement;       -- 12 条，目标级达成度
DELETE FROM course_achievement;    -- 12 条，课程级达成度
DELETE FROM major_achievement;     --  7 条，专业级达成度

-- ============================================
-- 第四步：重置自增计数器
-- ============================================
ALTER TABLE score_unlock_request AUTO_INCREMENT = 1;
ALTER TABLE obj_achievement       AUTO_INCREMENT = 1;
ALTER TABLE course_achievement    AUTO_INCREMENT = 1;
ALTER TABLE major_achievement     AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1; 