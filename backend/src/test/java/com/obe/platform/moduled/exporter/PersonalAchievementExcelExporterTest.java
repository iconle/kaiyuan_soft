package com.obe.platform.moduled.exporter;

import com.obe.platform.modulec.service.PersonalAchievementService.StudentAchievementDetail;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalAchievementExcelExporterTest {

    @Test
    void shouldGenerateSummaryAndObjectiveSheets() throws Exception {
        Map<Long, String> objectiveLabels = new LinkedHashMap<>();
        objectiveLabels.put(1L, "目标1");
        objectiveLabels.put(2L, "目标2");

        Map<Long, String> indicatorLabels = new LinkedHashMap<>();
        indicatorLabels.put(10L, "1.1");

        StudentAchievementDetail detail = new StudentAchievementDetail(
                100L,
                "20260001",
                "测试学生",
                new BigDecimal("0.7500"),
                Map.of(1L, new BigDecimal("0.8000"), 2L, new BigDecimal("0.7000")),
                Map.of(10L, new BigDecimal("0.7500")),
                objectiveLabels,
                indicatorLabels);

        byte[] excel = PersonalAchievementExcelExporter.generate(
                List.of(detail),
                objectiveLabels,
                indicatorLabels);

        assertTrue(excel.length > 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertEquals("个人达成度汇总", workbook.getSheetAt(0).getSheetName());
            assertEquals("课程目标明细", workbook.getSheetAt(1).getSheetName());
            assertEquals("20260001", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals(0.75, workbook.getSheetAt(0).getRow(1).getCell(2).getNumericCellValue(), 0.0001);
        }
    }
}
