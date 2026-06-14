package com.obe.platform.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Level 3: Major-level achievement calculation.
 *
 * Aggregates course-level indicator achievements into major-level achievements
 * using macro support weights W_c.
 *
 * G_k = Σ (E_k × W_c)   where Σ W_c = 1.0 for each indicator k
 */
public class Level3Calculator {

    /**
     * @param courseAchievements  Map<courseId, Map<indicatorId, E_k>> — Level-2 results per course
     * @param macroWeights        list of (courseId, indicatorId, weight W_c) records
     * @return Map<indicatorId, G_k>
     */
    public static Map<Long, BigDecimal> calcMajorAchievement(
            Map<Long, Map<Long, BigDecimal>> courseAchievements,
            List<MacroWeightRecord> macroWeights) {

        // Weights are already normalized by GlobalCalcService

        // Compute weighted sum
        Map<Long, BigDecimal> result = new HashMap<>();

        for (MacroWeightRecord w : macroWeights) {
            Map<Long, BigDecimal> courseResult = courseAchievements.get(w.courseId());
            if (courseResult == null) {
                continue;
            }
            BigDecimal ek = courseResult.get(w.indicatorId());
            if (ek == null) {
                continue;
            }
            BigDecimal contribution = ek.multiply(w.weight());
            result.merge(w.indicatorId(), contribution, BigDecimal::add);
        }

        // Round to 4 decimal places
        Map<Long, BigDecimal> rounded = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> e : result.entrySet()) {
            rounded.put(e.getKey(), e.getValue().setScale(4, RoundingMode.HALF_UP));
        }

        return rounded;
    }

    public record MacroWeightRecord(Long courseId, Long indicatorId, BigDecimal weight) {}
}
