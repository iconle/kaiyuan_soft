package com.obe.platform.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Level 1: Objective-level achievement calculation.
 *
 * C_ij = Σ(actual_score) / Σ(max_score) for student i on objective j
 * C̄_j  = (1/N) × Σ C_ij
 *
 * Supports multi-objective assessment binding: one assessment's score
 * contributes to ALL objectives it binds to.
 */
public class Level1Calculator {

    /**
     * @param scores              all student-assessment scores
     * @param objectiveId         the course objective to compute for
     * @param assessmentMaxMap    assessmentId → maxScore
     * @param assessmentObjMap    assessmentId → List<objectiveId> (N:M bindings)
     * @return class-average achievement
     */
    public static BigDecimal calcObjectiveAchievement(
            List<StudentScoreRecord> scores,
            Long objectiveId,
            Map<Long, BigDecimal> assessmentMaxMap,
            Map<Long, List<Long>> assessmentObjMap) {

        // Find assessments that bind to this objective
        List<Long> relevantIds = new ArrayList<>();
        for (Map.Entry<Long, List<Long>> e : assessmentObjMap.entrySet()) {
            if (e.getValue() != null && e.getValue().contains(objectiveId)) {
                relevantIds.add(e.getKey());
            }
        }
        if (relevantIds.isEmpty()) return BigDecimal.ZERO;

        Map<Long, List<StudentScoreRecord>> byStudent = scores.stream()
                .collect(Collectors.groupingBy(StudentScoreRecord::studentId));

        BigDecimal totalAchievement = BigDecimal.ZERO;
        int studentCount = 0;

        for (Map.Entry<Long, List<StudentScoreRecord>> entry : byStudent.entrySet()) {
            BigDecimal actualSum = BigDecimal.ZERO;
            BigDecimal maxSum = BigDecimal.ZERO;

            for (StudentScoreRecord score : entry.getValue()) {
                if (relevantIds.contains(score.assessmentId())) {
                    actualSum = actualSum.add(score.score());
                    BigDecimal max = assessmentMaxMap.get(score.assessmentId());
                    if (max != null) maxSum = maxSum.add(max);
                }
            }

            if (maxSum.compareTo(BigDecimal.ZERO) > 0) {
                totalAchievement = totalAchievement.add(
                        actualSum.divide(maxSum, 6, RoundingMode.HALF_UP));
                studentCount++;
            }
        }

        return studentCount == 0 ? BigDecimal.ZERO
                : totalAchievement.divide(BigDecimal.valueOf(studentCount), 4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate with question-level granularity. When questions exist, their scores
     * are used instead of the parent assessment's raw score.
     *
     * @param scores              all student scores (may have questionId set)
     * @param objectiveId         the course objective to compute for
     * @param assessmentMaxMap    assessmentId → maxScore (fallback when no questions)
     * @param assessmentObjMap    assessmentId → List<objectiveId>
     * @param questionMaxMap      questionId → maxScore
     * @param questionObjMap      questionId → List<objectiveId>
     */
    public static BigDecimal calcObjectiveAchievement(
            List<StudentScoreRecord> scores,
            Long objectiveId,
            Map<Long, BigDecimal> assessmentMaxMap,
            Map<Long, List<Long>> assessmentObjMap,
            Map<Long, BigDecimal> questionMaxMap,
            Map<Long, List<Long>> questionObjMap) {

        // Build sets of relevant questions and assessments for this objective
        Set<Long> relevantQuestionIds = new HashSet<>();
        Set<Long> assessmentsWithQuestions = new HashSet<>(); // assessments that have questions for this objective
        for (Map.Entry<Long, List<Long>> e : questionObjMap.entrySet()) {
            if (e.getValue() != null && e.getValue().contains(objectiveId)) {
                relevantQuestionIds.add(e.getKey());
            }
        }
        // Map questionId → assessmentId to find which assessments have questions
        Map<Long, Long> questionToAssessment = new HashMap<>();
        // Build from the question data: we already have questionMaxMap which is questionId→maxScore
        // We don't have a direct questionId→assessmentId map from the params, so we infer:
        // An assessment has questions for this objective if any of its questions are in relevantQuestionIds.
        // We'll determine this during the score iteration.

        Set<Long> relevantAssessIds = new HashSet<>();
        for (Map.Entry<Long, List<Long>> e : assessmentObjMap.entrySet()) {
            if (e.getValue() != null && e.getValue().contains(objectiveId)) {
                relevantAssessIds.add(e.getKey());
            }
        }
        if (relevantQuestionIds.isEmpty() && relevantAssessIds.isEmpty()) return BigDecimal.ZERO;

        // Determine which assessments have question coverage for this objective
        // by checking questions in relevantQuestionIds — we'll build this during score processing
        // by tracking which assessment_ids have question-level records

        Map<Long, List<StudentScoreRecord>> byStudent = scores.stream()
                .collect(Collectors.groupingBy(StudentScoreRecord::studentId));

        BigDecimal totalAchievement = BigDecimal.ZERO;
        int studentCount = 0;

        for (var entry : byStudent.entrySet()) {
            BigDecimal actualSum = BigDecimal.ZERO;
            BigDecimal maxSum = BigDecimal.ZERO;
            // Track which assessments had question scores in this student's records
            Set<Long> questionedAssessments = new HashSet<>();

            // First pass: collect which assessments have question-level scores
            for (StudentScoreRecord r : entry.getValue()) {
                if (r.questionId() != null && relevantQuestionIds.contains(r.questionId())) {
                    questionedAssessments.add(r.assessmentId());
                }
            }

            // Second pass: accumulate scores
            for (StudentScoreRecord r : entry.getValue()) {
                if (r.questionId() != null && relevantQuestionIds.contains(r.questionId())) {
                    actualSum = actualSum.add(r.score());
                    BigDecimal qMax = questionMaxMap.get(r.questionId());
                    if (qMax != null) maxSum = maxSum.add(qMax);
                } else if (r.questionId() == null && r.assessmentId() != null
                        && relevantAssessIds.contains(r.assessmentId())
                        && !questionedAssessments.contains(r.assessmentId())) {
                    // Only use assessment-level score if no questions cover this assessment
                    actualSum = actualSum.add(r.score());
                    BigDecimal aMax = assessmentMaxMap.get(r.assessmentId());
                    if (aMax != null) maxSum = maxSum.add(aMax);
                }
            }

            if (maxSum.compareTo(BigDecimal.ZERO) > 0) {
                totalAchievement = totalAchievement.add(
                        actualSum.divide(maxSum, 6, RoundingMode.HALF_UP));
                studentCount++;
            }
        }

        return studentCount == 0 ? BigDecimal.ZERO
                : totalAchievement.divide(BigDecimal.valueOf(studentCount), 4, RoundingMode.HALF_UP);
    }

    public record StudentScoreRecord(Long studentId, Long assessmentId, Long questionId, BigDecimal score) {}
}
