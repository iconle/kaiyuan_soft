/*
 Navicat Premium Data Transfer

 Source Server         : Mysql
 Source Server Type    : MySQL
 Source Server Version : 80026
 Source Host           : localhost:3306
 Source Schema         : obe_platform

 Target Server Type    : MySQL
 Target Server Version : 80026
 File Encoding         : 65001

 Date: 24/05/2026 21:20:58
*/

-- 创建 obe_platform 数据库，不存在则创建，指定字符集和排序规则
CREATE DATABASE IF NOT EXISTS obe_platform
DEFAULT CHARACTER SET utf8mb4  -- 字符集：utf8mb4 支持所有中文+emoji，是MySQL最佳字符集
DEFAULT COLLATE utf8mb4_unicode_ci;  -- 排序规则：通用 Unicode 排序，兼容性最好

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for assessment_objective
-- ----------------------------
DROP TABLE IF EXISTS `assessment_objective`;
CREATE TABLE `assessment_objective`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `assessment_id` bigint(0) NOT NULL COMMENT '考核点',
  `objective_id` bigint(0) NOT NULL COMMENT '课程目标',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_assessment_obj`(`assessment_id`, `objective_id`) USING BTREE,
  INDEX `fk_ao_objective`(`objective_id`) USING BTREE,
  CONSTRAINT `fk_ao_assessment` FOREIGN KEY (`assessment_id`) REFERENCES `assessment_point` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ao_objective` FOREIGN KEY (`objective_id`) REFERENCES `course_objective` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '考核点-目标多对多关联' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of assessment_objective
-- ----------------------------
INSERT INTO `assessment_objective` VALUES (28, 1, 1);
INSERT INTO `assessment_objective` VALUES (29, 1, 2);
INSERT INTO `assessment_objective` VALUES (30, 2, 2);
INSERT INTO `assessment_objective` VALUES (31, 2, 3);
INSERT INTO `assessment_objective` VALUES (27, 3, 1);
INSERT INTO `assessment_objective` VALUES (34, 4, 2);
INSERT INTO `assessment_objective` VALUES (32, 4, 3);
INSERT INTO `assessment_objective` VALUES (33, 4, 4);
INSERT INTO `assessment_objective` VALUES (37, 5, 2);
INSERT INTO `assessment_objective` VALUES (38, 5, 4);
INSERT INTO `assessment_objective` VALUES (43, 6, 5);
INSERT INTO `assessment_objective` VALUES (44, 6, 6);
INSERT INTO `assessment_objective` VALUES (45, 6, 7);
INSERT INTO `assessment_objective` VALUES (47, 7, 5);
INSERT INTO `assessment_objective` VALUES (46, 7, 6);
INSERT INTO `assessment_objective` VALUES (50, 8, 5);
INSERT INTO `assessment_objective` VALUES (49, 8, 6);
INSERT INTO `assessment_objective` VALUES (48, 8, 7);
INSERT INTO `assessment_objective` VALUES (39, 9, 8);
INSERT INTO `assessment_objective` VALUES (40, 10, 9);
INSERT INTO `assessment_objective` VALUES (41, 11, 8);
INSERT INTO `assessment_objective` VALUES (42, 11, 9);
INSERT INTO `assessment_objective` VALUES (16, 12, 10);
INSERT INTO `assessment_objective` VALUES (17, 12, 11);
INSERT INTO `assessment_objective` VALUES (18, 13, 11);
INSERT INTO `assessment_objective` VALUES (19, 13, 12);
INSERT INTO `assessment_objective` VALUES (21, 14, 10);
INSERT INTO `assessment_objective` VALUES (22, 14, 11);
INSERT INTO `assessment_objective` VALUES (20, 14, 12);

-- ----------------------------
-- Table structure for assessment_point
-- ----------------------------
DROP TABLE IF EXISTS `assessment_point`;
CREATE TABLE `assessment_point`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `outline_id` bigint(0) NOT NULL COMMENT '所属大纲',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '考核点名称',
  `max_score` decimal(6, 2) NOT NULL COMMENT '满分分值',
  `weight_percent` decimal(5, 2) NULL DEFAULT NULL COMMENT '占总成绩百分比(%)',
  `objective_id` bigint(0) NOT NULL COMMENT '绑定的课程目标',
  `sort_order` int(0) NULL DEFAULT NULL COMMENT '排序号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_assessment_outline`(`outline_id`) USING BTREE,
  INDEX `idx_assessment_obj`(`objective_id`) USING BTREE,
  CONSTRAINT `fk_ap_objective` FOREIGN KEY (`objective_id`) REFERENCES `course_objective` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ap_outline` FOREIGN KEY (`outline_id`) REFERENCES `course_outline` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '考核点' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of assessment_point
-- ----------------------------
INSERT INTO `assessment_point` VALUES (1, 1, '课后作业', 100.00, 20.00, 1, 1);
INSERT INTO `assessment_point` VALUES (2, 1, '上机实验', 100.00, 20.00, 2, 2);
INSERT INTO `assessment_point` VALUES (3, 1, '期中笔试', 100.00, 25.00, 1, 3);
INSERT INTO `assessment_point` VALUES (4, 1, '期末笔试', 100.00, 25.00, 3, 4);
INSERT INTO `assessment_point` VALUES (5, 1, '课程设计报告', 100.00, 10.00, 4, 5);
INSERT INTO `assessment_point` VALUES (6, 2, '课堂测验', 100.00, 20.00, 5, 1);
INSERT INTO `assessment_point` VALUES (7, 2, '编程作业', 100.00, 30.00, 6, 2);
INSERT INTO `assessment_point` VALUES (8, 2, '期末上机考试', 100.00, 50.00, 7, 3);
INSERT INTO `assessment_point` VALUES (9, 3, '平时作业', 100.00, 20.00, 8, 1);
INSERT INTO `assessment_point` VALUES (10, 3, '实验报告', 100.00, 30.00, 9, 2);
INSERT INTO `assessment_point` VALUES (11, 3, '期末考试', 100.00, 50.00, 8, 3);
INSERT INTO `assessment_point` VALUES (12, 4, '平时成绩', 100.00, 30.00, 10, 1);
INSERT INTO `assessment_point` VALUES (13, 4, '实验课成绩', 100.00, 20.00, 11, 2);
INSERT INTO `assessment_point` VALUES (14, 4, '期末成绩', 40.00, 50.00, 12, 3);

-- ----------------------------
-- Table structure for assessment_question
-- ----------------------------
DROP TABLE IF EXISTS `assessment_question`;
CREATE TABLE `assessment_question`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `assessment_id` bigint(0) NOT NULL COMMENT '所属考核点',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目名称，如\"期末大题1\"',
  `max_score` decimal(6, 2) NOT NULL COMMENT '满分分值',
  `sort_order` int(0) NULL DEFAULT NULL COMMENT '排序号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_question_assessment`(`assessment_id`) USING BTREE,
  CONSTRAINT `fk_q_assessment` FOREIGN KEY (`assessment_id`) REFERENCES `assessment_point` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '考核点题目' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of assessment_question
-- ----------------------------
INSERT INTO `assessment_question` VALUES (1, 1, '课后作业1', 50.00, 1);
INSERT INTO `assessment_question` VALUES (2, 1, '课后作业2', 50.00, 2);
INSERT INTO `assessment_question` VALUES (3, 2, '上机实验1', 30.00, 1);
INSERT INTO `assessment_question` VALUES (4, 2, '上机实验2', 30.00, 2);
INSERT INTO `assessment_question` VALUES (5, 2, '上机实验3', 40.00, 3);
INSERT INTO `assessment_question` VALUES (6, 3, '期中笔试选择题', 20.00, 1);
INSERT INTO `assessment_question` VALUES (7, 3, '其中笔试填空题', 20.00, 2);
INSERT INTO `assessment_question` VALUES (8, 3, '其中笔试应用题', 25.00, 3);
INSERT INTO `assessment_question` VALUES (9, 3, '期中笔试大题一', 10.00, 4);
INSERT INTO `assessment_question` VALUES (10, 3, '期中笔试大题二', 10.00, 5);
INSERT INTO `assessment_question` VALUES (11, 3, '期中笔试大题三', 15.00, 6);
INSERT INTO `assessment_question` VALUES (13, 4, '期末考试选择题1-5', 10.00, 1);
INSERT INTO `assessment_question` VALUES (14, 4, '期末考试选择题6-10', 10.00, 2);
INSERT INTO `assessment_question` VALUES (15, 4, '期末考试填空题11-15', 10.00, 3);
INSERT INTO `assessment_question` VALUES (16, 4, '期末考试填空题16-20', 10.00, 4);
INSERT INTO `assessment_question` VALUES (17, 4, '期末考试应用题1', 10.00, 5);
INSERT INTO `assessment_question` VALUES (18, 4, '期末考试应用题2', 10.00, 6);
INSERT INTO `assessment_question` VALUES (19, 4, '期末考试大题1', 10.00, 7);
INSERT INTO `assessment_question` VALUES (20, 4, '期末考试大题2', 15.00, 8);
INSERT INTO `assessment_question` VALUES (21, 4, '期末考试大题3', 15.00, 9);
INSERT INTO `assessment_question` VALUES (22, 5, '课程报告', 100.00, 1);
INSERT INTO `assessment_question` VALUES (23, 9, '平时作业1', 30.00, 1);
INSERT INTO `assessment_question` VALUES (24, 9, '平时作业2', 30.00, 2);
INSERT INTO `assessment_question` VALUES (25, 9, '平时作业3', 40.00, 3);
INSERT INTO `assessment_question` VALUES (26, 10, '实验报告1', 50.00, 1);
INSERT INTO `assessment_question` VALUES (27, 10, '实验报告2', 50.00, 2);
INSERT INTO `assessment_question` VALUES (28, 11, '期末考试题型一', 10.00, 1);
INSERT INTO `assessment_question` VALUES (29, 11, '期末考试题型二', 20.00, 2);
INSERT INTO `assessment_question` VALUES (30, 11, '期末考试题型三', 30.00, 3);
INSERT INTO `assessment_question` VALUES (31, 11, '期末考试题型4', 40.00, 4);
INSERT INTO `assessment_question` VALUES (32, 12, '平时作业1', 30.00, 1);
INSERT INTO `assessment_question` VALUES (33, 12, '平时作业2', 30.00, 2);
INSERT INTO `assessment_question` VALUES (34, 12, '平时作业3', 40.00, 3);
INSERT INTO `assessment_question` VALUES (35, 13, '实验1', 50.00, 1);
INSERT INTO `assessment_question` VALUES (36, 13, '实验2', 50.00, 2);
INSERT INTO `assessment_question` VALUES (37, 14, '期末题型一', 10.00, 1);
INSERT INTO `assessment_question` VALUES (38, 14, '期末题型二', 20.00, 2);
INSERT INTO `assessment_question` VALUES (39, 14, '期末题型三', 30.00, 3);
INSERT INTO `assessment_question` VALUES (40, 14, '期末大题一', 20.00, 4);
INSERT INTO `assessment_question` VALUES (41, 14, '期末大题二', 20.00, 5);
INSERT INTO `assessment_question` VALUES (42, 6, '课堂测验1', 30.00, 1);
INSERT INTO `assessment_question` VALUES (43, 6, '课堂测验2', 30.00, 2);
INSERT INTO `assessment_question` VALUES (44, 6, '课堂测验3', 40.00, 3);
INSERT INTO `assessment_question` VALUES (45, 7, '编程作业1', 50.00, 1);
INSERT INTO `assessment_question` VALUES (46, 7, '编程作业2', 50.00, 2);
INSERT INTO `assessment_question` VALUES (47, 8, '期末题型一', 20.00, 1);
INSERT INTO `assessment_question` VALUES (48, 8, '期末题型二', 20.00, 2);
INSERT INTO `assessment_question` VALUES (49, 8, '期末题型三', 60.00, 3);

