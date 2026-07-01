package com.obe.platform.moduleb.service;

import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.moduleb.entity.CourseObjective;
import com.obe.platform.moduleb.entity.ObjectiveIndicatorWeight;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 课程目标对指标点的「内部权重」导入服务。
 * <p>
 * 体验与「课程支撑矩阵导入 / 考核点导入」一致：下载标准模板 → 填写 → 上传。
 * 模板首列为「课程目标」（填写课程目标编号），其后每个指标点占一列（表头为指标点编号），
 * 单元格填写该课程目标对该指标点的内部贡献权重，并预填当前页面已有权重，支持「导出 → 编辑 → 回填」。
 * <p>
 * 导入只做解析并返回条目，不在本接口落库；前端将其合并进页面表格后，由用户点击
 * 「保存权重」统一保存（仍受每个指标点列合计必须为 1.00 的校验约束）。
 */
@Service
@RequiredArgsConstructor
public class WeightImportService {

    private final WeightService weightService;
    private final ObjectiveService objectiveService;

    private static final String SHEET_NAME = "内部权重矩阵";
    private static final String FIRST_HEADER = "课程目标";

    /**
     * 生成内部权重导入模板，预填当前教学班的课程目标与已有权重。
     */
    public byte[] generateTemplate(Long classId) {
        List<CourseObjective> objectives = objectiveService.listObjectives(classId);
        List<Indicator> indicators = weightService.getSupportedIndicators(classId);
        Map<String, BigDecimal> existing = loadExistingWeights(classId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Workbook workbook = new XSSFWorkbook()) {

            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle objStyle = buildFirstColumnStyle(workbook);
            CellStyle dataStyle = buildDataStyle(workbook);

            Sheet sheet = workbook.createSheet(SHEET_NAME);

            // 表头：课程目标 + 各指标点编号
            Row headerRow = sheet.createRow(0);
            Cell h0 = headerRow.createCell(0);
            h0.setCellValue(FIRST_HEADER);
            h0.setCellStyle(headerStyle);
            for (int i = 0; i < indicators.size(); i++) {
                Cell c = headerRow.createCell(i + 1);
                c.setCellValue(indicators.get(i).getIndicatorNo());
                c.setCellStyle(headerStyle);
            }

            // 每个课程目标一行：预填目标编号 + 已有权重
            int rowIdx = 1;
            for (CourseObjective obj : objectives) {
                Row row = sheet.createRow(rowIdx++);
                Cell c0 = row.createCell(0);
                if (obj.getObjNo() != null) c0.setCellValue(obj.getObjNo());
                c0.setCellStyle(objStyle);
                for (int i = 0; i < indicators.size(); i++) {
                    Cell c = row.createCell(i + 1);
                    BigDecimal w = existing.get(obj.getId() + "|" + indicators.get(i).getId());
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

            sheet.setColumnWidth(0, 20 * 256);
            for (int i = 0; i < indicators.size(); i++) sheet.setColumnWidth(i + 1, 16 * 256);
            sheet.createFreezePane(1, 1);

            // 说明 sheet
            Sheet note = workbook.createSheet("填写说明");
            String validObjNos = objectives.stream()
                    .map(CourseObjective::getObjNo).filter(Objects::nonNull)
                    .collect(Collectors.joining("、"));
            String validIndNos = indicators.stream()
                    .map(Indicator::getIndicatorNo).filter(Objects::nonNull)
                    .collect(Collectors.joining("、"));
            String[] lines = {
                    "内部权重导入模板说明：",
                    "1. 请勿修改第一行表头：首列必须为「课程目标」（填写课程目标编号），其余各列表头为指标点编号。",
                    "2. 课程目标编号必须是当前教学班已有的目标编号（" + (validObjNos.isEmpty() ? "暂无" : validObjNos) + "）。",
                    "3. 每个单元格填写该课程目标对指标点的内部贡献权重（0~1 之间的小数，如 0.3），留空表示不填写。",
                    "4. 同一指标点（同一列）所有课程目标的权重之和必须为 1.00，可在导入后于页面核对、调整，再点击「保存权重」生效。",
                    "5. 导入只更新页面数据，不会立即保存；请确认无误后再点击「保存权重」。",
                    "6. 完全空白的行会被自动跳过。",
                    validIndNos.isEmpty() ? "" : ("当前课程支撑的指标点：" + validIndNos)
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
            throw new BizException("生成内部权重导入模板失败: " + e.getMessage());
        }
    }

    /**
     * 解析上传文件，返回待合并的权重条目（不落库）。全部行级错误收集后一次性抛出。
     * 不校验列合计是否为 1.00 —— 该校验留给前端页面，便于用户导入后直接在页面调整。
     */
    public List<ObjectiveIndicatorWeight> parseImport(Long classId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件为空，请选择模板文件");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            throw new BizException("仅支持 .xlsx 格式文件，请点击「下载模板」获取正确模板");
        }

        List<CourseObjective> objectives = objectiveService.listObjectives(classId);
        Map<String, Long> objNoToId = new LinkedHashMap<>();
        for (CourseObjective o : objectives) {
            if (o.getObjNo() != null) objNoToId.put(o.getObjNo().trim(), o.getId());
        }
        if (objNoToId.isEmpty()) {
            throw new BizException("当前教学班尚未创建课程目标，请先在「课程目标」页面新增或导入目标");
        }

        List<Indicator> indicators = weightService.getSupportedIndicators(classId);
        Map<String, Long> noToId = new LinkedHashMap<>();
        for (Indicator ind : indicators) {
            if (ind.getIndicatorNo() != null) noToId.put(ind.getIndicatorNo().trim(), ind.getId());
        }
        if (noToId.isEmpty()) {
            throw new BizException("本课程尚未在宏观支撑矩阵中配置支撑关系，无法导入权重");
        }
        String validObjNos = String.join("、", objNoToId.keySet());
        String validIndNos = String.join("、", noToId.keySet());

        List<String> errors = new ArrayList<>();
        List<ObjectiveIndicatorWeight> parsed = new ArrayList<>();

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
                throw new BizException("模板格式不正确：首列应为「课程目标」，请点击「下载模板」获取标准模板");
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
                    errors.add("表头第" + (col + 1) + "列「" + h + "」不是当前课程支撑的指标点编号（有效：" + validIndNos + "）");
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

                String objNo = getCellString(row.getCell(0)).trim();
                if (objNo.isEmpty()) continue; // 空白行跳过

                Long objectiveId = objNoToId.get(objNo);
                if (objectiveId == null) {
                    errors.add("第" + dispRow + "行：课程目标「" + objNo + "」不存在（有效：" + validObjNos + "）");
                    continue;
                }

                for (Map.Entry<Integer, Long> e : colToIndicator.entrySet()) {
                    Cell cell = row.getCell(e.getKey());
                    String val = getCellString(cell).trim();
                    if (val.isEmpty()) continue;
                    BigDecimal w;
                    try {
                        w = new BigDecimal(val);
                    } catch (NumberFormatException ex) {
                        errors.add("第" + dispRow + "行：权重「" + val + "」不是有效数字");
                        continue;
                    }
                    if (w.signum() < 0 || w.compareTo(BigDecimal.ONE) > 0) {
                        errors.add("第" + dispRow + "行：权重「" + val + "」必须在 0~1 之间");
                        continue;
                    }
                    ObjectiveIndicatorWeight oiw = new ObjectiveIndicatorWeight();
                    oiw.setObjectiveId(objectiveId);
                    oiw.setIndicatorId(e.getValue());
                    oiw.setWeight(w);
                    parsed.add(oiw);
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
            throw new BizException("文件中没有可导入的权重数据，请填写权重后再上传");
        }
        return parsed;
    }

    /** 当前教学班已有内部权重：objectiveId|indicatorId -> weight */
    private Map<String, BigDecimal> loadExistingWeights(Long classId) {
        List<ObjectiveIndicatorWeight> weights = weightService.getWeights(classId);
        Map<String, BigDecimal> map = new HashMap<>();
        for (ObjectiveIndicatorWeight w : weights) {
            if (w.getWeight() != null) {
                map.put(w.getObjectiveId() + "|" + w.getIndicatorId(), w.getWeight());
            }
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

    private CellStyle buildFirstColumnStyle(Workbook wb) {
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
