package com.obe.platform.moduleb.controller;

import com.obe.platform.common.Result;
import com.obe.platform.moduleb.entity.AssessmentPoint;
import com.obe.platform.moduleb.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes/{classId}/assessments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping
    public Result<List<AssessmentPoint>> list(@PathVariable Long classId) {
        return Result.ok(assessmentService.listAssessments(classId));
    }

    @PostMapping
    public Result<AssessmentPoint> create(@PathVariable Long classId,
                                          @RequestBody AssessmentPoint point) {
        return Result.ok(assessmentService.createAssessment(classId, point));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long classId,
                               @PathVariable Long id,
                               @RequestBody AssessmentPoint point) {
        point.setId(id);
        assessmentService.updateAssessment(point);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long classId,
                               @PathVariable Long id) {
        assessmentService.deleteAssessment(id);
        return Result.ok();
    }
}