-- ----------------------------
-- Table structure for class_student
-- ----------------------------
DROP TABLE IF EXISTS `class_student`;
CREATE TABLE `class_student`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `class_id` bigint(0) NOT NULL COMMENT '教学班级',
  `student_id` bigint(0) NOT NULL COMMENT '学生',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_class_student`(`class_id`, `student_id`) USING BTREE,
  INDEX `idx_class_student_class`(`class_id`) USING BTREE,
  INDEX `fk_cs_student`(`student_id`) USING BTREE,
  CONSTRAINT `fk_cs_class` FOREIGN KEY (`class_id`) REFERENCES `teaching_class` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_cs_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 65 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '班级学生关联' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of class_student
-- ----------------------------
INSERT INTO `class_student` VALUES (1, 1, 1);
INSERT INTO `class_student` VALUES (2, 1, 2);
INSERT INTO `class_student` VALUES (3, 1, 3);
INSERT INTO `class_student` VALUES (4, 1, 4);
INSERT INTO `class_student` VALUES (5, 1, 5);
INSERT INTO `class_student` VALUES (32, 2, 1);
INSERT INTO `class_student` VALUES (33, 2, 2);
INSERT INTO `class_student` VALUES (34, 2, 3);
INSERT INTO `class_student` VALUES (35, 2, 4);
INSERT INTO `class_student` VALUES (36, 2, 5);
INSERT INTO `class_student` VALUES (65, 3, 1);
INSERT INTO `class_student` VALUES (67, 3, 2);
INSERT INTO `class_student` VALUES (44, 3, 3);
INSERT INTO `class_student` VALUES (66, 3, 4);
INSERT INTO `class_student` VALUES (68, 3, 5);
INSERT INTO `class_student` VALUES (45, 4, 1);
INSERT INTO `class_student` VALUES (46, 4, 2);
INSERT INTO `class_student` VALUES (47, 4, 3);
INSERT INTO `class_student` VALUES (48, 4, 4);
INSERT INTO `class_student` VALUES (49, 4, 5);

-- ----------------------------
-- Table structure for course
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '课程代码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '课程名称',
  `credit` decimal(3, 1) NULL DEFAULT NULL COMMENT '学分',
  `hours_theory` int(0) NULL DEFAULT NULL COMMENT '理论学时',
  `hours_experiment` int(0) NULL DEFAULT NULL COMMENT '实验学时',
  `category` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类别 (必修/选修)',
  `major_id` bigint(0) NULL DEFAULT NULL COMMENT '所属专业',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_course_code`(`code`) USING BTREE,
  INDEX `idx_major`(`major_id`) USING BTREE,
  CONSTRAINT `fk_course_major` FOREIGN KEY (`major_id`) REFERENCES `sys_major` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程库' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course
-- ----------------------------
INSERT INTO `course` VALUES (1, 'CS1001', '数据结构', 4.0, 64, 16, '必修', 1);
INSERT INTO `course` VALUES (2, 'CS1002', '高等数学B', 5.0, 80, 0, '必修', 1);
INSERT INTO `course` VALUES (3, 'CS1003', '线性代数', 3.0, 48, 0, '必修', 1);
INSERT INTO `course` VALUES (4, 'CS1004', '大学物理B', 4.0, 64, 16, '必修', 1);
INSERT INTO `course` VALUES (5, 'CS1005', 'C语言程序设计', 3.5, 48, 16, '必修', 1);
INSERT INTO `course` VALUES (6, 'CS1006', '计算机网络', 3.0, 48, 0, '必修', 1);
INSERT INTO `course` VALUES (7, 'CS1007', '数据库原理', 3.0, 48, 16, '必修', 1);
INSERT INTO `course` VALUES (8, 'CS1008', '学科教学理论与实践', 2.0, 32, 0, '必修', 1);

-- ----------------------------
-- Table structure for course_achievement
-- ----------------------------
DROP TABLE IF EXISTS `course_achievement`;
CREATE TABLE `course_achievement`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `class_id` bigint(0) NOT NULL COMMENT '教学班级',
  `indicator_id` bigint(0) NOT NULL COMMENT '指标点',
  `achievement` decimal(6, 4) NOT NULL COMMENT '课程级达成度 E_k',
  `calc_time` datetime(0) NOT NULL COMMENT '计算时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_class_indicator`(`class_id`, `indicator_id`) USING BTREE,
  INDEX `idx_course_achievement_indicator`(`indicator_id`) USING BTREE,
  CONSTRAINT `fk_ca_class` FOREIGN KEY (`class_id`) REFERENCES `teaching_class` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ca_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `indicator` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程级达成度 (第二级计算结果)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_achievement
-- ----------------------------
INSERT INTO `course_achievement` VALUES (49, 1, 1, 0.7935, '2026-05-24 13:38:37');
INSERT INTO `course_achievement` VALUES (50, 1, 3, 0.7935, '2026-05-24 13:38:37');
INSERT INTO `course_achievement` VALUES (51, 1, 4, 0.8538, '2026-05-24 13:38:37');
INSERT INTO `course_achievement` VALUES (52, 1, 8, 0.8104, '2026-05-24 13:38:37');
INSERT INTO `course_achievement` VALUES (53, 3, 3, 0.7863, '2026-05-24 14:04:30');
INSERT INTO `course_achievement` VALUES (54, 3, 7, 0.7895, '2026-05-24 14:04:30');
INSERT INTO `course_achievement` VALUES (55, 4, 3, 0.8119, '2026-05-24 14:22:49');
INSERT INTO `course_achievement` VALUES (56, 4, 7, 0.8611, '2026-05-24 14:22:49');
INSERT INTO `course_achievement` VALUES (57, 2, 3, 0.8880, '2026-05-24 14:42:02');
INSERT INTO `course_achievement` VALUES (58, 2, 5, 0.8950, '2026-05-24 14:42:02');
INSERT INTO `course_achievement` VALUES (59, 2, 6, 0.8930, '2026-05-24 14:42:02');
INSERT INTO `course_achievement` VALUES (60, 2, 8, 0.9000, '2026-05-24 14:42:02');

-- ----------------------------
-- Table structure for course_objective
-- ----------------------------
DROP TABLE IF EXISTS `course_objective`;
CREATE TABLE `course_objective`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `outline_id` bigint(0) NOT NULL COMMENT '所属大纲',
  `obj_no` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标编号, 如 1-1',
  `dimension` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '维度 (知识/能力/价值)',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标描述',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_outline`(`outline_id`) USING BTREE,
  CONSTRAINT `fk_obj_outline` FOREIGN KEY (`outline_id`) REFERENCES `course_outline` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程目标' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_objective
-- ----------------------------
INSERT INTO `course_objective` VALUES (1, 1, '1-1', '知识', '掌握线性表、栈、队列、树、图等基本数据结构的逻辑结构和物理存储实现，理解各类数据结构的特点和适用场景');
INSERT INTO `course_objective` VALUES (2, 1, '1-2', '能力', '能够根据实际应用场景选择合适的数据结构，综合运用所学知识设计高效算法解决复杂工程问题');
INSERT INTO `course_objective` VALUES (3, 1, '1-3', '能力', '掌握算法时间复杂度与空间复杂度的分析方法，能够评估和比较不同算法的效率并做出合理选择');
INSERT INTO `course_objective` VALUES (4, 1, '1-4', '价值', '培养严谨的程序设计习惯、计算思维能力和团队协作精神，树立软件工程职业道德意识');
INSERT INTO `course_objective` VALUES (5, 2, '2-1', '知识', '掌握C语言基本语法、数据类型、运算符与表达式、控制结构及函数的定义与调用方法');
INSERT INTO `course_objective` VALUES (6, 2, '2-2', '能力', '能够运用结构化程序设计方法，综合使用数组、指针和结构体等工具解决实际编程问题');
INSERT INTO `course_objective` VALUES (7, 2, '2-3', '能力', '掌握文件操作和模块化程序设计技术，具备独立开发中小规模应用程序的能力');
INSERT INTO `course_objective` VALUES (8, 3, '3-1', '知识', '理解计算机网络体系结构、OSI和TCP/IP参考模型，掌握物理层、数据链路层、网络层、传输层及应用层的核心协议原理');
INSERT INTO `course_objective` VALUES (9, 3, '3-2', '能力', '能够使用网络抓包工具分析协议报文格式，具备网络故障排查和中小规模组网设计的基本能力');
INSERT INTO `course_objective` VALUES (10, 4, '1-1', '知识', '123');
INSERT INTO `course_objective` VALUES (11, 4, '2-1', '能力', '123');
INSERT INTO `course_objective` VALUES (12, 4, '2-2', '价值', '12312');

