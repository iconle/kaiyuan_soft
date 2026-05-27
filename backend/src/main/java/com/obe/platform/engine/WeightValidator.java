package com.obe.platform.engine;

import com.obe.platform.common.BizException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Weight normalization validator — shared by Level 2 and Level 3 calculators.
 */
public class WeightValidator {

    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    /**
     * Validate that the sum of weights for each key (indicator/objective) equals 1.0.
     * @param weightSums  map of key → sum-of-weights
     * @param dimension   label used in error messages (e.g. "指标点", "课程目标")
     */
    public static void validateNormalization(Map<Long, BigDecimal> weightSums, String dimension) {
        for (Map.Entry<Long, BigDecimal> entry : weightSums.entrySet()) {
            BigDecimal diff = entry.getValue().subtract(ONE).abs();
            if (diff.compareTo(TOLERANCE) > 0) {
                throw new BizException(dimension + " " + entry.getKey() + " 的权重总和为 "
                        + entry.getValue() + "，应等于 1.0");
            }
        }
    }

    private WeightValidator() {}
}
