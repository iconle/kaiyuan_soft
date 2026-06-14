package com.obe.platform.modulea.controller;

import com.obe.platform.common.Result;
import com.obe.platform.modulea.entity.GradRequirement;
import com.obe.platform.modulea.entity.Indicator;
import com.obe.platform.modulea.service.GradReqService;
import com.obe.platform.modulea.service.IndicatorImportService;
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
@RequestMapping("/api/grad-req")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DIRECTOR') or hasRole('ACADEMIC')")
public class GradReqController {

    private final GradReqService gradReqService;
    private final IndicatorImportService indicatorImportService;

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

    /** 下载指标点导入模板（按所选专业预填毕业要求） */
    @GetMapping("/indicator-template")
    public ResponseEntity<byte[]> downloadIndicatorTemplate(@RequestParam Long majorId) {
        byte[] data = indicatorImportService.generateTemplate(majorId);
        String filename = URLEncoder.encode("指标点导入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    /** 上传并导入指标点；校验失败时返回逐条可读错误信息 */
    @PostMapping("/indicators/import")
    public Result<Integer> importIndicators(@RequestParam Long majorId,
                                            @RequestParam("file") MultipartFile file) {
        int count = indicatorImportService.importIndicators(majorId, file);
        return Result.ok(count);
    }
}
