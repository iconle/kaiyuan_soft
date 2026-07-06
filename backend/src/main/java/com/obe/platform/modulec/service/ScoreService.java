package com.obe.platform.modulec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.ClassStudent;
import com.obe.platform.modulea.entity.Student;
import com.obe.platform.modulea.mapper.ClassStudentMapper;
import com.obe.platform.modulea.mapper.StudentMapper;
import com.obe.platform.moduleb.entity.AssessmentPoint;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.mapper.AssessmentPointMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import com.obe.platform.modulec.entity.ScoreSheet;
import com.obe.platform.modulec.entity.StudentScore;
import com.obe.platform.modulec.mapper.ScoreSheetMapper;
import com.obe.platform.modulec.mapper.StudentScoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ScoreSheetMapper scoreSheetMapper;
    private final StudentScoreMapper studentScoreMapper;
    private final CourseOutlineMapper outlineMapper;
    private final AssessmentPointMapper assessmentPointMapper;
    private final com.obe.platform.moduleb.mapper.AssessmentQuestionMapper assessmentQuestionMapper;
    private final ClassStudentMapper classStudentMapper;
    private final StudentMapper studentMapper;
    private final com.obe.platform.moduleb.mapper.AssessmentObjectiveMapper assessmentObjectiveMapper;

    /**
     * Get or create the ScoreSheet for a teaching class.
     */
    public ScoreSheet getOrCreateSheet(Long classId) {
        ScoreSheet sheet = scoreSheetMapper.selectOne(
                new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, classId));
        if (sheet == null) {
            sheet = new ScoreSheet();
            sheet.setClassId(classId);
            sheet.setStatus("EMPTY");
            scoreSheetMapper.insert(sheet);
        }
        return sheet;
    }

    /**
     * Upload student scores from a parsed Excel file.
     * Replaces all existing scores for this sheet with the new data.
     */
    @Transactional
    public void importScores(Long classId, List<StudentScore> scores) {
        ScoreSheet sheet = getOrCreateSheet(classId);
        if ("LOCKED".equals(sheet.getStatus())) {
            throw new BizException("成绩单已锁定，无法导入。请联系教务管理员解锁");
        }

        // Delete existing scores for this sheet
        studentScoreMapper.delete(
                new LambdaQueryWrapper<StudentScore>().eq(StudentScore::getSheetId, sheet.getId()));

        // Insert new scores
        for (StudentScore s : scores) {
            s.setSheetId(sheet.getId());
            studentScoreMapper.insert(s);
        }

        // Update sheet status to IMPORTED
        sheet.setStatus("IMPORTED");
        scoreSheetMapper.updateById(sheet);
    }

    /**
     * Get score preview data for a class, structured for the frontend grid.
     */
    public ScorePreview getScorePreview(Long classId) {
        ScoreSheet sheet = getOrCreateSheet(classId);

        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));

        List<AssessmentPoint> assessments = List.of();
        if (outline != null) {
            assessments = assessmentPointMapper.selectList(
                    new LambdaQueryWrapper<AssessmentPoint>()
                            .eq(AssessmentPoint::getOutlineId, outline.getId())
                            .orderByAsc(AssessmentPoint::getSortOrder));
            // Populate N:M objective bindings
            if (!assessments.isEmpty()) {
                var aoList = assessmentObjectiveMapper.selectList(
                        new LambdaQueryWrapper<com.obe.platform.moduleb.entity.AssessmentObjective>()
                                .in(com.obe.platform.moduleb.entity.AssessmentObjective::getAssessmentId,
                                        assessments.stream().map(AssessmentPoint::getId).toList()));
                Map<Long, java.util.List<Long>> objMap = new HashMap<>();
                for (var ao : aoList) {
                    objMap.computeIfAbsent(ao.getAssessmentId(), k -> new ArrayList<>()).add(ao.getObjectiveId());
                }
                for (AssessmentPoint ap : assessments) {
                    ap.setObjectiveIds(objMap.getOrDefault(ap.getId(),
                            ap.getObjectiveId() != null ? java.util.List.of(ap.getObjectiveId()) : java.util.List.of()));
                }
            }
        }

        // Get class students
        List<ClassStudent> classStudents = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getClassId, classId));

        List<Long> studentIds = classStudents.stream().map(ClassStudent::getStudentId).toList();
        List<Student> students = studentIds.isEmpty() ? List.of() : studentMapper.selectBatchIds(studentIds);

        // Build student ID → Student map
        Map<Long, Student> studentMap = students.stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        // Get existing scores
        List<StudentScore> existingScores = List.of();
        if (sheet.getId() != null) {
            existingScores = studentScoreMapper.selectList(
                    new LambdaQueryWrapper<StudentScore>().eq(StudentScore::getSheetId, sheet.getId()));
        }

        // Group scores by studentId → assessmentId
        Map<Long, Map<Long, java.util.List<StudentScore>>> scoreMap = new HashMap<>();
        for (StudentScore s : existingScores) {
            scoreMap.computeIfAbsent(s.getStudentId(), k -> new HashMap<>())
                    .computeIfAbsent(s.getAssessmentId(), k -> new ArrayList<>())
                    .add(s);
        }

        // Build preview rows
        List<ScoreRow> rows = new ArrayList<>();
        for (ClassStudent cs : classStudents) {
            Student student = studentMap.get(cs.getStudentId());
            if (student == null) continue;

            Map<Long, java.util.List<StudentScore>> studentScores = scoreMap.getOrDefault(cs.getStudentId(), Map.of());
            List<ScoreCell> cells = new ArrayList<>();
            for (AssessmentPoint ap : assessments) {
                var apScores = studentScores.get(ap.getId());
                BigDecimal assessScore = null;
                Map<Long, BigDecimal> qScores = new HashMap<>();
                if (apScores != null) {
                    for (StudentScore ss : apScores) {
                        if (ss.getQuestionId() == null) assessScore = ss.getScore();
                        else qScores.put(ss.getQuestionId(), ss.getScore());
                    }
                }
                cells.add(new ScoreCell(ap.getId(), ap.getName(), assessScore, qScores));
            }

            rows.add(new ScoreRow(cs.getStudentId(), student.getStudentNo(), student.getName(), cells));
        }

        return new ScorePreview(sheet.getStatus(), assessments, rows, buildAssessmentHeaders(assessments));
    }

    private List<AssessmentHeader> buildAssessmentHeaders(List<AssessmentPoint> assessments) {
        return assessments.stream()
                .map(a -> new AssessmentHeader(a.getId(), a.getName(), a.getMaxScore(), a.getObjectiveId()))
                .toList();
    }

    /**
     * Update (supplement/correct) a single student score. Only allowed when not LOCKED.
     */
    @Transactional
    public void updateScore(Long classId, Long studentId, Long assessmentId, Long questionId, java.math.BigDecimal score) {
        ScoreSheet sheet = scoreSheetMapper.selectOne(
                new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, classId));
        if (sheet == null) {
            sheet = getOrCreateSheet(classId);
        }
        if ("LOCKED".equals(sheet.getStatus())) {
            throw new BizException("成绩单已锁定，无法修改。请联系教务管理员解锁");
        }

        BigDecimal maxScore = resolveMaxScore(assessmentId, questionId);
        if (maxScore != null && score != null && score.compareTo(maxScore) > 0) {
            throw new BizException("分数 " + score + " 超过该考核点满分 " + maxScore);
        }

        var wrapper = new LambdaQueryWrapper<StudentScore>()
                .eq(StudentScore::getSheetId, sheet.getId())
                .eq(StudentScore::getStudentId, studentId)
                .eq(StudentScore::getAssessmentId, assessmentId);
        if (questionId != null) {
            wrapper.eq(StudentScore::getQuestionId, questionId);
        } else {
            wrapper.isNull(StudentScore::getQuestionId);
        }
        StudentScore existing = studentScoreMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setScore(score);
            studentScoreMapper.updateById(existing);
        } else {
            StudentScore newScore = new StudentScore();
            newScore.setSheetId(sheet.getId());
            newScore.setStudentId(studentId);
            newScore.setAssessmentId(assessmentId);
            newScore.setQuestionId(questionId);
            newScore.setScore(score);
            studentScoreMapper.insert(newScore);
        }

        if (!"IMPORTED".equals(sheet.getStatus())) {
            sheet.setStatus("IMPORTED");
            scoreSheetMapper.updateById(sheet);
        }
    }

    /**
     * Resolve the upper-bound max score for input validation.
     * If questionId is provided, use the question's maxScore; otherwise use the assessment's maxScore.
     * Returns null if the corresponding record cannot be found (legacy behavior preserved).
     */
    private BigDecimal resolveMaxScore(Long assessmentId, Long questionId) {
        if (questionId != null) {
            var question = assessmentQuestionMapper.selectById(questionId);
            return question == null ? null : question.getMaxScore();
        }
        var assessment = assessmentPointMapper.selectById(assessmentId);
        return assessment == null ? null : assessment.getMaxScore();
    }

    // ---- DTOs ----

    public record ScorePreview(String status, List<AssessmentPoint> assessments,
                                List<ScoreRow> rows, List<AssessmentHeader> headers) {}

    public record AssessmentHeader(Long id, String name, java.math.BigDecimal maxScore, Long objectiveId) {}

    public record ScoreRow(Long studentId, String studentNo, String studentName, List<ScoreCell> cells) {}

    public record ScoreCell(Long assessmentId, String assessmentName, java.math.BigDecimal score,
                            java.util.Map<Long, java.math.BigDecimal> questionScores) {}
}
