package com.obe.platform.moduleb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.moduleb.entity.*;
import com.obe.platform.moduleb.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final AssessmentQuestionMapper questionMapper;
    private final QuestionObjectiveMapper questionObjectiveMapper;
    private final AssessmentPointMapper assessmentPointMapper;

    public List<AssessmentQuestion> listByAssessment(Long assessmentId) {
        List<AssessmentQuestion> list = questionMapper.selectList(
                new LambdaQueryWrapper<AssessmentQuestion>()
                        .eq(AssessmentQuestion::getAssessmentId, assessmentId)
                        .orderByAsc(AssessmentQuestion::getSortOrder));
        for (var q : list) {
            var qos = questionObjectiveMapper.selectList(
                    new LambdaQueryWrapper<QuestionObjective>()
                            .eq(QuestionObjective::getQuestionId, q.getId()));
            q.setObjectiveIds(qos.stream().map(QuestionObjective::getObjectiveId).toList());
        }
        return list;
    }

    @Transactional
    public AssessmentQuestion create(AssessmentQuestion q) {
        if (assessmentPointMapper.selectById(q.getAssessmentId()) == null)
            throw new BizException("考核点不存在");
        validateScoreSum(q.getAssessmentId(), q.getId(), q.getMaxScore());
        questionMapper.insert(q);
        saveObjectives(q.getId(), q.getObjectiveIds());
        return q;
    }

    @Transactional
    public void update(AssessmentQuestion q) {
        AssessmentQuestion existing = questionMapper.selectById(q.getId());
        if (existing == null) throw new BizException("题目不存在");
        validateScoreSum(existing.getAssessmentId(), q.getId(), q.getMaxScore());
        questionMapper.updateById(q);
        if (q.getObjectiveIds() != null) {
            questionObjectiveMapper.delete(
                    new LambdaQueryWrapper<QuestionObjective>()
                            .eq(QuestionObjective::getQuestionId, q.getId()));
            saveObjectives(q.getId(), q.getObjectiveIds());
        }
    }

    @Transactional
    public void delete(Long id) {
        questionObjectiveMapper.delete(
                new LambdaQueryWrapper<QuestionObjective>()
                        .eq(QuestionObjective::getQuestionId, id));
        questionMapper.deleteById(id);
    }

    private void validateScoreSum(Long assessmentId, Long excludeQuestionId, java.math.BigDecimal newScore) {
        var all = questionMapper.selectList(
                new LambdaQueryWrapper<AssessmentQuestion>()
                        .eq(AssessmentQuestion::getAssessmentId, assessmentId));
        java.math.BigDecimal sum = newScore != null ? newScore : java.math.BigDecimal.ZERO;
        for (var q : all) {
            if (!q.getId().equals(excludeQuestionId) && q.getMaxScore() != null) {
                sum = sum.add(q.getMaxScore());
            }
        }
        if (sum.compareTo(new java.math.BigDecimal("100.00")) > 0) {
            throw new BizException("题目总分(" + sum + ")超过100，请调整各题满分值");
        }
    }

    private void saveObjectives(Long qId, List<Long> objIds) {
        if (objIds == null || objIds.isEmpty()) return;
        for (Long objId : objIds) {
            var qo = new QuestionObjective();
            qo.setQuestionId(qId);
            qo.setObjectiveId(objId);
            questionObjectiveMapper.insert(qo);
        }
    }
}
