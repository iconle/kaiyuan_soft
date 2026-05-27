package com.obe.platform.modulec.controller;

import com.obe.platform.common.Result;
import com.obe.platform.modulec.entity.StudentScore;
import com.obe.platform.modulec.service.ExcelParseService;
import com.obe.platform.modulec.service.ExcelTemplateService;
import com.obe.platform.modulec.service.ScoreService;
import com.obe.platform.modulec.service.UnlockRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes/{classId}")
@RequiredArgsConstructor
public class ScoreController {

    private final ExcelTemplateService excelTemplateService;
    private final ExcelParseService excelParseService;
    private final ScoreService scoreService;
    private final UnlockRequestService unlockRequestService;

    /** Download score entry Excel template */
    @GetMapping("/score-template")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC')")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable Long classId) {
        byte[] data = excelTemplateService.generateTemplate(classId);
        String filename = URLEncoder.encode("成绩录入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    /** Upload completed score Excel file */
    @PostMapping("/scores/upload")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> uploadScores(@PathVariable Long classId,
                                      @RequestParam("file") MultipartFile file) {
        var sheet = scoreService.getOrCreateSheet(classId);
        List<StudentScore> scores = excelParseService.parseScoreFile(file, sheet.getId());
        scoreService.importScores(classId, scores);
        return Result.ok();
    }

    /** Get score preview data for the grid */
    @GetMapping("/scores")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public Result<ScoreService.ScorePreview> getScores(@PathVariable Long classId) {
        return Result.ok(scoreService.getScorePreview(classId));
    }

    /** Update a single score cell (supplement/correct) */
    @PutMapping("/scores")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> updateScore(@PathVariable Long classId,
                                     @RequestBody Map<String, Object> body) {
        Long studentId = Long.valueOf(body.get("studentId").toString());
        Long assessmentId = Long.valueOf(body.get("assessmentId").toString());
        Long questionId = body.containsKey("questionId") && body.get("questionId") != null
                ? Long.valueOf(body.get("questionId").toString()) : null;
        BigDecimal score = new BigDecimal(body.get("score").toString());
        scoreService.updateScore(classId, studentId, assessmentId, questionId, score);
        return Result.ok();
    }

    /** Get score sheet status */
    @GetMapping("/score-status")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public Result<Map<String, String>> getStatus(@PathVariable Long classId) {
        var sheet = scoreService.getOrCreateSheet(classId);
        return Result.ok(Map.of("status", sheet.getStatus()));
    }

    /** Teacher: submit correction request (勘误申请) */
    @PostMapping("/request-unlock")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Map<String, Object>> requestUnlock(@PathVariable Long classId,
                                                      @RequestBody Map<String, String> body) {
        Long teacherId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String reason = body.getOrDefault("reason", "");
        var req = unlockRequestService.submitRequest(classId, teacherId, reason);
        return Result.ok(Map.of("id", req.getId(), "status", req.getStatus()));
    }

    /** Teacher: view my unlock requests for this class */
    @GetMapping("/my-unlock-requests")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<List<com.obe.platform.modulec.entity.ScoreUnlockRequest>> myRequests(@PathVariable Long classId) {
        Long teacherId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.ok(unlockRequestService.getRequestsByClass(classId, teacherId));
    }

    /** Teacher: cancel my pending unlock request */
    @PostMapping("/cancel-unlock-request/{requestId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> cancelRequest(@PathVariable Long classId, @PathVariable Long requestId) {
        Long teacherId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        unlockRequestService.cancelRequest(requestId, teacherId);
        return Result.ok();
    }
}
