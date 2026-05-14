# 数据库设计与ER图

## 1. 核心实体关系图 (ER Model)

```
┌────────────────┐         ┌──────────────┐         ┌────────────────┐
│     Users      │◄───────►│  UserRoles   │◄───────►│     Roles      │
└────────────────┘         └──────────────┘         └────────────────┘
        ▲
        │ owns
        ▼
┌────────────────┐         ┌──────────────┐         ┌────────────────┐
│   Departments  │         │  Professions │         │   AcademicYear │
└────────────────┘         └──────────────┘         └────────────────┘
        │                        ▲
        │ belongs to             │ has
        ▼                        ▼
┌────────────────┐         ┌──────────────┐
│   Courses      │◄───────►│ Requirements │
└────────────────┘         └──────────────┘
        │                        │
        │ has                    │ has
        ▼                        ▼
┌────────────────┐         ┌──────────────────┐
│ CourseObjective│         │  RequirementItem │
└────────────────┘         │  (二级指标点)    │
        │                  └──────────────────┘
        │ has
        ▼
┌────────────────────┐     ┌─────────────────────┐
│  AssessmentPoint   │     │  SupportMatrix      │
│  (考核点)          │     │  (支撑矩阵)        │
└────────────────────┘     └─────────────────────┘
        │                         │
        │ has                     │ maps
        ▼                         ▼
┌────────────────────┐     ┌─────────────────────┐
│  StudentScore      │     │  MatrixWeight      │
│  (学生成绩)        │     │  (权重)            │
└────────────────────┘     └─────────────────────┘
        │
        │ belongs to
        ▼
┌────────────────────┐
│  Students         │
│  (学生名单)        │
└────────────────────┘
        │
        │ belongs to
        ▼
┌────────────────────┐
│  Classes          │
│  (教学班级)        │
└────────────────────┘
        │
        │ has
        ▼
┌────────────────────────────┐
│  CalculationResult         │
│  (计算结果-按层级)         │
│  ├─ ObjectiveLevel         │
│  ├─ CourseLevel            │
│  └─ ProfessionalLevel      │
└────────────────────────────┘
```

## 2. 核心表设计

### 2.1 用户权限相关表

