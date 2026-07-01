package com.obe.platform.moduleb.controller;

import com.obe.platform.common.Result;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.moduleb.entity.ObjectiveIndicatorWeight;
import com.obe.platform.moduleb.service.WeightImportService;
import com.obe.platform.moduleb.service.WeightService;
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
@RequestMapping("/api/classes/{classId}/weights")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class WeightController {

    private final WeightService weightService;
    private final WeightImportService weightImportService;

    @GetMapping
    public Result<List<ObjectiveIndicatorWeight>> list(@PathVariable Long classId) {
        return Result.ok(weightService.getWeights(classId));
    }

    @PutMapping
    public Result<Void> update(@PathVariable Long classId,
                               @RequestBody List<ObjectiveIndicatorWeight> weights) {
        weightService.updateWeights(classId, weights);
        return Result.ok();
    }

    @GetMapping("/supported-indicators")
    public Result<List<Indicator>> supportedIndicators(@PathVariable Long classId) {
        return Result.ok(weightService.getSupportedIndicators(classId));
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate(@PathVariable Long classId) {
        byte[] data = weightImportService.generateTemplate(classId);
        String filename = URLEncoder.encode("内部权重导入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/import")
    public Result<List<ObjectiveIndicatorWeight>> importWeights(@PathVariable Long classId,
                                                                @RequestParam("file") MultipartFile file) {
        return Result.ok(weightImportService.parseImport(classId, file));
    }
}
