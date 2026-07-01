package com.obe.platform.modulea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.Course;
import com.obe.platform.modulea.entity.GradRequirement;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.modulea.entity.MacroSupportMatrix;
import com.obe.platform.modulea.mapper.CourseMapper;
import com.obe.platform.modulea.mapper.GradRequirementMapper;
import com.obe.platform.modulea.mapper.IndicatorMapper;
import com.obe.platform.modulea.mapper.MacroSupportMatrixMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 课程支撑（宏观支撑矩阵）批量导入服务。
 * <p>
 * 与专业负责人已使用的「指标点导入模板」保持一致的使用体验与校验规则：
 * 下载标准模板 → 填写 → 上传导入。导入时严格校验上传文件的表头与字段，
 * 任何不一致都会收集成逐条、可操作的错误信息一次性反馈给用户，避免反复试错。
 */
@Service
@RequiredArgsConstructor
public class MacroMatrixImportService {

    private final MacroSupportMatrixMapper macroSupportMatrixMapper;
    private final CourseMapper courseMapper;
    private final IndicatorMapper indicatorMapper;
    private final GradRequirementMapper gradRequirementMapper;

    /** 模板表头，顺序与列名固定，导入时必须完全一致 */
    private static final String[] HEADERS = {"课程编号", "课程名称", "指标点编号", "支撑强度(H/M/L)", "支撑权重"};
    private static final String SHEET_NAME = "课程支撑导入";

