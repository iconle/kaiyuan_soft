package com.obe.platform.moduleb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.moduleb.entity.AssessmentPoint;
import com.obe.platform.moduleb.entity.CourseObjective;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.entity.ObjectiveIndicatorWeight;
import com.obe.platform.moduleb.mapper.AssessmentPointMapper;
import com.obe.platform.moduleb.mapper.CourseObjectiveMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import com.obe.platform.moduleb.mapper.ObjectiveIndicatorWeightMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ObjectiveService {

    /** 维度 -> 编号前缀。知识=1, 能力=2, 价值=3。导入与新增共用，保证编号规则一致。 */
    public static final Map<String, String> DIMENSION_PREFIX = Map.of(
            "知识", "1", "能力", "2", "价值", "3");

    /** 返回维度对应的编号前缀；维度非法或为空时返回 null。 */
    public static String dimensionPrefix(String dimension) {
        if (dimension == null) return null;
        return DIMENSION_PREFIX.get(dimension.trim());
    }

    private final CourseOutlineMapper outlineMapper;
    private final CourseObjectiveMapper objectiveMapper;
    private final ObjectiveIndicatorWeightMapper weightMapper;
    private final AssessmentPointMapper assessmentPointMapper;

    /**
     * List all objectives for a teaching class.
     * Finds the outline by classId, then returns all objectives under that outline.
     */
    public List<CourseObjective> listObjectives(Long classId) {
        CourseOutline outline = getOutlineByClassId(classId);
        if (outline == null) {
            return List.of();
        }
        return objectiveMapper.selectList(
                new LambdaQueryWrapper<CourseObjective>()
                        .eq(CourseObjective::getOutlineId, outline.getId())
                        .orderByAsc(CourseObjective::getObjNo));
    }

    /**
     * Create a new course objective. Auto-creates the outline if it does not exist.
     */
    @Transactional
    public CourseObjective createObjective(Long classId, CourseObjective objective) {
        CourseOutline outline = getOutlineByClassId(classId);
        if (outline == null) {
            outline = new CourseOutline();
            outline.setClassId(classId);
            outline.setStatus("DRAFT");
            outlineMapper.insert(outline);
        }
        objective.setOutlineId(outline.getId());
        if (objective.getDimension() != null) {
            objective.setDimension(objective.getDimension().trim());
        }
        if (objective.getObjNo() == null || objective.getObjNo().isBlank()) {
            String prefix = dimensionPrefix(objective.getDimension());
            if (prefix == null) {
                throw new BizException("维度必须为：知识、能力、价值");
            }
            long count = objectiveMapper.selectCount(
                    new LambdaQueryWrapper<CourseObjective>()
                            .eq(CourseObjective::getOutlineId, outline.getId())
                            .eq(CourseObjective::getDimension, objective.getDimension()));
            objective.setObjNo(prefix + "-" + (count + 1));
        }
        objectiveMapper.insert(objective);
        return objective;
    }

    /**
     * Update an existing course objective.
     */
    public void updateObjective(CourseObjective objective) {
        CourseObjective existing = objectiveMapper.selectById(objective.getId());
        if (existing == null) {
            throw new BizException("课程目标不存在");
        }
        objectiveMapper.updateById(objective);
    }

    /**
     * Delete a course objective along with its related weights and assessment points.
     */
    @Transactional
    public void deleteObjective(Long id) {
        CourseObjective existing = objectiveMapper.selectById(id);
        if (existing == null) {
            throw new BizException("课程目标不存在");
        }

        // Delete related indicator weights
        weightMapper.delete(
                new LambdaQueryWrapper<ObjectiveIndicatorWeight>()
                        .eq(ObjectiveIndicatorWeight::getObjectiveId, id));

        // Delete related assessment points
        assessmentPointMapper.delete(
                new LambdaQueryWrapper<AssessmentPoint>()
                        .eq(AssessmentPoint::getObjectiveId, id));

        // Delete the objective itself
        objectiveMapper.deleteById(id);
    }

    /**
     * Get the CourseOutline for a given classId. Returns null if not found.
     */
    CourseOutline getOutlineByClassId(Long classId) {
        return outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>()
                        .eq(CourseOutline::getClassId, classId));
    }
}
