package com.obe.platform.modulec.service;

import com.obe.platform.common.BizException;
import com.obe.platform.modulea.mapper.ClassStudentMapper;
import com.obe.platform.modulea.mapper.StudentMapper;
import com.obe.platform.moduleb.entity.AssessmentPoint;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.mapper.AssessmentPointMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import com.obe.platform.modulec.entity.ScoreSheet;
import com.obe.platform.modulec.mapper.ScoreSheetMapper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelParseServiceTest {

    @Mock private ScoreSheetMapper scoreSheetMapper;
    @Mock private CourseOutlineMapper outlineMapper;
    @Mock private AssessmentPointMapper assessmentPointMapper;
    @Mock private ClassStudentMapper classStudentMapper;
    @Mock private StudentMapper studentMapper;

    private ExcelParseService service;

    @BeforeEach
    void setUp() {
        service = new ExcelParseService(
                scoreSheetMapper, outlineMapper, assessmentPointMapper,
                classStudentMapper, studentMapper);
    }

    @Test
    void rejectsScoreWorkbookWhenHeadersDoNotMatchCurrentAssessments() throws Exception {
        ScoreSheet scoreSheet = new ScoreSheet();
        scoreSheet.setId(5L);
        scoreSheet.setClassId(9L);
        CourseOutline outline = new CourseOutline();
        outline.setId(3L);
        AssessmentPoint assessment = new AssessmentPoint();
        assessment.setId(7L);
        assessment.setName("期末考试");
        assessment.setMaxScore(new BigDecimal("100"));

        when(scoreSheetMapper.selectById(5L)).thenReturn(scoreSheet);
        when(outlineMapper.selectOne(any())).thenReturn(outline);
        when(assessmentPointMapper.selectList(any())).thenReturn(List.of(assessment));
        when(classStudentMapper.selectList(any())).thenReturn(List.of());
        when(studentMapper.selectBatchIds(any())).thenReturn(List.of());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("成绩录入");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("学号");
            header.createCell(1).setCellValue("姓名");
            header.createCell(2).setCellValue("错误考核点");
            var subHeader = sheet.createRow(1);
            subHeader.createCell(2).setCellValue("满分: 100");
            workbook.write(output);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "成绩.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());

            assertThatThrownBy(() -> service.parseScoreFile(file, 5L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("成绩模板表头不正确")
                    .hasMessageContaining("期末考试");
        }
    }
}
