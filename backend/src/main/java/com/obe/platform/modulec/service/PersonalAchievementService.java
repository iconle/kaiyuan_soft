package com.obe.platform.modulec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.engine.Level1Calculator;
import com.obe.platform.engine.Level2Calculator;
import com.obe.platform.engine.Level3Calculator;
import com.obe.platform.modulea.entity.ClassStudent;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.modulea.entity.Student;
import com.obe.platform.modulea.mapper.ClassStudentMapper;
import com.obe.platform.modulea.mapper.IndicatorMapper;
import com.obe.platform.modulea.mapper.StudentMapper;
import com.obe.platform.moduleb.entity.AssessmentObjective;
import com.obe.platform.moduleb.entity.AssessmentPoint;
import com.obe.platform.moduleb.entity.AssessmentQuestion;
import com.obe.platform.moduleb.entity.CourseObjective;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.entity.ObjectiveIndicatorWeight;
import com.obe.platform.moduleb.entity.QuestionObjective;
import com.obe.platform.moduleb.mapper.AssessmentObjectiveMapper;
import com.obe.platform.moduleb.mapper.AssessmentPointMapper;
import com.obe.platform.moduleb.mapper.AssessmentQuestionMapper;
import com.obe.platform.moduleb.mapper.CourseObjectiveMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import com.obe.platform.moduleb.mapper.ObjectiveIndicatorWeightMapper;
import com.obe.platform.moduleb.mapper.QuestionObjectiveMapper;
import com.obe.platform.modulec.entity.PersonalAchievement;
import com.obe.platform.modulec.entity.ScoreSheet;
import com.obe.platform.modulec.entity.StudentScore;
import com.obe.platform.modulec.mapper.PersonalAchievementMapper;
import com.obe.platform.modulec.mapper.ScoreSheetMapper;
import com.obe.platform.modulec.mapper.StudentScoreMapper;
import com.obe.platform.moduled.exporter.PersonalAchievementExcelExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalAchievementService {

    public static final String SCOPE_OBJECTIVE = "OBJECTIVE";
    public static final String SCOPE_COURSE = "COURSE";
    public static final String SCOPE_MAJOR = "MAJOR";

    private final ScoreSheetMapper scoreSheetMapper;
    private final StudentScoreMapper studentScoreMapper;
    private final CourseOutlineMapper outlineMapper;
    private final CourseObjectiveMapper objectiveMapper;
    private final AssessmentPointMapper assessmentPointMapper;
    private final AssessmentObjectiveMapper assessmentObjectiveMapper;
    private final AssessmentQuestionMapper assessmentQuestionMapper;
    private final QuestionObjectiveMapper questionObjectiveMapper;
    private final ObjectiveIndicatorWeightMapper weightMapper;
    private final ClassStudentMapper classStudentMapper;
    private final StudentMapper studentMapper;
    private final IndicatorMapper indicatorMapper;
    private final PersonalAchievementMapper personalAchievementMapper;

    public List<StudentAchievementSummary> list(Long classId) {
        return getClassDetails(classId).stream()
                .map(this::toSummary)
                .toList();
    }

    public StudentAchievementDetail getDetail(Long classId, Long studentId) {
        return getClassDetails(classId).stream()
                .filter(item -> item.studentId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new BizException("该学生不在当前教学班中"));
    }

    public List<StudentPointAchievement> listObjectiveStudents(Long classId, Long objectiveId) {
        List<PersonalAchievement> rows = personalAchievementMapper.selectList(
                new LambdaQueryWrapper<PersonalAchievement>()
                        .eq(PersonalAchievement::getClassId, classId)
                        .eq(PersonalAchievement::getScopeType, SCOPE_OBJECTIVE)
                        .eq(PersonalAchievement::getObjectiveId, objectiveId));
        if (!rows.isEmpty()) {
            return toStudentPointRows(rows);
        }
        return buildPointRowsFromDetails(
                calculateClassDetails(classId),
                detail -> detail.objectiveAchievements().getOrDefault(objectiveId, zero()));
    }

    public List<StudentPointAchievement> listCourseIndicatorStudents(Long classId, Long indicatorId) {
        List<PersonalAchievement> rows = personalAchievementMapper.selectList(
                new LambdaQueryWrapper<PersonalAchievement>()
                        .eq(PersonalAchievement::getClassId, classId)
                        .eq(PersonalAchievement::getScopeType, SCOPE_COURSE)
                        .eq(PersonalAchievement::getIndicatorId, indicatorId));
        if (!rows.isEmpty()) {
            return toStudentPointRows(rows);
        }
        return buildPointRowsFromDetails(
                calculateClassDetails(classId),
                detail -> detail.indicatorAchievements().getOrDefault(indicatorId, zero()));
    }

    public List<StudentPointAchievement> listMajorIndicatorStudents(
            Long majorId,
            Long semesterId,
            Long indicatorId) {
        List<PersonalAchievement> rows = personalAchievementMapper.selectList(
                new LambdaQueryWrapper<PersonalAchievement>()
                        .eq(PersonalAchievement::getMajorId, majorId)
                        .eq(PersonalAchievement::getSemesterId, semesterId)
                        .eq(PersonalAchievement::getScopeType, SCOPE_MAJOR)
                        .eq(PersonalAchievement::getIndicatorId, indicatorId));
        return toStudentPointRows(rows);
    }

    public byte[] exportExcel(Long classId) {
        List<StudentAchievementDetail> details = getClassDetails(classId);
        Map<Long, String> objectiveLabels = details.isEmpty()
                ? Map.of()
                : details.get(0).objectiveLabels();
        Map<Long, String> indicatorLabels = details.isEmpty()
                ? Map.of()
                : details.get(0).indicatorLabels();
        return PersonalAchievementExcelExporter.generate(details, objectiveLabels, indicatorLabels);
    }

    public void persistClassAchievements(Long classId, LocalDateTime calcTime) {
        List<StudentAchievementDetail> details = calculateClassDetails(classId);
        clearClassAchievements(classId);
        for (StudentAchievementDetail detail : details) {
            for (Map.Entry<Long, BigDecimal> entry : detail.objectiveAchievements().entrySet()) {
                insertPersonalAchievement(
                        detail.studentId(),
                        classId,
                        null,
                        null,
                        SCOPE_OBJECTIVE,
                        entry.getKey(),
                        null,
                        entry.getValue(),
                        calcTime);
            }
            for (Map.Entry<Long, BigDecimal> entry : detail.indicatorAchievements().entrySet()) {
                insertPersonalAchievement(
                        detail.studentId(),
                        classId,
                        null,
                        null,
                        SCOPE_COURSE,
                        null,
                        entry.getKey(),
                        entry.getValue(),
                        calcTime);
            }
        }
    }

    public void clearClassAchievements(Long classId) {
        personalAchievementMapper.delete(
                new LambdaQueryWrapper<PersonalAchievement>()
                        .eq(PersonalAchievement::getClassId, classId)
                        .in(PersonalAchievement::getScopeType, List.of(SCOPE_OBJECTIVE, SCOPE_COURSE)));
    }

    public void persistMajorAchievements(
            Long majorId,
            Long semesterId,
            Collection<Long> classIds,
            List<Level3Calculator.MacroWeightRecord> weightRecords,
            LocalDateTime calcTime) {
        List<Long> classIdList = classIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (classIdList.isEmpty() || weightRecords.isEmpty()) {
            clearMajorAchievements(majorId, semesterId);
            return;
        }

        for (Long classId : classIdList) {
            if (!hasCoursePersonalRows(classId)) {
                persistClassAchievements(classId, calcTime);
            }
        }

        List<PersonalAchievement> rows = personalAchievementMapper.selectList(
                new LambdaQueryWrapper<PersonalAchievement>()
                        .eq(PersonalAchievement::getScopeType, SCOPE_COURSE)
                        .in(PersonalAchievement::getClassId, classIdList));

        Map<Long, Map<Long, Map<Long, BigDecimal>>> byStudent = new LinkedHashMap<>();
        for (PersonalAchievement row : rows) {
            if (row.getStudentId() == null || row.getClassId() == null || row.getIndicatorId() == null) {
                continue;
            }
            byStudent.computeIfAbsent(row.getStudentId(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(row.getClassId(), key -> new LinkedHashMap<>())
                    .put(row.getIndicatorId(), row.getAchievement());
        }

        clearMajorAchievements(majorId, semesterId);
        for (Map.Entry<Long, Map<Long, Map<Long, BigDecimal>>> entry : byStudent.entrySet()) {
            Long studentId = entry.getKey();
            Map<Long, Map<Long, BigDecimal>> studentCourseData = entry.getValue();

            // Re-normalize weights within the student's enrolled courses.
            // Global weights sum to 1.0 across ALL cohort courses, but a student
            // only takes a subset — so we filter and re-normalize per student.
            Set<Long> studentClassIds = studentCourseData.keySet();

            // Filter weight records to only this student's classes
            List<Level3Calculator.MacroWeightRecord> studentWeights = new ArrayList<>();
            Map<Long, BigDecimal> indicatorSums = new HashMap<>();
            for (Level3Calculator.MacroWeightRecord w : weightRecords) {
                if (studentClassIds.contains(w.courseId())) {
                    studentWeights.add(w);
                    indicatorSums.merge(w.indicatorId(), w.weight(), BigDecimal::add);
                }
            }

            // Re-normalize: ensure sum = 1.0 for each indicator within this student's scope
            List<Level3Calculator.MacroWeightRecord> normalizedWeights = new ArrayList<>();
            for (Level3Calculator.MacroWeightRecord w : studentWeights) {
                BigDecimal sum = indicatorSums.get(w.indicatorId());
                if (sum != null && sum.compareTo(BigDecimal.ZERO) > 0
                        && sum.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.01")) > 0) {
                    log.warn("学生 {} 的指标点 {} 宏观支撑权重子集和为 {}，偏离 1.0 超过容差 0.01，已自动重新归一化；请检查专业 {} 学期 {} 的宏观支撑矩阵配置是否覆盖该学生全部修读课程",
                            studentId, w.indicatorId(), sum, majorId, semesterId);
                    BigDecimal factor = BigDecimal.ONE.divide(sum, 10, java.math.RoundingMode.HALF_UP);
                    normalizedWeights.add(new Level3Calculator.MacroWeightRecord(
                            w.courseId(), w.indicatorId(), w.weight().multiply(factor)));
                } else {
                    normalizedWeights.add(w);
                }
            }

            Map<Long, BigDecimal> achievements = Level3Calculator.calcMajorAchievement(
                    studentCourseData, normalizedWeights);
            for (Map.Entry<Long, BigDecimal> achievement : achievements.entrySet()) {
                insertPersonalAchievement(
                        studentId,
                        null,
                        majorId,
                        semesterId,
                        SCOPE_MAJOR,
                        null,
                        achievement.getKey(),
                        achievement.getValue(),
                        calcTime);
            }
        }
    }

    private void clearMajorAchievements(Long majorId, Long semesterId) {
        personalAchievementMapper.delete(
                new LambdaQueryWrapper<PersonalAchievement>()
                        .eq(PersonalAchievement::getMajorId, majorId)
                        .eq(PersonalAchievement::getSemesterId, semesterId)
                        .eq(PersonalAchievement::getScopeType, SCOPE_MAJOR));
    }

    private boolean hasCoursePersonalRows(Long classId) {
        Long count = personalAchievementMapper.selectCount(
                new LambdaQueryWrapper<PersonalAchievement>()
                        .eq(PersonalAchievement::getClassId, classId)
                        .eq(PersonalAchievement::getScopeType, SCOPE_COURSE));
        return count != null && count > 0;
    }

    private List<StudentAchievementDetail> getClassDetails(Long classId) {
        List<StudentAchievementDetail> persisted = listPersistedClassDetails(classId);
        if (!persisted.isEmpty()) {
            return persisted;
        }
        return calculateClassDetails(classId);
    }

    public List<StudentAchievementDetail> calculateClassDetails(Long classId) {
        CalculationContext context = loadContext(classId);
        return context.students().stream()
                .map(student -> calculateStudent(context, student))
                .toList();
    }

    private List<StudentAchievementDetail> listPersistedClassDetails(Long classId) {
        List<PersonalAchievement> rows = personalAchievementMapper.selectList(
                new LambdaQueryWrapper<PersonalAchievement>()
                        .eq(PersonalAchievement::getClassId, classId)
                        .in(PersonalAchievement::getScopeType, List.of(SCOPE_OBJECTIVE, SCOPE_COURSE)));
        if (rows.isEmpty()) {
            return List.of();
        }

        Set<Long> objectiveIds = rows.stream()
                .map(PersonalAchievement::getObjectiveId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> indicatorIds = rows.stream()
                .map(PersonalAchievement::getIndicatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> objectiveLabels = buildObjectiveLabels(objectiveIds);
        Map<Long, String> indicatorLabels = buildIndicatorLabels(indicatorIds);

        List<Student> students = loadClassStudents(classId);
        if (students.isEmpty()) {
            Set<Long> studentIds = rows.stream()
                    .map(PersonalAchievement::getStudentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            students = loadStudentsByIds(studentIds);
        }

        Map<Long, List<PersonalAchievement>> rowsByStudent = rows.stream()
                .filter(row -> row.getStudentId() != null)
                .collect(Collectors.groupingBy(
                        PersonalAchievement::getStudentId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<StudentAchievementDetail> details = new ArrayList<>();
        for (Student student : students) {
            List<PersonalAchievement> studentRows = rowsByStudent.getOrDefault(student.getId(), List.of());
            Map<Long, BigDecimal> objectiveAchievements = buildOrderedAchievements(
                    studentRows,
                    SCOPE_OBJECTIVE,
                    objectiveLabels.keySet(),
                    PersonalAchievement::getObjectiveId);
            Map<Long, BigDecimal> indicatorAchievements = buildOrderedAchievements(
                    studentRows,
                    SCOPE_COURSE,
                    indicatorLabels.keySet(),
                    PersonalAchievement::getIndicatorId);
            details.add(new StudentAchievementDetail(
                    student.getId(),
                    student.getStudentNo(),
                    student.getName(),
                    average(indicatorAchievements.values()),
                    objectiveAchievements,
                    indicatorAchievements,
                    objectiveLabels,
                    indicatorLabels));
        }
        return details;
    }

    private Map<Long, BigDecimal> buildOrderedAchievements(
            List<PersonalAchievement> rows,
            String scopeType,
            Collection<Long> orderedIds,
            Function<PersonalAchievement, Long> idGetter) {
        Map<Long, BigDecimal> rowMap = rows.stream()
                .filter(row -> scopeType.equals(row.getScopeType()))
                .filter(row -> idGetter.apply(row) != null)
                .collect(Collectors.toMap(
                        idGetter,
                        PersonalAchievement::getAchievement,
                        (left, right) -> left));
        Map<Long, BigDecimal> ordered = new LinkedHashMap<>();
        for (Long id : orderedIds) {
            ordered.put(id, rowMap.getOrDefault(id, zero()));
        }
        return ordered;
    }

    private StudentAchievementDetail calculateStudent(CalculationContext context, Student student) {
        List<Level1Calculator.StudentScoreRecord> studentScores = context.scores().stream()
                .filter(score -> score.getStudentId().equals(student.getId()))
                .map(score -> new Level1Calculator.StudentScoreRecord(
                        score.getStudentId(),
                        score.getAssessmentId(),
                        score.getQuestionId(),
                        score.getScore()))
                .toList();

        Map<Long, BigDecimal> objectiveAchievements = new LinkedHashMap<>();
        for (CourseObjective objective : context.objectives()) {
            BigDecimal achievement = Level1Calculator.calcObjectiveAchievement(
                    studentScores,
                    objective.getId(),
                    context.assessmentMaxMap(),
                    context.assessmentObjectiveMap(),
                    context.questionMaxMap(),
                    context.questionObjectiveMap());
            objectiveAchievements.put(objective.getId(), achievement);
        }

        Map<Long, BigDecimal> indicatorAchievements = Level2Calculator.calcCourseAchievement(
                objectiveAchievements,
                context.weightRecords());
        Map<Long, BigDecimal> orderedIndicatorAchievements = new LinkedHashMap<>();
        context.indicatorLabels().keySet().forEach(indicatorId ->
                orderedIndicatorAchievements.put(
                        indicatorId,
                        indicatorAchievements.getOrDefault(indicatorId, zero())));

        BigDecimal overallAchievement = average(orderedIndicatorAchievements.values());
        return new StudentAchievementDetail(
                student.getId(),
                student.getStudentNo(),
                student.getName(),
                overallAchievement,
                objectiveAchievements,
                orderedIndicatorAchievements,
                context.objectiveLabels(),
                context.indicatorLabels());
    }

    private CalculationContext loadContext(Long classId) {
        ScoreSheet sheet = scoreSheetMapper.selectOne(
                new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, classId));
        if (sheet == null) {
            throw new BizException("当前教学班尚未创建成绩单");
        }

        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));
        if (outline == null) {
            throw new BizException("课程大纲不存在，请先配置课程目标");
        }

        List<CourseObjective> objectives = objectiveMapper.selectList(
                new LambdaQueryWrapper<CourseObjective>()
                        .eq(CourseObjective::getOutlineId, outline.getId())
                        .orderByAsc(CourseObjective::getObjNo));
        if (objectives.isEmpty()) {
            throw new BizException("未配置课程目标");
        }

        List<AssessmentPoint> assessments = assessmentPointMapper.selectList(
                new LambdaQueryWrapper<AssessmentPoint>()
                        .eq(AssessmentPoint::getOutlineId, outline.getId())
                        .orderByAsc(AssessmentPoint::getSortOrder));
        if (assessments.isEmpty()) {
            throw new BizException("未配置考核点");
        }

        List<Long> assessmentIds = assessments.stream().map(AssessmentPoint::getId).toList();
        Map<Long, List<Long>> assessmentObjectiveMap = buildAssessmentObjectiveMap(assessments, assessmentIds);
        Map<Long, BigDecimal> assessmentMaxMap = assessments.stream()
                .collect(Collectors.toMap(
                        AssessmentPoint::getId,
                        AssessmentPoint::getMaxScore,
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<AssessmentQuestion> questions = assessmentQuestionMapper.selectList(
                new LambdaQueryWrapper<AssessmentQuestion>()
                        .in(AssessmentQuestion::getAssessmentId, assessmentIds)
                        .orderByAsc(AssessmentQuestion::getSortOrder));
        Map<Long, BigDecimal> questionMaxMap = questions.stream()
                .collect(Collectors.toMap(
                        AssessmentQuestion::getId,
                        AssessmentQuestion::getMaxScore,
                        (left, right) -> left,
                        LinkedHashMap::new));
        Map<Long, List<Long>> questionObjectiveMap = buildQuestionObjectiveMap(questions);

        List<ObjectiveIndicatorWeight> weights = weightMapper.selectList(
                new LambdaQueryWrapper<ObjectiveIndicatorWeight>()
                        .in(ObjectiveIndicatorWeight::getObjectiveId,
                                objectives.stream().map(CourseObjective::getId).toList()));
        if (weights.isEmpty()) {
            throw new BizException("未配置课程目标与指标点权重");
        }
        List<Level2Calculator.WeightRecord> weightRecords = weights.stream()
                .map(weight -> new Level2Calculator.WeightRecord(
                        weight.getObjectiveId(),
                        weight.getIndicatorId(),
                        weight.getWeight()))
                .toList();

        List<Student> students = loadClassStudents(classId);
        List<StudentScore> scores = studentScoreMapper.selectList(
                new LambdaQueryWrapper<StudentScore>().eq(StudentScore::getSheetId, sheet.getId()));

        Map<Long, String> objectiveLabels = objectives.stream()
                .collect(Collectors.toMap(
                        CourseObjective::getId,
                        CourseObjective::getObjNo,
                        (left, right) -> left,
                        LinkedHashMap::new));

        Set<Long> indicatorIds = weights.stream()
                .map(ObjectiveIndicatorWeight::getIndicatorId)
                .collect(Collectors.toSet());
        Map<Long, String> indicatorLabels = buildIndicatorLabels(indicatorIds);

        return new CalculationContext(
                students,
                scores,
                objectives,
                assessmentMaxMap,
                assessmentObjectiveMap,
                questionMaxMap,
                questionObjectiveMap,
                weightRecords,
                objectiveLabels,
                indicatorLabels);
    }

    private List<Student> loadClassStudents(Long classId) {
        List<ClassStudent> classStudents = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getClassId, classId));
        Set<Long> studentIds = classStudents.stream()
                .map(ClassStudent::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return loadStudentsByIds(studentIds);
    }

    private List<Student> loadStudentsByIds(Collection<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return studentMapper.selectBatchIds(studentIds).stream()
                .sorted(Comparator.comparing(
                        Student::getStudentNo,
                        Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private Map<Long, String> buildObjectiveLabels(Collection<Long> objectiveIds) {
        if (objectiveIds.isEmpty()) {
            return Map.of();
        }
        return objectiveMapper.selectBatchIds(objectiveIds).stream()
                .sorted(Comparator.comparing(
                        CourseObjective::getObjNo,
                        Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toMap(
                        CourseObjective::getId,
                        CourseObjective::getObjNo,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<Long, String> buildIndicatorLabels(Collection<Long> indicatorIds) {
        if (indicatorIds.isEmpty()) {
            return Map.of();
        }
        return indicatorMapper.selectBatchIds(indicatorIds).stream()
                .sorted(Comparator.comparing(
                        Indicator::getIndicatorNo,
                        Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toMap(
                        Indicator::getId,
                        indicator -> indicator.getIndicatorNo() == null
                                ? String.valueOf(indicator.getId())
                                : indicator.getIndicatorNo(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<Long, List<Long>> buildAssessmentObjectiveMap(
            List<AssessmentPoint> assessments,
            List<Long> assessmentIds) {
        List<AssessmentObjective> bindings = assessmentObjectiveMapper.selectList(
                new LambdaQueryWrapper<AssessmentObjective>()
                        .in(AssessmentObjective::getAssessmentId, assessmentIds));
        Map<Long, List<Long>> result = new HashMap<>();
        for (AssessmentObjective binding : bindings) {
            result.computeIfAbsent(binding.getAssessmentId(), key -> new ArrayList<>())
                    .add(binding.getObjectiveId());
        }
        for (AssessmentPoint assessment : assessments) {
            if (!result.containsKey(assessment.getId()) && assessment.getObjectiveId() != null) {
                result.put(assessment.getId(), List.of(assessment.getObjectiveId()));
            }
        }
        return result;
    }

    private Map<Long, List<Long>> buildQuestionObjectiveMap(List<AssessmentQuestion> questions) {
        if (questions.isEmpty()) {
            return Map.of();
        }
        List<QuestionObjective> bindings = questionObjectiveMapper.selectList(
                new LambdaQueryWrapper<QuestionObjective>()
                        .in(QuestionObjective::getQuestionId,
                                questions.stream().map(AssessmentQuestion::getId).toList()));
        Map<Long, List<Long>> result = new HashMap<>();
        for (QuestionObjective binding : bindings) {
            result.computeIfAbsent(binding.getQuestionId(), key -> new ArrayList<>())
                    .add(binding.getObjectiveId());
        }
        return result;
    }

    private List<StudentPointAchievement> toStudentPointRows(List<PersonalAchievement> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> studentIds = rows.stream()
                .map(PersonalAchievement::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Student> studentMap = loadStudentsByIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        return rows.stream()
                .map(row -> {
                    Student student = studentMap.get(row.getStudentId());
                    return new StudentPointAchievement(
                            row.getStudentId(),
                            student == null ? null : student.getStudentNo(),
                            student == null ? null : student.getName(),
                            row.getAchievement());
                })
                .sorted(pointComparator())
                .toList();
    }

    private List<StudentPointAchievement> buildPointRowsFromDetails(
            List<StudentAchievementDetail> details,
            Function<StudentAchievementDetail, BigDecimal> achievementGetter) {
        return details.stream()
                .map(detail -> new StudentPointAchievement(
                        detail.studentId(),
                        detail.studentNo(),
                        detail.studentName(),
                        achievementGetter.apply(detail)))
                .sorted(pointComparator())
                .toList();
    }

    private Comparator<StudentPointAchievement> pointComparator() {
        return Comparator.comparing(
                StudentPointAchievement::studentNo,
                Comparator.nullsLast(String::compareTo));
    }

    private void insertPersonalAchievement(
            Long studentId,
            Long classId,
            Long majorId,
            Long semesterId,
            String scopeType,
            Long objectiveId,
            Long indicatorId,
            BigDecimal achievement,
            LocalDateTime calcTime) {
        PersonalAchievement row = new PersonalAchievement();
        row.setStudentId(studentId);
        row.setClassId(classId);
        row.setMajorId(majorId);
        row.setSemesterId(semesterId);
        row.setScopeType(scopeType);
        row.setObjectiveId(objectiveId);
        row.setIndicatorId(indicatorId);
        row.setAchievement(achievement == null ? zero() : achievement);
        row.setCalcTime(calcTime);
        personalAchievementMapper.insert(row);
    }

    private StudentAchievementSummary toSummary(StudentAchievementDetail detail) {
        return new StudentAchievementSummary(
                detail.studentId(),
                detail.studentNo(),
                detail.studentName(),
                detail.overallAchievement(),
                detail.indicatorAchievements());
    }

    private BigDecimal average(Collection<BigDecimal> values) {
        if (values.isEmpty()) {
            return zero();
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    public record StudentAchievementSummary(
            Long studentId,
            String studentNo,
            String studentName,
            BigDecimal overallAchievement,
            Map<Long, BigDecimal> indicatorAchievements) {
    }

    public record StudentAchievementDetail(
            Long studentId,
            String studentNo,
            String studentName,
            BigDecimal overallAchievement,
            Map<Long, BigDecimal> objectiveAchievements,
            Map<Long, BigDecimal> indicatorAchievements,
            Map<Long, String> objectiveLabels,
            Map<Long, String> indicatorLabels) {
    }

    public record StudentPointAchievement(
            Long studentId,
            String studentNo,
            String studentName,
            BigDecimal achievement) {
    }

    private record CalculationContext(
            List<Student> students,
            List<StudentScore> scores,
            List<CourseObjective> objectives,
            Map<Long, BigDecimal> assessmentMaxMap,
            Map<Long, List<Long>> assessmentObjectiveMap,
            Map<Long, BigDecimal> questionMaxMap,
            Map<Long, List<Long>> questionObjectiveMap,
            List<Level2Calculator.WeightRecord> weightRecords,
            Map<Long, String> objectiveLabels,
            Map<Long, String> indicatorLabels) {
    }
}
