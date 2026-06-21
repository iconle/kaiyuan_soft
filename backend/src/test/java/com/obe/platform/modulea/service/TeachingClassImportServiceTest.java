package com.obe.platform.modulea.service;

import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.Course;
import com.obe.platform.modulea.entity.SysDictSemester;
import com.obe.platform.modulea.entity.SysRole;
import com.obe.platform.modulea.entity.SysUser;
import com.obe.platform.modulea.entity.TeachingClass;
import com.obe.platform.modulea.mapper.CourseMapper;
import com.obe.platform.modulea.mapper.SysDictSemesterMapper;
import com.obe.platform.modulea.mapper.SysRoleMapper;
import com.obe.platform.modulea.mapper.SysUserMapper;
import com.obe.platform.modulea.mapper.TeachingClassMapper;
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
class TeachingClassImportServiceTest {

    @Mock
    private CourseMapper courseMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysDictSemesterMapper semesterMapper;
    @Mock
    private TeachingClassMapper teachingClassMapper;

    private TeachingClassImportService service;

    @BeforeEach
    void setUp() {
        service = new TeachingClassImportService(
                courseMapper, userMapper, roleMapper, semesterMapper, teachingClassMapper);
    }

    @Test
    void templateUsesStrictTeachingClassHeaders() throws Exception {
        byte[] data = service.generateTemplate();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("教学班级导入");
            var header = workbook.getSheetAt(0).getRow(0);

            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("班级名称");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("课程名称");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("主讲教师用户名");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("开课学期");
        }
    }

    @Test
    void rejectsChangedTemplateHeaderWithActionableMessage() throws Exception {
        MockMultipartFile file = workbook("教学班级导入",
                "错误表头", "课程名称", "主讲教师用户名", "开课学期",
                "数据结构-2025秋-1班", "数据结构", "teacher_wang", "2025-2026-1");

        assertThatThrownBy(() -> service.importTeachingClasses(file))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("模板格式不正确")
                .hasMessageContaining("第1行第1列应为「班级名称」");
    }

    @Test
    void rejectsChangedSheetNameWithActionableMessage() throws Exception {
        MockMultipartFile file = workbook("随便改名",
                "班级名称", "课程名称", "主讲教师用户名", "开课学期",
                "数据结构-2025秋-1班", "数据结构", "teacher_wang", "2025-2026-1");

        assertThatThrownBy(() -> service.importTeachingClasses(file))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("第1个工作表应为「教学班级导入」")
                .hasMessageContaining("请不要修改工作表名称");
    }

    @Test
    void rejectsLegacyXlsFileWithActionableMessage() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "教学班级导入.xls",
                "application/vnd.ms-excel",
                new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.importTeachingClasses(file))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不支持.xls旧版Excel格式")
                .hasMessageContaining("保存为.xlsx后上传");
    }

    @Test
    void importsValidTeachingClassesAfterValidation() throws Exception {
        Course course = new Course();
        course.setId(2L);
        course.setName("数据结构");

        SysUser teacher = new SysUser();
        teacher.setId(4L);
        teacher.setUsername("teacher_wang");
        teacher.setRoleId(8L);
        teacher.setStatus(1);

        SysRole teacherRole = new SysRole();
        teacherRole.setId(8L);
        teacherRole.setRoleCode("TEACHER");

        SysDictSemester semester = new SysDictSemester();
        semester.setId(3L);
        semester.setLabel("2025-2026-1");

        when(courseMapper.selectList(any())).thenReturn(List.of(course));
        when(userMapper.selectList(any())).thenReturn(List.of(teacher));
        when(roleMapper.selectList(any())).thenReturn(List.of(teacherRole));
        when(semesterMapper.selectList(any())).thenReturn(List.of(semester));
        when(teachingClassMapper.selectList(any())).thenReturn(List.of());

        MockMultipartFile file = workbook("教学班级导入",
                "班级名称", "课程名称", "主讲教师用户名", "开课学期",
                "数据结构-2025秋-1班", "数据结构", "teacher_wang", "2025-2026-1");

        assertThat(service.importTeachingClasses(file)).isEqualTo(1);
        verify(teachingClassMapper).insert(any(TeachingClass.class));
    }

    private MockMultipartFile workbook(String sheetName,
                                       String h1, String h2, String h3, String h4,
                                       String v1, String v2, String v3, String v4) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(sheetName);
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue(h1);
            header.createCell(1).setCellValue(h2);
            header.createCell(2).setCellValue(h3);
            header.createCell(3).setCellValue(h4);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(v1);
            row.createCell(1).setCellValue(v2);
            row.createCell(2).setCellValue(v3);
            row.createCell(3).setCellValue(v4);
            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "教学班级导入.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }
}
