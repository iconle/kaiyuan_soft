package com.obe.platform.moduleb.controller;

import com.obe.platform.common.Result;
import com.obe.platform.moduleb.entity.CourseObjective;
import com.obe.platform.moduleb.service.ObjectiveService;
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
@RequestMapping("/api/classes/{classId}/objectives")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class ObjectiveController {

    private final ObjectiveService objectiveService;
    private final TeacherConfigImportService importService;

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

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        return excel(importService.generateObjectiveTemplate(), "课程目标导入模板.xlsx");
    }

    @PostMapping("/import")
    public Result<Integer> importObjectives(@PathVariable Long classId,
                                            @RequestParam("file") MultipartFile file) {
        return Result.ok(importService.importObjectives(classId, file));
    }

    private ResponseEntity<byte[]> excel(byte[] data, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
