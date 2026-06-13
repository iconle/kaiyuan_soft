package com.obe.platform.modulea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.GradRequirement;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.modulea.mapper.GradRequirementMapper;
import com.obe.platform.modulea.mapper.IndicatorMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 指标点批量导入服务。
 * <p>
 * 提供与「成绩录入模板」一致的使用体验：下载标准模板 → 填写 → 上传导入。
 * 导入时严格校验上传文件的表头与字段，任何不一致都会收集成逐条、可操作的错误信息
 * 一次性反馈给用户，避免反复试错。
 */
@Service
@RequiredArgsConstructor
public class IndicatorImportService {

    private final GradRequirementMapper gradRequirementMapper;
    private final IndicatorMapper indicatorMapper;

    /** 模板表头，顺序与列名固定，导入时必须完全一致 */
    private static final String[] HEADERS = {"毕业要求编号", "毕业要求标题", "指标点编号", "指标点描述"};
    private static final String SHEET_NAME = "指标点导入";

    /**
     * 生成指标点导入模板。
     * <p>
     * 会预填当前专业下已存在的毕业要求（编号 + 标题）作为行级提示，
     * 指标点列留空待用户填写；另附「填写说明」sheet。
     */
    public byte[] generateTemplate(Long majorId) {
        List<GradRequirement> requirements = gradRequirementMapper.selectList(
                new LambdaQueryWrapper<GradRequirement>()
                        .eq(GradRequirement::getMajorId, majorId)
                        .orderByAsc(GradRequirement::getReqNo));

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

            // 预填每个毕业要求一行（指标点列留空），方便用户对照填写
            int rowIdx = 1;
            for (GradRequirement req : requirements) {
                Row row = sheet.createRow(rowIdx++);
                Cell c0 = row.createCell(0);
                if (req.getReqNo() != null) c0.setCellValue(req.getReqNo());
                c0.setCellStyle(hintStyle);
                Cell c1 = row.createCell(1);
                if (req.getTitle() != null) c1.setCellValue(req.getTitle());
                c1.setCellStyle(hintStyle);
                row.createCell(2).setCellStyle(dataStyle);
                row.createCell(3).setCellStyle(dataStyle);
            }
            // 追加若干空白行
            for (int k = 0; k < 3; k++) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < HEADERS.length; i++) {
                    row.createCell(i).setCellStyle(dataStyle);
                }
            }

            sheet.setColumnWidth(0, 16 * 256);
            sheet.setColumnWidth(1, 32 * 256);
            sheet.setColumnWidth(2, 14 * 256);
            sheet.setColumnWidth(3, 60 * 256);
            sheet.createFreezePane(0, 1);

            // 说明 sheet
            Sheet note = workbook.createSheet("填写说明");
            String[] lines = {
                    "指标点导入模板说明：",
                    "1. 请勿修改第一行表头，列顺序与列名必须与模板完全一致，否则导入将报错。",
                    "2. 「毕业要求编号」必须填写当前专业中已存在的毕业要求编号（参见已预填行）。",
                    "3. 「毕业要求标题」仅作提示，可留空，不影响导入。",
                    "4. 「指标点编号」必填，例如 3-1、3-2，且不能与已有指标点重复。",
                    "5. 「指标点描述」必填。",
                    "6. 完全空白的行会被自动跳过。"
            };
            for (int i = 0; i < lines.length; i++) {
                note.createRow(i).createCell(0).setCellValue(lines[i]);
            }
            note.setColumnWidth(0, 80 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成指标点导入模板失败: " + e.getMessage());
        }
    }

    /**
     * 解析并导入指标点。
     * <p>
     * 全部行级错误会收集后一次性抛出，信息形如：
     * "导入失败，共 N 处问题：\n第3行：...\n第5行：..."
     */
    @Transactional
    public int importIndicators(Long majorId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件为空，请选择模板文件");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            throw new BizException("仅支持 .xlsx 格式文件，请点击「下载模板」获取正确模板");
        }

        // 当前专业的毕业要求：reqNo -> gradReqId
        List<GradRequirement> requirements = gradRequirementMapper.selectList(
                new LambdaQueryWrapper<GradRequirement>()
                        .eq(GradRequirement::getMajorId, majorId)
                        .orderByAsc(GradRequirement::getReqNo));
        Map<Integer, Long> reqNoToId = new LinkedHashMap<>();
        for (GradRequirement r : requirements) {
            if (r.getReqNo() != null) reqNoToId.put(r.getReqNo(), r.getId());
        }
        if (reqNoToId.isEmpty()) {
            throw new BizException("当前专业尚未创建任何毕业要求，请先新增毕业要求后再导入指标点");
        }
        String validReqNos = reqNoToId.keySet().stream()
                .sorted().map(String::valueOf).collect(Collectors.joining("、"));

        // 已存在指标点（gradReqId + 归一化编号）用于查重
        Set<String> existingKeys = new HashSet<>();
        if (!reqNoToId.isEmpty()) {
            List<Indicator> existing = indicatorMapper.selectList(
                    new LambdaQueryWrapper<Indicator>()
                            .in(Indicator::getGradReqId, reqNoToId.values()));
            for (Indicator ind : existing) {
                existingKeys.add(ind.getGradReqId() + "|" + normalize(ind.getIndicatorNo()));
            }
        }

        List<String> errors = new ArrayList<>();
        List<Indicator> toInsert = new ArrayList<>();
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

                String reqNoStr = getCellString(row.getCell(0)).trim();
                String indicatorNo = getCellString(row.getCell(2)).trim();
                String content = getCellString(row.getCell(3)).trim();

                // 完全空白行：跳过
                if (reqNoStr.isEmpty() && indicatorNo.isEmpty() && content.isEmpty()) continue;

                if (indicatorNo.isEmpty()) {
                    errors.add("第" + dispRow + "行：「指标点编号」不能为空");
                    continue;
                }
                if (content.isEmpty()) {
                    errors.add("第" + dispRow + "行：「指标点描述」不能为空");
                    continue;
                }
                if (reqNoStr.isEmpty()) {
                    errors.add("第" + dispRow + "行：「毕业要求编号」不能为空（有效编号：" + validReqNos + "）");
                    continue;
                }
                Integer reqNo;
                try {
                    reqNo = Integer.parseInt(reqNoStr);
                } catch (NumberFormatException ex) {
                    errors.add("第" + dispRow + "行：「毕业要求编号」必须为整数，实际为「" + reqNoStr
                            + "」（有效编号：" + validReqNos + "）");
                    continue;
                }
                Long gradReqId = reqNoToId.get(reqNo);
                if (gradReqId == null) {
                    errors.add("第" + dispRow + "行：「毕业要求编号 " + reqNo
                            + "」在当前专业不存在（有效编号：" + validReqNos + "）");
                    continue;
                }

                String key = gradReqId + "|" + normalize(indicatorNo);
                if (existingKeys.contains(key)) {
                    errors.add("第" + dispRow + "行：指标点编号「" + indicatorNo + "」已存在，请勿重复导入");
                    continue;
                }
                if (!fileKeys.add(key)) {
                    errors.add("第" + dispRow + "行：指标点编号「" + indicatorNo + "」在本文件中重复出现");
                    continue;
                }

                Indicator ind = new Indicator();
                ind.setGradReqId(gradReqId);
                ind.setIndicatorNo(indicatorNo);
                ind.setContent(content);
                toInsert.add(ind);
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
            throw new BizException("文件中没有可导入的指标点数据，请填写后再上传");
        }

        for (Indicator ind : toInsert) {
            indicatorMapper.insert(ind);
        }
        return toInsert.size();
    }

    /** 编号归一化：去空格 + 小写，用于查重比对 */
    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
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
