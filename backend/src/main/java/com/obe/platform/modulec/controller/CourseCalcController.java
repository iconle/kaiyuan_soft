package com.obe.platform.modulec.controller;

import com.obe.platform.common.Result;
import com.obe.platform.modulec.service.CourseCalcService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/classes/{classId}")
@RequiredArgsConstructor
public class CourseCalcController {

    private final CourseCalcService courseCalcService;

    /** Trigger course-level calculation (Phase 1) */
    @PostMapping("/compute")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<CourseCalcService.CalcResult> compute(@PathVariable Long classId,
                                                         @RequestParam Long operator) {
        return Result.ok(courseCalcService.compute(classId, operator));
    }

    /** Get cached calculation results for a class */
    @GetMapping("/compute/results")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public Result<CourseCalcService.CalcResult> getResults(@PathVariable Long classId) {
        return Result.ok(courseCalcService.getResults(classId));
    }
}
