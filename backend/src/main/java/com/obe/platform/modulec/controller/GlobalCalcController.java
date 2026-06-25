package com.obe.platform.modulec.controller;

import com.obe.platform.common.Result;
import com.obe.platform.modulec.service.CourseCalcService;
import com.obe.platform.modulec.service.GlobalCalcService;
import com.obe.platform.modulec.service.PersonalAchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/global")
@RequiredArgsConstructor
public class GlobalCalcController {

    private final GlobalCalcService globalCalcService;
    private final CourseCalcService courseCalcService;
    private final PersonalAchievementService personalAchievementService;

    /** Get dashboard: readiness status of all supporting courses */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<GlobalCalcService.DashboardData> dashboard(@RequestParam Long majorId) {
        return Result.ok(globalCalcService.getDashboard(majorId));
    }

    /** Trigger major-level calculation (Phase 2) */
    @PostMapping("/compute")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<GlobalCalcService.MajorCalcResult> compute(@RequestParam Long majorId,
                                                              @RequestParam Long semesterId,
                                                              @RequestParam Long operator) {
        return Result.ok(globalCalcService.compute(majorId, semesterId, operator));
    }

    /** Get existing major-level results */
    @GetMapping("/results")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<Map<Long, java.math.BigDecimal>> getResults(@RequestParam Long majorId,
                                                               @RequestParam Long semesterId) {
        return Result.ok(globalCalcService.getResults(majorId, semesterId));
    }

    @GetMapping("/personal-achievements")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<List<PersonalAchievementService.StudentPointAchievement>> personalAchievements(
            @RequestParam Long majorId,
            @RequestParam Long semesterId,
            @RequestParam Long indicatorId) {
        return Result.ok(personalAchievementService.listMajorIndicatorStudents(
                majorId,
                semesterId,
                indicatorId));
    }

}
