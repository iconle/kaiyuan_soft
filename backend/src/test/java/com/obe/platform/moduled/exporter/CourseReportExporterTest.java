package com.obe.platform.moduled.exporter;

import com.obe.platform.moduled.service.CourseReportService.CourseAssessmentResult;
import com.obe.platform.moduled.service.CourseReportService.CourseIndicatorResult;
import com.obe.platform.moduled.service.CourseReportService.CourseObjectiveResult;
import com.obe.platform.moduled.service.CourseReportService.CourseStudentScoreResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseReportExporterTest {

    @Test
    void generatesReadablePdfWithAllSections() throws Exception {
        byte[] pdf = PdfExporter.generateCourseReport(
                "计算机网络",
                "计算机科学与技术2023级1班",
                objectives(),
                indicators(),
                assessments(),
                LocalDateTime.of(2026, 6, 8, 12, 0));

        assertThat(pdf).isNotEmpty();
        try (var document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text)
                    .contains("课程达成度报告")
                    .contains("计算机网络")
                    .contains("课程目标达成度")
                    .contains("理解OSI与TCP/IP体系结构")
                    .contains("期末考试");
            assertThat(text).doesNotContain("????");
        }
    }

    @Test
    void generatesCourseSpecificExcelSheetsAndIndicatorNumbers() throws Exception {
        byte[] excel = CourseExcelExporter.generateCourseReport(
                "Data Structures",
                "Class 1",
                LocalDateTime.of(2026, 6, 8, 12, 0),
                objectives(),
                indicators(),
                assessments(),
                List.of(new CourseStudentScoreResult("20260001", "Alice", "Final exam", new BigDecimal("88"))));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(5);
            assertThat(workbook.getSheet("课程达成度汇总")).isNotNull();
            assertThat(workbook.getSheet("课程目标达成度")).isNotNull();
            assertThat(workbook.getSheet("指标点达成度")).isNotNull();
            assertThat(workbook.getSheet("考核点均分明细")).isNotNull();
            assertThat(workbook.getSheet("学生成绩明细")).isNotNull();
            assertThat(workbook.getSheet("指标点达成度").getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("1-1");
        }
    }

    @Test
    void generatesPdfEvenWhenTextContainsNewlines() throws Exception {
        // 课程目标描述里含换行符/制表符：过去会让 PDFBox showText 抛 U+000A in font，
        // 整份 PDF 生成失败、接口返回 JSON 错误，前端下载后打不开。
        List<CourseObjectiveResult> objs = List.of(new CourseObjectiveResult(
                "1-1", "知识", "掌握数据结构\n理解时间复杂度\t与空间复杂度",
                new BigDecimal("0.83")));
        byte[] pdf = PdfExporter.generateCourseReport(
                "数据结构", "计科2023级1班", objs, List.of(), List.of(),
                LocalDateTime.of(2026, 6, 28, 10, 0));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        try (var document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("数据结构").contains("时间复杂度").contains("空间复杂度");
        }
    }

    private List<CourseObjectiveResult> objectives() {
        return List.of(new CourseObjectiveResult(
                "1",
                "知识目标",
                "理解OSI与TCP/IP体系结构",
                new BigDecimal("0.82")));
    }

    private List<CourseIndicatorResult> indicators() {
        return List.of(new CourseIndicatorResult(
                "1-1",
                "能够运用网络基础知识分析复杂工程问题",
                new BigDecimal("0.79")));
    }

    private List<CourseAssessmentResult> assessments() {
        return List.of(new CourseAssessmentResult(
                "期末考试",
                "1",
                new BigDecimal("100"),
                new BigDecimal("81.50"),
                30));
    }
}
