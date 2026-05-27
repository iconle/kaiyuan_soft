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
    private final ClassStudentMapper classStudentMapper;
    private final StudentMapper studentMapper;

    /**
     * Generate a score-entry Excel template for the given teaching class.
     * <p>
     * The workbook layout:
     * - Row 0: Header row with column names (学号, 姓名, [assessment point names...])
     * - Row 1: Sub-header row with max scores for each assessment point
     * - Row 2+: One row per enrolled student with student number and name pre-filled
     * <p>
     * The first two columns are frozen.
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

        // 2. Query assessment points
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

        // 3. Query enrolled students
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

        // 4. Build the Excel workbook
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int totalColumns = 2 + assessmentPoints.size(); // 学号 + 姓名 + assessment points

            // Write the workbook manually using EasyExcel's underlying POI API
            // to have full control over the layout (merged header, sub-header, frozen panes)
            Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            Sheet excelSheet = workbook.createSheet("成绩录入");

            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle subHeaderStyle = workbook.createCellStyle();
            Font subHeaderFont = workbook.createFont();
            subHeaderFont.setFontHeightInPoints((short) 10);
            subHeaderFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            subHeaderStyle.setFont(subHeaderFont);
            subHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            subHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            subHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            subHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            subHeaderStyle.setBorderBottom(BorderStyle.THIN);
            subHeaderStyle.setBorderTop(BorderStyle.THIN);
            subHeaderStyle.setBorderLeft(BorderStyle.THIN);
            subHeaderStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setBorderBottom(BorderStyle.THIN);
            numberStyle.setBorderTop(BorderStyle.THIN);
            numberStyle.setBorderLeft(BorderStyle.THIN);
            numberStyle.setBorderRight(BorderStyle.THIN);
            numberStyle.setAlignment(HorizontalAlignment.CENTER);

            // Row 0: Header row
            Row headerRow = excelSheet.createRow(0);
            Cell h0 = headerRow.createCell(0);
            h0.setCellValue("学号");
            h0.setCellStyle(headerStyle);
            Cell h1 = headerRow.createCell(1);
            h1.setCellValue("姓名");
            h1.setCellStyle(headerStyle);

            for (int i = 0; i < assessmentPoints.size(); i++) {
                AssessmentPoint ap = assessmentPoints.get(i);
                Cell cell = headerRow.createCell(2 + i);
                cell.setCellValue(ap.getName());
                cell.setCellStyle(headerStyle);
            }

            // Row 1: Sub-header row (max scores)
            Row subHeaderRow = excelSheet.createRow(1);
            Cell sh0 = subHeaderRow.createCell(0);
            sh0.setCellValue("");
            sh0.setCellStyle(subHeaderStyle);
            Cell sh1 = subHeaderRow.createCell(1);
            sh1.setCellValue("");
            sh1.setCellStyle(subHeaderStyle);

            for (int i = 0; i < assessmentPoints.size(); i++) {
                AssessmentPoint ap = assessmentPoints.get(i);
                Cell cell = subHeaderRow.createCell(2 + i);
                if (ap.getMaxScore() != null) {
                    cell.setCellValue("满分: " + ap.getMaxScore().toPlainString());
                } else {
                    cell.setCellValue("");
                }
                cell.setCellStyle(subHeaderStyle);
            }

            // Data rows: one per student
            int rowIndex = 2;
            for (ClassStudent cs : classStudents) {
                Student student = studentMap.get(cs.getStudentId());
                if (student == null) {
                    continue;
                }
                Row dataRow = excelSheet.createRow(rowIndex++);

                Cell c0 = dataRow.createCell(0);
                c0.setCellValue(student.getStudentNo());
                c0.setCellStyle(dataStyle);

                Cell c1 = dataRow.createCell(1);
                c1.setCellValue(student.getName());
                c1.setCellStyle(dataStyle);

                // Leave assessment columns empty for score entry
                for (int i = 0; i < assessmentPoints.size(); i++) {
                    Cell cell = dataRow.createCell(2 + i);
                    cell.setCellStyle(numberStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < totalColumns; i++) {
                excelSheet.autoSizeColumn(i);
                // Add a little extra width for readability
                int currentWidth = excelSheet.getColumnWidth(i);
                excelSheet.setColumnWidth(i, currentWidth + 500);
            }

            // Freeze first two columns and the first two header rows
            excelSheet.createFreezePane(2, 2);

            workbook.write(out);
            workbook.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成Excel模板失败: " + e.getMessage());
        }
    }
}
