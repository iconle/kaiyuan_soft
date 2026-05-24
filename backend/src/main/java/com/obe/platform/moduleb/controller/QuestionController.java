package com.obe.platform.moduleb.controller;

import com.obe.platform.common.Result;
import com.obe.platform.moduleb.entity.AssessmentQuestion;
import com.obe.platform.moduleb.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessments/{assessmentId}/questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class QuestionController {

    private final QuestionService questionService;

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
}
