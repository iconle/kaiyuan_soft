package com.obe.platform.modulea.controller;

import com.obe.platform.common.Result;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.modulea.entity.MacroSupportMatrix;
import com.obe.platform.modulea.service.MacroMatrixImportService;
import com.obe.platform.modulea.service.MacroMatrixService;
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
@RequestMapping("/api/macro-matrix")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DIRECTOR')")
public class MacroMatrixController {

    private final MacroMatrixService macroMatrixService;
    private final MacroMatrixImportService macroMatrixImportService;

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

    /** 下载课程支撑导入模板（按所选专业预填现有支撑关系） */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate(@RequestParam Long majorId) {
        byte[] data = macroMatrixImportService.generateTemplate(majorId);
        String filename = URLEncoder.encode("课程支撑导入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    /** 上传并导入课程支撑；校验失败时返回逐条可读错误信息 */
    @PostMapping("/import")
    public Result<Integer> importMatrix(@RequestParam Long majorId,
                                        @RequestParam("file") MultipartFile file) {
        int count = macroMatrixImportService.importMatrix(majorId, file);
        return Result.ok(count);
    }
}
