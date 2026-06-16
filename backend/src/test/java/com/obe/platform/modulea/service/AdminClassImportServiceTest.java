package com.obe.platform.modulea.service;

import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.SysAdminClass;
import com.obe.platform.modulea.entity.SysMajor;
import com.obe.platform.modulea.mapper.SysAdminClassMapper;
import com.obe.platform.modulea.mapper.SysMajorMapper;
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
class AdminClassImportServiceTest {

    @Mock
    private SysMajorMapper majorMapper;

    @Mock
    private SysAdminClassMapper adminClassMapper;

    private AdminClassImportService service;

    @BeforeEach
    void setUp() {
        service = new AdminClassImportService(majorMapper, adminClassMapper);
    }

    @Test
    void templateUsesStrictAdminClassHeaders() throws Exception {
        byte[] data = service.generateTemplate();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            var header = workbook.getSheetAt(0).getRow(0);

            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("班级名称");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("所属专业");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("入学年份");
        }
    }

    @Test
    void rejectsChangedTemplateHeaderWithActionableMessage() throws Exception {
        MockMultipartFile file = workbook("错误表头", "所属专业", "入学年份", "软工2401", "软件工程", "2024");

        assertThatThrownBy(() -> service.importAdminClasses(file))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("模板格式不正确")
                .hasMessageContaining("第1行第1列应为「班级名称」");
    }

    @Test
    void importsValidAdminClassesAfterValidation() throws Exception {
        SysMajor major = new SysMajor();
        major.setId(3L);
        major.setName("软件工程");
        when(majorMapper.selectList(any())).thenReturn(List.of(major));
        when(adminClassMapper.selectList(any())).thenReturn(List.of());

        MockMultipartFile file = workbook("班级名称", "所属专业", "入学年份", "软工2401", "软件工程", "2024");

        assertThat(service.importAdminClasses(file)).isEqualTo(1);
        verify(adminClassMapper).insert(any(SysAdminClass.class));
    }

    private MockMultipartFile workbook(String h1, String h2, String h3,
                                       String v1, String v2, String v3) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("行政班级导入");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue(h1);
            header.createCell(1).setCellValue(h2);
            header.createCell(2).setCellValue(h3);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(v1);
            row.createCell(1).setCellValue(v2);
            row.createCell(2).setCellValue(v3);
            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "行政班级导入.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }
}
