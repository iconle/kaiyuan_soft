-- ============================================================
-- 修复 student_score 唯一键：纳入 question_id
-- 旧键: (sheet_id, student_id, assessment_id)
-- 新键: (sheet_id, student_id, assessment_id, question_id)
-- ============================================================
USE obe_platform;

ALTER TABLE student_score DROP INDEX uk_sheet_student_assessment;
ALTER TABLE student_score ADD UNIQUE INDEX uk_sheet_student_assessment_q
    (sheet_id, student_id, assessment_id, question_id);
