package com.obe.platform.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Level 2: Course-level achievement calculation.
 *
 * Aggregates objective-level achievements into indicator-level achievements
 * using internal contribution weights w_jk.
 *
 * E_k = Σ (C̄_j × w_jk)   where Σ w_jk = 1.0 for each indicator k
 */
public class Level2Calculator {

    /**
     * @param objAchievements  Map<objectiveId, C̄_j> — Level-1 results
     * @param weights          list of (objectiveId, indicatorId, weight) records
     * @return Map<indicatorId, E_k>
     */
    public static Map<Long, BigDecimal> calcCourseAchievement(
            Map<Long, BigDecimal> objAchievements,
            List<WeightRecord> weights) {

        // Validate normalization first
        Map<Long, BigDecimal> indicatorSums = new HashMap<>();
        for (WeightRecord w : weights) {
            indicatorSums.merge(w.indicatorId(), w.weight(), BigDecimal::add);
        }
        WeightValidator.validateNormalization(indicatorSums, "指标点");

        // Compute weighted sum
        Map<Long, BigDecimal> result = new HashMap<>();

        for (WeightRecord w : weights) {
            BigDecimal achievement = objAchievements.get(w.objectiveId());
            if (achievement == null) {
                achievement = BigDecimal.ZERO;
            }
            BigDecimal contribution = achievement.multiply(w.weight());
            result.merge(w.indicatorId(), contribution, BigDecimal::add);
        }

        // Round to 4 decimal places
        Map<Long, BigDecimal> rounded = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> e : result.entrySet()) {
            rounded.put(e.getKey(), e.getValue().setScale(4, RoundingMode.HALF_UP));
        }

        return rounded;
    }

    public record WeightRecord(Long objectiveId, Long indicatorId, BigDecimal weight) {}
}
