-- ============================================================
-- 清理无教学班的课程的宏观矩阵条目
-- 课程 2,3,4,7,8 无教学班级，其宏观权重无法通过课程级计算产出数据
-- 清理后请在支撑矩阵页面重新配置权重使每列 Σ=1.0
-- ============================================================
USE obe_platform;

-- 删除无教学班级的课程的宏观矩阵条目
DELETE FROM macro_support_matrix WHERE course_id IN (2, 3, 4, 7, 8);

-- 将课程1在指标1的权重调整为1.0（该指标点现在仅有课程1支撑）
UPDATE macro_support_matrix SET weight = 1.0000 WHERE course_id = 1 AND indicator_id = 1;

-- 验证：检查每个指标点的权重总和
SELECT indicator_id, SUM(weight) AS total_weight
FROM macro_support_matrix
GROUP BY indicator_id
HAVING ABS(SUM(weight) - 1.0) > 0.01;
