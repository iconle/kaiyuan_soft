package com.obe.platform.moduled.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.Course;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.modulea.entity.TeachingClass;
import com.obe.platform.modulea.mapper.CourseMapper;
import com.obe.platform.modulea.mapper.IndicatorMapper;
import com.obe.platform.modulea.mapper.TeachingClassMapper;
import com.obe.platform.moduleb.entity.AssessmentPoint;
import com.obe.platform.moduleb.entity.CourseObjective;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.mapper.CourseObjectiveMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import com.obe.platform.modulec.entity.CourseAchievement;
import com.obe.platform.modulec.entity.ObjAchievement;
import com.obe.platform.modulec.mapper.CourseAchievementMapper;
import com.obe.platform.modulec.mapper.ObjAchievementMapper;
import com.obe.platform.modulec.service.ScoreService;
import com.obe.platform.moduled.exporter.CourseExcelExporter;
import com.obe.platform.moduled.exporter.PdfExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseReportService {

    private final ObjAchievementMapper objAchievementMapper;
    private final CourseAchievementMapper courseAchievementMapper;
    private final CourseOutlineMapper outlineMapper;
    private final CourseObjectiveMapper objectiveMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseMapper courseMapper;
    private final IndicatorMapper indicatorMapper;
    private final ScoreService scoreService;

    public Map<String, Object> getReportData(Long classId) {
        CourseReportData report = buildReportData(classId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courseName", report.courseName());
        data.put("className", report.className());
        data.put("calcTime", report.calcTime());
        data.put("objectiveResults", objectiveResultMap(report.objectiveResults()));
        data.put("indicatorResults", indicatorResultMap(report.indicatorResults()));
        data.put("assessmentResults", report.assessmentResults());
        data.put("studentScoreDetails", report.studentScoreDetails());
        return data;
    }

    public byte[] generatePdf(Long classId) {
        CourseReportData data = buildReportData(classId);
        return PdfExporter.generateCourseReport(
                data.courseName(),
                data.className(),
                data.objectiveResults(),
                data.indicatorResults(),
                data.assessmentResults(),
                data.calcTime());
    }

    public byte[] generateExcel(Long classId) {
        CourseReportData data = buildReportData(classId);
        return CourseExcelExporter.generateCourseReport(
                data.courseName(),
                data.className(),
                data.calcTime(),
                data.objectiveResults(),
                data.indicatorResults(),
                data.assessmentResults(),
                data.studentScoreDetails());
    }

    private CourseReportData buildReportData(Long classId) {
        TeachingClass teachingClass = teachingClassMapper.selectById(classId);
        if (teachingClass == null) throw new BizException("教学班级不存在");

        Course course = courseMapper.selectById(teachingClass.getCourseId());
        String courseName = course != null ? course.getName() : "";

        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));
        List<CourseObjective> objectives = outline == null ? List.of() : objectiveMapper.selectList(
                new LambdaQueryWrapper<CourseObjective>()
                        .eq(CourseObjective::getOutlineId, outline.getId())
                        .orderByAsc(CourseObjective::getId));
        Map<Long, CourseObjective> objectiveById = objectives.stream()
                .collect(Collectors.toMap(CourseObjective::getId, Function.identity()));

        List<ObjAchievement> objAchievements = objAchievementMapper.selectList(
                new LambdaQueryWrapper<ObjAchievement>().eq(ObjAchievement::getClassId, classId));
        Map<Long, ObjAchievement> objAchievementByObjective = objAchievements.stream()
                .collect(Collectors.toMap(
                        ObjAchievement::getObjectiveId,
                        Function.identity(),
                        (left, right) -> left));

        List<CourseObjectiveResult> objectiveResults = new ArrayList<>();
        for (CourseObjective objective : objectives) {
            ObjAchievement achievement = objAchievementByObjective.get(objective.getId());
            objectiveResults.add(new CourseObjectiveResult(
                    objective.getObjNo(),
                    objective.getDimension(),
                    objective.getDescription(),
                    achievement != null ? achievement.getAchievement() : null));
        }

        List<CourseAchievement> courseAchievements = courseAchievementMapper.selectList(
                new LambdaQueryWrapper<CourseAchievement>().eq(CourseAchievement::getClassId, classId));
        Map<Long, Indicator> indicatorById = loadIndicators(courseAchievements);
        List<CourseIndicatorResult> indicatorResults = new ArrayList<>();
        LocalDateTime calcTime = null;
        for (CourseAchievement achievement : courseAchievements) {
            Indicator indicator = indicatorById.get(achievement.getIndicatorId());
            indicatorResults.add(new CourseIndicatorResult(
                    indicator != null ? indicator.getIndicatorNo() : String.valueOf(achievement.getIndicatorId()),
                    indicator != null ? indicator.getContent() : "",
                    achievement.getAchievement()));
            if (calcTime == null) calcTime = achievement.getCalcTime();
        }
        if (calcTime == null && !objAchievements.isEmpty()) {
            calcTime = objAchievements.get(0).getCalcTime();
        }

        ScoreService.ScorePreview preview = scoreService.getScorePreview(classId);
        List<CourseAssessmentResult> assessmentResults = buildAssessmentResults(preview, objectiveById);
        List<CourseStudentScoreResult> studentScoreDetails = buildStudentScoreDetails(preview);

        return new CourseReportData(
                courseName,
                teachingClass.getClassName(),
                calcTime,
                objectiveResults,
                indicatorResults,
                assessmentResults,
                studentScoreDetails);
    }

    private Map<Long, Indicator> loadIndicators(List<CourseAchievement> achievements) {
        List<Long> indicatorIds = achievements.stream()
                .map(CourseAchievement::getIndicatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (indicatorIds.isEmpty()) return Map.of();
        return indicatorMapper.selectBatchIds(indicatorIds).stream()
                .collect(Collectors.toMap(Indicator::getId, Function.identity()));
    }

    private List<CourseAssessmentResult> buildAssessmentResults(
            ScoreService.ScorePreview preview,
            Map<Long, CourseObjective> objectiveById) {
        if (preview == null || preview.assessments() == null) return List.of();

        List<CourseAssessmentResult> results = new ArrayList<>();
        for (AssessmentPoint assessment : preview.assessments()) {
            AverageScore averageScore = averageScore(preview, assessment.getId());
            results.add(new CourseAssessmentResult(
                    assessment.getName(),
                    formatObjectiveNos(assessment.getObjectiveIds(), objectiveById),
                    assessment.getMaxScore(),
                    averageScore.average(),
                    averageScore.count()));
        }
        return results;
    }

    private List<CourseStudentScoreResult> buildStudentScoreDetails(ScoreService.ScorePreview preview) {
        if (preview == null || preview.rows() == null) return List.of();

        List<CourseStudentScoreResult> results = new ArrayList<>();
        for (ScoreService.ScoreRow row : preview.rows()) {
            if (row.cells() == null) continue;
            for (ScoreService.ScoreCell cell : row.cells()) {
                results.add(new CourseStudentScoreResult(
                        row.studentNo(),
                        row.studentName(),
                        cell.assessmentName(),
                        cell.score()));
            }
        }
        return results;
    }

    private String formatObjectiveNos(
            List<Long> objectiveIds,
            Map<Long, CourseObjective> objectiveById) {
        if (objectiveIds == null || objectiveIds.isEmpty()) return "";
        return objectiveIds.stream()
                .map(id -> {
                    CourseObjective objective = objectiveById.get(id);
                    return objective != null ? objective.getObjNo() : String.valueOf(id);
                })
                .collect(Collectors.joining(", "));
    }

    private AverageScore averageScore(ScoreService.ScorePreview preview, Long assessmentId) {
        if (preview.rows() == null) return new AverageScore(null, 0);

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (ScoreService.ScoreRow row : preview.rows()) {
            if (row.cells() == null) continue;
            for (ScoreService.ScoreCell cell : row.cells()) {
                if (Objects.equals(cell.assessmentId(), assessmentId) && cell.score() != null) {
                    total = total.add(cell.score());
                    count++;
                }
            }
        }
        if (count == 0) return new AverageScore(null, 0);
        return new AverageScore(
                total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP),
                count);
    }

    private Map<String, BigDecimal> objectiveResultMap(List<CourseObjectiveResult> objectiveResults) {
        Map<String, BigDecimal> results = new LinkedHashMap<>();
        for (CourseObjectiveResult result : objectiveResults) {
            results.put(result.objectiveNo(), result.achievement());
        }
        return results;
    }

    private Map<String, BigDecimal> indicatorResultMap(List<CourseIndicatorResult> indicatorResults) {
        Map<String, BigDecimal> results = new LinkedHashMap<>();
        for (CourseIndicatorResult result : indicatorResults) {
            results.put(result.indicatorNo(), result.achievement());
        }
        return results;
    }

    private record AverageScore(BigDecimal average, int count) {}

    public record CourseReportData(
            String courseName,
            String className,
            LocalDateTime calcTime,
            List<CourseObjectiveResult> objectiveResults,
            List<CourseIndicatorResult> indicatorResults,
            List<CourseAssessmentResult> assessmentResults,
            List<CourseStudentScoreResult> studentScoreDetails) {}

    public record CourseObjectiveResult(
            String objectiveNo,
            String dimension,
            String description,
            BigDecimal achievement) {}

    public record CourseIndicatorResult(
            String indicatorNo,
            String content,
            BigDecimal achievement) {}

    public record CourseAssessmentResult(
            String assessmentName,
            String objectiveNos,
            BigDecimal maxScore,
            BigDecimal averageScore,
            int scoreCount) {}

    public record CourseStudentScoreResult(
            String studentNo,
            String studentName,
            String assessmentName,
            BigDecimal score) {}
}
