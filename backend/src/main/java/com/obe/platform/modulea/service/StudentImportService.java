package com.obe.platform.modulea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.Student;
import com.obe.platform.modulea.entity.SysAdminClass;
import com.obe.platform.modulea.entity.SysCollege;
import com.obe.platform.modulea.entity.SysMajor;
import com.obe.platform.modulea.mapper.StudentMapper;
import com.obe.platform.modulea.mapper.SysAdminClassMapper;
import com.obe.platform.modulea.mapper.SysCollegeMapper;
import com.obe.platform.modulea.mapper.SysMajorMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StudentImportService {

    private final StudentMapper studentMapper;
    private final SysCollegeMapper collegeMapper;
    private final SysMajorMapper majorMapper;
    private final SysAdminClassMapper adminClassMapper;

    private static final String[] TEMPLATE_HEADERS = {"学号*", "姓名*", "学院*", "专业*", "入学年份*", "行政班级*"};

    public byte[] generateTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("学生名单导入");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            headerRow.setHeight((short) 400);

            // 创建标题样式
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

            // 设置列宽
            sheet.setColumnWidth(0, 15 * 256);
            sheet.setColumnWidth(1, 15 * 256);
            sheet.setColumnWidth(2, 20 * 256);
            sheet.setColumnWidth(3, 20 * 256);
            sheet.setColumnWidth(4, 12 * 256);
            sheet.setColumnWidth(5, 20 * 256);

            // 填充标题
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 创建示例行
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("2024010121");
            exampleRow.createCell(1).setCellValue("张三");
            exampleRow.createCell(2).setCellValue("计算机科学与技术学院");
            exampleRow.createCell(3).setCellValue("计算机科学与技术");
            exampleRow.createCell(4).setCellValue(2024);
            exampleRow.createCell(5).setCellValue("计算机科学与技术2401班");

            // 添加说明行
            int noteRow = 3;
            Row noteHeader = sheet.createRow(noteRow++);
            noteHeader.createCell(0).setCellValue("填写说明：");

            String[] notes = {
                "1. 所有字段均为必填项",
                "2. 学号不能重复，否则将报错",
                "3. 学院、专业、行政班级填写名称，系统会自动匹配",
                "4. 入学年份填写数字，如：2024",
                "5. 示例行仅供参考，导入时请删除",
                "6. 一次最多可导入 1000 条学生记录"
            };

            for (String note : notes) {
                Row row = sheet.createRow(noteRow++);
                row.createCell(0).setCellValue(note);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    @Transactional(rollbackFor = Exception.class)
    public int importStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件为空，请选择文件");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BizException("仅支持 .xlsx 格式文件");
        }

        // 获取所有学院、专业、行政班级用于名称匹配
        List<SysCollege> colleges = collegeMapper.selectList(null);
        List<SysMajor> majors = majorMapper.selectList(null);
        List<SysAdminClass> adminClasses = adminClassMapper.selectList(null);

        Map<String, Long> collegeNameToId = new HashMap<>();
        for (SysCollege college : colleges) {
            if (college.getName() != null) {
                collegeNameToId.put(college.getName().trim(), college.getId());
            }
        }

        Map<String, SysMajor> majorNameToObj = new HashMap<>();
        for (SysMajor major : majors) {
            if (major.getName() != null) {
                majorNameToObj.put(major.getName().trim(), major);
            }
        }

        Map<String, SysAdminClass> adminClassNameToObj = new HashMap<>();
        for (SysAdminClass ac : adminClasses) {
            if (ac.getClassName() != null) {
                adminClassNameToObj.put(ac.getClassName().trim(), ac);
            }
        }

        List<Student> validStudents = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> studentNosInFile = new HashSet<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // 验证表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BizException("Excel 文件格式错误：缺少表头行");
            }

            List<String> actualHeaders = new ArrayList<>();
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                String headerValue = getCellString(headerRow.getCell(i));
                actualHeaders.add(headerValue == null ? "" : headerValue.replace("*", "").trim());
            }

            List<String> expectedHeaders = new ArrayList<>();
            for (String h : TEMPLATE_HEADERS) {
                expectedHeaders.add(h.replace("*", "").trim());
            }

            if (!actualHeaders.equals(expectedHeaders)) {
                throw new BizException("Excel 模板表头不正确，请重新下载模板。\n期望表头：" + String.join("、", expectedHeaders) +
                    "\n实际表头：" + String.join("、", actualHeaders));
            }

            // 检查学号是否重复（数据库中已存在）
            List<Student> existingStudents = studentMapper.selectList(null);
            Set<String> existingStudentNos = new HashSet<>();
            for (Student s : existingStudents) {
                existingStudentNos.add(s.getStudentNo());
            }

            // 解析数据行（从第2行开始，第1行是示例）
            int maxRows = Math.min(sheet.getLastRowNum() + 1, 1002); // 最多导入1000条 + 表头行
            if (maxRows <= 1) {
                throw new BizException("文件中没有可导入的学生数据");
            }
            if (maxRows > 1001) { // 1行表头 + 1000行数据
                throw new BizException("单次最多允许导入 1000 条数据，当前文件包含 " + (sheet.getLastRowNum() + 1) + " 条");
            }
            for (int r = 1; r < maxRows; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // 跳过空行
                boolean isGhostRow = true;
                // 遍历该行的所有有效列（比如 0 到 5 列）
                for (int c = 0; c < 6; c++) {
                    String cellValue = getCellString(row.getCell(c));
                    if (cellValue != null && !cellValue.isBlank()) {
                        isGhostRow = false; // 只要有一个格子有字，这就不是幽灵行，是用户填的数据
                        break;
                    }
                }

                // 如果整行连一个字都没有，直接跳过，不要给用户报错
                if (isGhostRow) {
                    continue;
                }

                Student student = new Student();
                List<String> rowErrors = new ArrayList<>();
                int rowNum = r + 1;

                // 学号（必填）
                String studentNo = getCellString(row.getCell(0));
                if (studentNo == null || studentNo.isBlank()) {
                    rowErrors.add("学号不能为空");
                } else {
                    studentNo = studentNo.trim();
                    if (studentNosInFile.contains(studentNo)) {
                        rowErrors.add("学号「" + studentNo + "」在文件中重复");
                    } else if (existingStudentNos.contains(studentNo)) {
                        rowErrors.add("学号「" + studentNo + "」已存在");
                    } else {
                        studentNosInFile.add(studentNo);
                        student.setStudentNo(studentNo);
                    }
                }

                // 姓名（必填）
                String name = getCellString(row.getCell(1));
                if (name == null || name.isBlank()) {
                    rowErrors.add("姓名不能为空");
                } else {
                    student.setName(name.trim());
                }

                // 学院（必填）
                String collegeName = getCellString(row.getCell(2));
                Long collegeId = null;
                if (collegeName == null || collegeName.isBlank()) {
                    rowErrors.add("学院不能为空");
                } else {
                    collegeName = collegeName.trim();
                    collegeId = collegeNameToId.get(collegeName);
                    if (collegeId == null) {
                        rowErrors.add("学院「" + collegeName + "」不存在");
                    } else {
                        student.setCollegeId(collegeId);
                    }
                }

                // 专业（必填）且必须属于所选学院
                String majorName = getCellString(row.getCell(3));
                if (majorName == null || majorName.isBlank()) {
                    rowErrors.add("专业不能为空");
                } else {
                    majorName = majorName.trim();
                    SysMajor major = majorNameToObj.get(majorName);
                    if (major == null) {
                        rowErrors.add("专业「" + majorName + "」不存在");
                    } else if (collegeId != null && !major.getCollegeId().equals(collegeId)) {
                        rowErrors.add("专业「" + majorName + "」不属于学院「" + collegeName + "」");
                    } else {
                        student.setMajorId(major.getId());
                    }
                }

                // 入学年份（必填）
                String enrollmentYearStr = getCellString(row.getCell(4));
                if (enrollmentYearStr == null || enrollmentYearStr.isBlank()) {
                    rowErrors.add("入学年份不能为空");
                } else {
                    try {
                        int year = Integer.parseInt(enrollmentYearStr.trim());
                        if (year < 2000 || year > 2100) {
                            rowErrors.add("入学年份必须在 2000-2100 之间");
                        } else {
                            student.setEnrollmentYear(year);
                        }
                    } catch (NumberFormatException e) {
                        rowErrors.add("入学年份必须是数字");
                    }
                }

                // 行政班级（必填）且必须属于所选专业
                String adminClassName = getCellString(row.getCell(5));
                if (adminClassName == null || adminClassName.isBlank()) {
                    rowErrors.add("行政班级不能为空");
                } else {
                    adminClassName = adminClassName.trim();
                    SysAdminClass adminClass = adminClassNameToObj.get(adminClassName);
                    if (adminClass == null) {
                        rowErrors.add("行政班级「" + adminClassName + "」不存在");
                    } else if (student.getMajorId() != null && !adminClass.getMajorId().equals(student.getMajorId())) {
                        rowErrors.add("行政班级「" + adminClassName + "」不属于所选专业");
                    } else {
                        student.setAdminClassId(adminClass.getId());
                    }
                }

                if (!rowErrors.isEmpty()) {
                    errors.add("第" + rowNum + "行：" + String.join("；", rowErrors));
                } else {
                    validStudents.add(student);
                }
            }

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("解析 Excel 文件失败: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new BizException("导入失败，共 " + errors.size() + " 处问题，请修改后重新上传：\n" + String.join("\n", errors));
        }

        if (validStudents.isEmpty()) {
            throw new BizException("文件中没有可导入的学生数据");
        }

        if (validStudents.size() > 1000) {
            throw new BizException("一次最多可导入 1000 条学生记录");
        }

        // 批量插入
        int imported = 0;
        for (Student student : validStudents) {
            studentMapper.insert(student);
            imported++;
        }

        return imported;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double value = cell.getNumericCellValue();
                // 如果是整数，去掉小数点
                if (value == (long) value) {
                    yield String.valueOf((long) value);
                } else {
                    yield String.valueOf(value);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> null;
        };
    }
}
