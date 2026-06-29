-- Issue #151: 学生个人达成度结果表
-- 用于持久化课程目标级、课程指标点级、专业三级指标点级的个人达成度。

CREATE TABLE IF NOT EXISTS `personal_achievement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL COMMENT '学生',
  `class_id` bigint NULL DEFAULT NULL COMMENT '教学班级，目标级/课程级个人达成度使用',
  `major_id` bigint NULL DEFAULT NULL COMMENT '专业，第三级个人达成度使用',
  `semester_id` bigint NULL DEFAULT NULL COMMENT '学年学期，第三级个人达成度使用',
  `scope_type` varchar(20) NOT NULL COMMENT '达成度层级: OBJECTIVE/COURSE/MAJOR',
  `objective_id` bigint NULL DEFAULT NULL COMMENT '课程目标，OBJECTIVE层级使用',
  `indicator_id` bigint NULL DEFAULT NULL COMMENT '毕业要求指标点，COURSE/MAJOR层级使用',
  `achievement` decimal(6, 4) NOT NULL COMMENT '学生个人达成度',
  `calc_time` datetime NOT NULL COMMENT '计算时间',
  PRIMARY KEY (`id`),
  INDEX `idx_pa_class_scope_student` (`class_id`, `scope_type`, `student_id`),
  INDEX `idx_pa_major_scope_student` (`major_id`, `semester_id`, `scope_type`, `student_id`),
  INDEX `idx_pa_objective` (`objective_id`),
  INDEX `idx_pa_indicator` (`indicator_id`),
  INDEX `idx_pa_student` (`student_id`),
  CONSTRAINT `fk_pa_class` FOREIGN KEY (`class_id`) REFERENCES `teaching_class` (`id`),
  CONSTRAINT `fk_pa_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `indicator` (`id`),
  CONSTRAINT `fk_pa_major` FOREIGN KEY (`major_id`) REFERENCES `sys_major` (`id`),
  CONSTRAINT `fk_pa_objective` FOREIGN KEY (`objective_id`) REFERENCES `course_objective` (`id`),
  CONSTRAINT `fk_pa_semester` FOREIGN KEY (`semester_id`) REFERENCES `sys_dict_semester` (`id`),
  CONSTRAINT `fk_pa_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生个人达成度结果表';
