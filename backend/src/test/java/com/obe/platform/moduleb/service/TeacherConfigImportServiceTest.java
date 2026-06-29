package com.obe.platform.moduleb.service;

import com.obe.platform.common.BizException;
import com.obe.platform.moduleb.entity.CourseObjective;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.mapper.AssessmentObjectiveMapper;
import com.obe.platform.moduleb.mapper.AssessmentPointMapper;
import com.obe.platform.moduleb.mapper.AssessmentQuestionMapper;
import com.obe.platform.moduleb.mapper.CourseObjectiveMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import com.obe.platform.moduleb.mapper.QuestionObjectiveMapper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherConfigImportServiceTest {

    @Mock private CourseOutlineMapper outlineMapper;
    @Mock private CourseObjectiveMapper objectiveMapper;
    @Mock private AssessmentPointMapper assessmentMapper;
    @Mock private AssessmentObjectiveMapper assessmentObjectiveMapper;
    @Mock private AssessmentQuestionMapper questionMapper;
    @Mock private QuestionObjectiveMapper questionObjectiveMapper;

    private TeacherConfigImportService service;

    @BeforeEach
    void setUp() {
        service = new TeacherConfigImportService(
                outlineMapper, objectiveMapper, assessmentMapper, assessmentObjectiveMapper,
                questionMapper, questionObjectiveMapper);
    }

    @Test
    void objectiveTemplateUsesStrictHeadersWithoutSampleData() throws Exception {
        byte[] data = service.generateObjectiveTemplate();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("维度");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("目标描述");
            assertThat(sheet.getRow(0).getCell(2)).isNull();
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEmpty();
        }
    }

    @Test
    void rejectsChangedTemplateHeaderWithActionableMessage() throws Exception {
        CourseOutline outline = new CourseOutline();
        outline.setId(10L);
        when(outlineMapper.selectOne(any())).thenReturn(outline);
        when(objectiveMapper.selectList(any())).thenReturn(List.of());

        MockMultipartFile file = workbook(
                new String[]{"课程目标编号", "维度", "目标描述"},
                new String[]{"1-1", "知识", "描述"});

        assertThatThrownBy(() -> service.importObjectives(1L, file))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("模板格式不正确")
                .hasMessageContaining("期望表头");
    }

    @Test
    void importsValidObjectivesAfterFullValidation() throws Exception {
        CourseOutline outline = new CourseOutline();
        outline.setId(10L);
        when(outlineMapper.selectOne(any())).thenReturn(outline);
        when(objectiveMapper.selectList(any())).thenReturn(List.of());

        MockMultipartFile file = workbook(
                new String[]{"维度", "目标描述"},
                new String[]{"知识", "掌握核心知识"});

        assertThat(service.importObjectives(1L, file)).isEqualTo(1);
        verify(objectiveMapper).insert(any(CourseObjective.class));
    }

    private MockMultipartFile workbook(String[] headers, String[] values) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("课程目标导入");
            var header = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
            }
            var row = sheet.createRow(1);
            for (int index = 0; index < values.length; index++) {
                row.createCell(index).setCellValue(values[index]);
            }
            workbook.write(output);
            return new MockMultipartFile(
                    "file", "课程目标.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }
}
