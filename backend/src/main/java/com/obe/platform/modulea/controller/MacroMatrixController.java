package com.obe.platform.modulea.controller;

import com.obe.platform.common.Result;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.modulea.entity.MacroSupportMatrix;
import com.obe.platform.modulea.service.MacroMatrixService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/macro-matrix")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DIRECTOR')")
public class MacroMatrixController {

    private final MacroMatrixService macroMatrixService;

    @GetMapping
    public Result<List<MacroSupportMatrix>> getMatrix(@RequestParam Long majorId) {
        return Result.ok(macroMatrixService.getMatrix(majorId));
    }

    @PutMapping
    public Result<Void> updateMatrix(@RequestBody List<MacroSupportMatrix> entries) {
        macroMatrixService.updateMatrix(entries);
        return Result.ok();
    }

    @GetMapping("/course/{courseId}/supported-indicators")
    public Result<List<Indicator>> getSupportedIndicators(@PathVariable Long courseId) {
        return Result.ok(macroMatrixService.getSupportedIndicators(courseId));
    }
}
