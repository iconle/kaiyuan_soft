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
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public final class PersonalAchievementExcelExporter {

    /** 达标阈值，与前端 CourseCompute 页面 achievementTagText 的判定口径一致（>= 0.7 达标） */
    private static final BigDecimal PASS_THRESHOLD = new BigDecimal("0.7");

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
            CellStyle numericStyle = createNumericStyle(workbook);

            writeSummarySheet(workbook, details, indicatorLabels, headerStyle, dataStyle, numericStyle);
            writeObjectiveSheet(workbook, details, objectiveLabels, headerStyle, dataStyle, numericStyle);

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
            CellStyle dataStyle,
            CellStyle numericStyle) {
        CellStyle redNumericStyle = createRedNumericStyle(workbook);
        Sheet sheet = workbook.createSheet("个人达成度汇总");
        Row header = sheet.createRow(0);
        writeCell(header, 0, "学号", headerStyle);
        writeCell(header, 1, "姓名", headerStyle);
        writeCell(header, 2, "综合达成度", headerStyle);

        int column = 3;
        for (String label : indicatorLabels.values()) {
            writeCell(header, column++, label, headerStyle);
        }

        // 累计综合达成度总和与未达标人数（综合达成度 < 0.7 视为未达标）
        BigDecimal overallSum = BigDecimal.ZERO;
        int totalCount = 0;
        int notPassCount = 0;

        int rowIndex = 1;
        for (StudentAchievementDetail detail : details) {
            Row row = sheet.createRow(rowIndex++);
            writeCell(row, 0, detail.studentNo(), dataStyle);
            writeCell(row, 1, detail.studentName(), dataStyle);
            writeNumericCell(row, 2, detail.overallAchievement(), numericStyle, redNumericStyle);
            column = 3;
            for (Long indicatorId : indicatorLabels.keySet()) {
                writeNumericCell(
                        row,
                        column++,
                        detail.indicatorAchievements().getOrDefault(indicatorId, BigDecimal.ZERO),
                        numericStyle,
                        redNumericStyle);
            }

            BigDecimal overall = detail.overallAchievement();
            if (overall != null) {
                totalCount++;
                overallSum = overallSum.add(overall);
                if (overall.compareTo(PASS_THRESHOLD) < 0) notPassCount++;
            }
        }

        // 末行汇总：综合达成度平均值 + 未达标人数
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        CellStyle summaryStyle = createDataStyle(workbook);
        summaryStyle.setFont(boldFont);
        CellStyle summaryNumericStyle = createNumericStyle(workbook);
        summaryNumericStyle.setFont(boldFont);

        int lastColumn = 3 + Math.max(0, indicatorLabels.size() - 1);
        Row summaryRow = sheet.createRow(rowIndex);
        for (int c = 0; c <= lastColumn; c++) {
            summaryRow.createCell(c).setCellStyle(summaryStyle);
        }
        summaryRow.getCell(0).setCellValue("综合达成度平均值");
        BigDecimal overallAvg = totalCount > 0
                ? overallSum.divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Cell avgCell = summaryRow.getCell(2);
        avgCell.setCellValue(overallAvg.doubleValue());
        avgCell.setCellStyle(summaryNumericStyle);
        summaryRow.getCell(3).setCellValue("未达标: " + notPassCount + " 人 / 共 " + totalCount + " 人");

        autoSize(sheet, 3 + indicatorLabels.size());
        sheet.createFreezePane(2, 1);
    }

    private static void writeObjectiveSheet(
            Workbook workbook,
            List<StudentAchievementDetail> details,
            Map<Long, String> objectiveLabels,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle numericStyle) {
        CellStyle redNumericStyle = createRedNumericStyle(workbook);
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
                writeNumericCell(
                        row,
                        3,
                        detail.objectiveAchievements().getOrDefault(objective.getKey(), BigDecimal.ZERO),
                        numericStyle,
                        redNumericStyle);
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

    private static CellStyle createNumericStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.0000"));
        return style;
    }

    private static CellStyle createRedNumericStyle(Workbook workbook) {
        CellStyle style = createNumericStyle(workbook);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        return style;
    }

    private static void writeNumericCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value.doubleValue() : 0);
        cell.setCellStyle(style);
    }

    /**
     * 写入数值单元格，低于达标阈值（0.7）时使用红色样式，否则使用普通样式。
     */
    private static void writeNumericCell(Row row, int column, BigDecimal value,
                                         CellStyle normalStyle, CellStyle redStyle) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value.doubleValue() : 0);
        boolean belowThreshold = value != null && value.compareTo(PASS_THRESHOLD) < 0;
        cell.setCellStyle(belowThreshold ? redStyle : normalStyle);
    }
}
