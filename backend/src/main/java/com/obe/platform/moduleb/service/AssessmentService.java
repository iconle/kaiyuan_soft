package com.obe.platform.moduleb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.moduleb.entity.AssessmentObjective;
import com.obe.platform.moduleb.entity.AssessmentPoint;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.mapper.AssessmentObjectiveMapper;
import com.obe.platform.moduleb.mapper.AssessmentPointMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentPointMapper assessmentPointMapper;
    private final AssessmentObjectiveMapper assessmentObjectiveMapper;
    private final CourseOutlineMapper outlineMapper;

    public List<AssessmentPoint> listAssessments(Long classId) {
        CourseOutline outline = getOutlineByClassId(classId);
        if (outline == null) return List.of();
        List<AssessmentPoint> list = assessmentPointMapper.selectList(
                new LambdaQueryWrapper<AssessmentPoint>()
                        .eq(AssessmentPoint::getOutlineId, outline.getId())
                        .orderByAsc(AssessmentPoint::getSortOrder));
        // Populate objectiveIds
        for (AssessmentPoint ap : list) {
            List<AssessmentObjective> aos = assessmentObjectiveMapper.selectList(
                    new LambdaQueryWrapper<AssessmentObjective>()
                            .eq(AssessmentObjective::getAssessmentId, ap.getId()));
            ap.setObjectiveIds(aos.stream().map(AssessmentObjective::getObjectiveId).toList());
        }
        return list;
    }

    @Transactional
    public AssessmentPoint createAssessment(Long classId, AssessmentPoint point) {
        CourseOutline outline = getOutlineByClassId(classId);
        if (outline == null) {
            outline = new CourseOutline();
            outline.setClassId(classId);
            outline.setStatus("DRAFT");
            outlineMapper.insert(outline);
        }
        point.setOutlineId(outline.getId());
        if (point.getObjectiveId() == null && point.getObjectiveIds() != null && !point.getObjectiveIds().isEmpty()) {
            point.setObjectiveId(point.getObjectiveIds().get(0));
        }
        assessmentPointMapper.insert(point);
        // Save objective bindings
        saveObjectives(point.getId(), point.getObjectiveIds());
        return point;
    }

    @Transactional
    public void updateAssessment(AssessmentPoint point) {
        if (assessmentPointMapper.selectById(point.getId()) == null)
            throw new BizException("考核要点不存在");
        assessmentPointMapper.updateById(point);
        // Replace objective bindings only if objectiveIds is provided (not null)
        if (point.getObjectiveIds() != null) {
            assessmentObjectiveMapper.delete(
                    new LambdaQueryWrapper<AssessmentObjective>()
                            .eq(AssessmentObjective::getAssessmentId, point.getId()));
            saveObjectives(point.getId(), point.getObjectiveIds());
        }
    }

    @Transactional
    public void deleteAssessment(Long id) {
        if (assessmentPointMapper.selectById(id) == null)
            throw new BizException("考核要点不存在");
        assessmentObjectiveMapper.delete(
                new LambdaQueryWrapper<AssessmentObjective>()
                        .eq(AssessmentObjective::getAssessmentId, id));
        assessmentPointMapper.deleteById(id);
    }

    /** Validate weight percent sum = 100 for all assessments in an outline */
    public void validateWeightSum(Long outlineId) {
        List<AssessmentPoint> all = assessmentPointMapper.selectList(
                new LambdaQueryWrapper<AssessmentPoint>()
                        .eq(AssessmentPoint::getOutlineId, outlineId));
        BigDecimal sum = BigDecimal.ZERO;
        for (AssessmentPoint ap : all) {
            if (ap.getWeightPercent() != null) sum = sum.add(ap.getWeightPercent());
        }
        if (sum.compareTo(new BigDecimal("100.00")) != 0) {
            throw new BizException("考核点权重百分比总和为 " + sum + "%，必须等于 100%");
        }
    }

    private void saveObjectives(Long assessmentId, List<Long> objectiveIds) {
        if (objectiveIds == null || objectiveIds.isEmpty()) return;
        for (Long objId : objectiveIds) {
            AssessmentObjective ao = new AssessmentObjective();
            ao.setAssessmentId(assessmentId);
            ao.setObjectiveId(objId);
            assessmentObjectiveMapper.insert(ao);
        }
    }

    private CourseOutline getOutlineByClassId(Long classId) {
        return outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>()
                        .eq(CourseOutline::getClassId, classId));
    }
}
