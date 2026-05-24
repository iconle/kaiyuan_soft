package com.obe.platform.moduled.controller;

import com.obe.platform.common.Result;
import com.obe.platform.moduled.service.CourseReportService;
import com.obe.platform.moduled.service.MajorReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final CourseReportService courseReportService;
    private final MajorReportService majorReportService;

    /** Get course-level report data (JSON) */
    @GetMapping("/course/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public Result<Map<String, Object>> getCourseReport(@PathVariable Long classId) {
        return Result.ok(courseReportService.getReportData(classId));
    }

    /** Download course-level PDF report */
    @GetMapping("/course/{classId}/pdf")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public ResponseEntity<byte[]> downloadCoursePdf(@PathVariable Long classId) {
        byte[] pdf = courseReportService.generatePdf(classId);
        String filename = URLEncoder.encode("课程达成度报告.pdf", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** Download course-level Excel report */
    @GetMapping("/course/{classId}/excel")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public ResponseEntity<byte[]> downloadCourseExcel(@PathVariable Long classId) {
        byte[] excel = courseReportService.generateExcel(classId);
        String filename = URLEncoder.encode("课程达成度报告.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    /** Get major-level report data (JSON, includes radar chart data) */
    @GetMapping("/major")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<Map<String, Object>> getMajorReport(@RequestParam Long majorId,
                                                       @RequestParam Long semesterId) {
        return Result.ok(majorReportService.getReportData(majorId, semesterId));
    }

    /** Get radar chart data specifically */
    @GetMapping("/major/radar")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<Map<String, Object>> getRadarData(@RequestParam Long majorId,
                                                     @RequestParam Long semesterId) {
        Map<String, Object> data = majorReportService.getReportData(majorId, semesterId);
        return Result.ok(Map.of("radarData", data.get("radarData")));
    }

    /** Download major-level drill-through Excel ledger */
    @GetMapping("/major/excel")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public ResponseEntity<byte[]> downloadMajorExcel(@RequestParam Long majorId,
                                                      @RequestParam Long semesterId) {
        byte[] excel = majorReportService.generateTraceExcel(majorId, semesterId);
        String filename = URLEncoder.encode("专业级达成度穿透式台账.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }
}
