package com.obe.platform.modulea.service;

import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.SysAdminClass;
import com.obe.platform.modulea.entity.SysMajor;
import com.obe.platform.modulea.mapper.SysAdminClassMapper;
import com.obe.platform.modulea.mapper.SysMajorMapper;
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
public class AdminClassImportService {

    private static final String[] TEMPLATE_HEADERS = {"班级名称", "所属专业", "入学年份"};
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final int MIN_ENROLLMENT_YEAR = 2010;
    private static final int MAX_ENROLLMENT_YEAR = 2030;

    private final SysMajorMapper majorMapper;
    private final SysAdminClassMapper adminClassMapper;

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("行政班级导入");
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

            int[] columnWidths = {24, 24, 14};
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                sheet.setColumnWidth(i, columnWidths[i] * 256);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            Sheet noteSheet = workbook.createSheet("填写说明");
            String[] notes = {
                    "请从第2行开始填写班级数据，第一行表头不要修改。",
                    "班级名称：必填，同一专业下不能重复，例如：软工2401。",
                    "所属专业：必填，填写系统中已经存在的专业名称。",
                    "入学年份：必填，填写2010-2030之间的数字，例如：2024。",
                    "系统会跳过完全空白的行；单次最多导入1000条数据。"
            };
            for (int i = 0; i < notes.length; i++) {
                noteSheet.createRow(i).createCell(0).setCellValue(notes[i]);
            }
            noteSheet.setColumnWidth(0, 70 * 256);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new BizException("生成班级导入模板失败：" + exception.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int importAdminClasses(MultipartFile file) {
        validateFile(file);

        List<SysAdminClass> validClasses = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> classKeysInFile = new HashSet<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BizException("Excel文件中没有工作表，请重新下载班级导入模板后填写。");
            }

            validateHeaders(sheet.getRow(0));
            List<SysMajor> majors = majorMapper.selectList(null);
            List<SysAdminClass> existingClasses = adminClassMapper.selectList(null);
            Map<String, SysMajor> majorByName = buildMajorMap(majors);
            Set<String> duplicatedMajorNames = findDuplicatedMajorNames(majors);
            Set<String> existingClassKeys = buildExistingClassKeys(existingClasses);
            int lastRowNum = sheet.getLastRowNum();
            int dataRows = 0;

            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (isBlankRow(row)) {
                    continue;
                }
                dataRows++;
                if (dataRows > MAX_IMPORT_ROWS) {
                    throw new BizException("单次最多导入1000条班级数据，请拆分文件后重新上传。");
                }
                parseRow(row, r + 1, majorByName, duplicatedMajorNames,
                        existingClassKeys, classKeysInFile, validClasses, errors);
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
            throw new BizException("文件中没有可导入的班级数据，请从第2行开始填写。");
        }