#### sys_users (用户表)
```sql
CREATE TABLE sys_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    department_id BIGINT,
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES sys_departments(id),
    INDEX idx_username (username),
    INDEX idx_department_id (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### sys_roles (角色表)
```sql
CREATE TABLE sys_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    role_code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### sys_user_roles (用户角色关联表)
```sql
CREATE TABLE sys_user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES sys_users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_roles(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.2 基础数据表

#### sys_departments (学院表)
```sql
CREATE TABLE sys_departments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(100) NOT NULL UNIQUE,
    dept_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dept_code (dept_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### sys_academic_years (学年学期表)
```sql
CREATE TABLE sys_academic_years (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    academic_year VARCHAR(20) NOT NULL UNIQUE,
    semester VARCHAR(20) NOT NULL,
    start_date DATE,
    end_date DATE,
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_academic_year (academic_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### profession (专业表)
```sql
CREATE TABLE profession (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prof_name VARCHAR(100) NOT NULL,
    prof_code VARCHAR(50) NOT NULL UNIQUE,
    dept_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (dept_id) REFERENCES sys_departments(id),
    FOREIGN KEY (academic_year_id) REFERENCES sys_academic_years(id),
    INDEX idx_prof_code (prof_code),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.3 毕业要求相关表

#### graduation_requirement (毕业要求表)
```sql
CREATE TABLE graduation_requirement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prof_id BIGINT NOT NULL,
    requirement_name VARCHAR(100) NOT NULL,
    requirement_code VARCHAR(50) NOT NULL,
    description TEXT,
    seq_num INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (prof_id) REFERENCES profession(id),
    UNIQUE KEY uk_prof_code (prof_id, requirement_code),
    INDEX idx_prof_id (prof_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### requirement_item (二级指标点表)
```sql
CREATE TABLE requirement_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    requirement_id BIGINT NOT NULL,
    item_code VARCHAR(50) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    description TEXT,
    seq_num INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (requirement_id) REFERENCES graduation_requirement(id),
    UNIQUE KEY uk_req_code (requirement_id, item_code),
    INDEX idx_requirement_id (requirement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.4 课程相关表

#### course (课程表)
```sql
CREATE TABLE course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prof_id BIGINT NOT NULL,
    course_name VARCHAR(100) NOT NULL,
    course_code VARCHAR(50) NOT NULL,
    credits DECIMAL(3,1),
    description TEXT,
    seq_num INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (prof_id) REFERENCES profession(id),
    UNIQUE KEY uk_prof_course (prof_id, course_code),
    INDEX idx_prof_id (prof_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### course_objective (课程目标表)
```sql
CREATE TABLE course_objective (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    objective_code VARCHAR(50) NOT NULL,
    objective_name VARCHAR(100) NOT NULL,
    description TEXT,
    seq_num INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES course(id),
    UNIQUE KEY uk_course_objective (course_id, objective_code),
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### assessment_point (考核点表)
```sql
CREATE TABLE assessment_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    objective_id BIGINT NOT NULL,
    point_code VARCHAR(50) NOT NULL,
    point_name VARCHAR(100) NOT NULL,
    full_score DECIMAL(10,2) NOT NULL,
    seq_num INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES course(id),
    FOREIGN KEY (objective_id) REFERENCES course_objective(id),
    UNIQUE KEY uk_course_point (course_id, point_code),
    INDEX idx_course_id (course_id),
    INDEX idx_objective_id (objective_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### objective_weight (内部权重表 - 目标对指标点的贡献度)
```sql
CREATE TABLE objective_weight (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    objective_id BIGINT NOT NULL,
    requirement_item_id BIGINT NOT NULL,
    weight DECIMAL(5,4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_obj_weight (course_id, objective_id, requirement_item_id),
    FOREIGN KEY (course_id) REFERENCES course(id),
    FOREIGN KEY (objective_id) REFERENCES course_objective(id),
    FOREIGN KEY (requirement_item_id) REFERENCES requirement_item(id),
    INDEX idx_course_objective (course_id, objective_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.5 支撑矩阵表

#### support_matrix (宏观支撑矩阵)
```sql
CREATE TABLE support_matrix (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prof_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    requirement_item_id BIGINT NOT NULL,
    support_weight DECIMAL(5,4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_support_matrix (prof_id, course_id, requirement_item_id),
    FOREIGN KEY (prof_id) REFERENCES profession(id),
    FOREIGN KEY (course_id) REFERENCES course(id),
    FOREIGN KEY (requirement_item_id) REFERENCES requirement_item(id),
    INDEX idx_prof_item (prof_id, requirement_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.6 学生与成绩表

#### classes (教学班级表)
```sql
CREATE TABLE classes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    class_name VARCHAR(100) NOT NULL,
    class_code VARCHAR(50) NOT NULL,
    teacher_id BIGINT,
    academic_year_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES course(id),
    FOREIGN KEY (teacher_id) REFERENCES sys_users(id),
    FOREIGN KEY (academic_year_id) REFERENCES sys_academic_years(id),
    UNIQUE KEY uk_class (course_id, academic_year_id, class_code),
    INDEX idx_course_id (course_id),
    INDEX idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### students (学生表)
```sql
CREATE TABLE students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_no VARCHAR(50) NOT NULL UNIQUE,
    student_name VARCHAR(100) NOT NULL,
    prof_id BIGINT NOT NULL,
    gender VARCHAR(10),
    email VARCHAR(100),
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (prof_id) REFERENCES profession(id),
    INDEX idx_prof_id (prof_id),
    INDEX idx_student_no (student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### class_students (班级学生关联表)
```sql
CREATE TABLE class_students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_class_student (class_id, student_id),
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### student_score (学生成绩表)
```sql
CREATE TABLE student_score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    assessment_point_id BIGINT NOT NULL,
    score DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_score (class_id, student_id, assessment_point_id),
    FOREIGN KEY (class_id) REFERENCES classes(id),
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (assessment_point_id) REFERENCES assessment_point(id),
    INDEX idx_class_student (class_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.7 计算结果表

#### calculation_objective_level (目标级计算结果)
```sql
CREATE TABLE calculation_objective_level (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    objective_id BIGINT NOT NULL,
    achievement_rate DECIMAL(5,4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_obj_level (class_id, student_id, objective_id),
    FOREIGN KEY (class_id) REFERENCES classes(id),
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (objective_id) REFERENCES course_objective(id),
    INDEX idx_class_student (class_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### calculation_course_level (课程级计算结果)
```sql
CREATE TABLE calculation_course_level (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    requirement_item_id BIGINT NOT NULL,
    achievement_rate DECIMAL(5,4) NOT NULL,
    status TINYINT DEFAULT 0,
    locked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_level (class_id, requirement_item_id),
    FOREIGN KEY (class_id) REFERENCES classes(id),
    FOREIGN KEY (requirement_item_id) REFERENCES requirement_item(id),
    INDEX idx_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### calculation_professional_level (专业级计算结果)
```sql
CREATE TABLE calculation_professional_level (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prof_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    requirement_item_id BIGINT NOT NULL,
    achievement_rate DECIMAL(5,4) NOT NULL,
    status TINYINT DEFAULT 0,
    locked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prof_level (prof_id, academic_year_id, requirement_item_id),
    FOREIGN KEY (prof_id) REFERENCES profession(id),
    FOREIGN KEY (academic_year_id) REFERENCES sys_academic_years(id),
    FOREIGN KEY (requirement_item_id) REFERENCES requirement_item(id),
    INDEX idx_prof_year (prof_id, academic_year_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 3. 索引优化策略

关键联合索引：
- `support_matrix`: (prof_id, requirement_item_id) 用于快速查询某指标点的支撑课程
- `student_score`: (class_id, student_id) 用于快速查询学生成绩
- `objective_weight`: (course_id, objective_id, requirement_item_id) 用于权重查询

## 4. 数据完整性约束

- 所有权重字段必须 ≥ 0 且 ≤ 1.0
- 权重求和校验 ∑weight = 1.0（在应用层实现）
- 学生成绩 ≥ 0 且 ≤ 对应考核点的满分
- 达成度 ≥ 0 且 ≤ 1.0
