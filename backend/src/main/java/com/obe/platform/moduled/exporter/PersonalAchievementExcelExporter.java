package com.obe.platform.moduled.exporter;

import com.obe.platform.common.BizException;
import com.obe.platform.modulec.service.PersonalAchievementService.StudentAchievementDetail;
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
import java.util.List;
import java.util.Map;

public final class PersonalAchievementExcelExporter {

    private PersonalAchievementExcelExporter() {
    }

    public static byte[] generate(
            List<StudentAchievementDetail> details,
            Map<Long, String> objectiveLabels,
            Map<Long, String> indicatorLabels) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            writeSummarySheet(workbook, details, indicatorLabels, headerStyle, dataStyle);
            writeObjectiveSheet(workbook, details, objectiveLabels, headerStyle, dataStyle);

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new BizException("个人达成度 Excel 导出失败: " + exception.getMessage());
        }
    }

    private static void writeSummarySheet(
            Workbook workbook,
            List<StudentAchievementDetail> details,
            Map<Long, String> indicatorLabels,
            CellStyle headerStyle,
            CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("个人达成度汇总");
        Row header = sheet.createRow(0);
        writeCell(header, 0, "学号", headerStyle);
        writeCell(header, 1, "姓名", headerStyle);
        writeCell(header, 2, "综合达成度", headerStyle);

        int column = 3;
        for (String label : indicatorLabels.values()) {
            writeCell(header, column++, label, headerStyle);
        }

        int rowIndex = 1;
        for (StudentAchievementDetail detail : details) {
            Row row = sheet.createRow(rowIndex++);
            writeCell(row, 0, detail.studentNo(), dataStyle);
            writeCell(row, 1, detail.studentName(), dataStyle);
            writeCell(row, 2, detail.overallAchievement(), dataStyle);
            column = 3;
            for (Long indicatorId : indicatorLabels.keySet()) {
                writeCell(
                        row,
                        column++,
                        detail.indicatorAchievements().getOrDefault(indicatorId, BigDecimal.ZERO),
                        dataStyle);
            }
        }
        autoSize(sheet, 3 + indicatorLabels.size());
        sheet.createFreezePane(2, 1);
    }

    private static void writeObjectiveSheet(
            Workbook workbook,
            List<StudentAchievementDetail> details,
            Map<Long, String> objectiveLabels,
            CellStyle headerStyle,
            CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("课程目标明细");
        Row header = sheet.createRow(0);
        String[] headers = {"学号", "姓名", "课程目标", "达成度"};
        for (int index = 0; index < headers.length; index++) {
            writeCell(header, index, headers[index], headerStyle);
        }

        int rowIndex = 1;
        for (StudentAchievementDetail detail : details) {
            for (Map.Entry<Long, String> objective : objectiveLabels.entrySet()) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, detail.studentNo(), dataStyle);
                writeCell(row, 1, detail.studentName(), dataStyle);
                writeCell(row, 2, objective.getValue(), dataStyle);
                writeCell(
                        row,
                        3,
                        detail.objectiveAchievements().getOrDefault(objective.getKey(), BigDecimal.ZERO),
                        dataStyle);
            }
        }
        autoSize(sheet, headers.length);
        sheet.createFreezePane(0, 1);
    }

    private static void writeCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : value.toString());
        }
        cell.setCellStyle(style);
    }

    private static void autoSize(Sheet sheet, int columnCount) {
        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
            sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 512, 12000));
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
}
