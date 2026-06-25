package com.obe.platform.modulec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.engine.Level1Calculator;
import com.obe.platform.engine.Level2Calculator;
import com.obe.platform.moduleb.entity.AssessmentPoint;
import com.obe.platform.moduleb.entity.CourseObjective;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.entity.ObjectiveIndicatorWeight;
import com.obe.platform.moduleb.mapper.AssessmentPointMapper;
import com.obe.platform.moduleb.mapper.CourseObjectiveMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import com.obe.platform.moduleb.mapper.ObjectiveIndicatorWeightMapper;
import com.obe.platform.modulec.entity.CourseAchievement;
import com.obe.platform.modulec.entity.ObjAchievement;
import com.obe.platform.modulec.entity.ScoreSheet;
import com.obe.platform.modulec.entity.StudentScore;
import com.obe.platform.modulec.mapper.CourseAchievementMapper;
import com.obe.platform.modulec.mapper.ObjAchievementMapper;
import com.obe.platform.modulec.mapper.ScoreSheetMapper;
import com.obe.platform.modulec.mapper.StudentScoreMapper;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseCalcService {

    private final ScoreSheetMapper scoreSheetMapper;
    private final StudentScoreMapper studentScoreMapper;
    private final CourseOutlineMapper outlineMapper;
    private final CourseObjectiveMapper objectiveMapper;
    private final AssessmentPointMapper assessmentPointMapper;
    private final ObjectiveIndicatorWeightMapper weightMapper;
    private final ObjAchievementMapper objAchievementMapper;
    private final CourseAchievementMapper courseAchievementMapper;
    private final com.obe.platform.modulea.mapper.IndicatorMapper indicatorMapper;
    private final com.obe.platform.moduleb.mapper.AssessmentObjectiveMapper assessmentObjectiveMapper;
    private final com.obe.platform.moduleb.mapper.AssessmentQuestionMapper assessmentQuestionMapper;
    private final com.obe.platform.moduleb.mapper.QuestionObjectiveMapper questionObjectiveMapper;
    private final PersonalAchievementService personalAchievementService;

    /**
     * Execute Phase 1 (Level 1 + Level 2) calculation for a given teaching class.
     * Runs atomically: all succeed or all roll back.
     *
     * @param classId  the teaching class ID
     * @param operator the user ID of the teacher triggering the calculation
     * @return calculation summary
     */
    @Transactional(rollbackFor = Exception.class)
    public CalcResult compute(Long classId, Long operator) {
        // 1. Find and validate the score sheet
        ScoreSheet sheet = scoreSheetMapper.selectOne(
                new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, classId));
        if (sheet == null || !"IMPORTED".equals(sheet.getStatus())) {
            throw new BizException("成绩单状态不正确（需要为\"已导入\"），当前状态: "
                    + (sheet == null ? "EMPTY" : sheet.getStatus()));
        }

        // 2. Load the course outline
        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));
        if (outline == null) {
            throw new BizException("课程大纲不存在，请先配置课程目标与考核点");
        }

        // 3. Load objectives and assessment points
        List<CourseObjective> objectives = objectiveMapper.selectList(
                new LambdaQueryWrapper<CourseObjective>().eq(CourseObjective::getOutlineId, outline.getId()));
        if (objectives.isEmpty()) {
            throw new BizException("未配置课程目标");
        }

        List<AssessmentPoint> assessments = assessmentPointMapper.selectList(
                new LambdaQueryWrapper<AssessmentPoint>().eq(AssessmentPoint::getOutlineId, outline.getId()));
        if (assessments.isEmpty()) {
            throw new BizException("未配置考核点");
        }

        // 4. Load assessment → objective N:M bindings
        List<Long> assessmentIds = assessments.stream().map(AssessmentPoint::getId).toList();
        List<com.obe.platform.moduleb.entity.AssessmentObjective> aoList = assessmentObjectiveMapper.selectList(
                new LambdaQueryWrapper<com.obe.platform.moduleb.entity.AssessmentObjective>()
                        .in(com.obe.platform.moduleb.entity.AssessmentObjective::getAssessmentId, assessmentIds));
        Map<Long, List<Long>> assessmentObjMap = new HashMap<>();
        for (var ao : aoList) {
            assessmentObjMap.computeIfAbsent(ao.getAssessmentId(), k -> new ArrayList<>()).add(ao.getObjectiveId());
        }
        // Fallback: use single objectiveId for assessments without N:M bindings
        for (AssessmentPoint ap : assessments) {
            if (!assessmentObjMap.containsKey(ap.getId()) && ap.getObjectiveId() != null) {
                assessmentObjMap.put(ap.getId(), List.of(ap.getObjectiveId()));
            }
        }
        if (assessmentObjMap.isEmpty()) {
            throw new BizException("所有考核点均未绑定课程目标");
        }

        // 5. Load student scores
        List<StudentScore> scores = studentScoreMapper.selectList(
                new LambdaQueryWrapper<StudentScore>().eq(StudentScore::getSheetId, sheet.getId()));
        if (scores.isEmpty()) {
            throw new BizException("未找到成绩数据");
        }

        // 6. Build assessment max-score map
        Map<Long, BigDecimal> assessmentMaxMap = new HashMap<>();
        for (AssessmentPoint ap : assessments) {
            assessmentMaxMap.put(ap.getId(), ap.getMaxScore());
        }

        // 7. Load questions and question-objective bindings
        List<com.obe.platform.moduleb.entity.AssessmentQuestion> questions = assessmentQuestionMapper.selectList(
                new LambdaQueryWrapper<com.obe.platform.moduleb.entity.AssessmentQuestion>()
                        .in(com.obe.platform.moduleb.entity.AssessmentQuestion::getAssessmentId, assessmentIds));
        Map<Long, BigDecimal> questionMaxMap = new HashMap<>();
        for (var q : questions) questionMaxMap.put(q.getId(), q.getMaxScore());

        Map<Long, List<Long>> questionObjMap = new HashMap<>();
        if (!questions.isEmpty()) {
            List<Long> qIds = questions.stream().map(q -> q.getId()).toList();
            List<com.obe.platform.moduleb.entity.QuestionObjective> qoList = questionObjectiveMapper.selectList(
                    new LambdaQueryWrapper<com.obe.platform.moduleb.entity.QuestionObjective>()
                            .in(com.obe.platform.moduleb.entity.QuestionObjective::getQuestionId, qIds));
            for (var qo : qoList) {
                questionObjMap.computeIfAbsent(qo.getQuestionId(), k -> new ArrayList<>()).add(qo.getObjectiveId());
            }
        }

        // 8. Convert scores to records (with questionId)
        List<Level1Calculator.StudentScoreRecord> scoreRecords = scores.stream()
                .map(s -> new Level1Calculator.StudentScoreRecord(
                        s.getStudentId(), s.getAssessmentId(), s.getQuestionId(), s.getScore()))
                .toList();

        // 9. Level 1: compute objective achievements using N:M bindings + questions
        Map<Long, BigDecimal> objAchievements = new HashMap<>();
        for (CourseObjective obj : objectives) {
            BigDecimal achievement = Level1Calculator.calcObjectiveAchievement(
                    scoreRecords, obj.getId(), assessmentMaxMap, assessmentObjMap,
                    questionMaxMap, questionObjMap);
            objAchievements.put(obj.getId(), achievement);
        }

        // 10. Load internal contribution weights
        List<ObjectiveIndicatorWeight> weights = weightMapper.selectList(
                new LambdaQueryWrapper<ObjectiveIndicatorWeight>()
                        .in(ObjectiveIndicatorWeight::getObjectiveId, objectives.stream().map(CourseObjective::getId).toList()));

        // Validate weight normalization
        Map<Long, BigDecimal> indicatorSums = weights.stream()
                .collect(Collectors.groupingBy(
                        ObjectiveIndicatorWeight::getIndicatorId,
                        Collectors.reducing(BigDecimal.ZERO, ObjectiveIndicatorWeight::getWeight, BigDecimal::add)));

        for (Map.Entry<Long, BigDecimal> entry : indicatorSums.entrySet()) {
            BigDecimal diff = entry.getValue().subtract(BigDecimal.ONE).abs();
            if (diff.compareTo(new BigDecimal("0.01")) > 0) {
                throw new BizException("指标点 " + entry.getKey() + " 的内部贡献权重总和为 "
                        + entry.getValue() + "，应等于 1.0。请检查权重分配");
            }
        }

        // 11. Level 2: compute course indicator achievements
        List<Level2Calculator.WeightRecord> weightRecords = weights.stream()
                .map(w -> new Level2Calculator.WeightRecord(w.getObjectiveId(), w.getIndicatorId(), w.getWeight()))
                .toList();

        Map<Long, BigDecimal> courseAchievements = Level2Calculator.calcCourseAchievement(
                objAchievements, weightRecords);

        // 12. Persist results
        LocalDateTime now = LocalDateTime.now();

        // Delete previous results
        objAchievementMapper.delete(
                new LambdaQueryWrapper<ObjAchievement>().eq(ObjAchievement::getClassId, classId));
        courseAchievementMapper.delete(
                new LambdaQueryWrapper<CourseAchievement>().eq(CourseAchievement::getClassId, classId));
        personalAchievementService.clearClassAchievements(classId);

        // Insert objective achievements
        for (Map.Entry<Long, BigDecimal> e : objAchievements.entrySet()) {
            ObjAchievement oa = new ObjAchievement();
            oa.setClassId(classId);
            oa.setObjectiveId(e.getKey());
            oa.setAchievement(e.getValue());
            oa.setCalcTime(now);
            objAchievementMapper.insert(oa);
        }

        // Insert course achievements
        for (Map.Entry<Long, BigDecimal> e : courseAchievements.entrySet()) {
            CourseAchievement ca = new CourseAchievement();
            ca.setClassId(classId);
            ca.setIndicatorId(e.getKey());
            ca.setAchievement(e.getValue());
            ca.setCalcTime(now);
            courseAchievementMapper.insert(ca);
        }

        personalAchievementService.persistClassAchievements(classId, now);

        // 13. Lock the score sheet
        sheet.setStatus("LOCKED");
        sheet.setLockedAt(now);
        sheet.setLockedBy(operator);
        scoreSheetMapper.updateById(sheet);

        // Build label maps
        Map<Long, String> objLabels = objectives.stream()
                .collect(Collectors.toMap(CourseObjective::getId, CourseObjective::getObjNo));
        List<Long> indicatorIds = courseAchievements.keySet().stream().toList();
        Map<Long, String> indLabels = new HashMap<>();
        if (!indicatorIds.isEmpty()) {
            var indicators = indicatorMapper.selectBatchIds(indicatorIds);
            for (var ind : indicators) indLabels.put(ind.getId(), ind.getIndicatorNo());
        }

        return new CalcResult(objAchievements, courseAchievements, now, objLabels, indLabels);
    }

    /**
     * Get cached calculation results for a class, if any.
     */
    public CalcResult getResults(Long classId) {
        List<ObjAchievement> objList = objAchievementMapper.selectList(
                new LambdaQueryWrapper<ObjAchievement>().eq(ObjAchievement::getClassId, classId));
        List<CourseAchievement> courseList = courseAchievementMapper.selectList(
                new LambdaQueryWrapper<CourseAchievement>().eq(CourseAchievement::getClassId, classId));

        Map<Long, BigDecimal> objMap = objList.stream()
                .collect(Collectors.toMap(ObjAchievement::getObjectiveId, ObjAchievement::getAchievement));
        Map<Long, BigDecimal> courseMap = courseList.stream()
                .collect(Collectors.toMap(CourseAchievement::getIndicatorId, CourseAchievement::getAchievement));

        LocalDateTime calcTime = objList.isEmpty() ? null : objList.get(0).getCalcTime();
        // Build labels
        Map<Long, String> objLabels = new HashMap<>();
        Map<Long, String> indLabels = new HashMap<>();
        if (!objMap.isEmpty()) {
            var objs = objectiveMapper.selectBatchIds(objMap.keySet());
            for (var o : objs) objLabels.put(o.getId(), o.getObjNo());
        }
        if (!courseMap.isEmpty()) {
            var inds = indicatorMapper.selectBatchIds(courseMap.keySet());
            for (var i : inds) indLabels.put(i.getId(), i.getIndicatorNo());
        }
        return new CalcResult(objMap, courseMap, calcTime, objLabels, indLabels);
    }

    /**
     * Unlock a score sheet, clearing calculated Level 1 & 2 results.
     */
    @Transactional
    public void unlockSheet(Long sheetId) {
        ScoreSheet sheet = scoreSheetMapper.selectById(sheetId);
        if (sheet == null) {
            throw new BizException("成绩单不存在");
        }
        if (!"LOCKED".equals(sheet.getStatus())) {
            throw new BizException("成绩单未锁定，无需解锁");
        }

        // Clear results
        objAchievementMapper.delete(
                new LambdaQueryWrapper<ObjAchievement>().eq(ObjAchievement::getClassId, sheet.getClassId()));
        courseAchievementMapper.delete(
                new LambdaQueryWrapper<CourseAchievement>().eq(CourseAchievement::getClassId, sheet.getClassId()));
        personalAchievementService.clearClassAchievements(sheet.getClassId());

        // Unlock
        sheet.setStatus("IMPORTED");
        sheet.setLockedAt(null);
        sheet.setLockedBy(null);
        scoreSheetMapper.updateById(sheet);
    }

    public record CalcResult(Map<Long, BigDecimal> objectiveAchievements,
                              Map<Long, BigDecimal> courseAchievements,
                              @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime calcTime,
                              Map<Long, String> objectiveLabels,
                              Map<Long, String> indicatorLabels) {}
}
