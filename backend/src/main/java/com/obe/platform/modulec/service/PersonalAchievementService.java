package com.obe.platform.modulec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.engine.Level1Calculator;
import com.obe.platform.engine.Level2Calculator;
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
import com.obe.platform.modulec.entity.ScoreSheet;
import com.obe.platform.modulec.entity.StudentScore;
import com.obe.platform.modulec.mapper.ScoreSheetMapper;
import com.obe.platform.modulec.mapper.StudentScoreMapper;
import com.obe.platform.moduled.exporter.PersonalAchievementExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalAchievementService {

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

    public List<StudentAchievementSummary> list(Long classId) {
        CalculationContext context = loadContext(classId);
        return context.students().stream()
                .map(student -> toSummary(calculateStudent(context, student)))
                .toList();
    }

    public StudentAchievementDetail getDetail(Long classId, Long studentId) {
        CalculationContext context = loadContext(classId);
        Student student = context.students().stream()
                .filter(item -> item.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new BizException("该学生不在当前教学班中"));
        return calculateStudent(context, student);
    }

    public byte[] exportExcel(Long classId) {
        CalculationContext context = loadContext(classId);
        List<StudentAchievementDetail> details = context.students().stream()
                .map(student -> calculateStudent(context, student))
                .toList();
        return PersonalAchievementExcelExporter.generate(
                details,
                context.objectiveLabels(),
                context.indicatorLabels());
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
                        indicatorAchievements.getOrDefault(indicatorId, BigDecimal.ZERO)));

        BigDecimal overallAchievement = average(orderedIndicatorAchievements.values().stream().toList());
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

        List<ClassStudent> classStudents = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getClassId, classId));
        List<Long> studentIds = classStudents.stream().map(ClassStudent::getStudentId).toList();
        List<Student> students = studentIds.isEmpty()
                ? List.of()
                : studentMapper.selectBatchIds(studentIds).stream()
                        .sorted(Comparator.comparing(
                                Student::getStudentNo,
                                Comparator.nullsLast(String::compareTo)))
                        .toList();

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
        Map<Long, Indicator> indicatorMap = indicatorIds.isEmpty()
                ? Map.of()
                : indicatorMapper.selectBatchIds(indicatorIds).stream()
                        .collect(Collectors.toMap(Indicator::getId, Function.identity()));
        Map<Long, String> indicatorLabels = weights.stream()
                .map(ObjectiveIndicatorWeight::getIndicatorId)
                .distinct()
                .sorted()
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> indicatorMap.containsKey(id)
                                ? indicatorMap.get(id).getIndicatorNo()
                                : String.valueOf(id),
                        (left, right) -> left,
                        LinkedHashMap::new));

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

    private StudentAchievementSummary toSummary(StudentAchievementDetail detail) {
        return new StudentAchievementSummary(
                detail.studentId(),
                detail.studentNo(),
                detail.studentName(),
                detail.overallAchievement(),
                detail.indicatorAchievements());
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
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
