package com.obe.platform.modulec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.obe.platform.modulec.mapper.ScoreSheetMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelTemplateService {

    private final ScoreSheetMapper scoreSheetMapper;
    private final CourseOutlineMapper outlineMapper;
    private final AssessmentPointMapper assessmentPointMapper;
    private final AssessmentQuestionMapper questionMapper;
    private final ClassStudentMapper classStudentMapper;
    private final StudentMapper studentMapper;

    /**
     * Generate a score-entry Excel template for the given teaching class.
     * <p>
     * The workbook contains one sheet per assessment point named "考核点名称" containing its questions.
     * <p>
     * Each sheet layout:
     * - Row 0: Header row with column names (学号, 姓名, [题目列表...], 总成绩)
     * - Row 1: Sub-header row with max scores
     * - Row 2+: One row per enrolled student
     *
     * @param classId the teaching class ID
     * @return byte array of the Excel file
     */
    public byte[] generateTemplate(Long classId) {
        // 1. Auto-create ScoreSheet if not exists
        ScoreSheet sheet = scoreSheetMapper.selectOne(
                new LambdaQueryWrapper<ScoreSheet>()
                        .eq(ScoreSheet::getClassId, classId));
        if (sheet == null) {
            sheet = new ScoreSheet();
            sheet.setClassId(classId);
            sheet.setStatus("EMPTY");
            scoreSheetMapper.insert(sheet);
        }

        // 2. Query assessment points with their questions
        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>()
                        .eq(CourseOutline::getClassId, classId));
        List<AssessmentPoint> assessmentPoints;
        if (outline != null) {
            assessmentPoints = assessmentPointMapper.selectList(
                    new LambdaQueryWrapper<AssessmentPoint>()
                            .eq(AssessmentPoint::getOutlineId, outline.getId())
                            .orderByAsc(AssessmentPoint::getSortOrder));
        } else {
            assessmentPoints = List.of();
        }

        // 3. Query questions for each assessment point
        Map<Long, List<AssessmentQuestion>> questionsMap = assessmentPoints.stream()
                .collect(Collectors.toMap(
                        AssessmentPoint::getId,
                        ap -> questionMapper.selectList(
                                new LambdaQueryWrapper<AssessmentQuestion>()
                                        .eq(AssessmentQuestion::getAssessmentId, ap.getId())
                                        .orderByAsc(AssessmentQuestion::getSortOrder))
                ));

        // 4. Query enrolled students
        List<ClassStudent> classStudents = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, classId));

        List<Student> students = new ArrayList<>();
        if (!classStudents.isEmpty()) {
            List<Long> studentIds = classStudents.stream()
                    .map(ClassStudent::getStudentId)
                    .toList();
            students = studentMapper.selectBatchIds(studentIds);
        }
        Map<Long, Student> studentMap = students.stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        // 5. Build the Excel workbook
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();

            // Create individual sheets for each assessment point
            for (AssessmentPoint ap : assessmentPoints) {
                List<AssessmentQuestion> questions = questionsMap.get(ap.getId());
                // Always create a sheet for each assessment point
                createAssessmentSheet(workbook, ap, questions != null ? questions : List.of(),
                        classStudents, studentMap);
            }

            workbook.write(out);
            workbook.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成Excel模板失败: " + e.getMessage());
        }
    }

    /**
     * Create a sheet for a single assessment point.
     * If the assessment has questions, create question-level columns.
     * If no questions, create a single assessment-level column.
     */
    private void createAssessmentSheet(Workbook workbook, AssessmentPoint ap,
                                       List<AssessmentQuestion> questions,
                                       List<ClassStudent> classStudents, Map<Long, Student> studentMap) {
        // Use assessment point name as sheet name (sanitize for Excel)
        String sheetName = sanitizeSheetName(ap.getName());
        Sheet excelSheet = workbook.createSheet(sheetName);

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle subHeaderStyle = createSubHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle numberStyle = createNumberStyle(workbook);

        // Row 0: Header row
        Row headerRow = excelSheet.createRow(0);
        createHeaderCell(headerRow, 0, "学号", headerStyle);
        createHeaderCell(headerRow, 1, "姓名", headerStyle);

        if (questions.isEmpty()) {
            // No questions - single assessment-level column
            createHeaderCell(headerRow, 2, "成绩", headerStyle);

            // Row 1: Sub-header with max score
            Row subHeaderRow = excelSheet.createRow(1);
            createSubHeaderCell(subHeaderRow, 0, "", subHeaderStyle);
            createSubHeaderCell(subHeaderRow, 1, "", subHeaderStyle);
            String maxValue = ap.getMaxScore() != null ? "满分: " + ap.getMaxScore().toPlainString() : "";
            createSubHeaderCell(subHeaderRow, 2, maxValue, subHeaderStyle);

            // Data rows
            int rowIndex = 2;
            for (ClassStudent cs : classStudents) {
                Student student = studentMap.get(cs.getStudentId());
                if (student == null) continue;

                Row dataRow = excelSheet.createRow(rowIndex++);
                createDataCell(dataRow, 0, student.getStudentNo(), dataStyle);
                createDataCell(dataRow, 1, student.getName(), dataStyle);
                createNumberCell(dataRow, 2, numberStyle);
            }

            autoSizeColumns(excelSheet, 3);
        } else {
            // Has questions - show question columns
            for (int i = 0; i < questions.size(); i++) {
                AssessmentQuestion q = questions.get(i);
                createHeaderCell(headerRow, 2 + i, q.getName(), headerStyle);
            }
            // Add a total column
            createHeaderCell(headerRow, 2 + questions.size(), "总成绩", headerStyle);

            // Row 1: Sub-header row (max scores)
            Row subHeaderRow = excelSheet.createRow(1);
            createSubHeaderCell(subHeaderRow, 0, "", subHeaderStyle);
            createSubHeaderCell(subHeaderRow, 1, "", subHeaderStyle);

            for (int i = 0; i < questions.size(); i++) {
                AssessmentQuestion q = questions.get(i);
                String maxValue = q.getMaxScore() != null ? "满分: " + q.getMaxScore().toPlainString() : "";
                createSubHeaderCell(subHeaderRow, 2 + i, maxValue, subHeaderStyle);
            }
            createSubHeaderCell(subHeaderRow, 2 + questions.size(), "", subHeaderStyle);

            // Data rows: one per student
            int rowIndex = 2;
            for (ClassStudent cs : classStudents) {
                Student student = studentMap.get(cs.getStudentId());
                if (student == null) continue;

                Row dataRow = excelSheet.createRow(rowIndex++);
                createDataCell(dataRow, 0, student.getStudentNo(), dataStyle);
                createDataCell(dataRow, 1, student.getName(), dataStyle);

                for (int i = 0; i < questions.size(); i++) {
                    createNumberCell(dataRow, 2 + i, numberStyle);
                }
                // Total column (read-only, formula would be set by user)
                createDataCell(dataRow, 2 + questions.size(), "", dataStyle);
            }

            autoSizeColumns(excelSheet, 3 + questions.size());
        }

        excelSheet.createFreezePane(2, 2);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createSubHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void createHeaderCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createSubHeaderCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createDataCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createNumberCell(Row row, int col, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(currentWidth + 500, 8000));
        }
    }

    /**
     * Sanitize sheet name to be valid for Excel (max 31 chars, no special chars).
     */
    private String sanitizeSheetName(String name) {
        // Remove invalid characters: []:*?/\'
        String sanitized = name.replaceAll("[:\\[\\]\\*\\?\\/\\\\']", "");
        // Truncate to 31 characters
        if (sanitized.length() > 31) {
            sanitized = sanitized.substring(0, 31);
        }
        return sanitized.isEmpty() ? "考核点" : sanitized;
    }
}
