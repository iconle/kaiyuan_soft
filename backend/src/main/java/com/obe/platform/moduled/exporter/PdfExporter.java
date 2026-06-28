package com.obe.platform.moduled.exporter;

import com.obe.platform.common.BizException;
import com.obe.platform.moduled.service.CourseReportService.CourseAssessmentResult;
import com.obe.platform.moduled.service.CourseReportService.CourseIndicatorResult;
import com.obe.platform.moduled.service.CourseReportService.CourseObjectiveResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.math.RoundingMode;

public class PdfExporter {

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 18;
    private static final float BOTTOM_MARGIN = 50;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static byte[] generateCourseReport(String courseName, String className,
                                               Map<String, BigDecimal> objectiveResults,
                                               Map<String, BigDecimal> indicatorResults,
                                               LocalDateTime calcTime) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            doc.addPage(page);

            PDFont font = loadFont(doc);

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float y = page.getMediaBox().getHeight() - MARGIN;

            // Title
            y = writeLine(cs, font, 16, MARGIN, y, "Course Attainment Report");
            y -= LINE_HEIGHT;

            y = writeLine(cs, font, 11, MARGIN, y,
                    "Course: " + courseName + "    Class: " + className);
            if (calcTime != null) {
                y = writeLine(cs, font, 10, MARGIN, y,
                        "Calc Time: " + calcTime.format(DTF));
            }
            y -= LINE_HEIGHT;

            // Objective results
            y = writeLine(cs, font, 13, MARGIN, y, "--- Objective Achievements (Level 1) ---");
            for (Map.Entry<String, BigDecimal> e : objectiveResults.entrySet()) {
                y = writeLine(cs, font, 10, MARGIN + 20, y,
                        "Objective " + e.getKey() + ": " + e.getValue());
            }
            y -= LINE_HEIGHT;

            // Indicator results
            y = writeLine(cs, font, 13, MARGIN, y, "--- Indicator Achievements (Level 2) ---");
            for (Map.Entry<String, BigDecimal> e : indicatorResults.entrySet()) {
                y = writeLine(cs, font, 10, MARGIN + 20, y,
                        "Indicator " + e.getKey() + ": " + e.getValue());
            }

