package com.obe.platform.modulea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeachingClassImportService {

    private static final String SHEET_NAME = "教学班级导入";
    private static final String[] TEMPLATE_HEADERS = {"班级名称", "课程名称", "主讲教师用户名", "开课学期"};
    private static final int MAX_IMPORT_ROWS = 1000;

    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDictSemesterMapper semesterMapper;
    private final TeachingClassMapper teachingClassMapper;

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            Row headerRow = sheet.createRow(0);
            headerRow.setHeight((short) 420);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            int[] columnWidths = {26, 26, 20, 20};
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                sheet.setColumnWidth(i, columnWidths[i] * 256);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, TEMPLATE_HEADERS.length - 1));

            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("数据结构-2025秋-1班");
            sample.createCell(1).setCellValue("数据结构");
            sample.createCell(2).setCellValue("teacher_wang");
            sample.createCell(3).setCellValue("2025-2026-1");

            Sheet noteSheet = workbook.createSheet("填写说明");
            String[] notes = {
                    "请从第2行开始填写教学班级数据，第1行表头和第1个工作表名称不要修改。",
                    "班级名称：必填，同一课程和同一学期下不能重复，例如：数据结构-2025秋-1班。",
                    "课程名称：必填，填写课程体系中已经存在的课程名称；如果系统中有重名课程，请先在课程体系中处理重名。",
                    "主讲教师用户名：必填，填写系统用户管理中角色为主讲教师的用户名，例如：teacher_wang。",
                    "开课学期：必填，填写系统学期字典中的学期标签，例如：2025-2026-1。",
                    "系统会跳过完全空白的行；单次最多导入1000条数据。"
            };
            for (int i = 0; i < notes.length; i++) {
                noteSheet.createRow(i).createCell(0).setCellValue(notes[i]);
            }
            noteSheet.setColumnWidth(0, 90 * 256);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new BizException("生成教学班级导入模板失败：" + exception.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int importTeachingClasses(MultipartFile file) {
        validateFile(file);

        List<TeachingClass> validClasses = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> classKeysInFile = new HashSet<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BizException("Excel文件中没有工作表，请重新下载教学班级导入模板后填写。");
            }

            validateSheetName(sheet);
            validateHeaders(sheet.getRow(0));

            Map<String, Course> courseByName = buildCourseMap(courseMapper.selectList(null));
            Set<String> duplicatedCourseNames = findDuplicatedCourseNames(courseMapper.selectList(null));
            List<SysUser> users = userMapper.selectList(null);
            Map<String, SysUser> teacherByUsername = buildTeacherMap(users);
            Set<Long> nonTeacherUserIds = findNonTeacherUserIds(users);
            Map<String, SysDictSemester> semesterByLabel = buildSemesterMap(semesterMapper.selectList(null));
            Set<String> duplicatedSemesterLabels = findDuplicatedSemesterLabels(semesterMapper.selectList(null));
            Set<String> existingClassKeys = buildExistingClassKeys(teachingClassMapper.selectList(null));

            int dataRows = 0;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (isBlankRow(row)) {
                    continue;
                }
                dataRows++;
                if (dataRows > MAX_IMPORT_ROWS) {
                    throw new BizException("单次最多导入1000条教学班级数据，请拆分文件后重新上传。");
                }
                parseRow(row, r + 1, courseByName, duplicatedCourseNames, teacherByUsername, nonTeacherUserIds,
                        semesterByLabel, duplicatedSemesterLabels, existingClassKeys, classKeysInFile,
                        validClasses, errors);
            }
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException("解析Excel文件失败：" + exception.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new BizException("导入失败，共 " + errors.size() + " 处问题，请修改后重新上传：\n"
                    + String.join("\n", errors));
        }
        if (validClasses.isEmpty()) {
            throw new BizException("文件中没有可导入的教学班级数据，请从第2行开始填写。");
        }

        int imported = 0;
        for (TeachingClass teachingClass : validClasses) {
            teachingClassMapper.insert(teachingClass);
            imported++;
        }
        return imported;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件为空，请选择教学班级导入Excel文件。");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BizException("仅支持.xlsx格式文件，请下载教学班级导入模板后填写。");
        }
    }

    private void validateSheetName(Sheet sheet) {
        if (!SHEET_NAME.equals(sheet.getSheetName())) {
            throw new BizException("模板格式不正确，请重新下载教学班级导入模板后填写。第1个工作表应为「"
                    + SHEET_NAME + "」，实际为「" + sheet.getSheetName()
                    + "」，请不要修改工作表名称或使用其他Excel文件。");
        }
    }

    private void validateHeaders(Row headerRow) {
        if (headerRow == null) {
            throw new BizException("模板格式不正确，请重新下载教学班级导入模板。缺少第1行表头。");
        }
        int maxColumns = Math.max(TEMPLATE_HEADERS.length, Math.max(0, headerRow.getLastCellNum()));
        for (int i = 0; i < maxColumns; i++) {
            String actual = getCellString(headerRow.getCell(i));
            if (i >= TEMPLATE_HEADERS.length) {
                if (actual != null && !actual.isBlank()) {
                    throw new BizException("模板格式不正确，请重新下载教学班级导入模板。第1行第"
                            + (i + 1) + "列多出表头「" + actual.trim() + "」，请删除该列。");
                }
                continue;
            }
            String expected = TEMPLATE_HEADERS[i];
            if (!expected.equals(actual == null ? "" : actual.trim())) {
                throw new BizException("模板格式不正确，请重新下载教学班级导入模板。第1行第"
                        + (i + 1) + "列应为「" + expected + "」，实际为「"
                        + (actual == null || actual.isBlank() ? "空" : actual.trim()) + "」。");
            }
        }
    }

    private void parseRow(Row row,
                          int rowNumber,
                          Map<String, Course> courseByName,
                          Set<String> duplicatedCourseNames,
                          Map<String, SysUser> teacherByUsername,
                          Set<Long> nonTeacherUserIds,
                          Map<String, SysDictSemester> semesterByLabel,
                          Set<String> duplicatedSemesterLabels,
                          Set<String> existingClassKeys,
                          Set<String> classKeysInFile,
                          List<TeachingClass> validClasses,
                          List<String> errors) {
        List<String> rowErrors = new ArrayList<>();
        String className = normalize(getCellString(row.getCell(0)));
        String courseName = normalize(getCellString(row.getCell(1)));
        String teacherUsername = normalize(getCellString(row.getCell(2)));
        String semesterLabel = normalize(getCellString(row.getCell(3)));

        if (className == null) {
            rowErrors.add("班级名称不能为空，请填写如「数据结构-2025秋-1班」");
        }

        Course course = null;
        if (courseName == null) {
            rowErrors.add("课程名称不能为空，请填写课程体系中已有的课程名称");
        } else if (duplicatedCourseNames.contains(courseName)) {
            rowErrors.add("系统中存在多个同名课程「" + courseName + "」，请先在课程体系中处理重名");
        } else {
            course = courseByName.get(courseName);
            if (course == null) {
                rowErrors.add("课程名称「" + courseName + "」不存在，请先在课程体系中新增或修改课程名称");
            }
        }

        SysUser teacher = null;
        if (teacherUsername == null) {
            rowErrors.add("主讲教师用户名不能为空，请填写用户管理中角色为主讲教师的用户名");
        } else {
            teacher = teacherByUsername.get(teacherUsername);
            if (teacher == null) {
                rowErrors.add("主讲教师用户名「" + teacherUsername + "」不存在，请先在用户管理中新增教师用户");
            } else if (nonTeacherUserIds.contains(teacher.getId())) {
                rowErrors.add("用户「" + teacherUsername + "」不是主讲教师角色，请更换为教师账号");
            } else if (teacher.getStatus() != null && teacher.getStatus() != 1) {
                rowErrors.add("主讲教师用户「" + teacherUsername + "」已停用，请启用后再导入或更换教师");
            }
        }

        SysDictSemester semester = null;
        if (semesterLabel == null) {
            rowErrors.add("开课学期不能为空，请填写学期字典中的学期标签");
        } else if (duplicatedSemesterLabels.contains(semesterLabel)) {
            rowErrors.add("系统中存在多个同名学期「" + semesterLabel + "」，请先在学期字典中处理重名");
        } else {
            semester = semesterByLabel.get(semesterLabel);
            if (semester == null) {
                rowErrors.add("开课学期「" + semesterLabel + "」不存在，请先在学期字典中新增或修改学期标签");
            }
        }

        if (className != null && course != null && semester != null) {
            String classKey = classKey(course.getId(), semester.getId(), className);
            if (existingClassKeys.contains(classKey)) {
                rowErrors.add("课程「" + courseName + "」在学期「" + semesterLabel + "」下已存在教学班级「"
                        + className + "」，请修改班级名称");
            } else if (!classKeysInFile.add(classKey)) {
                rowErrors.add("文件中重复填写了课程「" + courseName + "」、学期「" + semesterLabel
                        + "」下的教学班级「" + className + "」，请删除重复行");
            }
        }

        if (!rowErrors.isEmpty()) {
            errors.add("第" + rowNumber + "行：" + String.join("；", rowErrors));
            return;
        }

        TeachingClass teachingClass = new TeachingClass();
        teachingClass.setClassName(className);
        teachingClass.setCourseId(course.getId());
        teachingClass.setTeacherId(teacher.getId());
        teachingClass.setSemesterId(semester.getId());
        validClasses.add(teachingClass);
    }

    private Map<String, Course> buildCourseMap(List<Course> courses) {
        Map<String, Course> courseByName = new HashMap<>();
        for (Course course : courses) {
            String name = normalize(course.getName());
            if (name != null) {
                courseByName.putIfAbsent(name, course);
            }
        }
        return courseByName;
    }

    private Set<String> findDuplicatedCourseNames(List<Course> courses) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicated = new HashSet<>();
        for (Course course : courses) {
            String name = normalize(course.getName());
            if (name != null && !seen.add(name)) {
                duplicated.add(name);
            }
        }
        return duplicated;
    }

    private Map<String, SysUser> buildTeacherMap(List<SysUser> users) {
        Map<String, SysUser> userByUsername = new HashMap<>();
        for (SysUser user : users) {
            String username = normalize(user.getUsername());
            if (username != null) {
                userByUsername.putIfAbsent(username, user);
            }
        }
        return userByUsername;
    }

    private Set<Long> findNonTeacherUserIds(List<SysUser> users) {
        Set<Long> nonTeacherIds = new HashSet<>();
        Map<Long, SysRole> roles = new HashMap<>();
        for (SysRole role : roleMapper.selectList(null)) {
            roles.put(role.getId(), role);
        }
        for (SysUser user : users) {
            SysRole role = roles.get(user.getRoleId());
            if (role == null || !"TEACHER".equals(role.getRoleCode())) {
                nonTeacherIds.add(user.getId());
            }
        }
        return nonTeacherIds;
    }

    private Map<String, SysDictSemester> buildSemesterMap(List<SysDictSemester> semesters) {
        Map<String, SysDictSemester> semesterByLabel = new HashMap<>();
        for (SysDictSemester semester : semesters) {
            String label = normalize(semester.getLabel());
            if (label != null) {
                semesterByLabel.putIfAbsent(label, semester);
            }
        }
        return semesterByLabel;
    }

    private Set<String> findDuplicatedSemesterLabels(List<SysDictSemester> semesters) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicated = new HashSet<>();
        for (SysDictSemester semester : semesters) {
            String label = normalize(semester.getLabel());
            if (label != null && !seen.add(label)) {
                duplicated.add(label);
            }
        }
        return duplicated;
    }

    private Set<String> buildExistingClassKeys(List<TeachingClass> existingClasses) {
        Set<String> existingKeys = new HashSet<>();
        for (TeachingClass teachingClass : existingClasses) {
            String className = normalize(teachingClass.getClassName());
            if (teachingClass.getCourseId() != null && teachingClass.getSemesterId() != null && className != null) {
                existingKeys.add(classKey(teachingClass.getCourseId(), teachingClass.getSemesterId(), className));
            }
        }
        return existingKeys;
    }

    private boolean isBlankRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
            String value = getCellString(row.getCell(i));
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String classKey(Long courseId, Long semesterId, String className) {
        return courseId + "|" + semesterId + "|" + className;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String getCellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return new DataFormatter().formatCellValue(cell);
    }
}
