package com.obe.platform.modulec.service;

import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.ClassStudent;
import com.obe.platform.modulea.entity.Student;
import com.obe.platform.modulea.mapper.ClassStudentMapper;
import com.obe.platform.modulea.mapper.StudentMapper;
import com.obe.platform.moduleb.entity.AssessmentPoint;
import com.obe.platform.moduleb.entity.AssessmentQuestion;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.mapper.AssessmentPointMapper;
import com.obe.platform.moduleb.mapper.AssessmentQuestionMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import com.obe.platform.modulec.entity.ScoreSheet;
import com.obe.platform.modulec.entity.StudentScore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.modulec.mapper.ScoreSheetMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelParseService {

    private final ScoreSheetMapper scoreSheetMapper;
    private final CourseOutlineMapper outlineMapper;
    private final AssessmentPointMapper assessmentPointMapper;
    private final AssessmentQuestionMapper questionMapper;
    private final ClassStudentMapper classStudentMapper;
    private final StudentMapper studentMapper;

    /**
     * Parse an uploaded Excel score file and return the list of StudentScore records.
     * Supports two formats:
     * 1. Legacy single-sheet format with assessment-level scores
     * 2. Multi-sheet format with assessment sheets containing question-level or assessment-level scores
     *
     * @param file    the uploaded Excel file
     * @param sheetId the ScoreSheet ID
     * @return list of StudentScore records (not yet persisted)
     */
    public List<StudentScore> parseScoreFile(MultipartFile file, Long sheetId) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件为空，请选择下载的成绩录入模板");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BizException("仅支持.xlsx格式文件，请先下载成绩录入模板");
        }
        ScoreSheet scoreSheet = scoreSheetMapper.selectById(sheetId);
        if (scoreSheet == null) {
            throw new BizException("成绩单不存在");
        }

        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>()
                        .eq(CourseOutline::getClassId, scoreSheet.getClassId()));
        if (outline == null) {
            throw new BizException("课程大纲不存在，请先配置课程目标和考核点");
        }

        List<AssessmentPoint> assessmentPoints = assessmentPointMapper.selectList(
                new LambdaQueryWrapper<AssessmentPoint>()
                        .eq(AssessmentPoint::getOutlineId, outline.getId())
                        .orderByAsc(AssessmentPoint::getSortOrder));

        // Build assessment lookup: name -> assessment point
        Map<String, AssessmentPoint> assessmentNameMap = assessmentPoints.stream()
                .collect(Collectors.toMap(AssessmentPoint::getName, ap -> ap, (a, b) -> a));

        // Build question lookup: assessmentId -> list of questions
        Map<Long, List<AssessmentQuestion>> questionsMap = assessmentPoints.stream()
                .collect(Collectors.toMap(
                        AssessmentPoint::getId,
                        ap -> questionMapper.selectList(
                                new LambdaQueryWrapper<AssessmentQuestion>()
                                        .eq(AssessmentQuestion::getAssessmentId, ap.getId())
                                        .orderByAsc(AssessmentQuestion::getSortOrder))
                ));

        // Build student lookup: studentNo -> studentId
        List<ClassStudent> classStudents = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, scoreSheet.getClassId()));
        List<Long> studentIds = classStudents.stream().map(ClassStudent::getStudentId).toList();
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Map<String, Long> studentNoToId = students.stream()
                .collect(Collectors.toMap(Student::getStudentNo, Student::getId));
        Map<Long, Student> studentMap = students.stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        List<StudentScore> result = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> importedStudents = new HashSet<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            // Check if it's the new multi-sheet format
            if (workbook.getNumberOfSheets() > 1 || workbook.getSheetName(0).equals(assessmentPoints.get(0).getName())) {
                // Parse multi-sheet format - one sheet per assessment point
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    String sheetName = sheet.getSheetName();

                    // Find the assessment point by name
                    AssessmentPoint ap = assessmentPoints.stream()
                            .filter(a -> a.getName().equals(sheetName))
                            .findFirst()
                            .orElse(null);

                    if (ap != null) {
                        List<AssessmentQuestion> questions = questionsMap.getOrDefault(ap.getId(), List.of());
                        parseAssessmentSheet(sheet, ap, questions, studentNoToId,
                                studentMap, sheetId, importedStudents, result, errors);
                    }
                }
            } else {
                // Parse legacy single-sheet format
                Sheet sheet = workbook.getSheetAt(0);
                parseLegacySheet(sheet, assessmentPoints, studentNoToId,
                        studentMap, sheetId, importedStudents, result, errors);
            }

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("解析Excel文件失败: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new BizException("导入失败，共 " + errors.size() + " 处问题，请修改后重新上传：\n"
                    + String.join("\n", errors));
        }
        if (result.isEmpty()) {
            throw new BizException("文件中没有可导入的成绩数据");
        }
        return result;
    }

    /**
     * Parse the legacy single-sheet format with assessment-level scores.
     */
    private void parseLegacySheet(Sheet sheet, List<AssessmentPoint> assessmentPoints,
                                   Map<String, Long> studentNoToId, Map<Long, Student> studentMap,
                                   Long sheetId, Set<String> importedStudents,
                                   List<StudentScore> result, List<String> errors) {
        validateLegacyHeaders(sheet, assessmentPoints);

        for (int r = 2; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String studentNo = getCellString(row.getCell(0));
            if (studentNo == null || studentNo.isBlank()) continue;
            studentNo = studentNo.trim();

            Long studentId = studentNoToId.get(studentNo);
            if (studentId == null) {
                errors.add("第" + (r + 1) + "行：学号「" + studentNo + "」不在本教学班名单中");
                continue;
            }

            for (int i = 0; i < assessmentPoints.size(); i++) {
                AssessmentPoint ap = assessmentPoints.get(i);
                Cell cell = row.getCell(2 + i);

                String scoreText = getCellString(cell);
                if (scoreText != null && !scoreText.isBlank()) {
                    try {
                        BigDecimal score = new BigDecimal(scoreText.trim());
                        validateScore(score, ap.getMaxScore(), studentNo, ap.getName(), r + 1, errors);
                        if (score.compareTo(BigDecimal.ZERO) >= 0 && score.compareTo(ap.getMaxScore()) <= 0) {
                            StudentScore ss = new StudentScore();
                            ss.setSheetId(sheetId);
                            ss.setStudentId(studentId);
                            ss.setAssessmentId(ap.getId());
                            ss.setQuestionId(null);
                            ss.setScore(score);
                            result.add(ss);
                        }
                    } catch (NumberFormatException e) {
                        errors.add("第" + (r + 1) + "行：考核点「" + ap.getName()
                                + "」成绩必须为数字，实际为「" + scoreText + "」");
                    }
                }
            }
        }
    }

    /**
     * Parse an assessment sheet.
     * If the sheet has questions, parse question-level scores.
     * If no questions, parse assessment-level score.
     */
    private void parseAssessmentSheet(Sheet sheet, AssessmentPoint ap, List<AssessmentQuestion> questions,
                                       Map<String, Long> studentNoToId, Map<Long, Student> studentMap,
                                       Long sheetId, Set<String> importedStudents,
                                       List<StudentScore> result, List<String> errors) {
        for (int r = 2; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String studentNo = getCellString(row.getCell(0));
            if (studentNo == null || studentNo.isBlank()) continue;
            studentNo = studentNo.trim();

            Long studentId = studentNoToId.get(studentNo);
            if (studentId == null) {
                errors.add(ap.getName() + "第" + (r + 1) + "行：学号「" + studentNo + "」不在本教学班名单中");
                continue;
            }

            // If no questions, parse assessment-level score from column 2
            if (questions == null || questions.isEmpty()) {
                Cell cell = row.getCell(2);
                String scoreText = getCellString(cell);
                if (scoreText != null && !scoreText.isBlank()) {
                    try {
                        BigDecimal score = new BigDecimal(scoreText.trim());
                        if (score.compareTo(BigDecimal.ZERO) >= 0 && score.compareTo(ap.getMaxScore()) <= 0) {
                            StudentScore ss = new StudentScore();
                            ss.setSheetId(sheetId);
                            ss.setStudentId(studentId);
                            ss.setAssessmentId(ap.getId());
                            ss.setQuestionId(null);
                            ss.setScore(score);
                            result.add(ss);
                        }
                    } catch (NumberFormatException e) {
                        errors.add(ap.getName() + "第" + (r + 1) + "行：成绩必须为数字，实际为「" + scoreText + "」");
                    }
                }
            } else {
                // Parse question-level scores
                for (int i = 0; i < questions.size(); i++) {
                    AssessmentQuestion q = questions.get(i);
                    Cell cell = row.getCell(2 + i);

                    String scoreText = getCellString(cell);
                    if (scoreText == null || scoreText.isBlank()) {
                        // Skip if empty - user can fill in later
                        continue;
                    }

                    try {
                        BigDecimal score = new BigDecimal(scoreText.trim());
                        validateScore(score, q.getMaxScore(), studentNo, q.getName(), r + 1, errors);
                        if (score.compareTo(BigDecimal.ZERO) >= 0 && score.compareTo(q.getMaxScore()) <= 0) {
                            StudentScore ss = new StudentScore();
                            ss.setSheetId(sheetId);
                            ss.setStudentId(studentId);
                            ss.setAssessmentId(ap.getId());
                            ss.setQuestionId(q.getId());
                            ss.setScore(score);
                            result.add(ss);
                        }
                    } catch (NumberFormatException e) {
                        errors.add(ap.getName() + "第" + (r + 1) + "行：题目「" + q.getName()
                                + "」成绩必须为数字，实际为「" + scoreText + "」");
                    }
                }
            }
        }
    }

    private void validateScore(BigDecimal score, BigDecimal maxScore, String studentNo,
                                String itemName, int row, List<String> errors) {
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(maxScore) > 0) {
            errors.add("第" + row + "行：学号「" + studentNo + "」在「"
                    + itemName + "」的得分必须在0至" + maxScore + "之间，实际为" + score);
        }
    }

    private void validateLegacyHeaders(Sheet sheet, List<AssessmentPoint> assessmentPoints) {
        Row header = sheet.getRow(0);
        Row subHeader = sheet.getRow(1);
        List<String> expected = new ArrayList<>();
        expected.add("学号");
        expected.add("姓名");
        expected.addAll(assessmentPoints.stream().map(AssessmentPoint::getName).toList());
        List<String> actual = new ArrayList<>();
        for (int i = 0; i < expected.size(); i++) {
            String value = getCellString(header == null ? null : header.getCell(i));
            actual.add(value == null ? "" : value.trim());
        }
        if (!expected.equals(actual)) {
            throw new BizException("成绩模板表头不正确，请重新下载模板。\n期望表头："
                    + String.join("、", expected) + "\n实际表头：" + String.join("、", actual));
        }
        for (int i = 0; i < assessmentPoints.size(); i++) {
            AssessmentPoint assessment = assessmentPoints.get(i);
            String expectedMax = "满分: " + assessment.getMaxScore().toPlainString();
            String actualMax = getCellString(subHeader == null ? null : subHeader.getCell(i + 2));
            if (!expectedMax.equals(actualMax == null ? "" : actualMax.trim())) {
                throw new BizException("成绩模板第2行满分信息不正确：考核点「" + assessment.getName()
                        + "」应为「" + expectedMax + "」，实际为「"
                        + (actualMax == null ? "" : actualMax.trim()) + "」");
            }
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue())
                    .stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception exception) {
                    yield BigDecimal.valueOf(cell.getNumericCellValue())
                            .stripTrailingZeros().toPlainString();
                }
            }
            default -> null;
        };
    }
}
