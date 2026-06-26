package com.obe.platform.moduled.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.*;
import com.obe.platform.modulea.mapper.*;
import com.obe.platform.moduleb.entity.AssessmentPoint;
import com.obe.platform.moduleb.entity.CourseObjective;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.entity.ObjectiveIndicatorWeight;
import com.obe.platform.moduleb.mapper.AssessmentPointMapper;
import com.obe.platform.moduleb.mapper.CourseObjectiveMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import com.obe.platform.moduleb.mapper.ObjectiveIndicatorWeightMapper;
import com.obe.platform.modulec.entity.*;
import com.obe.platform.modulec.mapper.*;
import com.obe.platform.moduled.exporter.TraceExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MajorReportService {

    private final MajorAchievementMapper majorAchievementMapper;
    private final IndicatorMapper indicatorMapper;
    private final CourseMapper courseMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final MacroSupportMatrixMapper macroMatrixMapper;
    private final CourseAchievementMapper courseAchievementMapper;
    private final ObjAchievementMapper objAchievementMapper;
    private final CourseOutlineMapper outlineMapper;
    private final CourseObjectiveMapper objectiveMapper;
    private final ObjectiveIndicatorWeightMapper weightMapper;
    private final AssessmentPointMapper assessmentPointMapper;
    private final StudentScoreMapper studentScoreMapper;
    private final ScoreSheetMapper scoreSheetMapper;

    /**
     * Get major-level report data including radar chart data.
     */
    public Map<String, Object> getReportData(Long majorId, Long semesterId) {
        Map<String, Object> data = new LinkedHashMap<>();

        List<MajorAchievement> achievements = majorAchievementMapper.selectList(
                new LambdaQueryWrapper<MajorAchievement>()
                        .eq(MajorAchievement::getMajorId, majorId)
                        .eq(MajorAchievement::getSemesterId, semesterId));

        // Build indicator name lookup
        List<Indicator> indicators = indicatorMapper.selectList(
                new LambdaQueryWrapper<Indicator>());
        Map<Long, Indicator> indicatorMap = new HashMap<>();
        for (Indicator ind : indicators) {
            indicatorMap.put(ind.getId(), ind);
        }

        // Radar chart data: indicatorNo → G_k
        Map<String, BigDecimal> radarData = new LinkedHashMap<>();
        for (MajorAchievement ma : achievements) {
            Indicator ind = indicatorMap.get(ma.getIndicatorId());
            String label = ind != null ? ind.getIndicatorNo() : String.valueOf(ma.getIndicatorId());
            radarData.put(label, ma.getAchievement());
        }
        data.put("radarData", radarData);
        data.put("achievements", achievements);
        data.put("calcTime", achievements.isEmpty() ? null : achievements.get(0).getCalcTime());

        return data;
    }

    /**
     * Generate drill-through Excel ledger for a major.
     */
    public byte[] generateTraceExcel(Long majorId, Long semesterId) {
        // Get major achievements
        List<MajorAchievement> achievements = majorAchievementMapper.selectList(
                new LambdaQueryWrapper<MajorAchievement>()
                        .eq(MajorAchievement::getMajorId, majorId)
                        .eq(MajorAchievement::getSemesterId, semesterId));

        List<Indicator> indicators = indicatorMapper.selectList(new LambdaQueryWrapper<Indicator>());
        Map<Long, Indicator> indMap = new HashMap<>();
        for (Indicator ind : indicators) {
            indMap.put(ind.getId(), ind);
        }

        // Get all courses for this major
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getMajorId, majorId));

        // Get macro matrix
        List<MacroSupportMatrix> matrix = macroMatrixMapper.selectList(
                new LambdaQueryWrapper<MacroSupportMatrix>()
                        .in(MacroSupportMatrix::getCourseId,
                                courses.stream().map(Course::getId).toList()));

        Map<String, BigDecimal> majorResultsMap = new LinkedHashMap<>();
        for (MajorAchievement ma : achievements) {
            Indicator ind = indMap.get(ma.getIndicatorId());
            majorResultsMap.put(ind != null ? ind.getIndicatorNo() : String.valueOf(ma.getIndicatorId()),
                    ma.getAchievement());
        }

        List<TraceExcelExporter.IndicatorTraceSheet> traceSheets = new ArrayList<>();

        for (MajorAchievement ma : achievements) {
            Long indicatorId = ma.getIndicatorId();
            Indicator ind = indMap.get(indicatorId);
            String indNo = ind != null ? ind.getIndicatorNo() : String.valueOf(indicatorId);

            List<TraceExcelExporter.TraceRow> rows = new ArrayList<>();

            // For each course that supports this indicator
            for (MacroSupportMatrix m : matrix) {
                if (!m.getIndicatorId().equals(indicatorId)) continue;

                Course course = courses.stream()
                        .filter(c -> c.getId().equals(m.getCourseId()))
                        .findFirst().orElse(null);
                if (course == null) continue;

                List<TeachingClass> classes = teachingClassMapper.selectList(
                        new LambdaQueryWrapper<TeachingClass>().eq(TeachingClass::getCourseId, course.getId()));

                for (TeachingClass tc : classes) {
                    // Get course achievement for this indicator
                    CourseAchievement ca = courseAchievementMapper.selectOne(
                            new LambdaQueryWrapper<CourseAchievement>()
                                    .eq(CourseAchievement::getClassId, tc.getId())
                                    .eq(CourseAchievement::getIndicatorId, indicatorId));

                    BigDecimal ek = ca != null ? ca.getAchievement() : null;

                    // Get objective-level details
                    CourseOutline outline = outlineMapper.selectOne(
                            new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, tc.getId()));
                    if (outline == null) continue;

                    List<CourseObjective> objectives = objectiveMapper.selectList(
                            new LambdaQueryWrapper<CourseObjective>().eq(CourseObjective::getOutlineId, outline.getId()));

                    for (CourseObjective obj : objectives) {
                        ObjAchievement oa = objAchievementMapper.selectOne(
                                new LambdaQueryWrapper<ObjAchievement>()
                                        .eq(ObjAchievement::getClassId, tc.getId())
                                        .eq(ObjAchievement::getObjectiveId, obj.getId()));

                        ObjectiveIndicatorWeight wijk = weightMapper.selectOne(
                                new LambdaQueryWrapper<ObjectiveIndicatorWeight>()
                                        .eq(ObjectiveIndicatorWeight::getObjectiveId, obj.getId())
                                        .eq(ObjectiveIndicatorWeight::getIndicatorId, indicatorId));

                        List<AssessmentPoint> aps = assessmentPointMapper.selectList(
                                new LambdaQueryWrapper<AssessmentPoint>()
                                        .eq(AssessmentPoint::getObjectiveId, obj.getId()));

                        for (AssessmentPoint ap : aps) {
                            // Compute average score for this assessment
                            ScoreSheet ss = scoreSheetMapper.selectOne(
                                    new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, tc.getId()));
                            BigDecimal avgScore = null;
                            if (ss != null) {
                                List<StudentScore> scores = studentScoreMapper.selectList(
                                        new LambdaQueryWrapper<StudentScore>()
                                                .eq(StudentScore::getSheetId, ss.getId())
                                                .eq(StudentScore::getAssessmentId, ap.getId()));
                                if (!scores.isEmpty()) {
                                    // 按学生汇总得分(题目级录入时为该生各题目得分之和)，再对有成绩的学生求平均
                                    Map<Long, BigDecimal> byStudent = new LinkedHashMap<>();
                                    for (StudentScore sc : scores) {
                                        BigDecimal v = sc.getScore() == null ? BigDecimal.ZERO : sc.getScore();
                                        byStudent.merge(sc.getStudentId(), v, BigDecimal::add);
                                    }
                                    BigDecimal total = BigDecimal.ZERO;
                                    for (BigDecimal v : byStudent.values()) {
                                        total = total.add(v);
                                    }
                                    avgScore = total.divide(
                                            BigDecimal.valueOf(byStudent.size()), 2, RoundingMode.HALF_UP);
                                }
                            }

                            rows.add(new TraceExcelExporter.TraceRow(
                                    course.getName(), ek, m.getWeight(),
                                    obj.getObjNo(),
                                    oa != null ? oa.getAchievement() : null,
                                    wijk != null ? wijk.getWeight() : null,
                                    ap.getName(), ap.getMaxScore(), avgScore));
                        }
                    }
                }
            }

            traceSheets.add(new TraceExcelExporter.IndicatorTraceSheet(indNo, rows));
        }

        return TraceExcelExporter.generateTraceLedger(majorResultsMap, traceSheets);
    }
}