            cs.close();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("PDF export failed: " + e.getMessage());
        }
    }

    public static byte[] generateCourseReport(
            String courseName,
            String className,
            List<CourseObjectiveResult> objectiveResults,
            List<CourseIndicatorResult> indicatorResults,
            List<CourseAssessmentResult> assessmentResults,
            LocalDateTime calcTime) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDFont font = loadFont(doc);
            ReportWriter writer = new ReportWriter(doc, font);

            writer.writeLine("课程达成度报告", 16, MARGIN);
            writer.blank();
            writer.writeLine("课程名称: " + safe(courseName), 11, MARGIN);
            writer.writeLine("教学班级: " + safe(className), 11, MARGIN);
            writer.writeLine("计算时间: " + (calcTime != null ? calcTime.format(DTF) : "-"), 10, MARGIN);
            writer.blank();

            writer.writeLine("一、课程目标达成度（第一级）", 13, MARGIN);
            if (objectiveResults.isEmpty()) {
                writer.writeLine("暂无课程目标达成度数据", 10, MARGIN + 20);
            } else {
                for (CourseObjectiveResult item : objectiveResults) {
                    writer.writeLine(item.objectiveNo() + "  达成度: " + formatAchievement(item.achievement())
                        + "  " + safe(item.description()), 10, MARGIN + 20);;
                }
            }
            writer.blank();

            writer.writeLine("二、课程级指标点达成度（第二级）", 13, MARGIN);
            if (indicatorResults.isEmpty()) {
                writer.writeLine("暂无指标点达成度数据", 10, MARGIN + 20);
            } else {
                for (CourseIndicatorResult item : indicatorResults) {
                    writer.writeLine(item.indicatorNo() + "  达成度: " + formatAchievement(item.achievement())
                        + "  " + safe(item.content()), 10, MARGIN + 20);
                }
            }
            writer.blank();

            writer.writeLine("三、考核点均分明细", 13, MARGIN);
            if (assessmentResults.isEmpty()) {
                writer.writeLine("暂无考核点成绩数据", 10, MARGIN + 20);
            } else {
                for (CourseAssessmentResult item : assessmentResults) {
                    writer.writeLine(safe(item.assessmentName())
                            + "  目标: " + safe(item.objectiveNos())
                            + "  满分: " + formatScore(item.maxScore())
                            + "  平均分: " + formatScore(item.averageScore())
                            + "  人数: " + item.scoreCount(), 10, MARGIN + 20);
                }
            }

            writer.close();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("PDF export failed: " + e.getMessage());
        }
    }

    private static float writeLine(PDPageContentStream cs, PDFont font, float fontSize,
                                    float x, float y, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitizeForFont(font, text));
        cs.endText();
        return y - LINE_HEIGHT;
    }

    private static PDFont loadFont(PDDocument doc) throws IOException {
        try (InputStream input = PdfExporter.class.getResourceAsStream("/fonts/wqy-microhei.ttf")) {
            if (input == null) {
                throw new IOException("Bundled Chinese font /fonts/wqy-microhei.ttf not found");
            }
            return PDType0Font.load(doc, input);
        }
    }



    private static String formatAchievement(BigDecimal value) {
        return value != null ? value.setScale(4, RoundingMode.HALF_UP).toPlainString() : "-";
    }

    private static String formatScore(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "-";
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    /**
     * 清理 PDF 文本流无法渲染的字符，避免 PDFBox showText 抛异常导致整份报告生成失败：
     *  - 移除换行符 \n \r（PDF 文本流禁止，是最常见的崩溃原因，报错 U+000A/U+000D in font）
     *  - 制表符 \t 转空格
     *  - 移除其他 C0 控制字符
     *  - 跳过当前字体（文泉驿微米黑）不含的字形（如部分特殊符号），避免 U+XXXX in font
     */
    private static String sanitizeForFont(PDFont font, String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') continue;
            if (c == '\t') { sb.append(' '); continue; }
            if (c < 0x20) continue;
            try {
                font.getStringWidth(String.valueOf(c));
                sb.append(c);
            } catch (Exception ignore) {
                // 字体不含该字形，跳过
            }
        }
        return sb.toString();
    }

    private static class ReportWriter {
        private final PDDocument document;
        private final PDFont font;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;

        ReportWriter(PDDocument document, PDFont font) throws Exception {
            this.document = document;
            this.font = font;
            newPage();
        }

        void writeLine(String text, float fontSize, float x) throws Exception {
            for (String line : wrap(sanitizeForFont(font, text), fontSize, x)) {
                ensureSpace();
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(x, y);
                contentStream.showText(line);
                contentStream.endText();
                y -= LINE_HEIGHT;
            }
        }

        void blank() {
            y -= LINE_HEIGHT / 2;
        }

        void close() throws Exception {
            if (contentStream != null) {
                contentStream.close();
            }
        }

        private void ensureSpace() throws Exception {
            if (y < BOTTOM_MARGIN) {
                newPage();
            }
        }

        private void newPage() throws Exception {
            if (contentStream != null) {
                contentStream.close();
            }
            page = new PDPage();
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private List<String> wrap(String text, float fontSize, float x) throws Exception {
            float maxWidth = page.getMediaBox().getWidth() - x - MARGIN;
            String value = safe(text);
            List<String> lines = new java.util.ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                String candidate = line + String.valueOf(ch);
                if (!line.isEmpty() && textWidth(candidate, fontSize) > maxWidth) {
                    lines.add(line.toString());
                    line = new StringBuilder(String.valueOf(ch));
                } else {
                    line.append(ch);
                }
            }
            lines.add(line.toString());
            return lines;
        }

        private float textWidth(String text, float fontSize) throws Exception {
            return font.getStringWidth(text) / 1000 * fontSize;
        }
    }
}
