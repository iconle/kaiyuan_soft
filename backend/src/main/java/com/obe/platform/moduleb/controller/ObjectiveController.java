package com.obe.platform.moduleb.controller;

import com.obe.platform.common.Result;
import com.obe.platform.moduleb.entity.CourseObjective;
import com.obe.platform.moduleb.service.ObjectiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes/{classId}/objectives")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class ObjectiveController {

    private final ObjectiveService objectiveService;

    @GetMapping
    public Result<List<CourseObjective>> list(@PathVariable Long classId) {
        return Result.ok(objectiveService.listObjectives(classId));
    }

    @PostMapping
    public Result<CourseObjective> create(@PathVariable Long classId,
                                          @RequestBody CourseObjective objective) {
        return Result.ok(objectiveService.createObjective(classId, objective));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long classId,
                               @PathVariable Long id,
                               @RequestBody CourseObjective objective) {
        objective.setId(id);
        objectiveService.updateObjective(objective);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long classId,
                               @PathVariable Long id) {
        objectiveService.deleteObjective(id);
        return Result.ok();
    }
}
