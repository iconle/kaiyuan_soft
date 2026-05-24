-- ============================================================
-- 面向专业认证的毕业要求达成度统一计算平台
-- 数据库初始化脚本
-- 技术栈: MySQL 8.0+
-- 字符集: utf8mb4 / 排序规则: utf8mb4_unicode_ci
-- ============================================================

-- ============================================================
-- 建库
-- ============================================================

CREATE DATABASE IF NOT EXISTS obe_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE obe_platform;

-- ============================================================
-- 权限与字典模块
-- ============================================================

-- -----------------------------------------------------------
-- 学院字典
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_college (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_college_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学院字典';

-- -----------------------------------------------------------
-- 学年学期字典
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict_semester (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    academic_year  VARCHAR(9)  NOT NULL COMMENT '学年, 如 2025-2026',
    semester       TINYINT     NOT NULL COMMENT '学期 (1/2)',
    label          VARCHAR(30) NULL     COMMENT '显示名, 如 2025-2026学年第一学期',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学年学期字典';

-- -----------------------------------------------------------
-- 系统角色表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    role_code   VARCHAR(20)  NOT NULL COMMENT '角色编码, 如 ADMIN, TEACHER',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色显示名',
    description VARCHAR(200) NULL     COMMENT '角色描述',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 (1启用/0禁用)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 预置四种业务角色（使用 INSERT IGNORE 保证脚本可重复执行）
INSERT IGNORE INTO sys_role (role_code, role_name, description) VALUES
    ('ADMIN',    '系统管理员',  '负责系统初始化配置、全局字典管理、用户账号管理'),
    ('ACADEMIC', '教务管理员',  '负责导入培养方案、管理教学班级与学生名单、导出专业认证报告'),
    ('DIRECTOR', '专业负责人',  '负责录入毕业要求与指标点、维护宏观支撑矩阵、触发专业级计算'),
    ('TEACHER',  '主讲教师',    '负责编写课程大纲、设定权重、导入成绩、触发课程级计算');

-- -----------------------------------------------------------
-- 系统用户表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)   NOT NULL,
    password_hash VARCHAR(255)  NOT NULL COMMENT 'BCrypt 加密密码',
    real_name     VARCHAR(50)   NOT NULL,
    role_id       BIGINT        NOT NULL COMMENT '角色',
    college_id    BIGINT        NULL     COMMENT '所属学院',
    status        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 (1启用/0禁用)',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_role (role_id),
    KEY idx_college (college_id),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_user_college FOREIGN KEY (college_id) REFERENCES sys_college (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 预置默认管理员账号（密码: admin123，BCrypt 加密）
-- 注意: 生产环境部署后请立即修改默认密码
INSERT IGNORE INTO sys_user (username, password_hash, real_name, role_id, status) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 1, 1);

-- -----------------------------------------------------------
-- 专业字典
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_major (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(30)  NULL     COMMENT '专业代码',
    college_id  BIGINT       NULL     COMMENT '所属学院',
    PRIMARY KEY (id),
    UNIQUE KEY uk_major_code (code),
    KEY idx_college (college_id),
    CONSTRAINT fk_major_college FOREIGN KEY (college_id) REFERENCES sys_college (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业字典';

-- ============================================================
-- 培养方案与宏观矩阵模块
-- ============================================================

-- -----------------------------------------------------------
-- 毕业要求
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS grad_requirement (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    major_id BIGINT       NOT NULL COMMENT '所属专业',
    req_no   INT          NOT NULL COMMENT '编号 (1~8)',
    title    VARCHAR(200) NOT NULL COMMENT '要求标题',
    content  TEXT         NULL     COMMENT '详细描述',
    PRIMARY KEY (id),
    KEY idx_major (major_id),
    CONSTRAINT fk_grad_req_major FOREIGN KEY (major_id) REFERENCES sys_major (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='毕业要求';

-- -----------------------------------------------------------
-- 毕业要求指标点
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS indicator (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    grad_req_id   BIGINT      NOT NULL COMMENT '所属毕业要求',
    indicator_no  VARCHAR(5)  NOT NULL COMMENT '编号, 如 3-1',
    content       TEXT        NOT NULL COMMENT '指标点描述',
    PRIMARY KEY (id),
    KEY idx_grad_req (grad_req_id),
    CONSTRAINT fk_indicator_grad_req FOREIGN KEY (grad_req_id) REFERENCES grad_requirement (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='毕业要求指标点';

-- -----------------------------------------------------------
-- 课程库
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS course (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    code              VARCHAR(30)  NULL     COMMENT '课程代码',
    name              VARCHAR(100) NOT NULL COMMENT '课程名称',
    credit            DECIMAL(3,1) NULL     COMMENT '学分',
    hours_theory      INT          NULL     COMMENT '理论学时',
    hours_experiment  INT          NULL     COMMENT '实验学时',
    category          VARCHAR(30)  NULL     COMMENT '类别 (必修/选修)',
    major_id          BIGINT       NULL     COMMENT '所属专业',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_code (code),
    KEY idx_major (major_id),
    CONSTRAINT fk_course_major FOREIGN KEY (major_id) REFERENCES sys_major (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程库';

-- -----------------------------------------------------------
-- 宏观支撑矩阵 (课程 N:M 指标点, 含权重 W_c)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS macro_support_matrix (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    course_id      BIGINT       NOT NULL COMMENT '课程',
    indicator_id   BIGINT       NOT NULL COMMENT '指标点',
    support_level  ENUM('H','M','L') NULL COMMENT '支撑强度 (参考标记)',
    weight         DECIMAL(5,4) NULL     COMMENT '总支撑权重 W_c',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_indicator (course_id, indicator_id),
    KEY idx_macro_matrix_indicator (indicator_id),
    CONSTRAINT fk_macro_course FOREIGN KEY (course_id) REFERENCES course (id),
    CONSTRAINT fk_macro_indicator FOREIGN KEY (indicator_id) REFERENCES indicator (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宏观支撑矩阵 (含权重W_c)';

-- ============================================================
-- 教学班级与学生模块
-- ============================================================

-- -----------------------------------------------------------
-- 教学班级
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS teaching_class (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    course_id   BIGINT      NULL     COMMENT '课程',
    teacher_id  BIGINT      NULL     COMMENT '主讲教师',
    semester_id BIGINT      NULL     COMMENT '开课学期',
    class_name  VARCHAR(50) NULL     COMMENT '班级名称',
    PRIMARY KEY (id),
    KEY idx_course (course_id),
    KEY idx_teacher (teacher_id),
    KEY idx_semester (semester_id),
    CONSTRAINT fk_class_course FOREIGN KEY (course_id) REFERENCES course (id),
    CONSTRAINT fk_class_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user (id),
    CONSTRAINT fk_class_semester FOREIGN KEY (semester_id) REFERENCES sys_dict_semester (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教学班级';

-- -----------------------------------------------------------
-- 学生
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS student (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    student_no      VARCHAR(20) NOT NULL COMMENT '学号',
    name            VARCHAR(50) NOT NULL COMMENT '姓名',
    major_id        BIGINT      NULL     COMMENT '专业',
    enrollment_year INT         NULL     COMMENT '入学年份',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_no (student_no),
    KEY idx_major (major_id),
    CONSTRAINT fk_student_major FOREIGN KEY (major_id) REFERENCES sys_major (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生';

-- -----------------------------------------------------------
-- 班级学生关联 (教学班级 N:M 学生)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS class_student (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    class_id   BIGINT NOT NULL COMMENT '教学班级',
    student_id BIGINT NOT NULL COMMENT '学生',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_student (class_id, student_id),
    KEY idx_class_student_class (class_id),
    CONSTRAINT fk_cs_class FOREIGN KEY (class_id) REFERENCES teaching_class (id),
    CONSTRAINT fk_cs_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级学生关联';

-- ============================================================
-- 课程大纲与微观映射模块
-- ============================================================

-- -----------------------------------------------------------
-- 课程大纲
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS course_outline (
    id       BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL COMMENT '所属教学班级 (一对一)',
    status   ENUM('DRAFT','LOCKED') NOT NULL DEFAULT 'DRAFT' COMMENT '大纲状态',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class (class_id),
    CONSTRAINT fk_outline_class FOREIGN KEY (class_id) REFERENCES teaching_class (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程大纲';

-- -----------------------------------------------------------
-- 课程目标
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS course_objective (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    outline_id  BIGINT      NOT NULL COMMENT '所属大纲',
    obj_no      VARCHAR(10) NOT NULL COMMENT '目标编号, 如 1-1',
    dimension   VARCHAR(10) NULL     COMMENT '维度 (知识/能力/价值)',
    description TEXT        NOT NULL COMMENT '目标描述',
    PRIMARY KEY (id),
    KEY idx_outline (outline_id),
    CONSTRAINT fk_obj_outline FOREIGN KEY (outline_id) REFERENCES course_outline (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程目标';

-- -----------------------------------------------------------
-- 课程目标对指标点的内部贡献权重 (课程目标 N:M 指标点)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS objective_indicator_weight (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    objective_id  BIGINT       NOT NULL COMMENT '课程目标',
    indicator_id  BIGINT       NOT NULL COMMENT '毕业要求指标点',
    weight        DECIMAL(5,4) NOT NULL COMMENT '内部贡献权重 w_jk',
    PRIMARY KEY (id),
    UNIQUE KEY uk_obj_indicator (objective_id, indicator_id),
    KEY idx_obj_weight_indicator (indicator_id),
    CONSTRAINT fk_oiw_objective FOREIGN KEY (objective_id) REFERENCES course_objective (id),
    CONSTRAINT fk_oiw_indicator FOREIGN KEY (indicator_id) REFERENCES indicator (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程目标对指标点的内部贡献权重';

-- -----------------------------------------------------------
-- 考核点
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS assessment_point (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    outline_id   BIGINT       NOT NULL COMMENT '所属大纲',
    name         VARCHAR(100) NOT NULL COMMENT '考核点名称',
    max_score    DECIMAL(6,2) NOT NULL COMMENT '满分分值',
    objective_id BIGINT       NOT NULL COMMENT '绑定的课程目标',
    sort_order   INT          NULL     COMMENT '排序号',
    PRIMARY KEY (id),
    KEY idx_assessment_outline (outline_id),
    KEY idx_assessment_obj (objective_id),
    CONSTRAINT chk_max_score CHECK (max_score > 0),
    CONSTRAINT fk_ap_outline FOREIGN KEY (outline_id) REFERENCES course_outline (id),
    CONSTRAINT fk_ap_objective FOREIGN KEY (objective_id) REFERENCES course_objective (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考核点';

-- ============================================================
-- 成绩与计算结果模块
-- ============================================================

-- -----------------------------------------------------------
-- 成绩单主表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS score_sheet (
    id         BIGINT                         NOT NULL AUTO_INCREMENT,
    class_id   BIGINT                         NOT NULL COMMENT '教学班级',
    status     ENUM('EMPTY','IMPORTED','LOCKED') NOT NULL DEFAULT 'EMPTY' COMMENT '成绩单状态',
    locked_at  DATETIME                       NULL     COMMENT '锁定时间',
    locked_by  BIGINT                         NULL     COMMENT '执行锁定的操作人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class (class_id),
    KEY idx_locked_by (locked_by),
    CONSTRAINT fk_sheet_class FOREIGN KEY (class_id) REFERENCES teaching_class (id),
    CONSTRAINT fk_sheet_user FOREIGN KEY (locked_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩单主表';

-- -----------------------------------------------------------
-- 学生考核点成绩明细
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS student_score (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    sheet_id       BIGINT       NOT NULL COMMENT '成绩单',
    student_id     BIGINT       NOT NULL COMMENT '学生',
    assessment_id  BIGINT       NOT NULL COMMENT '考核点',
    score          DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '实际得分',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sheet_student_assessment (sheet_id, student_id, assessment_id),
    KEY idx_student_score_sheet (sheet_id),
    KEY idx_student_score_student (student_id),
    CONSTRAINT fk_ss_sheet FOREIGN KEY (sheet_id) REFERENCES score_sheet (id),
    CONSTRAINT fk_ss_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_ss_assessment FOREIGN KEY (assessment_id) REFERENCES assessment_point (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生考核点成绩明细';

-- -----------------------------------------------------------
-- 目标级达成度 (第一级计算结果)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS obj_achievement (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    class_id      BIGINT       NOT NULL COMMENT '教学班级',
    objective_id  BIGINT       NOT NULL COMMENT '课程目标',
    achievement   DECIMAL(6,4) NOT NULL COMMENT '班级目标达成度',
    calc_time     DATETIME     NOT NULL COMMENT '计算时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_objective (class_id, objective_id),
    CONSTRAINT fk_oa_class FOREIGN KEY (class_id) REFERENCES teaching_class (id),
    CONSTRAINT fk_oa_objective FOREIGN KEY (objective_id) REFERENCES course_objective (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目标级达成度 (第一级计算结果)';

-- -----------------------------------------------------------
-- 课程级达成度 (第二级计算结果)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS course_achievement (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    class_id      BIGINT       NOT NULL COMMENT '教学班级',
    indicator_id  BIGINT       NOT NULL COMMENT '指标点',
    achievement   DECIMAL(6,4) NOT NULL COMMENT '课程级达成度 E_k',
    calc_time     DATETIME     NOT NULL COMMENT '计算时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_indicator (class_id, indicator_id),
    KEY idx_course_achievement_indicator (indicator_id),
    CONSTRAINT fk_ca_class FOREIGN KEY (class_id) REFERENCES teaching_class (id),
    CONSTRAINT fk_ca_indicator FOREIGN KEY (indicator_id) REFERENCES indicator (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程级达成度 (第二级计算结果)';

-- -----------------------------------------------------------
-- 专业级达成度 (第三级计算结果)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS major_achievement (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    major_id      BIGINT       NOT NULL COMMENT '专业',
    indicator_id  BIGINT       NOT NULL COMMENT '指标点',
    semester_id   BIGINT       NOT NULL COMMENT '学年学期',
    achievement   DECIMAL(6,4) NOT NULL COMMENT '专业级达成度 G_k',
    calc_time     DATETIME     NOT NULL COMMENT '计算时间',
    triggered_by  BIGINT       NULL     COMMENT '触发者',
    PRIMARY KEY (id),
    UNIQUE KEY uk_major_indicator_semester (major_id, indicator_id, semester_id),
    CONSTRAINT fk_ma_major FOREIGN KEY (major_id) REFERENCES sys_major (id),
    CONSTRAINT fk_ma_indicator FOREIGN KEY (indicator_id) REFERENCES indicator (id),
    CONSTRAINT fk_ma_semester FOREIGN KEY (semester_id) REFERENCES sys_dict_semester (id),
    CONSTRAINT fk_ma_user FOREIGN KEY (triggered_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业级达成度 (第三级计算结果)';
