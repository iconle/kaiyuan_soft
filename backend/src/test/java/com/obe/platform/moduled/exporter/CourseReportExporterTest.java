package com.obe.platform.moduled.exporter;

import com.obe.platform.moduled.service.CourseReportService.CourseAssessmentResult;
import com.obe.platform.moduled.service.CourseReportService.CourseIndicatorResult;
import com.obe.platform.moduled.service.CourseReportService.CourseObjectiveResult;
import com.obe.platform.moduled.service.CourseReportService.CourseStudentScoreResult;
import org.apache.pdfbox.Loader;
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
                "Data Structures",
                "Class 1",
                objectives(),
                indicators(),
                assessments(),
                LocalDateTime.of(2026, 6, 8, 12, 0));

        assertThat(pdf).isNotEmpty();
        try (var document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
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

    private List<CourseObjectiveResult> objectives() {
        return List.of(new CourseObjectiveResult(
                "1",
                "Knowledge",
                "Understand core data structures",
                new BigDecimal("0.82")));
    }

    private List<CourseIndicatorResult> indicators() {
        return List.of(new CourseIndicatorResult(
                "1-1",
                "Apply foundational knowledge",
                new BigDecimal("0.79")));
    }

    private List<CourseAssessmentResult> assessments() {
        return List.of(new CourseAssessmentResult(
                "Final exam",
                "1",
                new BigDecimal("100"),
                new BigDecimal("81.50"),
                30));
    }
}