-- ----------------------------
-- Table structure for course_outline
-- ----------------------------
DROP TABLE IF EXISTS `course_outline`;
CREATE TABLE `course_outline`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `class_id` bigint(0) NOT NULL COMMENT '所属教学班级 (一对一)',
  `status` enum('DRAFT','LOCKED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '大纲状态',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_class`(`class_id`) USING BTREE,
  CONSTRAINT `fk_outline_class` FOREIGN KEY (`class_id`) REFERENCES `teaching_class` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程大纲' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_outline
-- ----------------------------
INSERT INTO `course_outline` VALUES (1, 1, 'DRAFT');
INSERT INTO `course_outline` VALUES (2, 2, 'DRAFT');
INSERT INTO `course_outline` VALUES (3, 3, 'DRAFT');
INSERT INTO `course_outline` VALUES (4, 4, 'DRAFT');

-- ----------------------------
-- Table structure for grad_requirement
-- ----------------------------
DROP TABLE IF EXISTS `grad_requirement`;
CREATE TABLE `grad_requirement`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `major_id` bigint(0) NOT NULL COMMENT '所属专业',
  `req_no` int(0) NOT NULL COMMENT '编号 (1~8)',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '要求标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '详细描述',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_major`(`major_id`) USING BTREE,
  CONSTRAINT `fk_grad_req_major` FOREIGN KEY (`major_id`) REFERENCES `sys_major` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '毕业要求' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of grad_requirement
-- ----------------------------
INSERT INTO `grad_requirement` VALUES (1, 1, 1, '师德规范', '遵守法律法规，具有人文底蕴和科学精神');
INSERT INTO `grad_requirement` VALUES (2, 1, 2, '教育情怀', '具有从教意愿和积极从教的心理');
INSERT INTO `grad_requirement` VALUES (3, 1, 3, '学科素养', '掌握所教学科的基本知识、基本原理和基本技能');
INSERT INTO `grad_requirement` VALUES (4, 1, 4, '教学能力', '在教育实践中能够进行教学设计、实施和评价');
INSERT INTO `grad_requirement` VALUES (5, 1, 5, '班级指导', '掌握班级组织与建设的工作规律和基本方法');
INSERT INTO `grad_requirement` VALUES (6, 1, 6, '综合育人', '了解学科育人的理论与方法');
INSERT INTO `grad_requirement` VALUES (7, 1, 7, '学会反思', '具有终身学习与专业发展意识');
INSERT INTO `grad_requirement` VALUES (8, 1, 8, '沟通合作', '理解学习共同体的作用，具有团队协作精神');

-- ----------------------------
-- Table structure for indicator
-- ----------------------------
DROP TABLE IF EXISTS `indicator`;
CREATE TABLE `indicator`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `grad_req_id` bigint(0) NOT NULL COMMENT '所属毕业要求',
  `indicator_no` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '编号, 如 3-1',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标点描述',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_grad_req`(`grad_req_id`) USING BTREE,
  CONSTRAINT `fk_indicator_grad_req` FOREIGN KEY (`grad_req_id`) REFERENCES `grad_requirement` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '毕业要求指标点' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of indicator
-- ----------------------------
INSERT INTO `indicator` VALUES (1, 3, '3-1', '掌握数学、物理等自然科学基础知识，能用于解释计算机领域的复杂问题');
INSERT INTO `indicator` VALUES (3, 3, '3-3', '掌握计算机系统的分析与设计的基本方法，具有综合运用所学理论和技术分析解决实际问题的能力');
INSERT INTO `indicator` VALUES (4, 3, '3-4', '了解信息技术教育的理论和方法，具有开展中学信息技术教学的能力');
INSERT INTO `indicator` VALUES (5, 4, '4-1', '能够基于学科教学知识进行教学设计');
INSERT INTO `indicator` VALUES (6, 4, '4-2', '能够依据课程标准进行教学实施和评价');
INSERT INTO `indicator` VALUES (7, 4, '4-3', '能够运用信息技术手段辅助教学');
INSERT INTO `indicator` VALUES (8, 7, '7-1', '具有终身学习与专业发展意识，了解计算机学科发展前沿');

-- ----------------------------
-- Table structure for macro_support_matrix
-- ----------------------------
DROP TABLE IF EXISTS `macro_support_matrix`;
CREATE TABLE `macro_support_matrix`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `course_id` bigint(0) NOT NULL COMMENT '课程',
  `indicator_id` bigint(0) NOT NULL COMMENT '指标点',
  `support_level` enum('H','M','L') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '支撑强度 (参考标记)',
  `weight` decimal(5, 4) NULL DEFAULT NULL COMMENT '总支撑权重 W_c',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_course_indicator`(`course_id`, `indicator_id`) USING BTREE,
  INDEX `idx_macro_matrix_indicator`(`indicator_id`) USING BTREE,
  CONSTRAINT `fk_macro_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_macro_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `indicator` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '宏观支撑矩阵 (含权重W_c)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of macro_support_matrix
-- ----------------------------
INSERT INTO `macro_support_matrix` VALUES (63, 1, 1, 'H', 1.0000);
INSERT INTO `macro_support_matrix` VALUES (64, 1, 3, 'H', 0.3000);
INSERT INTO `macro_support_matrix` VALUES (65, 1, 4, 'M', 1.0000);
INSERT INTO `macro_support_matrix` VALUES (66, 5, 3, 'M', 0.3000);
INSERT INTO `macro_support_matrix` VALUES (67, 5, 5, 'H', 1.0000);
INSERT INTO `macro_support_matrix` VALUES (68, 5, 6, 'M', 1.0000);
INSERT INTO `macro_support_matrix` VALUES (69, 6, 3, 'M', 0.4000);
INSERT INTO `macro_support_matrix` VALUES (70, 6, 7, 'H', 1.0000);
INSERT INTO `macro_support_matrix` VALUES (71, 1, 8, 'M', 0.6000);
INSERT INTO `macro_support_matrix` VALUES (72, 5, 8, 'M', 0.4000);

-- ----------------------------
-- Table structure for major_achievement
-- ----------------------------
DROP TABLE IF EXISTS `major_achievement`;
CREATE TABLE `major_achievement`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `major_id` bigint(0) NOT NULL COMMENT '专业',
  `indicator_id` bigint(0) NOT NULL COMMENT '指标点',
  `semester_id` bigint(0) NOT NULL COMMENT '学年学期',
  `achievement` decimal(6, 4) NOT NULL COMMENT '专业级达成度 G_k',
  `calc_time` datetime(0) NOT NULL COMMENT '计算时间',
  `triggered_by` bigint(0) NULL DEFAULT NULL COMMENT '触发者',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_major_indicator_semester`(`major_id`, `indicator_id`, `semester_id`) USING BTREE,
  INDEX `fk_ma_indicator`(`indicator_id`) USING BTREE,
  INDEX `fk_ma_semester`(`semester_id`) USING BTREE,
  INDEX `fk_ma_user`(`triggered_by`) USING BTREE,
  CONSTRAINT `fk_ma_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `indicator` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ma_major` FOREIGN KEY (`major_id`) REFERENCES `sys_major` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ma_semester` FOREIGN KEY (`semester_id`) REFERENCES `sys_dict_semester` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ma_user` FOREIGN KEY (`triggered_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专业级达成度 (第三级计算结果)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of major_achievement
-- ----------------------------
INSERT INTO `major_achievement` VALUES (50, 1, 1, 1, 0.7935, '2026-05-24 17:05:50', 1);
INSERT INTO `major_achievement` VALUES (51, 1, 3, 1, 0.8241, '2026-05-24 17:05:50', 1);
INSERT INTO `major_achievement` VALUES (52, 1, 4, 1, 0.8538, '2026-05-24 17:05:50', 1);
INSERT INTO `major_achievement` VALUES (53, 1, 5, 1, 0.8950, '2026-05-24 17:05:50', 1);
INSERT INTO `major_achievement` VALUES (54, 1, 6, 1, 0.8930, '2026-05-24 17:05:50', 1);
INSERT INTO `major_achievement` VALUES (55, 1, 7, 1, 0.8253, '2026-05-24 17:05:50', 1);
INSERT INTO `major_achievement` VALUES (56, 1, 8, 1, 0.8462, '2026-05-24 17:05:50', 1);

-- ----------------------------
-- Table structure for obj_achievement
-- ----------------------------
DROP TABLE IF EXISTS `obj_achievement`;
CREATE TABLE `obj_achievement`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `class_id` bigint(0) NOT NULL COMMENT '教学班级',
  `objective_id` bigint(0) NOT NULL COMMENT '课程目标',
  `achievement` decimal(6, 4) NOT NULL COMMENT '班级目标达成度',
  `calc_time` datetime(0) NOT NULL COMMENT '计算时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_class_objective`(`class_id`, `objective_id`) USING BTREE,
  INDEX `fk_oa_objective`(`objective_id`) USING BTREE,
  CONSTRAINT `fk_oa_class` FOREIGN KEY (`class_id`) REFERENCES `teaching_class` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_oa_objective` FOREIGN KEY (`objective_id`) REFERENCES `course_objective` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '目标级达成度 (第一级计算结果)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of obj_achievement
-- ----------------------------
INSERT INTO `obj_achievement` VALUES (49, 1, 1, 0.8070, '2026-05-24 13:38:37');
INSERT INTO `obj_achievement` VALUES (50, 1, 2, 0.8407, '2026-05-24 13:38:37');
INSERT INTO `obj_achievement` VALUES (51, 1, 3, 0.7800, '2026-05-24 13:38:37');
INSERT INTO `obj_achievement` VALUES (52, 1, 4, 0.8538, '2026-05-24 13:38:37');
INSERT INTO `obj_achievement` VALUES (53, 3, 8, 0.7800, '2026-05-24 14:04:30');
INSERT INTO `obj_achievement` VALUES (54, 3, 9, 0.7958, '2026-05-24 14:04:30');
INSERT INTO `obj_achievement` VALUES (55, 4, 10, 0.7767, '2026-05-24 14:22:49');
INSERT INTO `obj_achievement` VALUES (56, 4, 11, 0.8354, '2026-05-24 14:22:49');
INSERT INTO `obj_achievement` VALUES (57, 4, 12, 0.8611, '2026-05-24 14:22:49');
INSERT INTO `obj_achievement` VALUES (58, 2, 5, 0.8859, '2026-05-24 14:42:02');
INSERT INTO `obj_achievement` VALUES (59, 2, 6, 0.8900, '2026-05-24 14:42:02');
INSERT INTO `obj_achievement` VALUES (60, 2, 7, 0.9000, '2026-05-24 14:42:02');

-- ----------------------------
-- Table structure for objective_indicator_weight
-- ----------------------------
DROP TABLE IF EXISTS `objective_indicator_weight`;
CREATE TABLE `objective_indicator_weight`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `objective_id` bigint(0) NOT NULL COMMENT '课程目标',
  `indicator_id` bigint(0) NOT NULL COMMENT '毕业要求指标点',
  `weight` decimal(5, 4) NOT NULL COMMENT '内部贡献权重 w_jk',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_obj_indicator`(`objective_id`, `indicator_id`) USING BTREE,
  INDEX `idx_obj_weight_indicator`(`indicator_id`) USING BTREE,
  CONSTRAINT `fk_oiw_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `indicator` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_oiw_objective` FOREIGN KEY (`objective_id`) REFERENCES `course_objective` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '课程目标对指标点的内部贡献权重' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of objective_indicator_weight
-- ----------------------------
INSERT INTO `objective_indicator_weight` VALUES (15, 1, 1, 0.5000);
INSERT INTO `objective_indicator_weight` VALUES (16, 1, 3, 0.5000);
INSERT INTO `objective_indicator_weight` VALUES (17, 2, 8, 0.5000);
INSERT INTO `objective_indicator_weight` VALUES (18, 3, 1, 0.5000);
INSERT INTO `objective_indicator_weight` VALUES (19, 3, 3, 0.5000);
INSERT INTO `objective_indicator_weight` VALUES (20, 3, 8, 0.5000);
INSERT INTO `objective_indicator_weight` VALUES (21, 4, 4, 1.0000);
INSERT INTO `objective_indicator_weight` VALUES (22, 10, 3, 0.4000);
INSERT INTO `objective_indicator_weight` VALUES (23, 11, 3, 0.6000);
INSERT INTO `objective_indicator_weight` VALUES (24, 12, 7, 1.0000);
INSERT INTO `objective_indicator_weight` VALUES (25, 8, 3, 0.6000);
INSERT INTO `objective_indicator_weight` VALUES (26, 8, 7, 0.4000);
INSERT INTO `objective_indicator_weight` VALUES (27, 9, 3, 0.4000);
INSERT INTO `objective_indicator_weight` VALUES (28, 9, 7, 0.6000);
INSERT INTO `objective_indicator_weight` VALUES (29, 5, 3, 0.5000);
INSERT INTO `objective_indicator_weight` VALUES (30, 5, 6, 0.5000);
INSERT INTO `objective_indicator_weight` VALUES (31, 6, 3, 0.5000);
INSERT INTO `objective_indicator_weight` VALUES (32, 6, 5, 0.5000);
INSERT INTO `objective_indicator_weight` VALUES (33, 7, 5, 0.5000);
INSERT INTO `objective_indicator_weight` VALUES (34, 7, 6, 0.5000);
INSERT INTO `objective_indicator_weight` VALUES (35, 7, 8, 1.0000);

-- ----------------------------
-- Table structure for question_objective
-- ----------------------------
DROP TABLE IF EXISTS `question_objective`;
CREATE TABLE `question_objective`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(0) NOT NULL COMMENT '题目',
  `objective_id` bigint(0) NOT NULL COMMENT '课程目标',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_question_obj`(`question_id`, `objective_id`) USING BTREE,
  INDEX `fk_qo_objective`(`objective_id`) USING BTREE,
  CONSTRAINT `fk_qo_objective` FOREIGN KEY (`objective_id`) REFERENCES `course_objective` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_qo_question` FOREIGN KEY (`question_id`) REFERENCES `assessment_question` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '题目-目标关联' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of question_objective
-- ----------------------------
INSERT INTO `question_objective` VALUES (3, 1, 1);
INSERT INTO `question_objective` VALUES (6, 2, 1);
INSERT INTO `question_objective` VALUES (5, 2, 2);
INSERT INTO `question_objective` VALUES (7, 3, 2);
INSERT INTO `question_objective` VALUES (8, 4, 3);
INSERT INTO `question_objective` VALUES (9, 5, 2);
INSERT INTO `question_objective` VALUES (10, 5, 3);
INSERT INTO `question_objective` VALUES (11, 6, 1);
INSERT INTO `question_objective` VALUES (12, 7, 1);
INSERT INTO `question_objective` VALUES (13, 8, 1);
INSERT INTO `question_objective` VALUES (14, 9, 1);
INSERT INTO `question_objective` VALUES (15, 10, 1);
INSERT INTO `question_objective` VALUES (16, 11, 1);
INSERT INTO `question_objective` VALUES (19, 13, 2);
INSERT INTO `question_objective` VALUES (20, 14, 3);
INSERT INTO `question_objective` VALUES (21, 15, 3);
INSERT INTO `question_objective` VALUES (22, 16, 3);
INSERT INTO `question_objective` VALUES (23, 17, 3);
INSERT INTO `question_objective` VALUES (24, 17, 4);
INSERT INTO `question_objective` VALUES (25, 18, 3);
INSERT INTO `question_objective` VALUES (26, 18, 4);
INSERT INTO `question_objective` VALUES (27, 19, 2);
INSERT INTO `question_objective` VALUES (28, 19, 3);
INSERT INTO `question_objective` VALUES (29, 19, 4);
INSERT INTO `question_objective` VALUES (30, 20, 2);
INSERT INTO `question_objective` VALUES (31, 20, 3);
INSERT INTO `question_objective` VALUES (32, 20, 4);
INSERT INTO `question_objective` VALUES (33, 21, 2);
INSERT INTO `question_objective` VALUES (34, 21, 3);
INSERT INTO `question_objective` VALUES (35, 21, 4);
INSERT INTO `question_objective` VALUES (36, 22, 2);
INSERT INTO `question_objective` VALUES (37, 22, 4);
INSERT INTO `question_objective` VALUES (38, 23, 8);
INSERT INTO `question_objective` VALUES (39, 24, 8);
INSERT INTO `question_objective` VALUES (40, 25, 8);
INSERT INTO `question_objective` VALUES (41, 26, 9);
INSERT INTO `question_objective` VALUES (42, 27, 9);
INSERT INTO `question_objective` VALUES (43, 28, 8);
INSERT INTO `question_objective` VALUES (44, 29, 9);
INSERT INTO `question_objective` VALUES (46, 30, 8);
INSERT INTO `question_objective` VALUES (45, 30, 9);
INSERT INTO `question_objective` VALUES (47, 31, 8);
INSERT INTO `question_objective` VALUES (48, 31, 9);
INSERT INTO `question_objective` VALUES (49, 32, 10);
INSERT INTO `question_objective` VALUES (50, 33, 11);
INSERT INTO `question_objective` VALUES (51, 34, 10);
INSERT INTO `question_objective` VALUES (52, 34, 11);
INSERT INTO `question_objective` VALUES (53, 35, 11);
INSERT INTO `question_objective` VALUES (54, 35, 12);
INSERT INTO `question_objective` VALUES (55, 36, 11);
INSERT INTO `question_objective` VALUES (56, 36, 12);
INSERT INTO `question_objective` VALUES (57, 37, 10);
INSERT INTO `question_objective` VALUES (58, 37, 12);
INSERT INTO `question_objective` VALUES (59, 38, 11);
INSERT INTO `question_objective` VALUES (61, 39, 11);
INSERT INTO `question_objective` VALUES (60, 39, 12);
INSERT INTO `question_objective` VALUES (62, 40, 10);
INSERT INTO `question_objective` VALUES (63, 40, 11);
INSERT INTO `question_objective` VALUES (64, 40, 12);
INSERT INTO `question_objective` VALUES (65, 41, 10);
INSERT INTO `question_objective` VALUES (66, 41, 11);
INSERT INTO `question_objective` VALUES (67, 41, 12);
INSERT INTO `question_objective` VALUES (68, 42, 5);
INSERT INTO `question_objective` VALUES (69, 43, 6);
INSERT INTO `question_objective` VALUES (70, 44, 5);
INSERT INTO `question_objective` VALUES (71, 44, 6);
INSERT INTO `question_objective` VALUES (72, 44, 7);
INSERT INTO `question_objective` VALUES (73, 45, 5);
INSERT INTO `question_objective` VALUES (74, 46, 5);
INSERT INTO `question_objective` VALUES (75, 46, 6);
INSERT INTO `question_objective` VALUES (76, 47, 5);
INSERT INTO `question_objective` VALUES (77, 48, 5);
INSERT INTO `question_objective` VALUES (78, 48, 6);
INSERT INTO `question_objective` VALUES (79, 49, 5);
INSERT INTO `question_objective` VALUES (80, 49, 6);
INSERT INTO `question_objective` VALUES (81, 49, 7);

-- ----------------------------
-- Table structure for score_sheet
-- ----------------------------
DROP TABLE IF EXISTS `score_sheet`;
CREATE TABLE `score_sheet`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `class_id` bigint(0) NOT NULL COMMENT '教学班级',
  `status` enum('EMPTY','IMPORTED','LOCKED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EMPTY' COMMENT '成绩单状态',
  `locked_at` datetime(0) NULL DEFAULT NULL COMMENT '锁定时间',
  `locked_by` bigint(0) NULL DEFAULT NULL COMMENT '执行锁定的操作人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_class`(`class_id`) USING BTREE,
  INDEX `idx_locked_by`(`locked_by`) USING BTREE,
  CONSTRAINT `fk_sheet_class` FOREIGN KEY (`class_id`) REFERENCES `teaching_class` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sheet_user` FOREIGN KEY (`locked_by`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '成绩单主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of score_sheet
-- ----------------------------
INSERT INTO `score_sheet` VALUES (1, 1, 'LOCKED', '2026-05-24 13:38:37', 4);
INSERT INTO `score_sheet` VALUES (2, 2, 'LOCKED', '2026-05-24 14:42:02', 5);
INSERT INTO `score_sheet` VALUES (3, 3, 'LOCKED', '2026-05-24 14:04:30', 4);
INSERT INTO `score_sheet` VALUES (4, 4, 'LOCKED', '2026-05-24 14:22:49', 4);

-- ----------------------------
-- Table structure for score_unlock_request
-- ----------------------------
DROP TABLE IF EXISTS `score_unlock_request`;
CREATE TABLE `score_unlock_request`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `sheet_id` bigint(0) NOT NULL COMMENT '成绩单ID',
  `class_id` bigint(0) NOT NULL COMMENT '教学班级ID',
  `requester_id` bigint(0) NOT NULL COMMENT '申请人（主讲教师）',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '勘误原因',
  `status` enum('PENDING','APPROVED','REJECTED','UNLOCKED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '工单状态: PENDING=待教务审核, APPROVED=教务已同意待管理员解锁, REJECTED=已拒绝, UNLOCKED=管理员已解锁',
  `reviewer_id` bigint(0) NULL DEFAULT NULL COMMENT '审批人',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `reviewed_at` datetime(0) NULL DEFAULT NULL COMMENT '审批时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sheet`(`sheet_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `fk_ur_requester`(`requester_id`) USING BTREE,
  INDEX `fk_ur_reviewer`(`reviewer_id`) USING BTREE,
  CONSTRAINT `fk_ur_requester` FOREIGN KEY (`requester_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ur_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ur_sheet` FOREIGN KEY (`sheet_id`) REFERENCES `score_sheet` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '成绩勘误工单（解锁请求）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of score_unlock_request
-- ----------------------------
INSERT INTO `score_unlock_request` VALUES (1, 1, 1, 4, '成绩录入错误，请求解锁', 'REJECTED', 2, '2026-05-23 18:05:21', '2026-05-23 18:15:55');
INSERT INTO `score_unlock_request` VALUES (2, 1, 1, 4, '再次申请', 'UNLOCKED', 1, '2026-05-23 18:16:13', '2026-05-23 18:40:59');
INSERT INTO `score_unlock_request` VALUES (3, 1, 1, 4, '提交申请', 'REJECTED', 1, '2026-05-23 22:39:11', '2026-05-23 22:48:13');
INSERT INTO `score_unlock_request` VALUES (4, 1, 1, 4, '申请', 'UNLOCKED', 1, '2026-05-23 22:48:59', '2026-05-23 22:49:16');
INSERT INTO `score_unlock_request` VALUES (5, 1, 1, 4, '修改数据', 'UNLOCKED', 1, '2026-05-23 23:22:58', '2026-05-23 23:23:13');

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `student_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学号',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '姓名',
  `college_id` bigint(0) NULL DEFAULT NULL COMMENT '所属学院',
  `major_id` bigint(0) NULL DEFAULT NULL COMMENT '专业',
  `enrollment_year` int(0) NULL DEFAULT NULL COMMENT '入学年份',
  `admin_class_id` bigint(0) NULL DEFAULT NULL COMMENT '所属行政班级',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_no`(`student_no`) USING BTREE,
  INDEX `idx_major`(`major_id`) USING BTREE,
  INDEX `idx_admin_class`(`admin_class_id`) USING BTREE,
  INDEX `idx_student_college`(`college_id`) USING BTREE,
  CONSTRAINT `fk_student_admin_class` FOREIGN KEY (`admin_class_id`) REFERENCES `sys_admin_class` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_student_college` FOREIGN KEY (`college_id`) REFERENCES `sys_college` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_student_major` FOREIGN KEY (`major_id`) REFERENCES `sys_major` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES (1, '2024010101', '陈思远', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (2, '2024010102', '林雨桐', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (3, '2024010103', '张浩然', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (4, '2024010104', '王诗涵', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (5, '2024010105', '刘子轩', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (6, '2024010106', '杨紫萱', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (7, '2024010107', '赵一鸣', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (8, '2024010108', '吴若曦', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (9, '2024010109', '黄天翔', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (10, '2024010110', '周语嫣', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (11, '2024010111', '徐明轩', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (12, '2024010112', '孙艺菲', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (13, '2024010113', '马睿哲', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (14, '2024010114', '朱晓蕾', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (15, '2024010115', '胡嘉豪', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (16, '2024010116', '郭思琪', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (17, '2024010117', '何宇轩', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (18, '2024010118', '罗诗韵', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (19, '2024010119', '邓子墨', 1, 1, 2024, 2);
INSERT INTO `student` VALUES (20, '2024010120', '曹雅婷', 1, 1, 2024, 2);

-- ----------------------------
-- Table structure for student_score
-- ----------------------------
DROP TABLE IF EXISTS `student_score`;
CREATE TABLE `student_score`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `sheet_id` bigint(0) NOT NULL COMMENT '成绩单',
  `student_id` bigint(0) NOT NULL COMMENT '学生',
  `assessment_id` bigint(0) NOT NULL COMMENT '考核点',
  `question_id` bigint(0) NULL DEFAULT NULL COMMENT '题目ID（题目级得分）',
  `score` decimal(6, 2) NOT NULL COMMENT '实际得分',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_score_sheet`(`sheet_id`) USING BTREE,
  INDEX `idx_student_score_student`(`student_id`) USING BTREE,
  INDEX `fk_ss_assessment`(`assessment_id`) USING BTREE,
  UNIQUE INDEX `uk_sheet_student_assessment_q`(`sheet_id`, `student_id`, `assessment_id`, `question_id`) USING BTREE,
  INDEX `idx_ss_question`(`question_id`) USING BTREE,
  CONSTRAINT `fk_ss_assessment` FOREIGN KEY (`assessment_id`) REFERENCES `assessment_point` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ss_sheet` FOREIGN KEY (`sheet_id`) REFERENCES `score_sheet` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ss_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ss_question` FOREIGN KEY (`question_id`) REFERENCES `assessment_question` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 415 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生考核点成绩明细' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student_score
-- ----------------------------
INSERT INTO `student_score` VALUES (1, 1, 1, 1, NULL, 90.00);
INSERT INTO `student_score` VALUES (2, 1, 1, 2, NULL, 70.00);
INSERT INTO `student_score` VALUES (3, 1, 1, 3, NULL, 90.00);
INSERT INTO `student_score` VALUES (4, 1, 1, 4, NULL, 90.00);
INSERT INTO `student_score` VALUES (5, 1, 1, 5, NULL, 70.00);
INSERT INTO `student_score` VALUES (6, 1, 2, 1, NULL, 80.00);
INSERT INTO `student_score` VALUES (7, 1, 2, 2, NULL, 80.00);
INSERT INTO `student_score` VALUES (8, 1, 2, 3, NULL, 80.00);
INSERT INTO `student_score` VALUES (9, 1, 2, 4, NULL, 80.00);
INSERT INTO `student_score` VALUES (10, 1, 2, 5, NULL, 80.00);
INSERT INTO `student_score` VALUES (11, 1, 3, 1, NULL, 90.00);
INSERT INTO `student_score` VALUES (12, 1, 3, 2, NULL, 90.00);
INSERT INTO `student_score` VALUES (13, 1, 3, 3, NULL, 70.00);
INSERT INTO `student_score` VALUES (14, 1, 3, 4, NULL, 90.00);
INSERT INTO `student_score` VALUES (15, 1, 3, 5, NULL, 70.00);
INSERT INTO `student_score` VALUES (16, 1, 4, 1, NULL, 70.00);
INSERT INTO `student_score` VALUES (17, 1, 4, 2, NULL, 80.00);
INSERT INTO `student_score` VALUES (18, 1, 4, 3, NULL, 80.00);
INSERT INTO `student_score` VALUES (19, 1, 4, 4, NULL, 80.00);
INSERT INTO `student_score` VALUES (20, 1, 4, 5, NULL, 80.00);
INSERT INTO `student_score` VALUES (21, 1, 5, 1, NULL, 90.00);
INSERT INTO `student_score` VALUES (22, 1, 5, 2, NULL, 70.00);
INSERT INTO `student_score` VALUES (23, 1, 5, 3, NULL, 90.00);
INSERT INTO `student_score` VALUES (24, 1, 5, 4, NULL, 70.00);
INSERT INTO `student_score` VALUES (25, 1, 5, 5, NULL, 90.00);
INSERT INTO `student_score` VALUES (101, 2, 1, 6, NULL, 20.00);
INSERT INTO `student_score` VALUES (102, 2, 1, 7, NULL, 30.00);
INSERT INTO `student_score` VALUES (103, 2, 1, 8, NULL, 50.00);
INSERT INTO `student_score` VALUES (104, 2, 2, 6, NULL, 20.00);
INSERT INTO `student_score` VALUES (105, 2, 2, 7, NULL, 30.00);
INSERT INTO `student_score` VALUES (106, 2, 2, 8, NULL, 50.00);
INSERT INTO `student_score` VALUES (107, 2, 3, 6, NULL, 20.00);
INSERT INTO `student_score` VALUES (108, 2, 3, 7, NULL, 30.00);
INSERT INTO `student_score` VALUES (109, 2, 3, 8, NULL, 50.00);
INSERT INTO `student_score` VALUES (110, 2, 4, 6, NULL, 20.00);
INSERT INTO `student_score` VALUES (111, 2, 4, 7, NULL, 20.00);
INSERT INTO `student_score` VALUES (112, 2, 4, 8, NULL, 50.00);
INSERT INTO `student_score` VALUES (113, 2, 5, 6, NULL, 20.00);
INSERT INTO `student_score` VALUES (114, 2, 5, 7, NULL, 30.00);
INSERT INTO `student_score` VALUES (115, 2, 5, 8, NULL, 50.00);
INSERT INTO `student_score` VALUES (161, 3, 3, 9, NULL, 20.00);
INSERT INTO `student_score` VALUES (162, 3, 3, 10, NULL, 30.00);
INSERT INTO `student_score` VALUES (163, 3, 3, 11, NULL, 50.00);
INSERT INTO `student_score` VALUES (164, 4, 1, 12, NULL, 40.00);
INSERT INTO `student_score` VALUES (175, 1, 1, 1, 1, 45.00);
INSERT INTO `student_score` VALUES (176, 1, 1, 1, 2, 45.00);
INSERT INTO `student_score` VALUES (177, 1, 2, 1, 1, 30.00);
INSERT INTO `student_score` VALUES (178, 1, 2, 1, 2, 45.00);
INSERT INTO `student_score` VALUES (179, 1, 3, 1, 1, 40.00);
INSERT INTO `student_score` VALUES (180, 1, 3, 1, 2, 45.00);
INSERT INTO `student_score` VALUES (181, 1, 4, 1, 1, 35.00);
INSERT INTO `student_score` VALUES (182, 1, 4, 1, 2, 35.00);
INSERT INTO `student_score` VALUES (183, 1, 5, 1, 1, 40.00);
INSERT INTO `student_score` VALUES (184, 1, 5, 1, 2, 40.00);
INSERT INTO `student_score` VALUES (185, 1, 1, 2, 3, 25.00);
INSERT INTO `student_score` VALUES (186, 1, 1, 2, 4, 25.00);
INSERT INTO `student_score` VALUES (187, 1, 1, 2, 5, 30.00);
INSERT INTO `student_score` VALUES (188, 1, 2, 2, 3, 20.00);
INSERT INTO `student_score` VALUES (189, 1, 2, 2, 4, 25.00);
INSERT INTO `student_score` VALUES (190, 1, 2, 2, 5, 35.00);
INSERT INTO `student_score` VALUES (191, 1, 3, 2, 3, 30.00);
INSERT INTO `student_score` VALUES (192, 1, 3, 2, 4, 25.00);
INSERT INTO `student_score` VALUES (193, 1, 3, 2, 5, 35.00);
INSERT INTO `student_score` VALUES (194, 1, 4, 2, 3, 30.00);
INSERT INTO `student_score` VALUES (195, 1, 4, 2, 4, 25.00);
INSERT INTO `student_score` VALUES (196, 1, 4, 2, 5, 30.00);
INSERT INTO `student_score` VALUES (197, 1, 5, 2, 3, 20.00);
INSERT INTO `student_score` VALUES (198, 1, 5, 2, 4, 20.00);
INSERT INTO `student_score` VALUES (199, 1, 5, 2, 5, 30.00);
INSERT INTO `student_score` VALUES (200, 1, 1, 3, 6, 16.00);
INSERT INTO `student_score` VALUES (201, 1, 1, 3, 7, 16.00);
INSERT INTO `student_score` VALUES (202, 1, 1, 3, 8, 20.00);
INSERT INTO `student_score` VALUES (203, 1, 1, 3, 9, 8.00);
INSERT INTO `student_score` VALUES (204, 1, 1, 3, 10, 8.00);
INSERT INTO `student_score` VALUES (205, 1, 1, 3, 11, 10.00);
INSERT INTO `student_score` VALUES (206, 1, 2, 3, 6, 20.00);
INSERT INTO `student_score` VALUES (207, 1, 3, 3, 6, 20.00);
INSERT INTO `student_score` VALUES (208, 1, 4, 3, 6, 18.00);
INSERT INTO `student_score` VALUES (209, 1, 5, 3, 6, 16.00);
INSERT INTO `student_score` VALUES (210, 1, 2, 3, 7, 20.00);
INSERT INTO `student_score` VALUES (211, 1, 3, 3, 7, 14.00);
INSERT INTO `student_score` VALUES (212, 1, 4, 3, 7, 14.00);
INSERT INTO `student_score` VALUES (213, 1, 5, 3, 7, 14.00);
INSERT INTO `student_score` VALUES (214, 1, 2, 3, 8, 15.00);
INSERT INTO `student_score` VALUES (215, 1, 3, 3, 8, 20.00);
INSERT INTO `student_score` VALUES (216, 1, 4, 3, 8, 25.00);
INSERT INTO `student_score` VALUES (217, 1, 5, 3, 8, 18.00);
INSERT INTO `student_score` VALUES (218, 1, 2, 3, 9, 8.00);
INSERT INTO `student_score` VALUES (219, 1, 3, 3, 9, 8.00);
INSERT INTO `student_score` VALUES (220, 1, 4, 3, 9, 8.00);
INSERT INTO `student_score` VALUES (221, 1, 5, 3, 9, 8.00);
INSERT INTO `student_score` VALUES (222, 1, 2, 3, 10, 8.00);
INSERT INTO `student_score` VALUES (223, 1, 3, 3, 10, 10.00);
INSERT INTO `student_score` VALUES (224, 1, 4, 3, 10, 10.00);
INSERT INTO `student_score` VALUES (225, 1, 5, 3, 10, 10.00);
INSERT INTO `student_score` VALUES (226, 1, 2, 3, 11, 15.00);
INSERT INTO `student_score` VALUES (227, 1, 3, 3, 11, 10.00);
INSERT INTO `student_score` VALUES (228, 1, 4, 3, 11, 10.00);
INSERT INTO `student_score` VALUES (229, 1, 5, 3, 11, 10.00);
INSERT INTO `student_score` VALUES (230, 1, 1, 4, 13, 8.00);
INSERT INTO `student_score` VALUES (231, 1, 1, 4, 14, 8.00);
INSERT INTO `student_score` VALUES (232, 1, 1, 4, 15, 8.00);
INSERT INTO `student_score` VALUES (233, 1, 1, 4, 16, 8.00);
INSERT INTO `student_score` VALUES (234, 1, 1, 4, 17, 8.00);
INSERT INTO `student_score` VALUES (235, 1, 1, 4, 18, 8.00);
INSERT INTO `student_score` VALUES (236, 1, 1, 4, 19, 8.00);
INSERT INTO `student_score` VALUES (237, 1, 1, 4, 20, 8.00);
INSERT INTO `student_score` VALUES (238, 1, 1, 4, 21, 8.00);
INSERT INTO `student_score` VALUES (239, 1, 2, 4, 13, 9.00);
INSERT INTO `student_score` VALUES (240, 1, 2, 4, 14, 9.00);
INSERT INTO `student_score` VALUES (241, 1, 2, 4, 15, 9.00);
INSERT INTO `student_score` VALUES (242, 1, 2, 4, 16, 9.00);
INSERT INTO `student_score` VALUES (243, 1, 2, 4, 17, 9.00);
INSERT INTO `student_score` VALUES (244, 1, 2, 4, 18, 9.00);
INSERT INTO `student_score` VALUES (245, 1, 2, 4, 19, 9.00);
INSERT INTO `student_score` VALUES (246, 1, 2, 4, 20, 9.00);
INSERT INTO `student_score` VALUES (247, 1, 2, 4, 21, 9.00);
INSERT INTO `student_score` VALUES (248, 1, 3, 4, 13, 10.00);
INSERT INTO `student_score` VALUES (249, 1, 3, 4, 14, 10.00);
INSERT INTO `student_score` VALUES (250, 1, 3, 4, 15, 10.00);
INSERT INTO `student_score` VALUES (251, 1, 3, 4, 16, 10.00);
INSERT INTO `student_score` VALUES (252, 1, 3, 4, 17, 10.00);
INSERT INTO `student_score` VALUES (253, 1, 3, 4, 18, 10.00);
INSERT INTO `student_score` VALUES (254, 1, 3, 4, 19, 10.00);
INSERT INTO `student_score` VALUES (255, 1, 3, 4, 20, 10.00);
INSERT INTO `student_score` VALUES (256, 1, 3, 4, 21, 10.00);
INSERT INTO `student_score` VALUES (257, 1, 4, 4, 13, 8.00);
INSERT INTO `student_score` VALUES (258, 1, 4, 4, 14, 8.00);
INSERT INTO `student_score` VALUES (259, 1, 4, 4, 15, 8.00);
INSERT INTO `student_score` VALUES (260, 1, 5, 4, 15, 8.00);
INSERT INTO `student_score` VALUES (261, 1, 5, 4, 14, 8.00);
INSERT INTO `student_score` VALUES (262, 1, 5, 4, 13, 8.00);
INSERT INTO `student_score` VALUES (263, 1, 4, 4, 16, 8.00);
INSERT INTO `student_score` VALUES (264, 1, 5, 4, 16, 8.00);
INSERT INTO `student_score` VALUES (265, 1, 4, 4, 17, 8.00);
INSERT INTO `student_score` VALUES (266, 1, 5, 4, 17, 8.00);
INSERT INTO `student_score` VALUES (267, 1, 4, 4, 18, 8.00);
INSERT INTO `student_score` VALUES (268, 1, 5, 4, 18, 8.00);
INSERT INTO `student_score` VALUES (269, 1, 4, 4, 19, 8.00);
INSERT INTO `student_score` VALUES (270, 1, 5, 4, 19, 8.00);
INSERT INTO `student_score` VALUES (271, 1, 4, 4, 20, 8.00);
INSERT INTO `student_score` VALUES (272, 1, 5, 4, 20, 8.00);
INSERT INTO `student_score` VALUES (273, 1, 4, 4, 21, 8.00);
INSERT INTO `student_score` VALUES (274, 1, 5, 4, 21, 8.00);
INSERT INTO `student_score` VALUES (275, 1, 1, 5, 22, 90.00);
INSERT INTO `student_score` VALUES (276, 1, 2, 5, 22, 94.00);
INSERT INTO `student_score` VALUES (277, 1, 3, 5, 22, 96.00);
INSERT INTO `student_score` VALUES (278, 1, 4, 5, 22, 90.00);
INSERT INTO `student_score` VALUES (279, 1, 5, 5, 22, 98.00);
INSERT INTO `student_score` VALUES (280, 3, 1, 9, 23, 25.00);
INSERT INTO `student_score` VALUES (281, 3, 2, 9, 24, 25.00);
INSERT INTO `student_score` VALUES (282, 3, 3, 9, 25, 25.00);
INSERT INTO `student_score` VALUES (283, 3, 1, 9, 24, 28.00);
INSERT INTO `student_score` VALUES (284, 3, 1, 9, 25, 35.00);
INSERT INTO `student_score` VALUES (285, 3, 2, 9, 23, 28.00);
INSERT INTO `student_score` VALUES (286, 3, 2, 9, 25, 30.00);
INSERT INTO `student_score` VALUES (287, 3, 3, 9, 23, 20.00);
INSERT INTO `student_score` VALUES (288, 3, 3, 9, 24, 25.00);
INSERT INTO `student_score` VALUES (289, 3, 4, 9, 23, 20.00);
INSERT INTO `student_score` VALUES (290, 3, 4, 9, 24, 25.00);
INSERT INTO `student_score` VALUES (291, 3, 4, 9, 25, 35.00);
INSERT INTO `student_score` VALUES (292, 3, 5, 9, 23, 20.00);
INSERT INTO `student_score` VALUES (293, 3, 5, 9, 24, 20.00);
INSERT INTO `student_score` VALUES (294, 3, 5, 9, 25, 30.00);
INSERT INTO `student_score` VALUES (295, 3, 1, 10, 26, 45.00);
INSERT INTO `student_score` VALUES (296, 3, 1, 10, 27, 45.00);
INSERT INTO `student_score` VALUES (297, 3, 2, 10, 26, 40.00);
INSERT INTO `student_score` VALUES (298, 3, 2, 10, 27, 40.00);
INSERT INTO `student_score` VALUES (299, 3, 3, 10, 26, 40.00);
INSERT INTO `student_score` VALUES (300, 3, 3, 10, 27, 45.00);
INSERT INTO `student_score` VALUES (301, 3, 4, 10, 26, 40.00);
INSERT INTO `student_score` VALUES (302, 3, 4, 10, 27, 35.00);
INSERT INTO `student_score` VALUES (303, 3, 5, 10, 26, 40.00);
INSERT INTO `student_score` VALUES (304, 3, 5, 10, 27, 40.00);
INSERT INTO `student_score` VALUES (305, 3, 1, 11, 28, 8.00);
INSERT INTO `student_score` VALUES (306, 3, 1, 11, 29, 15.00);
INSERT INTO `student_score` VALUES (307, 3, 1, 11, 30, 25.00);
INSERT INTO `student_score` VALUES (308, 3, 1, 11, 31, 35.00);
INSERT INTO `student_score` VALUES (309, 3, 2, 11, 28, 6.00);
INSERT INTO `student_score` VALUES (310, 3, 2, 11, 29, 12.00);
INSERT INTO `student_score` VALUES (311, 3, 2, 11, 30, 20.00);
INSERT INTO `student_score` VALUES (312, 3, 2, 11, 31, 25.00);
INSERT INTO `student_score` VALUES (313, 3, 3, 11, 28, 8.00);
INSERT INTO `student_score` VALUES (314, 3, 3, 11, 29, 16.00);
INSERT INTO `student_score` VALUES (315, 3, 3, 11, 30, 15.00);
INSERT INTO `student_score` VALUES (316, 3, 3, 11, 31, 36.00);
INSERT INTO `student_score` VALUES (317, 3, 4, 11, 28, 8.00);
INSERT INTO `student_score` VALUES (318, 3, 4, 11, 29, 16.00);
INSERT INTO `student_score` VALUES (319, 3, 4, 11, 30, 20.00);
INSERT INTO `student_score` VALUES (320, 3, 4, 11, 31, 35.00);
INSERT INTO `student_score` VALUES (321, 3, 5, 11, 28, 8.00);
INSERT INTO `student_score` VALUES (322, 3, 5, 11, 29, 14.00);
INSERT INTO `student_score` VALUES (323, 3, 5, 11, 30, 26.00);
INSERT INTO `student_score` VALUES (324, 3, 5, 11, 31, 36.00);
INSERT INTO `student_score` VALUES (325, 4, 1, 12, 32, 25.00);
INSERT INTO `student_score` VALUES (326, 4, 1, 12, 33, 25.00);
INSERT INTO `student_score` VALUES (327, 4, 1, 12, 34, 35.00);
INSERT INTO `student_score` VALUES (328, 4, 2, 12, 32, 20.00);
INSERT INTO `student_score` VALUES (329, 4, 2, 12, 33, 25.00);
INSERT INTO `student_score` VALUES (330, 4, 2, 12, 34, 30.00);
INSERT INTO `student_score` VALUES (331, 4, 3, 12, 32, 25.00);
INSERT INTO `student_score` VALUES (332, 4, 3, 12, 33, 25.00);
INSERT INTO `student_score` VALUES (333, 4, 3, 12, 34, 25.00);
INSERT INTO `student_score` VALUES (334, 4, 4, 12, 32, 20.00);
INSERT INTO `student_score` VALUES (335, 4, 4, 12, 33, 20.00);
INSERT INTO `student_score` VALUES (336, 4, 4, 12, 34, 30.00);
INSERT INTO `student_score` VALUES (337, 4, 5, 12, 32, 25.00);
INSERT INTO `student_score` VALUES (338, 4, 5, 12, 33, 25.00);
INSERT INTO `student_score` VALUES (339, 4, 5, 12, 34, 25.00);
INSERT INTO `student_score` VALUES (340, 4, 1, 13, 35, 45.00);
INSERT INTO `student_score` VALUES (341, 4, 1, 13, 36, 45.00);
INSERT INTO `student_score` VALUES (342, 4, 2, 13, 35, 40.00);
INSERT INTO `student_score` VALUES (343, 4, 2, 13, 36, 45.00);
INSERT INTO `student_score` VALUES (344, 4, 3, 13, 35, 45.00);
INSERT INTO `student_score` VALUES (345, 4, 3, 13, 36, 40.00);
INSERT INTO `student_score` VALUES (346, 4, 4, 13, 35, 48.00);
INSERT INTO `student_score` VALUES (347, 4, 4, 13, 36, 45.00);
INSERT INTO `student_score` VALUES (348, 4, 5, 13, 35, 40.00);
INSERT INTO `student_score` VALUES (349, 4, 5, 13, 36, 40.00);
INSERT INTO `student_score` VALUES (350, 4, 1, 14, 37, 8.00);
INSERT INTO `student_score` VALUES (351, 4, 2, 14, 37, 8.00);
INSERT INTO `student_score` VALUES (352, 4, 3, 14, 37, 8.00);
INSERT INTO `student_score` VALUES (353, 4, 4, 14, 37, 8.00);
INSERT INTO `student_score` VALUES (354, 4, 5, 14, 37, 8.00);
INSERT INTO `student_score` VALUES (355, 4, 1, 14, 38, 18.00);
INSERT INTO `student_score` VALUES (356, 4, 2, 14, 38, 20.00);
INSERT INTO `student_score` VALUES (357, 4, 3, 14, 38, 16.00);
INSERT INTO `student_score` VALUES (358, 4, 4, 14, 38, 14.00);
INSERT INTO `student_score` VALUES (359, 4, 5, 14, 38, 18.00);
INSERT INTO `student_score` VALUES (360, 4, 1, 14, 39, 28.00);
INSERT INTO `student_score` VALUES (361, 4, 2, 14, 39, 30.00);
INSERT INTO `student_score` VALUES (362, 4, 3, 14, 39, 26.00);
INSERT INTO `student_score` VALUES (363, 4, 4, 14, 39, 26.00);
INSERT INTO `student_score` VALUES (364, 4, 5, 14, 39, 26.00);
INSERT INTO `student_score` VALUES (365, 4, 1, 14, 40, 16.00);
INSERT INTO `student_score` VALUES (366, 4, 2, 14, 40, 18.00);
INSERT INTO `student_score` VALUES (367, 4, 3, 14, 40, 16.00);
INSERT INTO `student_score` VALUES (368, 4, 4, 14, 40, 18.00);
INSERT INTO `student_score` VALUES (369, 4, 5, 14, 40, 16.00);
INSERT INTO `student_score` VALUES (370, 4, 1, 14, 41, 18.00);
INSERT INTO `student_score` VALUES (371, 4, 2, 14, 41, 16.00);
INSERT INTO `student_score` VALUES (372, 4, 3, 14, 41, 16.00);
INSERT INTO `student_score` VALUES (373, 4, 4, 14, 41, 16.00);
INSERT INTO `student_score` VALUES (374, 4, 5, 14, 41, 16.00);
INSERT INTO `student_score` VALUES (375, 2, 1, 6, 42, 26.00);
INSERT INTO `student_score` VALUES (376, 2, 1, 6, 43, 26.00);
INSERT INTO `student_score` VALUES (377, 2, 1, 6, 44, 36.00);
INSERT INTO `student_score` VALUES (378, 2, 2, 6, 42, 22.00);
INSERT INTO `student_score` VALUES (379, 2, 2, 6, 43, 26.00);
INSERT INTO `student_score` VALUES (380, 2, 2, 6, 44, 38.00);
INSERT INTO `student_score` VALUES (381, 2, 3, 6, 42, 24.00);
INSERT INTO `student_score` VALUES (382, 2, 3, 6, 43, 24.00);
INSERT INTO `student_score` VALUES (383, 2, 3, 6, 44, 34.00);
INSERT INTO `student_score` VALUES (384, 2, 4, 6, 42, 24.00);
INSERT INTO `student_score` VALUES (385, 2, 4, 6, 43, 28.00);
INSERT INTO `student_score` VALUES (386, 2, 4, 6, 44, 38.00);
INSERT INTO `student_score` VALUES (387, 2, 5, 6, 42, 26.00);
INSERT INTO `student_score` VALUES (388, 2, 5, 6, 43, 28.00);
INSERT INTO `student_score` VALUES (389, 2, 5, 6, 44, 36.00);
INSERT INTO `student_score` VALUES (390, 2, 1, 7, 45, 45.00);
INSERT INTO `student_score` VALUES (391, 2, 1, 7, 46, 45.00);
INSERT INTO `student_score` VALUES (392, 2, 2, 7, 45, 50.00);
INSERT INTO `student_score` VALUES (393, 2, 2, 7, 46, 45.00);
INSERT INTO `student_score` VALUES (394, 2, 3, 7, 45, 40.00);
INSERT INTO `student_score` VALUES (395, 2, 3, 7, 46, 45.00);
INSERT INTO `student_score` VALUES (396, 2, 4, 7, 45, 45.00);
INSERT INTO `student_score` VALUES (397, 2, 4, 7, 46, 40.00);
INSERT INTO `student_score` VALUES (398, 2, 5, 7, 45, 40.00);
INSERT INTO `student_score` VALUES (399, 2, 5, 7, 46, 45.00);
INSERT INTO `student_score` VALUES (400, 2, 1, 8, 47, 18.00);
INSERT INTO `student_score` VALUES (401, 2, 1, 8, 48, 18.00);
INSERT INTO `student_score` VALUES (402, 2, 1, 8, 49, 50.00);
INSERT INTO `student_score` VALUES (403, 2, 2, 8, 47, 20.00);
INSERT INTO `student_score` VALUES (404, 2, 2, 8, 48, 16.00);
INSERT INTO `student_score` VALUES (405, 2, 2, 8, 49, 55.00);
INSERT INTO `student_score` VALUES (406, 2, 3, 8, 47, 20.00);
INSERT INTO `student_score` VALUES (407, 2, 3, 8, 48, 18.00);
INSERT INTO `student_score` VALUES (408, 2, 3, 8, 49, 50.00);
INSERT INTO `student_score` VALUES (409, 2, 4, 8, 47, 18.00);
INSERT INTO `student_score` VALUES (410, 2, 4, 8, 48, 16.00);
INSERT INTO `student_score` VALUES (411, 2, 4, 8, 49, 55.00);
INSERT INTO `student_score` VALUES (412, 2, 5, 8, 47, 20.00);
INSERT INTO `student_score` VALUES (413, 2, 5, 8, 48, 20.00);
INSERT INTO `student_score` VALUES (414, 2, 5, 8, 49, 58.00);

-- ----------------------------
-- Table structure for sys_admin_class
-- ----------------------------
DROP TABLE IF EXISTS `sys_admin_class`;
CREATE TABLE `sys_admin_class`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `major_id` bigint(0) NOT NULL COMMENT '所属专业',
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级名称，如 计算机科学与技术2501班',
  `enrollment_year` int(0) NOT NULL COMMENT '入学年份',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_major_class`(`major_id`, `class_name`) USING BTREE,
  INDEX `idx_major`(`major_id`) USING BTREE,
  CONSTRAINT `fk_admin_class_major` FOREIGN KEY (`major_id`) REFERENCES `sys_major` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '行政班级' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_admin_class
-- ----------------------------
INSERT INTO `sys_admin_class` VALUES (1, 1, '计算机科学与技术2301班', 2023);
INSERT INTO `sys_admin_class` VALUES (2, 1, '计算机科学与技术2401班', 2024);
INSERT INTO `sys_admin_class` VALUES (3, 1, '计算机科学与技术2501班', 2025);

-- ----------------------------
-- Table structure for sys_college
-- ----------------------------
DROP TABLE IF EXISTS `sys_college`;
CREATE TABLE `sys_college`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_college_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学院字典' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_college
-- ----------------------------
INSERT INTO `sys_college` VALUES (2, '数理医学院');
INSERT INTO `sys_college` VALUES (1, '计算机科学与技术学院');

-- ----------------------------
-- Table structure for sys_dict_semester
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_semester`;
CREATE TABLE `sys_dict_semester`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `academic_year` varchar(9) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学年, 如 2025-2026',
  `semester` tinyint(0) NOT NULL COMMENT '学期 (1/2)',
  `label` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '显示名, 如 2025-2026学年第一学期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学年学期字典' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_semester
-- ----------------------------
INSERT INTO `sys_dict_semester` VALUES (1, '2025-2026', 1, '2025-2026学年第一学期');
INSERT INTO `sys_dict_semester` VALUES (2, '2025-2026', 2, '2025-2026学年第二学期');

-- ----------------------------
-- Table structure for sys_major
-- ----------------------------
DROP TABLE IF EXISTS `sys_major`;
CREATE TABLE `sys_major`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '专业代码',
  `college_id` bigint(0) NULL DEFAULT NULL COMMENT '所属学院',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_major_code`(`code`) USING BTREE,
  INDEX `idx_college`(`college_id`) USING BTREE,
  CONSTRAINT `fk_major_college` FOREIGN KEY (`college_id`) REFERENCES `sys_college` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专业字典' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_major
-- ----------------------------
INSERT INTO `sys_major` VALUES (1, '计算机科学与技术2024级', '080901', 1);
INSERT INTO `sys_major` VALUES (2, '生物医学工程2026级', '085410', 2);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `role_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码, 如 ADMIN, TEACHER',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色显示名',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色描述',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态 (1启用/0禁用)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'ADMIN', '系统管理员', '负责系统初始化配置、全局字典管理、用户账号管理', 1);
INSERT INTO `sys_role` VALUES (2, 'ACADEMIC', '教务管理员', '负责导入培养方案、管理教学班级与学生名单、导出专业认证报告', 1);
INSERT INTO `sys_role` VALUES (3, 'DIRECTOR', '专业负责人', '负责录入毕业要求与指标点、维护宏观支撑矩阵、触发专业级计算', 1);
INSERT INTO `sys_role` VALUES (4, 'TEACHER', '主讲教师', '负责编写课程大纲、设定权重、导入成绩、触发课程级计算', 1);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt 加密密码',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_id` bigint(0) NOT NULL COMMENT '角色',
  `college_id` bigint(0) NULL DEFAULT NULL COMMENT '所属学院',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态 (1启用/0禁用)',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  INDEX `idx_role`(`role_id`) USING BTREE,
  INDEX `idx_college`(`college_id`) USING BTREE,
  CONSTRAINT `fk_user_college` FOREIGN KEY (`college_id`) REFERENCES `sys_college` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 1, NULL, 1, '2026-05-18 21:31:57', '2026-05-18 21:31:57');
INSERT INTO `sys_user` VALUES (2, 'academic01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '教务员张老师', 2, 1, 1, '2026-05-18 22:25:35', '2026-05-18 22:25:35');
INSERT INTO `sys_user` VALUES (3, 'director01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '专业负责人李老师', 3, 1, 1, '2026-05-18 22:25:35', '2026-05-18 22:25:35');
INSERT INTO `sys_user` VALUES (4, 'teacher_wang', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王老师', 4, 1, 1, '2026-05-18 22:25:35', '2026-05-18 22:25:35');
INSERT INTO `sys_user` VALUES (5, 'teacher_zhao', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵老师', 4, 1, 1, '2026-05-18 22:25:35', '2026-05-18 22:25:35');

-- ----------------------------
-- Table structure for teaching_class
-- ----------------------------
DROP TABLE IF EXISTS `teaching_class`;
CREATE TABLE `teaching_class`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `course_id` bigint(0) NULL DEFAULT NULL COMMENT '课程',
  `teacher_id` bigint(0) NULL DEFAULT NULL COMMENT '主讲教师',
  `semester_id` bigint(0) NULL DEFAULT NULL COMMENT '开课学期',
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '班级名称',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course`(`course_id`) USING BTREE,
  INDEX `idx_teacher`(`teacher_id`) USING BTREE,
  INDEX `idx_semester`(`semester_id`) USING BTREE,
  CONSTRAINT `fk_class_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_class_semester` FOREIGN KEY (`semester_id`) REFERENCES `sys_dict_semester` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_class_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教学班级' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of teaching_class
-- ----------------------------
INSERT INTO `teaching_class` VALUES (1, 1, 4, 1, '数据结构-2025秋-1班');
INSERT INTO `teaching_class` VALUES (2, 5, 5, 1, 'C语言程序设计-2025秋-1班');
INSERT INTO `teaching_class` VALUES (3, 6, 4, 2, '计算机网络-2026春-1班');
INSERT INTO `teaching_class` VALUES (4, 6, 4, 1, '计算机网络-2025秋-1班');

SET FOREIGN_KEY_CHECKS = 1;
