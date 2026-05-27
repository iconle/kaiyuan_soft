package com.obe.platform.moduleb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.Course;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.modulea.entity.MacroSupportMatrix;
import com.obe.platform.modulea.entity.TeachingClass;
import com.obe.platform.modulea.mapper.CourseMapper;
import com.obe.platform.modulea.mapper.IndicatorMapper;
import com.obe.platform.modulea.mapper.MacroSupportMatrixMapper;
import com.obe.platform.modulea.mapper.TeachingClassMapper;
import com.obe.platform.moduleb.entity.CourseObjective;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.entity.ObjectiveIndicatorWeight;
import com.obe.platform.moduleb.mapper.CourseObjectiveMapper;
import com.obe.platform.moduleb.mapper.ObjectiveIndicatorWeightMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeightService {

    private static final BigDecimal WEIGHT_SUM_TARGET = BigDecimal.ONE;
    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private final ObjectiveService objectiveService;
    private final ObjectiveIndicatorWeightMapper weightMapper;
    private final CourseObjectiveMapper objectiveMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseMapper courseMapper;
    private final MacroSupportMatrixMapper macroSupportMatrixMapper;
    private final IndicatorMapper indicatorMapper;

    /**
     * Get all indicator weights for every objective in the outline of the given class.
     */
    public List<ObjectiveIndicatorWeight> getWeights(Long classId) {
        CourseOutline outline = objectiveService.getOutlineByClassId(classId);
        if (outline == null) {
            return List.of();
        }

        List<CourseObjective> objectives = objectiveMapper.selectList(
                new LambdaQueryWrapper<CourseObjective>()
                        .eq(CourseObjective::getOutlineId, outline.getId()));

        if (objectives.isEmpty()) {
            return List.of();
        }

        List<Long> objectiveIds = objectives.stream()
                .map(CourseObjective::getId)
                .toList();

        return weightMapper.selectList(
                new LambdaQueryWrapper<ObjectiveIndicatorWeight>()
                        .in(ObjectiveIndicatorWeight::getObjectiveId, objectiveIds));
    }

    /**
     * Batch update indicator weights. Validates that the sum of weights for each
     * indicator across all objectives equals 1.0 (tolerance 0.01).
     */
    @Transactional
    public void updateWeights(Long classId, List<ObjectiveIndicatorWeight> weights) {
        CourseOutline outline = objectiveService.getOutlineByClassId(classId);
        if (outline == null) {
            throw new BizException("课程大纲不存在，请先创建课程目标");
        }

        // Validate that all objectiveIds belong to this outline
        List<CourseObjective> objectives = objectiveMapper.selectList(
                new LambdaQueryWrapper<CourseObjective>()
                        .eq(CourseObjective::getOutlineId, outline.getId()));
        Map<Long, CourseObjective> objectiveMap = objectives.stream()
                .collect(Collectors.toMap(CourseObjective::getId, o -> o));

        for (ObjectiveIndicatorWeight w : weights) {
            if (!objectiveMap.containsKey(w.getObjectiveId())) {
                throw new BizException("目标ID " + w.getObjectiveId() + " 不属于当前教学班的大纲");
            }
        }

        // Validate: for each indicator, the sum of weights across objectives must equal 1.0
        Map<Long, BigDecimal> indicatorWeightSums = weights.stream()
                .collect(Collectors.groupingBy(
                        ObjectiveIndicatorWeight::getIndicatorId,
                        Collectors.reducing(BigDecimal.ZERO, ObjectiveIndicatorWeight::getWeight, BigDecimal::add)));

        for (Map.Entry<Long, BigDecimal> entry : indicatorWeightSums.entrySet()) {
            BigDecimal diff = entry.getValue().subtract(WEIGHT_SUM_TARGET).abs();
            if (diff.compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new BizException("指标点 " + entry.getKey() + " 的权重总和为 "
                        + entry.getValue() + "，应等于 1.0");
            }
        }

        // Delete existing weights for all objectives in this outline, then insert new ones
        List<Long> objectiveIds = new ArrayList<>(objectiveMap.keySet());
        weightMapper.delete(
                new LambdaQueryWrapper<ObjectiveIndicatorWeight>()
                        .in(ObjectiveIndicatorWeight::getObjectiveId, objectiveIds));

        for (ObjectiveIndicatorWeight w : weights) {
            w.setId(null); // ensure auto-increment
            weightMapper.insert(w);
        }
    }

    /**
     * Get the list of indicators that this course supports from the macro support matrix.
     * Resolves courseId through the teaching class.
     */
    public List<Indicator> getSupportedIndicators(Long classId) {
        TeachingClass teachingClass = teachingClassMapper.selectById(classId);
        if (teachingClass == null) {
            throw new BizException("教学班不存在");
        }

        Course course = courseMapper.selectById(teachingClass.getCourseId());
        if (course == null) {
            throw new BizException("课程不存在");
        }

        // Find all macro support matrix entries for this course
        List<MacroSupportMatrix> matrixEntries = macroSupportMatrixMapper.selectList(
                new LambdaQueryWrapper<MacroSupportMatrix>()
                        .eq(MacroSupportMatrix::getCourseId, course.getId()));

        if (matrixEntries.isEmpty()) {
            return List.of();
        }

        List<Long> indicatorIds = matrixEntries.stream()
                .map(MacroSupportMatrix::getIndicatorId)
                .distinct()
                .toList();

        return indicatorMapper.selectBatchIds(indicatorIds);
    }
}
