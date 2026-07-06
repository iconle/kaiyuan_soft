package com.obe.platform.engine;

import com.obe.platform.common.BizException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Weight normalization validator — shared by Level 2 and Level 3 calculators.
 *
 * <p>The tolerance below (±0.01) is intentionally loose to absorb minor floating-point
 * rounding introduced when professional staff manually enter weights as percentages
 * (e.g. 0.33 + 0.33 + 0.34). The OBE accreditation standard expects each weight set
 * to sum to exactly 1.0 — operators should treat any validation pass within tolerance
 * as a warning sign that rounding has occurred, not as a clean bill of health.</p>
 *
 * <p>For a three-level cascading calculation (Level1 → Level2 → Level3), the worst-case
 * accumulated deviation approaches 3% and may affect the comparability of achievement
 * metrics. If a stricter tolerance is required for a particular report, callers should
 * re-validate the persisted weights with a tightened threshold before triggering
 * calculation.</p>
 */
public class WeightValidator {

    private static final BigDecimal ONE = BigDecimal.ONE;
    /**
     * Maximum absolute deviation from 1.0 permitted when verifying weight sums.
     * Chosen to absorb manual percentage-entry rounding (e.g. 0.33 + 0.33 + 0.34).
     */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    /**
     * Validate that the sum of weights for each key (indicator/objective) equals 1.0
     * within the documented tolerance.
     * @param weightSums  map of key → sum-of-weights
     * @param dimension   label used in error messages (e.g. "指标点", "课程目标")
     */
    public static void validateNormalization(Map<Long, BigDecimal> weightSums, String dimension) {
        for (Map.Entry<Long, BigDecimal> entry : weightSums.entrySet()) {
            BigDecimal diff = entry.getValue().subtract(ONE).abs();
            if (diff.compareTo(TOLERANCE) > 0) {
                throw new BizException(dimension + " " + entry.getKey() + " 的权重总和为 "
                        + entry.getValue() + "，应等于 1.0（允许容差 ±" + TOLERANCE + "）");
            }
        }
    }

    private WeightValidator() {}
}
