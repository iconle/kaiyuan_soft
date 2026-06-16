package com.obe.platform.moduleb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.moduleb.entity.AssessmentObjective;
import com.obe.platform.moduleb.entity.AssessmentPoint;
import com.obe.platform.moduleb.entity.AssessmentQuestion;
import com.obe.platform.moduleb.entity.CourseObjective;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.entity.QuestionObjective;
import com.obe.platform.moduleb.mapper.AssessmentObjectiveMapper;
import com.obe.platform.moduleb.mapper.AssessmentPointMapper;
import com.obe.platform.moduleb.mapper.AssessmentQuestionMapper;
import com.obe.platform.moduleb.mapper.CourseObjectiveMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import com.obe.platform.moduleb.mapper.QuestionObjectiveMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherConfigImportService {

    private static final String[] OBJECTIVE_HEADERS = {"课程目标编号", "维度", "目标描述"};
    private static final String[] ASSESSMENT_HEADERS = {
            "考核点名称", "满分", "权重(%)", "绑定课程目标编号", "排序号"
    };
    private static final String[] QUESTION_HEADERS = {
            "题目名称", "满分", "绑定课程目标编号", "排序号"
    };

    private final CourseOutlineMapper outlineMapper;
    private final CourseObjectiveMapper objectiveMapper;
    private final AssessmentPointMapper assessmentMapper;
    private final AssessmentObjectiveMapper assessmentObjectiveMapper;
    private final AssessmentQuestionMapper questionMapper;
    private final QuestionObjectiveMapper questionObjectiveMapper;

    public byte[] generateObjectiveTemplate() {
        return generateTemplate("课程目标导入", OBJECTIVE_HEADERS, List.of(), List.of(
                "课程目标编号、维度、目标描述均为必填项。",
                "填写示例：1-1｜知识｜掌握课程核心知识。",
                "课程目标编号不能与当前教学班已有编号或文件内其他行重复。",
                "请勿修改第一行表头；完全空白的行会被忽略。"
        ));
    }

    public byte[] generateAssessmentTemplate(List<CourseObjective> objectives) {
        String firstObjective = objectives.isEmpty() ? "1-1" : objectives.get(0).getObjNo();
        return generateTemplate("考核点导入", ASSESSMENT_HEADERS, List.of(), List.of(
                "填写示例：期末考试｜100｜50｜" + firstObjective + "｜1。",
                "绑定多个课程目标时，请使用英文逗号分隔，例如：1-1,2-1。",
                "课程目标编号必须已存在于当前教学班。",
                "满分必须大于0；权重范围为0至100，导入后总权重不能超过100%。",
                "请勿修改第一行表头；完全空白的行会被忽略。"
        ));
    }

    public byte[] generateQuestionTemplate(AssessmentPoint assessment, List<CourseObjective> objectives) {
        String objectiveNos = objectives.stream()
                .map(CourseObjective::getObjNo)
                .collect(Collectors.joining(","));
        return generateTemplate("考核点题目导入", QUESTION_HEADERS, List.of(), List.of(
                "当前考核点：" + assessment.getName() + "，满分：" + assessment.getMaxScore(),
                "填写示例：第1题｜20｜" + objectiveNos + "｜1。",
                "题目绑定目标只能选择当前考核点已绑定的课程目标：" + objectiveNos,
                "满分必须大于0，导入后该考核点题目总分不能超过100。",
                "请勿修改第一行表头；完全空白的行会被忽略。"
        ));
    }

    @Transactional
    public int importObjectives(Long classId, MultipartFile file) {
        CourseOutline outline = getOrCreateOutline(classId);
        Set<String> existing = objectiveMapper.selectList(
                        new LambdaQueryWrapper<CourseObjective>()
                                .eq(CourseObjective::getOutlineId, outline.getId()))
                .stream().map(item -> normalize(item.getObjNo())).collect(Collectors.toSet());

        List<String> errors = new ArrayList<>();
        Set<String> fileNos = new HashSet<>();
        List<CourseObjective> rows = new ArrayList<>();
        parseRows(file, OBJECTIVE_HEADERS, (row, rowNo) -> {
            String objNo = text(row.getCell(0));
            String dimension = text(row.getCell(1));
            String description = text(row.getCell(2));
            if (allBlank(objNo, dimension, description)) return;
            if (objNo.isBlank()) errors.add(error(rowNo, "课程目标编号不能为空"));
            if (dimension.isBlank()) errors.add(error(rowNo, "维度不能为空"));
            if (description.isBlank()) errors.add(error(rowNo, "目标描述不能为空"));
            String key = normalize(objNo);
            if (!objNo.isBlank() && existing.contains(key)) {
                errors.add(error(rowNo, "课程目标编号「" + objNo + "」已存在"));
            } else if (!objNo.isBlank() && !fileNos.add(key)) {
                errors.add(error(rowNo, "课程目标编号「" + objNo + "」在文件中重复"));
            }
            if (objNo.isBlank() || dimension.isBlank() || description.isBlank()
                    || existing.contains(key) || rows.stream().anyMatch(item -> normalize(item.getObjNo()).equals(key))) {
                return;
            }
            CourseObjective objective = new CourseObjective();
            objective.setOutlineId(outline.getId());
            objective.setObjNo(objNo);
            objective.setDimension(dimension);
            objective.setDescription(description);
            rows.add(objective);
        });
        ensureImportable(errors, rows, "课程目标");
        rows.forEach(objectiveMapper::insert);
        return rows.size();
    }

    @Transactional
    public int importAssessments(Long classId, MultipartFile file) {
        CourseOutline outline = getOrCreateOutline(classId);
        List<CourseObjective> objectives = listObjectives(outline.getId());
        if (objectives.isEmpty()) {
            throw new BizException("当前教学班尚未创建课程目标，请先导入或新增课程目标");
        }
        Map<String, Long> objectiveIds = objectives.stream().collect(Collectors.toMap(
                item -> normalize(item.getObjNo()), CourseObjective::getId, (a, b) -> a, LinkedHashMap::new));
        List<AssessmentPoint> existingAssessments = assessmentMapper.selectList(
                new LambdaQueryWrapper<AssessmentPoint>().eq(AssessmentPoint::getOutlineId, outline.getId()));
        Set<String> existingNames = existingAssessments.stream()
                .map(item -> normalize(item.getName())).collect(Collectors.toSet());
        BigDecimal existingWeight = existingAssessments.stream()
                .map(AssessmentPoint::getWeightPercent).filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> errors = new ArrayList<>();
        Set<String> fileNames = new HashSet<>();
        List<AssessmentImportRow> rows = new ArrayList<>();
        parseRows(file, ASSESSMENT_HEADERS, (row, rowNo) -> {
            String name = text(row.getCell(0));
            String maxScoreText = text(row.getCell(1));
            String weightText = text(row.getCell(2));
            String objectiveText = text(row.getCell(3));
            String sortText = text(row.getCell(4));
            if (allBlank(name, maxScoreText, weightText, objectiveText, sortText)) return;
            BigDecimal maxScore = positiveDecimal(maxScoreText, rowNo, "满分", errors);
            BigDecimal weight = decimalInRange(weightText, rowNo, "权重", BigDecimal.ZERO,
                    new BigDecimal("100"), errors);
            Integer sortOrder = positiveInteger(sortText, rowNo, "排序号", errors);
            if (name.isBlank()) errors.add(error(rowNo, "考核点名称不能为空"));
            String nameKey = normalize(name);
            if (!name.isBlank() && existingNames.contains(nameKey)) {
                errors.add(error(rowNo, "考核点名称「" + name + "」已存在"));
            } else if (!name.isBlank() && !fileNames.add(nameKey)) {
                errors.add(error(rowNo, "考核点名称「" + name + "」在文件中重复"));
            }
            List<Long> boundIds = resolveObjectiveIds(objectiveText, objectiveIds, rowNo, errors);
            if (!name.isBlank() && maxScore != null && weight != null && sortOrder != null
                    && !boundIds.isEmpty() && !existingNames.contains(nameKey)
                    && rows.stream().noneMatch(item -> normalize(item.point().getName()).equals(nameKey))) {
                AssessmentPoint point = new AssessmentPoint();
                point.setOutlineId(outline.getId());
                point.setName(name);
                point.setMaxScore(maxScore);
                point.setWeightPercent(weight);
                point.setSortOrder(sortOrder);
                rows.add(new AssessmentImportRow(point, boundIds));
            }
        });
        BigDecimal importedWeight = rows.stream().map(item -> item.point().getWeightPercent())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (existingWeight.add(importedWeight).compareTo(new BigDecimal("100")) > 0) {
            errors.add("导入后考核点总权重为 " + existingWeight.add(importedWeight)
                    + "%，超过100%；当前已有权重为 " + existingWeight + "%");
        }
        ensureImportable(errors, rows, "考核点");
        for (AssessmentImportRow row : rows) {
            assessmentMapper.insert(row.point());
            for (Long objectiveId : row.objectiveIds()) {
                AssessmentObjective relation = new AssessmentObjective();
                relation.setAssessmentId(row.point().getId());
                relation.setObjectiveId(objectiveId);
                assessmentObjectiveMapper.insert(relation);
            }
        }
        return rows.size();
    }

    @Transactional
    public int importQuestions(Long assessmentId, MultipartFile file) {
        AssessmentPoint assessment = requireAssessment(assessmentId);
        Map<String, Long> allowedObjectives = listAssessmentObjectives(assessmentId).stream()
                .collect(Collectors.toMap(item -> normalize(item.getObjNo()), CourseObjective::getId));
        if (allowedObjectives.isEmpty()) {
            throw new BizException("当前考核点尚未绑定课程目标，请先完成考核点映射");
        }
        List<AssessmentQuestion> existingQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<AssessmentQuestion>()
                        .eq(AssessmentQuestion::getAssessmentId, assessmentId));
        Set<String> existingNames = existingQuestions.stream()
                .map(item -> normalize(item.getName())).collect(Collectors.toSet());
        BigDecimal existingScore = existingQuestions.stream().map(AssessmentQuestion::getMaxScore)
                .filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> errors = new ArrayList<>();
        Set<String> fileNames = new HashSet<>();
        List<QuestionImportRow> rows = new ArrayList<>();
        parseRows(file, QUESTION_HEADERS, (row, rowNo) -> {
            String name = text(row.getCell(0));
            String maxScoreText = text(row.getCell(1));
            String objectiveText = text(row.getCell(2));
            String sortText = text(row.getCell(3));
            if (allBlank(name, maxScoreText, objectiveText, sortText)) return;
            BigDecimal maxScore = positiveDecimal(maxScoreText, rowNo, "满分", errors);
            Integer sortOrder = positiveInteger(sortText, rowNo, "排序号", errors);
            if (name.isBlank()) errors.add(error(rowNo, "题目名称不能为空"));
            String nameKey = normalize(name);
            if (!name.isBlank() && existingNames.contains(nameKey)) {
                errors.add(error(rowNo, "题目名称「" + name + "」已存在"));
            } else if (!name.isBlank() && !fileNames.add(nameKey)) {
                errors.add(error(rowNo, "题目名称「" + name + "」在文件中重复"));
            }
            List<Long> boundIds = resolveObjectiveIds(objectiveText, allowedObjectives, rowNo, errors);
            if (!name.isBlank() && maxScore != null && sortOrder != null && !boundIds.isEmpty()
                    && !existingNames.contains(nameKey)
                    && rows.stream().noneMatch(item -> normalize(item.question().getName()).equals(nameKey))) {
                AssessmentQuestion question = new AssessmentQuestion();
                question.setAssessmentId(assessmentId);
                question.setName(name);
                question.setMaxScore(maxScore);
                question.setSortOrder(sortOrder);
                rows.add(new QuestionImportRow(question, boundIds));
            }
        });
        BigDecimal importedScore = rows.stream().map(item -> item.question().getMaxScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (existingScore.add(importedScore).compareTo(new BigDecimal("100")) > 0) {
            errors.add("导入后题目总分为 " + existingScore.add(importedScore)
                    + "，超过100；当前已有题目总分为 " + existingScore);
        }
        ensureImportable(errors, rows, "题目");
        for (QuestionImportRow row : rows) {
            questionMapper.insert(row.question());
            for (Long objectiveId : row.objectiveIds()) {
                QuestionObjective relation = new QuestionObjective();
                relation.setQuestionId(row.question().getId());
                relation.setObjectiveId(objectiveId);
                questionObjectiveMapper.insert(relation);
            }
        }
        return rows.size();
    }

    public List<CourseObjective> listObjectivesForClass(Long classId) {
        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));
        return outline == null ? List.of() : listObjectives(outline.getId());
    }

    public AssessmentPoint requireAssessment(Long assessmentId) {
        AssessmentPoint assessment = assessmentMapper.selectById(assessmentId);
        if (assessment == null) throw new BizException("考核点不存在");
        return assessment;
    }

    public List<CourseObjective> listAssessmentObjectives(Long assessmentId) {
        List<AssessmentObjective> relations = assessmentObjectiveMapper.selectList(
                new LambdaQueryWrapper<AssessmentObjective>()
                        .eq(AssessmentObjective::getAssessmentId, assessmentId));
        if (relations.isEmpty()) return List.of();
        return objectiveMapper.selectBatchIds(
                relations.stream().map(AssessmentObjective::getObjectiveId).toList());
    }

    private CourseOutline getOrCreateOutline(Long classId) {
        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));
        if (outline == null) {
            outline = new CourseOutline();
            outline.setClassId(classId);
            outline.setStatus("DRAFT");
            outlineMapper.insert(outline);
        }
        return outline;
    }

    private List<CourseObjective> listObjectives(Long outlineId) {
        return objectiveMapper.selectList(new LambdaQueryWrapper<CourseObjective>()
                .eq(CourseObjective::getOutlineId, outlineId)
                .orderByAsc(CourseObjective::getObjNo));
    }

    private byte[] generateTemplate(String sheetName, String[] headers,
                                    List<List<String>> examples, List<String> instructions) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle dataStyle = dataStyle(workbook);
            Sheet sheet = workbook.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (List<String> example : examples) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = row.createCell(i);
                    if (i < example.size()) cell.setCellValue(example.get(i));
                    cell.setCellStyle(dataStyle);
                }
            }
            for (int i = 0; i < 5; i++) {
                Row row = sheet.createRow(rowIndex++);
                for (int j = 0; j < headers.length; j++) row.createCell(j).setCellStyle(dataStyle);
            }
            for (int i = 0; i < headers.length; i++) sheet.setColumnWidth(i, i == 2 ? 50 * 256 : 22 * 256);
            sheet.createFreezePane(0, 1);
            Sheet note = workbook.createSheet("填写说明");
            for (int i = 0; i < instructions.size(); i++) {
                note.createRow(i).createCell(0).setCellValue((i + 1) + ". " + instructions.get(i));
            }
            note.setColumnWidth(0, 100 * 256);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception exception) {
            throw new BizException("生成导入模板失败: " + exception.getMessage());
        }
    }

    private void parseRows(MultipartFile file, String[] expectedHeaders, RowConsumer consumer) {
        validateFile(file);
        try (InputStream input = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            validateHeaders(sheet.getRow(0), expectedHeaders);
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row != null) consumer.accept(row, index + 1);
            }
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException("解析Excel文件失败: " + exception.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException("上传文件为空，请选择标准模板文件");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BizException("仅支持.xlsx格式文件，请先下载标准模板");
        }
    }

    private void validateHeaders(Row header, String[] expected) {
        List<String> actual = new ArrayList<>();
        for (int i = 0; i < expected.length; i++) actual.add(header == null ? "" : text(header.getCell(i)));
        if (!Arrays.asList(expected).equals(actual)) {
            throw new BizException("模板格式不正确，请重新下载标准模板。\n期望表头："
                    + String.join("、", expected) + "\n实际表头：" + String.join("、", actual));
        }
    }

    private List<Long> resolveObjectiveIds(String value, Map<String, Long> available,
                                           int rowNo, List<String> errors) {
        if (value.isBlank()) {
            errors.add(error(rowNo, "绑定课程目标编号不能为空"));
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String part : value.split("[,，]")) {
            String objNo = part.trim();
            if (objNo.isBlank() || !seen.add(normalize(objNo))) continue;
            Long id = available.get(normalize(objNo));
            if (id == null) {
                errors.add(error(rowNo, "课程目标编号「" + objNo + "」不存在或不允许绑定；可选编号："
                        + String.join("、", available.keySet())));
            } else {
                ids.add(id);
            }
        }
        return ids;
    }

    private BigDecimal positiveDecimal(String value, int rowNo, String field, List<String> errors) {
        BigDecimal decimal = parseDecimal(value, rowNo, field, errors);
        if (decimal != null && decimal.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(error(rowNo, field + "必须大于0，实际为「" + value + "」"));
            return null;
        }
        return decimal;
    }

    private BigDecimal decimalInRange(String value, int rowNo, String field,
                                      BigDecimal min, BigDecimal max, List<String> errors) {
        BigDecimal decimal = parseDecimal(value, rowNo, field, errors);
        if (decimal != null && (decimal.compareTo(min) < 0 || decimal.compareTo(max) > 0)) {
            errors.add(error(rowNo, field + "必须在" + min + "至" + max + "之间，实际为「" + value + "」"));
            return null;
        }
        return decimal;
    }

    private BigDecimal parseDecimal(String value, int rowNo, String field, List<String> errors) {
        if (value.isBlank()) {
            errors.add(error(rowNo, field + "不能为空"));
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            errors.add(error(rowNo, field + "必须为数字，实际为「" + value + "」"));
            return null;
        }
    }

    private Integer positiveInteger(String value, int rowNo, String field, List<String> errors) {
        if (value.isBlank()) {
            errors.add(error(rowNo, field + "不能为空"));
            return null;
        }
        try {
            int number = new BigDecimal(value).intValueExact();
            if (number < 1) throw new ArithmeticException();
            return number;
        } catch (Exception exception) {
            errors.add(error(rowNo, field + "必须为大于0的整数，实际为「" + value + "」"));
            return null;
        }
    }

    private void ensureImportable(List<String> errors, List<?> rows, String type) {
        if (!errors.isEmpty()) {
            throw new BizException("导入失败，共 " + errors.size() + " 处问题，请修改后重新上传：\n"
                    + String.join("\n", errors));
        }
        if (rows.isEmpty()) throw new BizException("文件中没有可导入的" + type + "数据");
    }

    private String text(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            BigDecimal value = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
            return value.toPlainString();
        }
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                return cell.getStringCellValue().trim();
            } catch (Exception ignored) {
                return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            }
        }
        return cell.toString().trim();
    }

    private boolean allBlank(String... values) {
        return Arrays.stream(values).allMatch(String::isBlank);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String error(int rowNo, String message) {
        return "第" + rowNo + "行：" + message;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = dataStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle dataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    @FunctionalInterface
    private interface RowConsumer {
        void accept(Row row, int rowNo);
    }

    private record AssessmentImportRow(AssessmentPoint point, List<Long> objectiveIds) {}
    private record QuestionImportRow(AssessmentQuestion question, List<Long> objectiveIds) {}
}
