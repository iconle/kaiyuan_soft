package com.obe.platform.moduled.exporter;

import com.obe.platform.common.BizException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Drill-through Excel ledger exporter for major-level report.
 * Creates a multi-sheet workbook where each sheet corresponds to one indicator point,
 * showing the computation trace from course → indicator → objective → assessment scores.
 */
public class TraceExcelExporter {

    /**
     * Generate the drill-through Excel ledger.
     *
     * @param indicatorResults      Map<indicatorNo, G_k> — major-level results
     * @param indicatorTraceData    per-indicator trace data
     * @return Excel byte array
     */
    public static byte[] generateTraceLedger(
            Map<String, BigDecimal> indicatorResults,
            List<IndicatorTraceSheet> indicatorTraceData) {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Summary sheet
            Sheet summarySheet = workbook.createSheet("专业级达成度汇总");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            Row headerRow = summarySheet.createRow(0);
            headerRow.createCell(0).setCellValue("指标点");
            headerRow.createCell(1).setCellValue("专业级达成度 G_k");
            headerRow.getCell(0).setCellStyle(headerStyle);
            headerRow.getCell(1).setCellStyle(headerStyle);

            int rowIdx = 1;
            for (Map.Entry<String, BigDecimal> e : indicatorResults.entrySet()) {
                Row row = summarySheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getKey());
                row.createCell(1).setCellValue(e.getValue().doubleValue());
                row.getCell(0).setCellStyle(dataStyle);
                row.getCell(1).setCellStyle(dataStyle);
            }

            // Per-indicator drill-down sheets
            for (IndicatorTraceSheet trace : indicatorTraceData) {
                Sheet sheet = workbook.createSheet("指标点" + trace.indicatorNo());
                Row hRow = sheet.createRow(0);
                String[] colHeaders = {"课程", "课程级达成度 E_k", "总支撑权重 W_c",
                        "课程目标", "目标达成度", "内部权重 w_jk", "考核点", "满分", "平均得分"};
                for (int i = 0; i < colHeaders.length; i++) {
                    Cell cell = hRow.createCell(i);
                    cell.setCellValue(colHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }

                int r = 1;
                for (TraceRow tr : trace.rows()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(tr.courseName());
                    row.createCell(1).setCellValue(tr.ek() != null ? tr.ek().doubleValue() : 0);
                    row.createCell(2).setCellValue(tr.wc() != null ? tr.wc().doubleValue() : 0);
                    row.createCell(3).setCellValue(tr.objectiveNo());
                    row.createCell(4).setCellValue(tr.objAchievement() != null ? tr.objAchievement().doubleValue() : 0);
                    row.createCell(5).setCellValue(tr.wjk() != null ? tr.wjk().doubleValue() : 0);
                    row.createCell(6).setCellValue(tr.assessmentName());
                    row.createCell(7).setCellValue(tr.maxScore() != null ? tr.maxScore().doubleValue() : 0);
                    row.createCell(8).setCellValue(tr.avgScore() != null ? tr.avgScore().doubleValue() : 0);
                    for (int i = 0; i < colHeaders.length; i++) {
                        row.getCell(i).setCellStyle(dataStyle);
                    }
                }

                // Auto-size columns
                for (int i = 0; i < colHeaders.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成穿透式台账失败: " + e.getMessage());
        }
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    public record IndicatorTraceSheet(String indicatorNo, List<TraceRow> rows) {}

    public record TraceRow(String courseName, BigDecimal ek, BigDecimal wc,
                            String objectiveNo, BigDecimal objAchievement, BigDecimal wjk,
                            String assessmentName, BigDecimal maxScore, BigDecimal avgScore) {}
}
