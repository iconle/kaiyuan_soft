package com.obe.platform.moduled.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.Course;
import com.obe.platform.modulea.entity.TeachingClass;
import com.obe.platform.modulea.mapper.CourseMapper;
import com.obe.platform.modulea.mapper.TeachingClassMapper;
import com.obe.platform.moduleb.entity.CourseObjective;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.mapper.CourseObjectiveMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import com.obe.platform.modulec.entity.CourseAchievement;
import com.obe.platform.modulec.entity.ObjAchievement;
import com.obe.platform.modulec.mapper.CourseAchievementMapper;
import com.obe.platform.modulec.mapper.ObjAchievementMapper;
import com.obe.platform.modulec.service.ScoreService;
import com.obe.platform.moduled.exporter.PdfExporter;
import com.obe.platform.moduled.exporter.TraceExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseReportService {

    private final ObjAchievementMapper objAchievementMapper;
    private final CourseAchievementMapper courseAchievementMapper;
    private final CourseOutlineMapper outlineMapper;
    private final CourseObjectiveMapper objectiveMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseMapper courseMapper;
    private final ScoreService scoreService;

    /**
     * Build course-level report data.
     */
    public Map<String, Object> getReportData(Long classId) {
        Map<String, Object> data = new LinkedHashMap<>();

        TeachingClass tc = teachingClassMapper.selectById(classId);
        if (tc == null) throw new BizException("教学班级不存在");

        Course course = courseMapper.selectById(tc.getCourseId());
        data.put("courseName", course != null ? course.getName() : "");
        data.put("className", tc.getClassName());

        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));

        // Objective achievements
        List<ObjAchievement> objList = objAchievementMapper.selectList(
                new LambdaQueryWrapper<ObjAchievement>().eq(ObjAchievement::getClassId, classId));

        Map<String, BigDecimal> objectiveMap = new LinkedHashMap<>();
        if (outline != null) {
            List<CourseObjective> objectives = objectiveMapper.selectList(
                    new LambdaQueryWrapper<CourseObjective>().eq(CourseObjective::getOutlineId, outline.getId()));
            for (CourseObjective obj : objectives) {
                BigDecimal val = objList.stream()
                        .filter(o -> o.getObjectiveId().equals(obj.getId()))
                        .findFirst()
                        .map(ObjAchievement::getAchievement)
                        .orElse(null);
                objectiveMap.put(obj.getObjNo(), val);
            }
        }
        data.put("objectiveResults", objectiveMap);

        // Course indicator achievements
        List<CourseAchievement> courseList = courseAchievementMapper.selectList(
                new LambdaQueryWrapper<CourseAchievement>().eq(CourseAchievement::getClassId, classId));
        Map<Long, BigDecimal> indicatorMap = new LinkedHashMap<>();
        LocalDateTime calcTime = null;
        for (CourseAchievement ca : courseList) {
            indicatorMap.put(ca.getIndicatorId(), ca.getAchievement());
            if (calcTime == null) calcTime = ca.getCalcTime();
        }
        data.put("indicatorResults", indicatorMap);
        data.put("calcTime", calcTime);

        // Score preview
        data.put("scorePreview", scoreService.getScorePreview(classId));

        return data;
    }

    /** Generate course-level PDF report */
    public byte[] generatePdf(Long classId) {
        Map<String, Object> data = getReportData(classId);

        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> objResults = (Map<String, BigDecimal>) data.get("objectiveResults");
        @SuppressWarnings("unchecked")
        Map<Long, BigDecimal> indResults = (Map<Long, BigDecimal>) data.get("indicatorResults");

        Map<String, BigDecimal> indStrMap = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> e : indResults.entrySet()) {
            indStrMap.put(String.valueOf(e.getKey()), e.getValue());
        }

        return PdfExporter.generateCourseReport(
                (String) data.get("courseName"),
                (String) data.get("className"),
                objResults,
                indStrMap,
                (LocalDateTime) data.get("calcTime"));
    }

    /** Generate course-level Excel workbook with score details */
    public byte[] generateExcel(Long classId) {
        Map<String, Object> data = getReportData(classId);

        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> objResults = (Map<String, BigDecimal>) data.get("objectiveResults");
        @SuppressWarnings("unchecked")
        Map<Long, BigDecimal> indResults = (Map<Long, BigDecimal>) data.get("indicatorResults");

        Map<String, BigDecimal> indStrMap = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> e : indResults.entrySet()) {
            indStrMap.put("指标点" + e.getKey(), e.getValue());
        }

        // Build trace rows from score preview
        List<TraceExcelExporter.TraceRow> rows = new ArrayList<>();
        ScoreService.ScorePreview preview = scoreService.getScorePreview(classId);
        if (preview != null && preview.headers() != null) {
            for (ScoreService.AssessmentHeader h : preview.headers()) {
                BigDecimal avgScore = BigDecimal.ZERO;
                int count = 0;
                if (preview.rows() != null) {
                    for (ScoreService.ScoreRow row : preview.rows()) {
                        for (ScoreService.ScoreCell cell : row.cells()) {
                            if (cell.assessmentId().equals(h.id()) && cell.score() != null) {
                                avgScore = avgScore.add(cell.score());
                                count++;
                            }
                        }
                    }
                }
                if (count > 0) {
                    avgScore = avgScore.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
                }
                rows.add(new TraceExcelExporter.TraceRow(
                        (String) data.get("courseName"), null, null,
                        "目标" + h.objectiveId(),
                        objResults != null ? objResults.get("目标" + h.objectiveId()) : null, null,
                        h.name(), h.maxScore(), avgScore));
            }
        }

        if (indStrMap.isEmpty()) {
            indStrMap.put("（暂无计算结果）", BigDecimal.ZERO);
        }

        return TraceExcelExporter.generateTraceLedger(indStrMap,
                List.of(new TraceExcelExporter.IndicatorTraceSheet("课程达成度明细", rows)));
    }
}