    /**
     * 生成课程支撑导入模板。
     * <p>
     * 会预填当前专业下已存在的课程支撑关系（课程编号、课程名称、指标点编号、支撑强度、支撑权重），
     * 方便用户在现有数据上增删改；另附「填写说明」sheet，列出当前专业可选的课程编号与指标点编号。
     */
    public byte[] generateTemplate(Long majorId) {
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getMajorId, majorId));
        Map<Long, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        List<Indicator> indicators = loadIndicators(majorId);
        Map<Long, Indicator> indicatorMap = indicators.stream()
                .collect(Collectors.toMap(Indicator::getId, i -> i));

        List<MacroSupportMatrix> existing = courses.isEmpty() ? Collections.emptyList() :
                macroSupportMatrixMapper.selectList(
                        new LambdaQueryWrapper<MacroSupportMatrix>()
                                .in(MacroSupportMatrix::getCourseId, courseMap.keySet()));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Workbook workbook = new XSSFWorkbook()) {

            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle hintStyle = buildHintStyle(workbook);
            CellStyle dataStyle = buildDataStyle(workbook);

            Sheet sheet = workbook.createSheet(SHEET_NAME);

            // 第 0 行：表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            // 预填已存在的课程支撑关系
            int rowIdx = 1;
            for (MacroSupportMatrix m : existing) {
                Course course = courseMap.get(m.getCourseId());
                Indicator ind = indicatorMap.get(m.getIndicatorId());
                if (course == null || ind == null) continue;
                Row row = sheet.createRow(rowIdx++);
                Cell c0 = row.createCell(0);
                c0.setCellValue(course.getCode() == null ? "" : course.getCode());
                c0.setCellStyle(hintStyle);
                Cell c1 = row.createCell(1);
                c1.setCellValue(course.getName() == null ? "" : course.getName());
                c1.setCellStyle(hintStyle);
                Cell c2 = row.createCell(2);
                c2.setCellValue(ind.getIndicatorNo() == null ? "" : ind.getIndicatorNo());
                c2.setCellStyle(dataStyle);
                Cell c3 = row.createCell(3);
                c3.setCellValue(m.getSupportLevel() == null ? "" : m.getSupportLevel());
                c3.setCellStyle(dataStyle);
                Cell c4 = row.createCell(4);
                if (m.getWeight() != null) c4.setCellValue(m.getWeight().doubleValue());
                c4.setCellStyle(dataStyle);
            }
            // 追加若干空白行
            for (int k = 0; k < 3; k++) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < HEADERS.length; i++) {
                    row.createCell(i).setCellStyle(i < 2 ? hintStyle : dataStyle);
                }
            }

            sheet.setColumnWidth(0, 16 * 256);
            sheet.setColumnWidth(1, 32 * 256);
            sheet.setColumnWidth(2, 16 * 256);
            sheet.setColumnWidth(3, 18 * 256);
            sheet.setColumnWidth(4, 14 * 256);
            sheet.createFreezePane(0, 1);

            // 说明 sheet
            Sheet note = workbook.createSheet("填写说明");
            List<String> lines = new ArrayList<>();
            lines.add("课程支撑（宏观支撑矩阵）导入模板说明：");
            lines.add("1. 请勿修改第一行表头，列顺序与列名必须与模板完全一致，否则导入将报错。");
            lines.add("2. 「课程编号」必填，必须是当前专业中已存在的课程编号（参见下方可选列表）。");
            lines.add("3. 「课程名称」仅作提示，可留空，不影响导入。");
            lines.add("4. 「指标点编号」必填，必须是当前专业中已存在的指标点编号（如 3-1，参见下方可选列表）。");
            lines.add("5. 「支撑强度」必填，只能填 H、M、L 三个字母之一。");
            lines.add("6. 「支撑权重」必填，必须是 0~1 之间的数字。");
            lines.add("7. 同一课程对同一指标点只能出现一次；同一指标点下所有课程的支撑权重之和必须等于 1.00。");
            lines.add("8. 导入会整体替换当前专业的课程支撑关系，请基于下载的模板修改后再上传，避免遗漏已有数据。");
            lines.add("9. 完全空白的行会被自动跳过。");
            lines.add("");
            lines.add("当前专业可选课程编号：");
            lines.add(courses.stream()
                    .map(c -> (c.getCode() == null ? "" : c.getCode()) + " " + (c.getName() == null ? "" : c.getName()))
                    .collect(Collectors.joining("；")));
            lines.add("");
            lines.add("当前专业可选指标点编号：");
            lines.add(indicators.stream()
                    .map(Indicator::getIndicatorNo)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("、")));

            for (int i = 0; i < lines.size(); i++) {
                note.createRow(i).createCell(0).setCellValue(lines.get(i));
            }
            note.setColumnWidth(0, 90 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成课程支撑导入模板失败: " + e.getMessage());
        }
    }

    /**
     * 解析并导入课程支撑关系。
     * <p>
     * 全部行级错误会收集后一次性抛出，信息形如：
     * "导入失败，共 N 处问题：\n第3行：...\n第5行：..."
     * 校验通过后整体替换当前专业的课程支撑关系。
     */
    @Transactional
    public int importMatrix(Long majorId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件为空，请选择模板文件");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            throw new BizException("仅支持 .xlsx 格式文件，请点击「下载模板」获取正确模板");
        }

        // 当前专业的课程：课程编号 -> courseId
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getMajorId, majorId));
        if (courses.isEmpty()) {
            throw new BizException("当前专业尚未创建任何课程，请先新增课程后再导入课程支撑");
        }
        Map<String, Long> codeToCourseId = new LinkedHashMap<>();
        for (Course c : courses) {
            if (c.getCode() != null) codeToCourseId.put(c.getCode().trim(), c.getId());
        }
        String validCourseCodes = String.join("、", codeToCourseId.keySet());
        Map<Long, String> courseIdToCode = courses.stream()
                .collect(Collectors.toMap(Course::getId, c -> c.getCode() == null ? "" : c.getCode()));

        // 当前专业的指标点：指标点编号 -> indicatorId
        List<Indicator> indicators = loadIndicators(majorId);
        if (indicators.isEmpty()) {
            throw new BizException("当前专业尚未创建任何毕业要求指标点，请先配置指标点后再导入课程支撑");
        }
        Map<String, Long> noToIndicatorId = new LinkedHashMap<>();
        for (Indicator ind : indicators) {
            if (ind.getIndicatorNo() != null) noToIndicatorId.put(ind.getIndicatorNo().trim(), ind.getId());
        }
        String validIndicatorNos = String.join("、", noToIndicatorId.keySet());
        Map<Long, Indicator> indicatorIdToEntity = indicators.stream()
                .collect(Collectors.toMap(Indicator::getId, i -> i));

        List<String> errors = new ArrayList<>();
        List<MacroSupportMatrix> toInsert = new ArrayList<>();
        Set<String> fileKeys = new HashSet<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) sheet = workbook.getSheetAt(0);

            // 校验表头：必须与本服务生成的模板一致
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BizException("模板表头缺失，请点击「下载模板」获取标准模板"
                        + "（首行应为：" + String.join("、", HEADERS) + "）");
            }
            List<String> actualHeaders = new ArrayList<>();
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = headerRow.getCell(i);
                actualHeaders.add(c == null ? "" : getCellString(c).trim());
            }
            for (int i = 0; i < HEADERS.length; i++) {
                if (!HEADERS[i].equals(actualHeaders.get(i))) {
                    throw new BizException("模板格式不正确，请点击「下载模板」获取标准模板。\n"
                            + "期望表头：" + String.join("、", HEADERS) + "\n"
                            + "实际表头：" + String.join("、", actualHeaders));
                }
            }

            // 逐行解析（数据从第 1 行开始）
            int last = sheet.getLastRowNum();
            for (int r = 1; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                int dispRow = r + 1; // Excel 显示行号（含表头）

                String courseCode = getCellString(row.getCell(0)).trim();
                String courseName = getCellString(row.getCell(1)).trim();
                String indicatorNo = getCellString(row.getCell(2)).trim();
                String levelStr = getCellString(row.getCell(3)).trim();
                String weightStr = getCellString(row.getCell(4)).trim();

                // 完全空白行：跳过
                if (courseCode.isEmpty() && courseName.isEmpty() && indicatorNo.isEmpty()
                        && levelStr.isEmpty() && weightStr.isEmpty()) continue;

                if (courseCode.isEmpty()) {
                    errors.add("第" + dispRow + "行：「课程编号」不能为空（有效课程编号：" + validCourseCodes + "）");
                    continue;
                }
                Long courseId = codeToCourseId.get(courseCode);
                if (courseId == null) {
                    errors.add("第" + dispRow + "行：「课程编号 " + courseCode
                            + "」在当前专业不存在（有效课程编号：" + validCourseCodes + "）");
                    continue;
                }
                if (indicatorNo.isEmpty()) {
                    errors.add("第" + dispRow + "行：「指标点编号」不能为空（有效指标点编号：" + validIndicatorNos + "）");
                    continue;
                }
                Long indicatorId = noToIndicatorId.get(indicatorNo);
                if (indicatorId == null) {
                    errors.add("第" + dispRow + "行：「指标点编号 " + indicatorNo
                            + "」在当前专业不存在（有效指标点编号：" + validIndicatorNos + "）");
                    continue;
                }
                if (levelStr.isEmpty()) {
                    errors.add("第" + dispRow + "行：「支撑强度」不能为空（只能填 H、M、L）");
                    continue;
                }
                String levelUpper = levelStr.toUpperCase();
                if (!levelUpper.equals("H") && !levelUpper.equals("M") && !levelUpper.equals("L")) {
                    errors.add("第" + dispRow + "行：「支撑强度」必须为 H、M 或 L，实际为「" + levelStr + "」");
                    continue;
                }
                if (weightStr.isEmpty()) {
                    errors.add("第" + dispRow + "行：「支撑权重」不能为空（需为 0~1 之间的数字）");
                    continue;
                }
                BigDecimal weight;
                try {
                    weight = new BigDecimal(weightStr);
                } catch (NumberFormatException ex) {
                    errors.add("第" + dispRow + "行：「支撑权重」必须为数字，实际为「" + weightStr + "」");
                    continue;
                }
                if (weight.compareTo(BigDecimal.ZERO) < 0 || weight.compareTo(BigDecimal.ONE) > 0) {
                    errors.add("第" + dispRow + "行：「支撑权重」必须在 0~1 之间，实际为「" + weightStr + "」");
                    continue;
                }

                String key = courseId + "|" + indicatorId;
                if (!fileKeys.add(key)) {
                    errors.add("第" + dispRow + "行：课程「" + courseCode + "」对指标点「" + indicatorNo
                            + "」的支撑在本文件中重复出现");
                    continue;
                }

                MacroSupportMatrix m = new MacroSupportMatrix();
                m.setCourseId(courseId);
                m.setIndicatorId(indicatorId);
                m.setSupportLevel(levelUpper);
                m.setWeight(weight);
                toInsert.add(m);
            }

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("解析Excel文件失败: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new BizException("导入失败，共 " + errors.size() + " 处问题，请修正后重新上传：\n"
                    + String.join("\n", errors));
        }
        if (toInsert.isEmpty()) {
            throw new BizException("文件中没有可导入的课程支撑数据，请填写后再上传");
        }

        // 同一指标点下所有课程的支撑权重之和必须等于 1.00
        Map<Long, BigDecimal> sumByIndicator = new LinkedHashMap<>();
        Map<Long, List<String>> coursesByIndicator = new LinkedHashMap<>();
        for (MacroSupportMatrix m : toInsert) {
            sumByIndicator.merge(m.getIndicatorId(), m.getWeight(), BigDecimal::add);
            coursesByIndicator.computeIfAbsent(m.getIndicatorId(), k -> new ArrayList<>())
                    .add(courseIdToCode.getOrDefault(m.getCourseId(), String.valueOf(m.getCourseId())));
        }
        BigDecimal tolerance = new BigDecimal("0.01");
        for (Map.Entry<Long, BigDecimal> e : sumByIndicator.entrySet()) {
            BigDecimal sum = e.getValue();
            if (sum.subtract(BigDecimal.ONE).abs().compareTo(tolerance) > 0) {
                Indicator ind = indicatorIdToEntity.get(e.getKey());
                String indNo = ind != null ? ind.getIndicatorNo() : String.valueOf(e.getKey());
                String sumText = sum.setScale(2, RoundingMode.HALF_UP).toPlainString();
                String courseList = String.join("、", coursesByIndicator.get(e.getKey()));
                throw new BizException("指标点「" + indNo + "」的支撑权重之和为 " + sumText
                        + "，不等于 1.00，请调整以下课程的支撑权重：" + courseList);
            }
        }

        // 整体替换当前专业的课程支撑关系：先删除该专业全部课程的现有支撑，再插入新数据
        macroSupportMatrixMapper.delete(
                new LambdaQueryWrapper<MacroSupportMatrix>()
                        .in(MacroSupportMatrix::getCourseId, courseIdToCode.keySet()));
        for (MacroSupportMatrix m : toInsert) {
            m.setId(null);
            macroSupportMatrixMapper.insert(m);
        }
        return toInsert.size();
    }

    /** 查询某专业下的全部指标点（经毕业要求关联） */
    private List<Indicator> loadIndicators(Long majorId) {
        List<GradRequirement> reqs = gradRequirementMapper.selectList(
                new LambdaQueryWrapper<GradRequirement>()
                        .eq(GradRequirement::getMajorId, majorId));
        if (reqs.isEmpty()) return new ArrayList<>();
        List<Long> reqIds = reqs.stream().map(GradRequirement::getId).collect(Collectors.toList());
        return indicatorMapper.selectList(
                new LambdaQueryWrapper<Indicator>()
                        .in(Indicator::getGradReqId, reqIds));
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                if (v == Math.floor(v) && !Double.isInfinite(v)) {
                    yield String.valueOf((long) v);
                }
                yield String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception ex) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorders(style);
        return style;
    }

    private CellStyle buildHintStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorders(style);
        return style;
    }

    private CellStyle buildDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        applyBorders(style);
        return style;
    }

    private void applyBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