        int imported = 0;
        for (SysAdminClass adminClass : validClasses) {
            adminClassMapper.insert(adminClass);
            imported++;
        }
        return imported;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件为空，请选择班级导入Excel文件。");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BizException("仅支持.xlsx格式文件，请下载班级导入模板后填写。");
        }
    }

    private void validateHeaders(Row headerRow) {
        if (headerRow == null) {
            throw new BizException("模板格式不正确，请重新下载班级导入模板。缺少第1行表头。");
        }
        int maxColumns = Math.max(TEMPLATE_HEADERS.length, Math.max(0, headerRow.getLastCellNum()));
        for (int i = 0; i < maxColumns; i++) {
            String actual = getCellString(headerRow.getCell(i));
            if (i >= TEMPLATE_HEADERS.length) {
                if (actual != null && !actual.isBlank()) {
                    throw new BizException("模板格式不正确，请重新下载班级导入模板。第1行第"
                            + (i + 1) + "列多出表头「" + actual.trim() + "」，请删除该列。");
                }
                continue;
            }
            String expected = TEMPLATE_HEADERS[i];
            if (!expected.equals(actual == null ? "" : actual.trim())) {
                throw new BizException("模板格式不正确，请重新下载班级导入模板。第1行第"
                        + (i + 1) + "列应为「" + expected + "」，实际为「"
                        + (actual == null || actual.isBlank() ? "空" : actual.trim()) + "」。");
            }
        }
    }

    private void parseRow(Row row,
                          int rowNumber,
                          Map<String, SysMajor> majorByName,
                          Set<String> duplicatedMajorNames,
                          Set<String> existingClassKeys,
                          Set<String> classKeysInFile,
                          List<SysAdminClass> validClasses,
                          List<String> errors) {
        List<String> rowErrors = new ArrayList<>();
        String className = normalize(getCellString(row.getCell(0)));
        String majorName = normalize(getCellString(row.getCell(1)));
        String enrollmentYearText = normalize(getCellString(row.getCell(2)));

        if (className == null) {
            rowErrors.add("班级名称不能为空，请填写如「软工2401」");
        }

        SysMajor major = null;
        if (majorName == null) {
            rowErrors.add("所属专业不能为空，请填写系统中已有的专业名称");
        } else if (duplicatedMajorNames.contains(majorName)) {
            rowErrors.add("系统中存在多个同名专业「" + majorName + "」，请先在专业管理中处理重名");
        } else {
            major = majorByName.get(majorName);
            if (major == null) {
                rowErrors.add("所属专业「" + majorName + "」不存在，请先在专业管理中新增或修改专业名称");
            }
        }

        Integer enrollmentYear = parseEnrollmentYear(enrollmentYearText, rowErrors);

        if (className != null && major != null) {
            String classKey = classKey(major.getId(), className);
            if (existingClassKeys.contains(classKey)) {
                rowErrors.add("专业「" + majorName + "」下已存在班级「" + className + "」，请修改班级名称");
            } else if (!classKeysInFile.add(classKey)) {
                rowErrors.add("文件中重复填写了专业「" + majorName + "」下的班级「" + className + "」，请删除重复行");
            }
        }

        if (!rowErrors.isEmpty()) {
            errors.add("第" + rowNumber + "行：" + String.join("；", rowErrors));
            return;
        }

        SysAdminClass adminClass = new SysAdminClass();
        adminClass.setClassName(className);
        adminClass.setMajorId(major.getId());
        adminClass.setEnrollmentYear(enrollmentYear);
        validClasses.add(adminClass);
    }

    private Integer parseEnrollmentYear(String value, List<String> rowErrors) {
        if (value == null) {
            rowErrors.add("入学年份不能为空，请填写2010-2030之间的数字");
            return null;
        }
        try {
            int year = Integer.parseInt(value);
            if (year < MIN_ENROLLMENT_YEAR || year > MAX_ENROLLMENT_YEAR) {
                rowErrors.add("入学年份必须在2010-2030之间，请修改为正确年份");
                return null;
            }
            return year;
        } catch (NumberFormatException exception) {
            rowErrors.add("入学年份必须是数字，请填写如「2024」");
            return null;
        }
    }

    private Map<String, SysMajor> buildMajorMap(List<SysMajor> majors) {
        Map<String, SysMajor> majorByName = new HashMap<>();
        for (SysMajor major : majors) {
            String name = normalize(major.getName());
            if (name != null) {
                majorByName.putIfAbsent(name, major);
            }
        }
        return majorByName;
    }

    private Set<String> findDuplicatedMajorNames(List<SysMajor> majors) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicated = new HashSet<>();
        for (SysMajor major : majors) {
            String name = normalize(major.getName());
            if (name != null && !seen.add(name)) {
                duplicated.add(name);
            }
        }
        return duplicated;
    }

    private Set<String> buildExistingClassKeys(List<SysAdminClass> existingClasses) {
        Set<String> existingKeys = new HashSet<>();
        for (SysAdminClass adminClass : existingClasses) {
            String className = normalize(adminClass.getClassName());
            if (adminClass.getMajorId() != null && className != null) {
                existingKeys.add(classKey(adminClass.getMajorId(), className));
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

    private String classKey(Long majorId, String className) {
        return majorId + "|" + className;
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
