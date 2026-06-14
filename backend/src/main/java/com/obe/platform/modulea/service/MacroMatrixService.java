package com.obe.platform.modulea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.Course;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.modulea.entity.MacroSupportMatrix;
import com.obe.platform.modulea.mapper.CourseMapper;
import com.obe.platform.modulea.mapper.IndicatorMapper;
import com.obe.platform.modulea.mapper.MacroSupportMatrixMapper;
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
public class MacroMatrixService {

    private final MacroSupportMatrixMapper macroSupportMatrixMapper;
    private final CourseMapper courseMapper;
    private final IndicatorMapper indicatorMapper;

    public List<MacroSupportMatrix> getMatrix(Long majorId) {
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getMajorId, majorId));

        if (courses.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> courseIds = courses.stream()
                .map(Course::getId)
                .collect(Collectors.toList());

        return macroSupportMatrixMapper.selectList(
                new LambdaQueryWrapper<MacroSupportMatrix>()
                        .in(MacroSupportMatrix::getCourseId, courseIds));
    }

    @Transactional
    public void updateMatrix(List<MacroSupportMatrix> entries) {
        Map<Long, BigDecimal> indicatorWeightSums = entries.stream()
                .filter(e -> e.getWeight() != null)
                .collect(Collectors.groupingBy(
                        MacroSupportMatrix::getIndicatorId,
                        Collectors.reducing(BigDecimal.ZERO, MacroSupportMatrix::getWeight, BigDecimal::add)));

        for (Map.Entry<Long, BigDecimal> entry : indicatorWeightSums.entrySet()) {
            BigDecimal sum = entry.getValue();
            BigDecimal expected = BigDecimal.ONE;
            BigDecimal tolerance = new BigDecimal("0.01");
            BigDecimal diff = sum.subtract(expected).abs();
            if (diff.compareTo(tolerance) > 0) {
                Indicator indicator = indicatorMapper.selectById(entry.getKey());
                String indicatorLabel = indicator != null
                        ? indicator.getIndicatorNo()
                        : String.valueOf(entry.getKey());
                throw new BizException("指标点 " + indicatorLabel + " 的权重之和为 " + sum + "，不等于 1.0");
            }
        }

        // Delete existing entries for involved courses, then re-insert
        List<Long> courseIds = entries.stream().map(MacroSupportMatrix::getCourseId).distinct().toList();
        for (Long courseId : courseIds) {
            macroSupportMatrixMapper.delete(
                    new LambdaQueryWrapper<MacroSupportMatrix>()
                            .eq(MacroSupportMatrix::getCourseId, courseId));
        }
        for (MacroSupportMatrix entry : entries) {
            entry.setId(null);
            macroSupportMatrixMapper.insert(entry);
        }
    }

    public List<Indicator> getSupportedIndicators(Long courseId) {
        List<MacroSupportMatrix> matrices = macroSupportMatrixMapper.selectList(
                new LambdaQueryWrapper<MacroSupportMatrix>()
                        .eq(MacroSupportMatrix::getCourseId, courseId));

        if (matrices.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> indicatorIds = matrices.stream()
                .map(MacroSupportMatrix::getIndicatorId)
                .distinct()
                .collect(Collectors.toList());

        return indicatorMapper.selectBatchIds(indicatorIds);
    }
}
