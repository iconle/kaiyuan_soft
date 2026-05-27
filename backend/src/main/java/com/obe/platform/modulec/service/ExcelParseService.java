package com.obe.platform.modulec.service;

import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.ClassStudent;
import com.obe.platform.modulea.entity.Student;
import com.obe.platform.modulea.mapper.ClassStudentMapper;
import com.obe.platform.modulea.mapper.StudentMapper;
import com.obe.platform.moduleb.entity.AssessmentPoint;
import com.obe.platform.moduleb.entity.CourseOutline;
import com.obe.platform.moduleb.mapper.AssessmentPointMapper;
import com.obe.platform.moduleb.mapper.CourseOutlineMapper;
import com.obe.platform.modulec.entity.ScoreSheet;
import com.obe.platform.modulec.entity.StudentScore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.modulec.mapper.ScoreSheetMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelParseService {

    private final ScoreSheetMapper scoreSheetMapper;
    private final CourseOutlineMapper outlineMapper;
    private final AssessmentPointMapper assessmentPointMapper;
    private final ClassStudentMapper classStudentMapper;
    private final StudentMapper studentMapper;

    /**
     * Parse an uploaded Excel score file and return the list of StudentScore records.
     * The first two columns map to 学号 (studentNo) and 姓名 (name), then assessment scores.
     * Rows 0-1 are headers and are skipped. Data rows start at row 2.
     *
     * @param file    the uploaded Excel file
     * @param sheetId the ScoreSheet ID
     * @return list of StudentScore records (not yet persisted)
     */
    public List<StudentScore> parseScoreFile(MultipartFile file, Long sheetId) {
        ScoreSheet scoreSheet = scoreSheetMapper.selectById(sheetId);
        if (scoreSheet == null) {
            throw new BizException("成绩单不存在");
        }

        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>()
                        .eq(CourseOutline::getClassId, scoreSheet.getClassId()));
        if (outline == null) {
            throw new BizException("课程大纲不存在，请先配置课程目标和考核点");
        }

        List<AssessmentPoint> assessmentPoints = assessmentPointMapper.selectList(
                new LambdaQueryWrapper<AssessmentPoint>()
                        .eq(AssessmentPoint::getOutlineId, outline.getId())
                        .orderByAsc(AssessmentPoint::getSortOrder));

        // Build student lookup: studentNo → studentId
        List<ClassStudent> classStudents = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, scoreSheet.getClassId()));
        List<Long> studentIds = classStudents.stream().map(ClassStudent::getStudentId).toList();
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Map<String, Long> studentNoToId = students.stream()
                .collect(Collectors.toMap(Student::getStudentNo, Student::getId));

        List<StudentScore> result = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int r = 2; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String studentNo = getCellString(row.getCell(0));
                if (studentNo == null || studentNo.isBlank()) continue;

                Long studentId = studentNoToId.get(studentNo.trim());
                if (studentId == null) {
                    throw new BizException("学号 " + studentNo + " 不在本教学班名单中 (行" + (r + 1) + ")");
                }

                for (int i = 0; i < assessmentPoints.size(); i++) {
                    AssessmentPoint ap = assessmentPoints.get(i);
                    Cell cell = row.getCell(2 + i);

                    BigDecimal score = BigDecimal.ZERO;
                    if (cell != null) {
                        try {
                            double val = cell.getNumericCellValue();
                            score = BigDecimal.valueOf(val);
                        } catch (Exception e) {
                            String s = getCellString(cell);
                            if (s != null && !s.isBlank()) {
                                score = new BigDecimal(s);
                            }
                        }
                    }

                    if (score.compareTo(ap.getMaxScore()) > 0) {
                        throw new BizException("学号 " + studentNo + " 在考核点「" + ap.getName()
                                + "」的得分(" + score + ")超过满分(" + ap.getMaxScore() + "), 行" + (r + 1));
                    }

                    StudentScore ss = new StudentScore();
                    ss.setSheetId(sheetId);
                    ss.setStudentId(studentId);
                    ss.setAssessmentId(ap.getId());
                    ss.setScore(score);
                    result.add(ss);
                }
            }

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("解析Excel文件失败: " + e.getMessage());
        }

        return result;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }
}
