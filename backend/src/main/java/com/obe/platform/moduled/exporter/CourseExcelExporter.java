package com.obe.platform.moduled.exporter;

import com.obe.platform.common.BizException;
import com.obe.platform.moduled.service.CourseReportService.CourseAssessmentResult;
import com.obe.platform.moduled.service.CourseReportService.CourseIndicatorResult;
import com.obe.platform.moduled.service.CourseReportService.CourseObjectiveResult;
import com.obe.platform.moduled.service.CourseReportService.CourseStudentScoreResult;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CourseExcelExporter {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static byte[] generateCourseReport(
            String courseName,
            String className,
            LocalDateTime calcTime,
            List<CourseObjectiveResult> objectiveResults,
            List<CourseIndicatorResult> indicatorResults,
            List<CourseAssessmentResult> assessmentResults,
            List<CourseStudentScoreResult> studentScoreDetails) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            writeSummarySheet(workbook, headerStyle, dataStyle, courseName, className, calcTime,
                    objectiveResults, indicatorResults, assessmentResults);
            writeObjectiveSheet(workbook, headerStyle, dataStyle, objectiveResults);
            writeIndicatorSheet(workbook, headerStyle, dataStyle, indicatorResults);
            writeAssessmentSheet(workbook, headerStyle, dataStyle, assessmentResults);
            writeStudentScoreSheet(workbook, headerStyle, dataStyle, studentScoreDetails);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("课程报表 Excel 导出失败: " + e.getMessage());
        }
    }

    private static void writeSummarySheet(
            Workbook workbook,
            CellStyle headerStyle,
            CellStyle dataStyle,
            String courseName,
            String className,
            LocalDateTime calcTime,
            List<CourseObjectiveResult> objectiveResults,
            List<CourseIndicatorResult> indicatorResults,
            List<CourseAssessmentResult> assessmentResults) {
        Sheet sheet = workbook.createSheet("课程达成度汇总");
        int rowIndex = 0;

        rowIndex = writeKeyValue(sheet, rowIndex, "课程名称", courseName, headerStyle, dataStyle);
        rowIndex = writeKeyValue(sheet, rowIndex, "教学班级", className, headerStyle, dataStyle);
        rowIndex = writeKeyValue(sheet, rowIndex, "计算时间", calcTime != null ? calcTime.format(DTF) : "-", headerStyle, dataStyle);
        rowIndex++;

        rowIndex = writeSectionTitle(sheet, rowIndex, "课程目标达成度", headerStyle);
        rowIndex = writeRows(sheet, rowIndex,
                new String[]{"目标编号", "达成度"},
                objectiveResults.stream()
                        .map(item -> new Object[]{item.objectiveNo(), item.achievement()})
                        .toList(),
                headerStyle, dataStyle);
        rowIndex++;

        rowIndex = writeSectionTitle(sheet, rowIndex, "课程级指标点达成度", headerStyle);
        rowIndex = writeRows(sheet, rowIndex,
                new String[]{"指标点编号", "达成度"},
                indicatorResults.stream()
                        .map(item -> new Object[]{item.indicatorNo(), item.achievement()})
                        .toList(),
                headerStyle, dataStyle);
        rowIndex++;

        writeSectionTitle(sheet, rowIndex, "考核点均分", headerStyle);
        writeRows(sheet, rowIndex + 1,
                new String[]{"考核点", "关联课程目标", "满分", "平均得分", "有效人数"},
                assessmentResults.stream()
                        .map(item -> new Object[]{
                                item.assessmentName(),
                                item.objectiveNos(),
                                item.maxScore(),
                                item.averageScore(),
                                item.scoreCount()
                        })
                        .toList(),
                headerStyle, dataStyle);

        autoSize(sheet, 5);
    }

    private static void writeObjectiveSheet(
            Workbook workbook,
            CellStyle headerStyle,
            CellStyle dataStyle,
            List<CourseObjectiveResult> objectiveResults) {
        Sheet sheet = workbook.createSheet("课程目标达成度");
        writeRows(sheet, 0,
                new String[]{"目标编号", "维度", "目标描述", "达成度"},
                objectiveResults.stream()
                        .map(item -> new Object[]{
                                item.objectiveNo(),
                                item.dimension(),
                                item.description(),
                                item.achievement()
                        })
                        .toList(),
                headerStyle, dataStyle);
        autoSize(sheet, 4);
    }

    private static void writeIndicatorSheet(
            Workbook workbook,
            CellStyle headerStyle,
            CellStyle dataStyle,
            List<CourseIndicatorResult> indicatorResults) {
        Sheet sheet = workbook.createSheet("指标点达成度");
        writeRows(sheet, 0,
                new String[]{"指标点编号", "指标点内容", "达成度"},
                indicatorResults.stream()
                        .map(item -> new Object[]{
                                item.indicatorNo(),
                                item.content(),
                                item.achievement()
                        })
                        .toList(),
                headerStyle, dataStyle);
        autoSize(sheet, 3);
    }

    private static void writeAssessmentSheet(
            Workbook workbook,
            CellStyle headerStyle,
            CellStyle dataStyle,
            List<CourseAssessmentResult> assessmentResults) {
        Sheet sheet = workbook.createSheet("考核点均分明细");
        writeRows(sheet, 0,
                new String[]{"考核点", "关联课程目标", "满分", "平均得分", "有效人数"},
                assessmentResults.stream()
                        .map(item -> new Object[]{
                                item.assessmentName(),
                                item.objectiveNos(),
                                item.maxScore(),
                                item.averageScore(),
                                item.scoreCount()
                        })
                        .toList(),
                headerStyle, dataStyle);
        autoSize(sheet, 5);
    }

    private static void writeStudentScoreSheet(
            Workbook workbook,
            CellStyle headerStyle,
            CellStyle dataStyle,
            List<CourseStudentScoreResult> studentScoreDetails) {
        Sheet sheet = workbook.createSheet("学生成绩明细");
        writeRows(sheet, 0,
                new String[]{"学号", "姓名", "考核点", "得分"},
                studentScoreDetails.stream()
                        .map(item -> new Object[]{
                                item.studentNo(),
                                item.studentName(),
                                item.assessmentName(),
                                item.score()
                        })
                        .toList(),
                headerStyle, dataStyle);
        autoSize(sheet, 4);
    }

    private static int writeKeyValue(
            Sheet sheet,
            int rowIndex,
            String key,
            String value,
            CellStyle headerStyle,
            CellStyle dataStyle) {
        Row row = sheet.createRow(rowIndex);
        Cell keyCell = row.createCell(0);
        keyCell.setCellValue(key);
        keyCell.setCellStyle(headerStyle);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value != null ? value : "");
        valueCell.setCellStyle(dataStyle);
        return rowIndex + 1;
    }

    private static int writeSectionTitle(Sheet sheet, int rowIndex, String title, CellStyle headerStyle) {
        Row row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(headerStyle);
        return rowIndex + 1;
    }

    private static int writeRows(
            Sheet sheet,
            int rowIndex,
            String[] headers,
            List<Object[]> rows,
            CellStyle headerStyle,
            CellStyle dataStyle) {
        Row headerRow = sheet.createRow(rowIndex++);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        for (Object[] values : rows) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.createCell(i);
                setValue(cell, i < values.length ? values[i] : null);
                cell.setCellStyle(dataStyle);
            }
        }
        return rowIndex;
    }

    private static void setValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private static void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(style);
        return style;
    }

    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        applyBorder(style);
        return style;
    }

    private static void applyBorder(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
