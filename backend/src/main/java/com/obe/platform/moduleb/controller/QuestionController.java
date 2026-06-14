package com.obe.platform.moduleb.controller;

import com.obe.platform.common.Result;
import com.obe.platform.moduleb.entity.AssessmentQuestion;
import com.obe.platform.moduleb.service.QuestionService;
import com.obe.platform.moduleb.service.TeacherConfigImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/assessments/{assessmentId}/questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class QuestionController {

    private final QuestionService questionService;
    private final TeacherConfigImportService importService;

    @GetMapping
    public Result<List<AssessmentQuestion>> list(@PathVariable Long assessmentId) {
        return Result.ok(questionService.listByAssessment(assessmentId));
    }

    @PostMapping
    public Result<AssessmentQuestion> create(@PathVariable Long assessmentId,
                                              @RequestBody AssessmentQuestion q) {
        q.setAssessmentId(assessmentId);
        return Result.ok(questionService.create(q));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long assessmentId,
                                @PathVariable Long id,
                                @RequestBody AssessmentQuestion q) {
        q.setId(id);
        questionService.update(q);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long assessmentId,
                                @PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate(@PathVariable Long assessmentId) {
        byte[] data = importService.generateQuestionTemplate(
                importService.requireAssessment(assessmentId),
                importService.listAssessmentObjectives(assessmentId));
        String filename = URLEncoder.encode("考核点题目导入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/import")
    public Result<Integer> importQuestions(@PathVariable Long assessmentId,
                                           @RequestParam("file") MultipartFile file) {
        return Result.ok(importService.importQuestions(assessmentId, file));
    }
}
