package com.obe.platform.modulea.controller;

import com.obe.platform.common.Result;
import com.obe.platform.modulea.entity.GradRequirement;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.modulea.service.GradReqService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grad-req")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DIRECTOR') or hasRole('ACADEMIC')")
public class GradReqController {

    private final GradReqService gradReqService;

    @GetMapping
    public Result<List<GradRequirement>> list(@RequestParam Long majorId) {
        return Result.ok(gradReqService.listByMajor(majorId));
    }

    @PostMapping
    public Result<Void> create(@RequestBody GradRequirement requirement) {
        gradReqService.create(requirement);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody GradRequirement requirement) {
        requirement.setId(id);
        gradReqService.update(requirement);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        gradReqService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/indicators")
    public Result<Void> addIndicator(@PathVariable Long id, @RequestBody Indicator indicator) {
        gradReqService.addIndicator(id, indicator);
        return Result.ok();
    }

    @PutMapping("/indicators/{id}")
    public Result<Void> updateIndicator(@PathVariable Long id, @RequestBody Indicator indicator) {
        indicator.setId(id);
        gradReqService.updateIndicator(indicator);
        return Result.ok();
    }

    @DeleteMapping("/indicators/{id}")
    public Result<Void> deleteIndicator(@PathVariable Long id) {
        gradReqService.deleteIndicator(id);
        return Result.ok();
    }
}
