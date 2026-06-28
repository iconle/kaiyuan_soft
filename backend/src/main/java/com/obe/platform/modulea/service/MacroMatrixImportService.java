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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程支撑矩阵批量导入服务。
 * <p>
 * 与「指标点导入」体验一致：下载标准模板 → 填写 → 上传。
 * 模板首列为「课程名称」，其后每个指标点占一列（表头为指标点编号），单元格填写支撑权重。
 * <p>
 * 导入只做解析并返回条目，不在本接口落库；前端将其合并进页面表格后，由用户点击
 * 「提交生效」统一保存（仍受列合计必须为 1.00 的校验约束）。
 */
@Service
@RequiredArgsConstructor
public class MacroMatrixImportService {

    private final CourseMapper courseMapper;
    private final GradRequirementMapper gradRequirementMapper;
    private final IndicatorMapper indicatorMapper;
    private final MacroSupportMatrixMapper macroSupportMatrixMapper;

    private static final String SHEET_NAME = "课程支撑矩阵";
    private static final String FIRST_HEADER = "课程名称";

    /**
     * 生成课程支撑矩阵导入模板。
     * 预填当前专业的课程名称及已有权重，支持「导出 → 编辑 → 回填」。
     */
    public byte[] generateTemplate(Long majorId) {
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getMajorId, majorId));
        List<Indicator> indicators = loadIndicatorsByMajor(majorId);
        Map<String, BigDecimal> existingWeights = loadExistingWeights(majorId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Workbook workbook = new XSSFWorkbook()) {

            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle hintStyle = buildHintStyle(workbook);
            CellStyle dataStyle = buildDataStyle(workbook);

            Sheet sheet = workbook.createSheet(SHEET_NAME);

            // 表头：课程名称 + 各指标点编号
            Row headerRow = sheet.createRow(0);
            Cell h0 = headerRow.createCell(0);
            h0.setCellValue(FIRST_HEADER);
            h0.setCellStyle(headerStyle);
            for (int i = 0; i < indicators.size(); i++) {
                Cell c = headerRow.createCell(i + 1);
                c.setCellValue(indicators.get(i).getIndicatorNo());
                c.setCellStyle(headerStyle);
            }

            // 每个课程一行：预填课程名 + 已有权重
            int rowIdx = 1;
            for (Course course : courses) {
                Row row = sheet.createRow(rowIdx++);
                Cell c0 = row.createCell(0);
                if (course.getName() != null) c0.setCellValue(course.getName());
                c0.setCellStyle(hintStyle);
                for (int i = 0; i < indicators.size(); i++) {
                    Cell c = row.createCell(i + 1);
                    String key = course.getId() + "|" + indicators.get(i).getId();
                    BigDecimal w = existingWeights.get(key);
                    if (w != null && w.signum() > 0) c.setCellValue(w.doubleValue());
                    c.setCellStyle(dataStyle);
                }
            }
            // 追加空白行
            for (int k = 0; k < 3; k++) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i <= indicators.size(); i++) {
                    row.createCell(i).setCellStyle(dataStyle);
                }
            }

            sheet.setColumnWidth(0, 28 * 256);
            for (int i = 0; i < indicators.size(); i++) sheet.setColumnWidth(i + 1, 14 * 256);
            sheet.createFreezePane(1, 1);

            // 说明 sheet
            Sheet note = workbook.createSheet("填写说明");
            String validNos = indicators.stream()
                    .map(Indicator::getIndicatorNo)
                    .collect(Collectors.joining("、"));
            String[] lines = {
                    "课程支撑矩阵导入模板说明：",
                    "1. 请勿修改第一行表头：首列必须为「课程名称」，其余各列表头为指标点编号，否则导入将报错。",
                    "2. 「课程名称」必须是当前专业中已存在的课程（参见已预填行），否则该行报错。",
                    "3. 每个指标点列下填写该课程对该指标点的支撑权重（0~1 之间的小数，如 0.3），留空表示不支撑。",
                    "4. 同一指标点所有课程的权重之和需为 1.00，可在导入后于页面核对并调整，再点击「提交生效」保存。",
                    "5. 导入为合并更新：覆盖同课程同指标点的权重、新增不存在的支撑；不会删除已有支撑。",
                    "6. 完全空白的行会被自动跳过。",
                    validNos.isEmpty() ? "" : ("当前专业指标点：" + validNos)
            };
            for (int i = 0; i < lines.length; i++) {
                if (lines[i] != null && !lines[i].isEmpty()) {
                    note.createRow(i).createCell(0).setCellValue(lines[i]);
                }
            }
            note.setColumnWidth(0, 80 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成课程支撑矩阵导入模板失败: " + e.getMessage());
        }
    }

    /**
     * 解析上传文件，返回待合并的支撑条目（不落库）。全部行级错误收集后一次性抛出。
     */
    public List<MacroSupportMatrix> parseImport(Long majorId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件为空，请选择模板文件");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            throw new BizException("仅支持 .xlsx 格式文件，请点击「下载模板」获取正确模板");
        }

        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getMajorId, majorId));
        Map<String, Long> nameToId = new LinkedHashMap<>();
        for (Course c : courses) {
            if (c.getName() != null) nameToId.put(c.getName().trim(), c.getId());
        }

        List<Indicator> indicators = loadIndicatorsByMajor(majorId);
        Map<String, Long> noToId = new LinkedHashMap<>();
        for (Indicator ind : indicators) {
            if (ind.getIndicatorNo() != null) noToId.put(ind.getIndicatorNo().trim(), ind.getId());
        }
        if (noToId.isEmpty()) {
            throw new BizException("当前专业尚未配置任何指标点，请先在「毕业要求管理」中配置指标点");
        }
        String validNos = String.join("、", noToId.keySet());

        List<String> errors = new ArrayList<>();
        List<MacroSupportMatrix> parsed = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BizException("模板表头缺失，请点击「下载模板」获取标准模板");
            }
            Cell firstCell = headerRow.getCell(0);
            String firstHeader = firstCell == null ? "" : getCellString(firstCell).trim();
            if (!FIRST_HEADER.equals(firstHeader)) {
                throw new BizException("模板格式不正确：首列应为「课程名称」，请点击「下载模板」获取标准模板");
            }

            // 建立 列号 -> 指标点Id
            Map<Integer, Long> colToIndicator = new LinkedHashMap<>();
            int lastCol = headerRow.getLastCellNum();
            for (int col = 1; col < lastCol; col++) {
                Cell c = headerRow.getCell(col);
                String h = c == null ? "" : getCellString(c).trim();
                if (h.isEmpty()) continue;
                Long indId = noToId.get(h);
                if (indId == null) {
                    errors.add("表头第" + (col + 1) + "列「" + h + "」不是当前专业的指标点编号（有效：" + validNos + "）");
                    continue;
                }
                colToIndicator.put(col, indId);
            }
            if (colToIndicator.isEmpty()) {
                throw new BizException("未识别到任何指标点列表头，请使用「下载模板」生成的标准模板（表头需为指标点编号）");
            }

            int last = sheet.getLastRowNum();
            for (int r = 1; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                int dispRow = r + 1;

                String courseName = getCellString(row.getCell(0)).trim();
                if (courseName.isEmpty()) continue; // 空白行跳过

                Long courseId = nameToId.get(courseName);
                if (courseId == null) {
                    errors.add("第" + dispRow + "行：课程「" + courseName + "」不在当前专业的课程列表中");
                    continue;
                }

                for (Map.Entry<Integer, Long> e : colToIndicator.entrySet()) {
                    Cell cell = row.getCell(e.getKey());
                    String val = getCellString(cell).trim();
                    if (val.isEmpty()) continue;
                    BigDecimal w;
                    try {
                        w = new BigDecimal(val).setScale(2, RoundingMode.HALF_UP);
                    } catch (NumberFormatException ex) {
                        errors.add("第" + dispRow + "行：权重「" + val + "」不是有效数字");
                        continue;
                    }
                    if (w.signum() < 0 || w.compareTo(BigDecimal.ONE) > 0) {
                        errors.add("第" + dispRow + "行：权重「" + val + "」必须在 0~1 之间");
                        continue;
                    }
                    MacroSupportMatrix m = new MacroSupportMatrix();
                    m.setCourseId(courseId);
                    m.setIndicatorId(e.getValue());
                    m.setSupportLevel("M");
                    m.setWeight(w);
                    parsed.add(m);
                }
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
        if (parsed.isEmpty()) {
            throw new BizException("文件中没有可导入的支撑数据，请填写权重后再上传");
        }
        return parsed;
    }

    /** 按毕业要求顺序聚合当前专业的指标点 */
    private List<Indicator> loadIndicatorsByMajor(Long majorId) {
        List<GradRequirement> reqs = gradRequirementMapper.selectList(
                new LambdaQueryWrapper<GradRequirement>()
                        .eq(GradRequirement::getMajorId, majorId)
                        .orderByAsc(GradRequirement::getReqNo));
        List<Indicator> result = new ArrayList<>();
        for (GradRequirement req : reqs) {
            result.addAll(indicatorMapper.selectList(
                    new LambdaQueryWrapper<Indicator>()
                            .eq(Indicator::getGradReqId, req.getId())
                            .orderByAsc(Indicator::getIndicatorNo)));
        }
        return result;
    }

    /** 当前专业已有支撑权重：courseId|indicatorId -> weight */
    private Map<String, BigDecimal> loadExistingWeights(Long majorId) {
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getMajorId, majorId));
        if (courses.isEmpty()) return new HashMap<>();
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
        List<MacroSupportMatrix> entries = macroSupportMatrixMapper.selectList(
                new LambdaQueryWrapper<MacroSupportMatrix>()
                        .in(MacroSupportMatrix::getCourseId, courseIds));
        Map<String, BigDecimal> map = new HashMap<>();
        for (MacroSupportMatrix m : entries) {
            map.put(m.getCourseId() + "|" + m.getIndicatorId(), m.getWeight());
        }
        return map;
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
