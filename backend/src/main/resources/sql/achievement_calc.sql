/*
 Navicat Premium Data Transfer

 Source Server         : mvonline
 Source Server Type    : MySQL
 Source Server Version : 80012
 Source Host           : localhost:3306
 Source Schema         : achievement_calc

 Target Server Type    : MySQL
 Target Server Version : 80012
 File Encoding         : 65001

 Date: 14/05/2026 14:40:21
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for assessment_point
-- ----------------------------
DROP TABLE IF EXISTS `assessment_point`;
CREATE TABLE `assessment_point`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '考核点ID',
  `course_id` bigint(20) NOT NULL COMMENT '课程ID',
  `objective_id` bigint(20) NOT NULL COMMENT '课程目标ID',
  `point_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '考核点代码 如AP1',
  `point_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '考核点名称',
  `full_score` decimal(10, 2) NOT NULL COMMENT '满分分值',
  `seq_num` int(11) NULL DEFAULT NULL COMMENT '序号',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_course_point`(`course_id`, `point_code`) USING BTREE,
  INDEX `idx_course_id`(`course_id`) USING BTREE,
  INDEX `idx_objective_id`(`objective_id`) USING BTREE,
  CONSTRAINT `assessment_point_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `assessment_point_ibfk_2` FOREIGN KEY (`objective_id`) REFERENCES `course_objective` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '考核点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of assessment_point
-- ----------------------------
INSERT INTO `assessment_point` VALUES (12, 1, 1, 'AP1', '平时作业1', 10.00, NULL, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `assessment_point` VALUES (13, 1, 1, 'AP2', '平时作业2', 10.00, NULL, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `assessment_point` VALUES (14, 1, 2, 'AP3', '实验报告1', 20.00, NULL, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `assessment_point` VALUES (15, 1, 2, 'AP4', '实验报告2', 20.00, NULL, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `assessment_point` VALUES (16, 1, 3, 'AP5', '期末考试', 40.00, NULL, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `assessment_point` VALUES (17, 2, 5, 'AP1', '课堂测验', 15.00, NULL, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `assessment_point` VALUES (18, 2, 5, 'AP2', '作业', 20.00, NULL, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `assessment_point` VALUES (19, 2, 6, 'AP3', '期末考试', 65.00, NULL, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `assessment_point` VALUES (20, 3, 7, 'AP1', '实验报告', 30.00, NULL, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `assessment_point` VALUES (21, 3, 8, 'AP2', '期末考试', 50.00, NULL, '2026-05-14 09:00:08', '2026-05-14 09:00:08');

-- ----------------------------
-- Table structure for calculation_course_level
-- ----------------------------
DROP TABLE IF EXISTS `calculation_course_level`;
CREATE TABLE `calculation_course_level`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `class_id` bigint(20) NOT NULL COMMENT '班级ID',
  `requirement_item_id` bigint(20) NOT NULL COMMENT '指标点ID',
  `achievement_rate` decimal(5, 4) NOT NULL COMMENT '达成度 0.0000-1.0000',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态 0=未锁定 1=已锁定',
  `locked_at` timestamp(0) NULL DEFAULT NULL COMMENT '锁定时间',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_course_level`(`class_id`, `requirement_item_id`) USING BTREE,
  INDEX `requirement_item_id`(`requirement_item_id`) USING BTREE,
  INDEX `idx_class_id`(`class_id`) USING BTREE,
  CONSTRAINT `calculation_course_level_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `calculation_course_level_ibfk_2` FOREIGN KEY (`requirement_item_id`) REFERENCES `requirement_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程级达成度计算结果' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of calculation_course_level
-- ----------------------------

-- ----------------------------
-- Table structure for calculation_objective_level
-- ----------------------------
DROP TABLE IF EXISTS `calculation_objective_level`;
CREATE TABLE `calculation_objective_level`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `class_id` bigint(20) NOT NULL COMMENT '班级ID',
  `student_id` bigint(20) NOT NULL COMMENT '学生ID',
  `objective_id` bigint(20) NOT NULL COMMENT '课程目标ID',
  `achievement_rate` decimal(5, 4) NOT NULL COMMENT '达成度 0.0000-1.0000',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_obj_level`(`class_id`, `student_id`, `objective_id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE,
  INDEX `objective_id`(`objective_id`) USING BTREE,
  INDEX `idx_class_student`(`class_id`, `student_id`) USING BTREE,
  CONSTRAINT `calculation_objective_level_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `calculation_objective_level_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `calculation_objective_level_ibfk_3` FOREIGN KEY (`objective_id`) REFERENCES `course_objective` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '目标级达成度计算结果' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of calculation_objective_level
-- ----------------------------

-- ----------------------------
-- Table structure for calculation_professional_level
-- ----------------------------
DROP TABLE IF EXISTS `calculation_professional_level`;
CREATE TABLE `calculation_professional_level`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `prof_id` bigint(20) NOT NULL COMMENT '专业ID',
  `academic_year_id` bigint(20) NOT NULL COMMENT '学年ID',
  `requirement_item_id` bigint(20) NOT NULL COMMENT '指标点ID',
  `achievement_rate` decimal(5, 4) NOT NULL COMMENT '达成度 0.0000-1.0000',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态 0=未锁定 1=已锁定',
  `locked_at` timestamp(0) NULL DEFAULT NULL COMMENT '锁定时间',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_prof_level`(`prof_id`, `academic_year_id`, `requirement_item_id`) USING BTREE,
  INDEX `academic_year_id`(`academic_year_id`) USING BTREE,
  INDEX `requirement_item_id`(`requirement_item_id`) USING BTREE,
  INDEX `idx_prof_year`(`prof_id`, `academic_year_id`) USING BTREE,
  CONSTRAINT `calculation_professional_level_ibfk_1` FOREIGN KEY (`prof_id`) REFERENCES `profession` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `calculation_professional_level_ibfk_2` FOREIGN KEY (`academic_year_id`) REFERENCES `sys_academic_years` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `calculation_professional_level_ibfk_3` FOREIGN KEY (`requirement_item_id`) REFERENCES `requirement_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专业级达成度计算结果' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of calculation_professional_level
-- ----------------------------

-- ----------------------------
-- Table structure for class_students
-- ----------------------------
DROP TABLE IF EXISTS `class_students`;
CREATE TABLE `class_students`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `class_id` bigint(20) NOT NULL COMMENT '班级ID',
  `student_id` bigint(20) NOT NULL COMMENT '学生ID',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_class_student`(`class_id`, `student_id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE,
  INDEX `idx_class_id`(`class_id`) USING BTREE,
  CONSTRAINT `class_students_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `class_students_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '班级学生关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of class_students
-- ----------------------------
INSERT INTO `class_students` VALUES (1, 1, 1, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (2, 1, 2, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (3, 1, 3, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (4, 1, 4, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (5, 1, 5, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (6, 2, 6, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (7, 2, 7, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (8, 2, 8, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (9, 2, 9, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (10, 2, 10, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (11, 3, 1, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (12, 3, 2, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (13, 3, 3, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (14, 3, 4, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (15, 3, 5, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (16, 3, 6, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (17, 3, 7, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (18, 3, 8, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (19, 3, 9, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (20, 3, 10, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (21, 4, 1, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (22, 4, 2, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (23, 4, 3, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (24, 4, 4, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (25, 4, 5, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (26, 5, 1, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (27, 5, 2, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (28, 5, 3, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (29, 5, 4, '2026-05-14 09:00:08');
INSERT INTO `class_students` VALUES (30, 5, 5, '2026-05-14 09:00:08');

-- ----------------------------
-- Table structure for classes
-- ----------------------------
DROP TABLE IF EXISTS `classes`;
CREATE TABLE `classes`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '班级ID',
  `course_id` bigint(20) NOT NULL COMMENT '课程ID',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级名称',
  `class_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级代码',
  `teacher_id` bigint(20) NULL DEFAULT NULL COMMENT '教师ID',
  `academic_year_id` bigint(20) NULL DEFAULT NULL COMMENT '学年ID',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_class`(`course_id`, `academic_year_id`, `class_code`) USING BTREE,
  INDEX `academic_year_id`(`academic_year_id`) USING BTREE,
  INDEX `idx_course_id`(`course_id`) USING BTREE,
  INDEX `idx_teacher_id`(`teacher_id`) USING BTREE,
  CONSTRAINT `classes_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `classes_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `sys_users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `classes_ibfk_3` FOREIGN KEY (`academic_year_id`) REFERENCES `sys_academic_years` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教学班级表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of classes
-- ----------------------------
INSERT INTO `classes` VALUES (1, 1, '计算机2401-01', 'CS101-2401-01', 2, 1, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `classes` VALUES (2, 1, '计算机2401-02', 'CS101-2401-02', 2, 1, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `classes` VALUES (3, 2, '计算机2401-01', 'CS102-2401-01', 2, 1, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `classes` VALUES (4, 3, '计算机2401-01', 'CS103-2401-01', 2, 1, '2026-05-14 09:00:08', '2026-05-14 09:00:08');
INSERT INTO `classes` VALUES (5, 4, '计算机2401-01', 'CS104-2401-01', 2, 1, '2026-05-14 09:00:08', '2026-05-14 09:00:08');

-- ----------------------------
-- Table structure for course
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `prof_id` bigint(20) NOT NULL COMMENT '专业ID',
  `course_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '课程名称',
  `course_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '课程代码',
  `credits` decimal(3, 1) NULL DEFAULT NULL COMMENT '学分',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '课程描述',
  `seq_num` int(11) NULL DEFAULT NULL COMMENT '序号',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_prof_course`(`prof_id`, `course_code`) USING BTREE,
  INDEX `idx_prof_id`(`prof_id`) USING BTREE,
  CONSTRAINT `course_ibfk_1` FOREIGN KEY (`prof_id`) REFERENCES `profession` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course
-- ----------------------------
INSERT INTO `course` VALUES (1, 1, '数据结构', 'CS101', 4.0, NULL, 1, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `course` VALUES (2, 1, '离散数学', 'CS102', 3.0, NULL, 2, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `course` VALUES (3, 1, '操作系统', 'CS103', 4.0, NULL, 3, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `course` VALUES (4, 1, '数据库原理', 'CS104', 3.0, NULL, 4, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `course` VALUES (5, 1, '编译原理', 'CS105', 3.0, NULL, 5, '2026-05-14 08:48:34', '2026-05-14 08:48:34');

-- ----------------------------
-- Table structure for course_objective
-- ----------------------------
DROP TABLE IF EXISTS `course_objective`;
CREATE TABLE `course_objective`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '课程目标ID',
  `course_id` bigint(20) NOT NULL COMMENT '课程ID',
  `objective_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标代码 如CO1',
  `objective_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '目标描述',
  `seq_num` int(11) NULL DEFAULT NULL COMMENT '序号',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_course_objective`(`course_id`, `objective_code`) USING BTREE,
  INDEX `idx_course_id`(`course_id`) USING BTREE,
  CONSTRAINT `course_objective_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程目标表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_objective
-- ----------------------------
INSERT INTO `course_objective` VALUES (1, 1, 'CO1', '理解数据结构基本概念', NULL, 1, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `course_objective` VALUES (2, 1, 'CO2', '掌握常用数据结构的实现', NULL, 2, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `course_objective` VALUES (3, 1, 'CO3', '能分析算法复杂度', NULL, 3, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `course_objective` VALUES (4, 2, 'CO1', '理解离散数学基本概念', NULL, 1, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `course_objective` VALUES (5, 2, 'CO2', '掌握逻辑推理方法', NULL, 2, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `course_objective` VALUES (6, 3, 'CO1', '理解操作系统原理', NULL, 1, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `course_objective` VALUES (7, 3, 'CO2', '掌握进程管理', NULL, 2, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `course_objective` VALUES (8, 3, 'CO3', '掌握内存管理', NULL, 3, '2026-05-14 08:48:34', '2026-05-14 08:48:34');

-- ----------------------------
-- Table structure for graduation_requirement
-- ----------------------------
DROP TABLE IF EXISTS `graduation_requirement`;
CREATE TABLE `graduation_requirement`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '毕业要求ID',
  `prof_id` bigint(20) NOT NULL COMMENT '专业ID',
  `requirement_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '要求名称 如工程知识',
  `requirement_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '要求代码 如RE001',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '描述',
  `seq_num` int(11) NULL DEFAULT NULL COMMENT '序号',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_prof_code`(`prof_id`, `requirement_code`) USING BTREE,
  INDEX `idx_prof_id`(`prof_id`) USING BTREE,
  CONSTRAINT `graduation_requirement_ibfk_1` FOREIGN KEY (`prof_id`) REFERENCES `profession` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '毕业要求表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of graduation_requirement
-- ----------------------------
INSERT INTO `graduation_requirement` VALUES (1, 1, '工程知识', 'RE001', NULL, 1, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `graduation_requirement` VALUES (2, 1, '问题分析', 'RE002', NULL, 2, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `graduation_requirement` VALUES (3, 1, '设计/开发解决方案', 'RE003', NULL, 3, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `graduation_requirement` VALUES (4, 1, '研究', 'RE004', NULL, 4, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `graduation_requirement` VALUES (5, 1, '现代工具使用', 'RE005', NULL, 5, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `graduation_requirement` VALUES (6, 1, '工程与社会', 'RE006', NULL, 6, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `graduation_requirement` VALUES (7, 1, '环境和可持续发展', 'RE007', NULL, 7, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `graduation_requirement` VALUES (8, 1, '职业规范', 'RE008', NULL, 8, '2026-05-14 08:48:34', '2026-05-14 08:48:34');

-- ----------------------------
-- Table structure for objective_weight
-- ----------------------------
DROP TABLE IF EXISTS `objective_weight`;
CREATE TABLE `objective_weight`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '权重ID',
  `course_id` bigint(20) NOT NULL COMMENT '课程ID',
  `objective_id` bigint(20) NOT NULL COMMENT '课程目标ID',
  `requirement_item_id` bigint(20) NOT NULL COMMENT '指标点ID',
  `weight` decimal(5, 4) NOT NULL COMMENT '权重 0.0000-1.0000',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_obj_weight`(`course_id`, `objective_id`, `requirement_item_id`) USING BTREE,
  INDEX `objective_id`(`objective_id`) USING BTREE,
  INDEX `requirement_item_id`(`requirement_item_id`) USING BTREE,
  INDEX `idx_course_objective`(`course_id`, `objective_id`) USING BTREE,
  CONSTRAINT `objective_weight_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `objective_weight_ibfk_2` FOREIGN KEY (`objective_id`) REFERENCES `course_objective` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `objective_weight_ibfk_3` FOREIGN KEY (`requirement_item_id`) REFERENCES `requirement_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '内部权重表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of objective_weight
-- ----------------------------

-- ----------------------------
-- Table structure for profession
-- ----------------------------
DROP TABLE IF EXISTS `profession`;
CREATE TABLE `profession`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '专业ID',
  `prof_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业名称',
  `prof_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业代码',
  `dept_id` bigint(20) NOT NULL COMMENT '学院ID',
  `academic_year_id` bigint(20) NOT NULL COMMENT '学年ID',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '专业描述',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `prof_code`(`prof_code`) USING BTREE,
  INDEX `academic_year_id`(`academic_year_id`) USING BTREE,
  INDEX `idx_prof_code`(`prof_code`) USING BTREE,
  INDEX `idx_dept_id`(`dept_id`) USING BTREE,
  CONSTRAINT `profession_ibfk_1` FOREIGN KEY (`dept_id`) REFERENCES `sys_departments` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `profession_ibfk_2` FOREIGN KEY (`academic_year_id`) REFERENCES `sys_academic_years` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专业表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of profession
-- ----------------------------
INSERT INTO `profession` VALUES (1, '计算机科学与技术', 'CS2024', 1, 1, NULL, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `profession` VALUES (2, '软件工程', 'SE2024', 1, 1, NULL, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `profession` VALUES (3, '信息工程', 'IE2024', 2, 1, NULL, '2026-05-14 08:48:34', '2026-05-14 08:48:34');

-- ----------------------------
-- Table structure for requirement_item
-- ----------------------------
DROP TABLE IF EXISTS `requirement_item`;
CREATE TABLE `requirement_item`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '指标点ID',
  `requirement_id` bigint(20) NOT NULL COMMENT '毕业要求ID',
  `item_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标点代码 如1.1',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标点名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '描述',
  `seq_num` int(11) NULL DEFAULT NULL COMMENT '序号',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_req_code`(`requirement_id`, `item_code`) USING BTREE,
  INDEX `idx_requirement_id`(`requirement_id`) USING BTREE,
  CONSTRAINT `requirement_item_ibfk_1` FOREIGN KEY (`requirement_id`) REFERENCES `graduation_requirement` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '二级指标点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of requirement_item
-- ----------------------------
INSERT INTO `requirement_item` VALUES (1, 1, '1.1', '能应用数学知识', NULL, 1, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `requirement_item` VALUES (2, 1, '1.2', '能应用计算机科学基础知识', NULL, 2, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `requirement_item` VALUES (3, 1, '1.3', '能应用工程基础知识', NULL, 3, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `requirement_item` VALUES (4, 2, '2.1', '能识别和分析问题', NULL, 1, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `requirement_item` VALUES (5, 2, '2.2', '能建立数学模型', NULL, 2, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `requirement_item` VALUES (6, 3, '3.1', '能进行系统设计', NULL, 1, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `requirement_item` VALUES (7, 3, '3.2', '能编写程序代码', NULL, 2, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `requirement_item` VALUES (8, 4, '4.1', '能进行文献研究', NULL, 1, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `requirement_item` VALUES (9, 4, '4.2', '能进行实验研究', NULL, 2, '2026-05-14 08:48:34', '2026-05-14 08:48:34');

-- ----------------------------
-- Table structure for student_score
-- ----------------------------
DROP TABLE IF EXISTS `student_score`;
CREATE TABLE `student_score`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '成绩ID',
  `class_id` bigint(20) NOT NULL COMMENT '班级ID',
  `student_id` bigint(20) NOT NULL COMMENT '学生ID',
  `assessment_point_id` bigint(20) NOT NULL COMMENT '考核点ID',
  `score` decimal(10, 2) NOT NULL COMMENT '实际得分',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_score`(`class_id`, `student_id`, `assessment_point_id`) USING BTREE,
  INDEX `student_id`(`student_id`) USING BTREE,
  INDEX `assessment_point_id`(`assessment_point_id`) USING BTREE,
  INDEX `idx_class_student`(`class_id`, `student_id`) USING BTREE,
  CONSTRAINT `student_score_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `student_score_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `student_score_ibfk_3` FOREIGN KEY (`assessment_point_id`) REFERENCES `assessment_point` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生成绩表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student_score
-- ----------------------------

-- ----------------------------
-- Table structure for students
-- ----------------------------
DROP TABLE IF EXISTS `students`;
CREATE TABLE `students`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '学生ID',
  `student_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学号',
  `student_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学生名称',
  `prof_id` bigint(20) NOT NULL COMMENT '专业ID',
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '性别',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态 1=在读 0=毕业',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `student_no`(`student_no`) USING BTREE,
  INDEX `idx_prof_id`(`prof_id`) USING BTREE,
  INDEX `idx_student_no`(`student_no`) USING BTREE,
  CONSTRAINT `students_ibfk_1` FOREIGN KEY (`prof_id`) REFERENCES `profession` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of students
-- ----------------------------
INSERT INTO `students` VALUES (1, '2024001', '张三', 1, '男', NULL, 1, '2026-05-14 09:00:08');
INSERT INTO `students` VALUES (2, '2024002', '李四', 1, '女', NULL, 1, '2026-05-14 09:00:08');
INSERT INTO `students` VALUES (3, '2024003', '王五', 1, '男', NULL, 1, '2026-05-14 09:00:08');
INSERT INTO `students` VALUES (4, '2024004', '赵六', 1, '女', NULL, 1, '2026-05-14 09:00:08');
INSERT INTO `students` VALUES (5, '2024005', '孙七', 1, '男', NULL, 1, '2026-05-14 09:00:08');
INSERT INTO `students` VALUES (6, '2024006', '周八', 1, '女', NULL, 1, '2026-05-14 09:00:08');
INSERT INTO `students` VALUES (7, '2024007', '吴九', 1, '男', NULL, 1, '2026-05-14 09:00:08');
INSERT INTO `students` VALUES (8, '2024008', '郑十', 1, '女', NULL, 1, '2026-05-14 09:00:08');
INSERT INTO `students` VALUES (9, '2024009', '刘十一', 1, '男', NULL, 1, '2026-05-14 09:00:08');
INSERT INTO `students` VALUES (10, '2024010', '陈十二', 1, '女', NULL, 1, '2026-05-14 09:00:08');

-- ----------------------------
-- Table structure for support_matrix
-- ----------------------------
DROP TABLE IF EXISTS `support_matrix`;
CREATE TABLE `support_matrix`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '矩阵ID',
  `prof_id` bigint(20) NOT NULL COMMENT '专业ID',
  `course_id` bigint(20) NOT NULL COMMENT '课程ID',
  `requirement_item_id` bigint(20) NOT NULL COMMENT '指标点ID',
  `support_weight` decimal(5, 4) NOT NULL COMMENT '支撑权重 0.0000-1.0000',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_support_matrix`(`prof_id`, `course_id`, `requirement_item_id`) USING BTREE,
  INDEX `course_id`(`course_id`) USING BTREE,
  INDEX `requirement_item_id`(`requirement_item_id`) USING BTREE,
  INDEX `idx_prof_item`(`prof_id`, `requirement_item_id`) USING BTREE,
  CONSTRAINT `support_matrix_ibfk_1` FOREIGN KEY (`prof_id`) REFERENCES `profession` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `support_matrix_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `support_matrix_ibfk_3` FOREIGN KEY (`requirement_item_id`) REFERENCES `requirement_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '支撑矩阵' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of support_matrix
-- ----------------------------

-- ----------------------------
-- Table structure for sys_academic_years
-- ----------------------------
DROP TABLE IF EXISTS `sys_academic_years`;
CREATE TABLE `sys_academic_years`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '学年ID',
  `academic_year` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学年 如2024-2025',
  `semester` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学期 秋季/春季',
  `start_date` date NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结束日期',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态 1=启用 0=禁用',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_academic_year_semester`(`academic_year`, `semester`) USING BTREE,
  INDEX `idx_academic_year`(`academic_year`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学年学期表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_academic_years
-- ----------------------------
INSERT INTO `sys_academic_years` VALUES (1, '2024-2025', '秋季', '2024-09-01', '2025-01-31', 1, '2026-05-14 08:48:34');
INSERT INTO `sys_academic_years` VALUES (2, '2024-2025', '春季', '2025-02-01', '2025-06-30', 1, '2026-05-14 08:48:34');
INSERT INTO `sys_academic_years` VALUES (3, '2025-2026', '秋季', '2025-09-01', '2026-01-31', 1, '2026-05-14 08:48:34');

-- ----------------------------
-- Table structure for sys_departments
-- ----------------------------
DROP TABLE IF EXISTS `sys_departments`;
CREATE TABLE `sys_departments`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '学院ID',
  `dept_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学院名称',
  `dept_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学院代码',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `dept_name`(`dept_name`) USING BTREE,
  UNIQUE INDEX `dept_code`(`dept_code`) USING BTREE,
  INDEX `idx_dept_code`(`dept_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学院表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_departments
-- ----------------------------
INSERT INTO `sys_departments` VALUES (1, '计算机科学与技术学院', 'CST', NULL, '2026-05-14 08:48:34');
INSERT INTO `sys_departments` VALUES (2, '信息工程学院', 'IE', NULL, '2026-05-14 08:48:34');
INSERT INTO `sys_departments` VALUES (3, '电子工程学院', 'EE', NULL, '2026-05-14 08:48:34');

-- ----------------------------
-- Table structure for sys_roles
-- ----------------------------
DROP TABLE IF EXISTS `sys_roles`;
CREATE TABLE `sys_roles`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色代码',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `role_name`(`role_name`) USING BTREE,
  UNIQUE INDEX `role_code`(`role_code`) USING BTREE,
  INDEX `idx_role_code`(`role_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_roles
-- ----------------------------
INSERT INTO `sys_roles` VALUES (1, '系统管理员', 'admin', '系统管理员，拥有全部权限', '2026-05-14 08:48:34');
INSERT INTO `sys_roles` VALUES (2, '教务管理员', 'teach_admin', '教务管理员，管理全局数据', '2026-05-14 08:48:34');
INSERT INTO `sys_roles` VALUES (3, '专业负责人', 'prof_lead', '专业负责人，配置支撑矩阵', '2026-05-14 08:48:34');
INSERT INTO `sys_roles` VALUES (4, '课程教师', 'teacher', '课程教师，录入成绩', '2026-05-14 08:48:34');

-- ----------------------------
-- Table structure for sys_user_roles
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_roles`;
CREATE TABLE `sys_user_roles`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id`, `role_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_role_id`(`role_id`) USING BTREE,
  CONSTRAINT `sys_user_roles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `sys_users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `sys_user_roles_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `sys_roles` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_roles
-- ----------------------------
INSERT INTO `sys_user_roles` VALUES (1, 1, 1, '2026-05-14 08:48:34');
INSERT INTO `sys_user_roles` VALUES (2, 2, 4, '2026-05-14 08:48:34');

-- ----------------------------
-- Table structure for sys_users
-- ----------------------------
DROP TABLE IF EXISTS `sys_users`;
CREATE TABLE `sys_users`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（加密）',
  `full_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '姓名',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态 1=启用 0=禁用',
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE,
  INDEX `idx_username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_users
-- ----------------------------
INSERT INTO `sys_users` VALUES (1, 'admin', '$2a$10$slYQmyNdGzin7olVN3p5be0HJGhAZGHvBqXfKYZ8bHkD.TXM.3EJa', '管理员', 'admin@example.com', NULL, 1, '2026-05-14 08:48:34', '2026-05-14 08:48:34');
INSERT INTO `sys_users` VALUES (2, 'teacher', '$2a$10$slYQmyNdGzin7olVN3p5be0HJGhAZGHvBqXfKYZ8bHkD.TXM.3EJa', '教师', 'teacher@example.com', NULL, 1, '2026-05-14 08:48:34', '2026-05-14 08:48:34');

SET FOREIGN_KEY_CHECKS = 1;
