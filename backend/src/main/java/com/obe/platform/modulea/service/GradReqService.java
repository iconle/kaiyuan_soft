package com.obe.platform.modulea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.GradRequirement;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.modulea.mapper.GradRequirementMapper;
import com.obe.platform.modulea.mapper.IndicatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GradReqService {

    private final GradRequirementMapper gradRequirementMapper;
    private final IndicatorMapper indicatorMapper;

    public List<GradRequirement> listByMajor(Long majorId) {
        List<GradRequirement> requirements = gradRequirementMapper.selectList(
                new LambdaQueryWrapper<GradRequirement>()
                        .eq(GradRequirement::getMajorId, majorId)
                        .orderByAsc(GradRequirement::getReqNo));

        for (GradRequirement req : requirements) {
            List<Indicator> indicators = indicatorMapper.selectList(
                    new LambdaQueryWrapper<Indicator>()
                            .eq(Indicator::getGradReqId, req.getId())
                            .orderByAsc(Indicator::getIndicatorNo));
            req.setIndicators(indicators);
        }

        return requirements;
    }

    @Transactional
    public void create(GradRequirement requirement) {
        gradRequirementMapper.insert(requirement);
    }

    @Transactional
    public void update(GradRequirement requirement) {
        GradRequirement existing = gradRequirementMapper.selectById(requirement.getId());
        if (existing == null) {
            throw new BizException("毕业要求不存在");
        }
        gradRequirementMapper.updateById(requirement);
    }

    @Transactional
    public void delete(Long id) {
        GradRequirement existing = gradRequirementMapper.selectById(id);
        if (existing == null) {
            throw new BizException("毕业要求不存在");
        }
        indicatorMapper.delete(new LambdaQueryWrapper<Indicator>()
                .eq(Indicator::getGradReqId, id));
        gradRequirementMapper.deleteById(id);
    }

    @Transactional
    public void addIndicator(Long gradReqId, Indicator indicator) {
        GradRequirement existing = gradRequirementMapper.selectById(gradReqId);
        if (existing == null) {
            throw new BizException("毕业要求不存在");
        }
        indicator.setGradReqId(gradReqId);

        // 自动生成指标点编号
        if (indicator.getIndicatorNo() == null || indicator.getIndicatorNo().isBlank()) {
            indicator.setIndicatorNo(generateNextIndicatorNo(gradReqId, existing.getReqNo()));
        }

        indicatorMapper.insert(indicator);
    }

    /**
     * 生成下一个指标点编号
     * 格式：毕业要求编号-序号，如 3-1, 3-2, 3-3...
     */
    private String generateNextIndicatorNo(Long gradReqId, Integer reqNo) {
        List<Indicator> existing = indicatorMapper.selectList(
                new LambdaQueryWrapper<Indicator>()
                        .eq(Indicator::getGradReqId, gradReqId));

        int maxSeq = 0;
        for (Indicator ind : existing) {
            String no = ind.getIndicatorNo();
            if (no != null && no.startsWith(reqNo + "-")) {
                try {
                    String seqStr = no.substring((reqNo + "-").length());
                    int seq = Integer.parseInt(seqStr);
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException ignored) {
                    // 忽略格式不正确的编号
                }
            }
        }
        return reqNo + "-" + (maxSeq + 1);
    }

    @Transactional
    public void updateIndicator(Indicator indicator) {
        Indicator existing = indicatorMapper.selectById(indicator.getId());
        if (existing == null) {
            throw new BizException("指标点不存在");
        }
        indicatorMapper.updateById(indicator);
    }

    @Transactional
    public void deleteIndicator(Long id) {
        Indicator existing = indicatorMapper.selectById(id);
        if (existing == null) {
            throw new BizException("指标点不存在");
        }
        indicatorMapper.deleteById(id);
    }
}
