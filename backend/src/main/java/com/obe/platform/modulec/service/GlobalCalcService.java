package com.obe.platform.modulec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.engine.Level3Calculator;
import com.obe.platform.modulea.entity.Course;
import com.obe.platform.modulea.entity.MacroSupportMatrix;
import com.obe.platform.modulea.entity.SysDictSemester;
import com.obe.platform.modulea.entity.SysUser;
import com.obe.platform.modulea.entity.TeachingClass;
import com.obe.platform.modulea.mapper.CourseMapper;
import com.obe.platform.modulea.mapper.MacroSupportMatrixMapper;
import com.obe.platform.modulea.mapper.SysDictSemesterMapper;
import com.obe.platform.modulea.mapper.SysUserMapper;
import com.obe.platform.modulea.mapper.TeachingClassMapper;
import com.obe.platform.modulec.entity.CourseAchievement;
import com.obe.platform.modulec.entity.MajorAchievement;
import com.obe.platform.modulec.entity.ScoreSheet;
import com.obe.platform.modulec.mapper.CourseAchievementMapper;
import com.obe.platform.modulec.mapper.MajorAchievementMapper;
import com.obe.platform.modulec.mapper.ScoreSheetMapper;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalCalcService {

    private final MacroSupportMatrixMapper macroSupportMatrixMapper;
    private final CourseAchievementMapper courseAchievementMapper;
    private final MajorAchievementMapper majorAchievementMapper;
    private final ScoreSheetMapper scoreSheetMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final SysDictSemesterMapper semesterMapper;
    private final PersonalAchievementService personalAchievementService;

    /**
     * Check the readiness status of courses that support the given major.
     * Optionally filter by semester and/or grade (enrollment year).
     *
     * @param majorId    the major ID (required)
     * @param semesterId optional semester filter (null = all semesters)
     * @param grade      optional grade filter, e.g. 2024 (null = all grades)
     */
    public DashboardData getDashboard(Long majorId, Long semesterId, Integer grade) {
        List<MacroSupportMatrix> matrix = macroSupportMatrixMapper.selectList(
                new LambdaQueryWrapper<MacroSupportMatrix>());

        // Filter entries for courses belonging to this major
        List<Long> courseIds = matrix.stream()
                .map(MacroSupportMatrix::getCourseId)
                .distinct()
                .toList();

        List<Course> courses = courseMapper.selectBatchIds(courseIds).stream()
                .filter(c -> majorId.equals(c.getMajorId()))
                .toList();

        List<DashboardData.CourseStatus> courseStatuses = new ArrayList<>();
        int lockedCount = 0;
        int totalCount = 0;

        for (Course course : courses) {
            LambdaQueryWrapper<TeachingClass> classWrapper =
                    new LambdaQueryWrapper<TeachingClass>()
                            .eq(TeachingClass::getCourseId, course.getId());
            if (semesterId != null) {
                classWrapper.eq(TeachingClass::getSemesterId, semesterId);
            }
            if (grade != null) {
                classWrapper.eq(TeachingClass::getGrade, grade);
            }
            List<TeachingClass> classes = teachingClassMapper.selectList(classWrapper);

            for (TeachingClass tc : classes) {
                totalCount++;
                ScoreSheet sheet = scoreSheetMapper.selectOne(
                        new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, tc.getId()));
                String status = sheet == null ? "EMPTY" : sheet.getStatus();
                if ("LOCKED".equals(status)) lockedCount++;

                String teacherName = "";
                if (tc.getTeacherId() != null) {
                    SysUser teacher = userMapper.selectById(tc.getTeacherId());
                    teacherName = teacher != null ? teacher.getRealName() : "";
                }
                String semesterName = "";
                if (tc.getSemesterId() != null) {
                    SysDictSemester sem = semesterMapper.selectById(tc.getSemesterId());
                    semesterName = sem != null ? sem.getLabel() : "";
                }
                courseStatuses.add(new DashboardData.CourseStatus(
                        course.getId(), course.getName(), tc.getId(),
                        tc.getClassName(), status, teacherName, semesterName,
                        sheet != null ? sheet.getLockedAt() : null));
            }
        }

        boolean allReady = totalCount > 0 && lockedCount == totalCount;
        List<Long> incompleteClassIds = courseStatuses.stream()
                .filter(cs -> !"LOCKED".equals(cs.status()))
                .map(DashboardData.CourseStatus::classId)
                .toList();

        return new DashboardData(allReady, lockedCount, totalCount,
                incompleteClassIds, courseStatuses);
    }

    /**
     * Execute Phase 2 (Level 3) calculation for a major.
     *
     * @param majorId    the major ID
     * @param semesterId the semester for storing results
     * @param grade      optional grade filter (null = all grades)
     * @param operator   the user triggering the calculation
     */
    @Transactional(rollbackFor = Exception.class)
    public MajorCalcResult compute(Long majorId, Long semesterId, Integer grade, Long operator) {
        // 1. Check readiness — when grade is specified, don't also filter by semester
        //    (a single grade spans multiple semesters)
        Long filterSemesterId = (grade != null) ? null : semesterId;
        DashboardData dashboard = getDashboard(majorId, filterSemesterId, grade);
        if (!dashboard.allReady()) {
            throw new BizException("存在未完成课程级计算的课程: "
                    + dashboard.incompleteClassIds());
        }

        // 2. Validate macro weight normalization
        List<MacroSupportMatrix> matrix = macroSupportMatrixMapper.selectList(
                new LambdaQueryWrapper<MacroSupportMatrix>());

        // Only entries for courses in this major
        List<Long> allCourseIds = matrix.stream()
                .map(MacroSupportMatrix::getCourseId)
                .distinct()
                .toList();

        Set<Long> majorCourseIds = courseMapper.selectBatchIds(allCourseIds).stream()
                .filter(c -> majorId.equals(c.getMajorId()))
                .map(Course::getId)
                .collect(Collectors.toSet());

        List<MacroSupportMatrix> relevantMatrix = matrix.stream()
                .filter(m -> majorCourseIds.contains(m.getCourseId()))
                .toList();

        // Only validate weights for courses that actually have teaching classes
        Set<Long> coursesWithClasses = dashboard.courseStatuses().stream()
                .map(DashboardData.CourseStatus::courseId).collect(Collectors.toSet());
        List<MacroSupportMatrix> validMatrix = relevantMatrix.stream()
                .filter(m -> coursesWithClasses.contains(m.getCourseId()))
                .toList();

        // 3. Gather course-level achievements
        Map<Long, Map<Long, BigDecimal>> courseAchievements = new HashMap<>();
        for (DashboardData.CourseStatus cs : dashboard.courseStatuses()) {
            List<CourseAchievement> achievements = courseAchievementMapper.selectList(
                    new LambdaQueryWrapper<CourseAchievement>()
                            .eq(CourseAchievement::getClassId, cs.classId()));
            Map<Long, BigDecimal> indicatorMap = achievements.stream()
                    .collect(Collectors.toMap(
                            CourseAchievement::getIndicatorId,
                            CourseAchievement::getAchievement));
            courseAchievements.put(cs.classId(), indicatorMap);
        }

        // 4. Count classes per course and build per-class weights
        Map<Long, Long> courseClassCount = new HashMap<>();
        for (DashboardData.CourseStatus cs : dashboard.courseStatuses()) {
            courseClassCount.merge(cs.courseId(), 1L, Long::sum);
        }

        // Build raw per-class weights (split by class count, no normalization yet)
        List<Level3Calculator.MacroWeightRecord> rawWeights = new ArrayList<>();
        for (MacroSupportMatrix m : validMatrix) {
            if (m.getWeight() == null) continue;
            Long classCount = courseClassCount.getOrDefault(m.getCourseId(), 1L);
            BigDecimal perClassW = m.getWeight().divide(BigDecimal.valueOf(classCount), 10, java.math.RoundingMode.HALF_UP);
            for (DashboardData.CourseStatus cs : dashboard.courseStatuses()) {
                if (cs.courseId().equals(m.getCourseId())) {
                    rawWeights.add(new Level3Calculator.MacroWeightRecord(
                            cs.classId(), m.getIndicatorId(), perClassW));
                }
            }
        }

        // Normalize at per-class level: Σ W_per_class across all classes = 1.0 for each indicator
        Map<Long, BigDecimal> perClassSums = new HashMap<>();
        for (var w : rawWeights) {
            perClassSums.merge(w.indicatorId(), w.weight(), BigDecimal::add);
        }
        Map<Long, BigDecimal> normalizeFactors = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> e : perClassSums.entrySet()) {
            if (e.getValue().compareTo(BigDecimal.ZERO) > 0
                    && e.getValue().subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.01")) > 0) {
                normalizeFactors.put(e.getKey(), BigDecimal.ONE.divide(e.getValue(), 10, java.math.RoundingMode.HALF_UP));
            }
        }

        List<Level3Calculator.MacroWeightRecord> weightRecords = new ArrayList<>();
        for (var w : rawWeights) {
            BigDecimal factor = normalizeFactors.get(w.indicatorId());
            BigDecimal finalW = factor != null ? w.weight().multiply(factor) : w.weight();
            weightRecords.add(new Level3Calculator.MacroWeightRecord(
                    w.courseId(), w.indicatorId(), finalW));
        }

        // 5. Level 3: compute
        Map<Long, BigDecimal> majorAchievements = Level3Calculator.calcMajorAchievement(
                courseAchievements, weightRecords);

        // 6. Persist results
        LocalDateTime now = LocalDateTime.now();

        // Delete previous results
        majorAchievementMapper.delete(
                new LambdaQueryWrapper<MajorAchievement>()
                        .eq(MajorAchievement::getMajorId, majorId)
                        .eq(MajorAchievement::getSemesterId, semesterId));

        for (Map.Entry<Long, BigDecimal> e : majorAchievements.entrySet()) {
            MajorAchievement ma = new MajorAchievement();
            ma.setMajorId(majorId);
            ma.setIndicatorId(e.getKey());
            ma.setSemesterId(semesterId);
            ma.setGrade(grade);
            ma.setAchievement(e.getValue());
            ma.setCalcTime(now);
            ma.setTriggeredBy(operator);
            majorAchievementMapper.insert(ma);
        }

        personalAchievementService.persistMajorAchievements(
                majorId,
                semesterId,
                dashboard.courseStatuses().stream()
                        .map(DashboardData.CourseStatus::classId)
                        .toList(),
                weightRecords,
                now);

        return new MajorCalcResult(majorAchievements, now);
    }

    /**
     * Get existing major-level results.
     */
    public Map<Long, BigDecimal> getResults(Long majorId, Long semesterId, Integer grade) {
        LambdaQueryWrapper<MajorAchievement> wrapper = new LambdaQueryWrapper<MajorAchievement>()
                .eq(MajorAchievement::getMajorId, majorId)
                .eq(MajorAchievement::getSemesterId, semesterId);
        if (grade != null) {
            wrapper.eq(MajorAchievement::getGrade, grade);
        }
        List<MajorAchievement> list = majorAchievementMapper.selectList(wrapper);
        return list.stream()
                .collect(Collectors.toMap(
                        MajorAchievement::getIndicatorId,
                        MajorAchievement::getAchievement));
    }

    // ---- DTOs ----

    public record DashboardData(boolean allReady, int lockedCount, int totalCount,
                                 List<Long> incompleteClassIds, List<CourseStatus> courseStatuses) {
        public record CourseStatus(Long courseId, String courseName, Long classId,
                                    String className, String status, String teacherName,
                                    String semesterName,
                                    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime lockedAt) {}
    }

    public record MajorCalcResult(Map<Long, BigDecimal> achievements,
                                  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime calcTime) {}
}
