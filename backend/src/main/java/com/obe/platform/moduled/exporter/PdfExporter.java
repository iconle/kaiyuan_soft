package com.obe.platform.moduled.exporter;

import com.obe.platform.common.BizException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class PdfExporter {

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 18;
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

    private static float writeLine(PDPageContentStream cs, PDFont font, float fontSize,
                                    float x, float y, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - LINE_HEIGHT;
    }

    private static PDFont loadFont(PDDocument doc) {
        // Try Chinese fonts first
        for (String path : new String[]{
                "/fonts/NotoSansSC-Regular.ttf",
                "/fonts/SimHei.ttf",
                "/fonts/simsun.ttf"}) {
            try {
                InputStream is = PdfExporter.class.getResourceAsStream(path);
                if (is != null) {
                    return PDType0Font.load(doc, is);
                }
            } catch (Exception ignored) {}
        }
        // Fallback to built-in Helvetica
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }
}
