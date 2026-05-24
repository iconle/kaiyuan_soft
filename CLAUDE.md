# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

OBE Platform — a graduation-requirement attainment calculation platform for Chinese university program accreditation. Three-level weighted calculation engine (assessment questions → course objectives → course indicators → major-level achievement), replacing manual Excel workflows.

Monorepo: Vue 3 SPA frontend + Spring Boot 3 backend, RESTful API, JWT + RBAC (4 roles: ADMIN, ACADEMIC, DIRECTOR, TEACHER), MySQL 8.0.

## Commands

```bash
# Backend (JDK 17, Maven)
cd backend
mvn spring-boot:run                         # start dev server on :8080

# Frontend (Node 18+)
cd frontend
npm install
npm run dev                                 # start Vite dev server on :5173, proxies /api → :8080
npm run build                               # production build

# Database
mysql -u root -p < docs/database/init.sql   # schema + roles
# Full test data dump (all modules)
mysql -u root -p < docs/database/obe_platform.sql
```

## Architecture

### Backend package layout

```
com.obe.platform
├── common/          # Result<T>, PageResult<T>, BizException, GlobalExceptionHandler
├── config/          # SecurityConfig, CorsConfig, JwtConfig, MyBatisPlusConfig
├── security/        # JwtTokenProvider, JwtAuthFilter, RbacAuthority
├── engine/          # Level1Calculator, Level2Calculator, Level3Calculator, WeightValidator
├── modulea/         # Auth, Users, Dict (college/major/semester/admin-class), Courses,
│                    #   Students, GradRequirements+Indicators, MacroSupportMatrix, TeachingClass
├── moduleb/         # CourseOutline, CourseObjective, ObjectiveIndicatorWeight,
│                    #   AssessmentPoint, AssessmentObjective, AssessmentQuestion, QuestionObjective
├── modulec/         # ScoreSheet, StudentScore, ScoreService, ExcelTemplateService,
│                    #   ExcelParseService, CourseCalcService, GlobalCalcService,
│                    #   ScoreUnlockRequest, UnlockRequestService, achievement entities
└── moduled/         # ReportController, CourseReportService, MajorReportService,
                     #   PdfExporter, TraceExcelExporter
```

Each module follows the same layer pattern: `controller/` → `service/` → `mapper/` + `entity/`. Controllers are role-gated with `@PreAuthorize("hasRole('...')")`. All responses wrapped in `Result<T>` (code=200 for success). MyBatis-Plus with `LambdaQueryWrapper`.

### Frontend route/role layout

| Role | Routes | Views |
|------|--------|-------|
| ADMIN | `/admin/users`, `/admin/students`, `/admin/classes`, `/admin/dict`, `/admin/score-unlock` | UserManage, StudentManage, ClassManage, DictManage, ScoreUnlock |
| ACADEMIC | `/academic/courses`, `/academic/teaching-classes`, `/academic/dashboard`, `/admin/students`, `/director/global-compute`, `/admin/score-unlock` | CourseImport, TeachingClassManage, GlobalDashboard, StudentManage, GlobalCompute, ScoreUnlock |
| DIRECTOR | `/director/grad-req`, `/director/macro-matrix`, `/director/global-compute`, `/academic/dashboard` | GradReqManage, MacroMatrix, GlobalCompute, GlobalDashboard |
| TEACHER | `/teacher/:classId/objectives`, `/teacher/:classId/weights`, `/teacher/:classId/assessments`, `/teacher/:classId/questions`, `/teacher/:classId/scores`, `/teacher/:classId/compute` | ObjectiveSetup, WeightAssign, AssessmentMap, QuestionSetup, ScoreImport, CourseCompute |

Router guard reads `localStorage('token')` and `localStorage('roleCode')`. Sidebar shows role-filtered menu items. API modules in `src/api/` mirror backend controllers. Teacher sidebar includes course class switcher with semester filter.

### Three-level calculation (key formulas)

- **Level 1 (objective):** `C_ij = Σ(question_score) / Σ(question_max)` per objective, per student. Supports N:M assessment→objective and question→objective bindings. Question scores take precedence; assessment-level scores only used when no questions exist for that assessment.
- **Level 2 (course):** `E_k = Σ(C̄_j × w_jk)` where `w_jk` is internal contribution weight. Σw = 1.0 per indicator.
- **Level 3 (major):** `G_k = Σ(E_k × W_c)` where `W_c` is macro support weight. Weights auto-normalized, split equally among a course's teaching classes. ΣW = 1.0 per indicator.

### Score unlock workflow (勘误工单)

Three-tier: TEACHER submits request (PENDING) → ACADEMIC reviews (APPROVED/REJECTED) → ADMIN unlocks (UNLOCKED, sheet status → IMPORTED). `score_unlock_request` table tracks states. Score sheet's achievement data is cleared on unlock.

### Key conventions

- Entity IDs are auto-increment `BIGINT`, exposed as `Long` in Java and `number` in JS
- `Result<T>` unwrap: Axios interceptor checks `res.code !== 200`; blob responses skip JSON unwrapping
- JWT token stored as `Bearer <token>` in Authorization header; expiration 24h by default
- Element Plus icons must be explicitly imported from `@element-plus/icons-vue`
- `student_score` unique key: `(sheet_id, student_id, assessment_id, question_id)`
- Vue Router `:key="$route.fullPath"` on router-view ensures component remount on class switch
- `vue-router` beforeEach guard redirects unauthorized roles to home; `sessionStorage('permDenied')` triggers a toast warning
