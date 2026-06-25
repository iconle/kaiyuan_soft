package com.obe.platform.modulec.controller;

import com.obe.platform.common.Result;
import com.obe.platform.modulec.service.PersonalAchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/classes/{classId}/personal-achievements")
@RequiredArgsConstructor
public class PersonalAchievementController {

    private final PersonalAchievementService personalAchievementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public Result<List<PersonalAchievementService.StudentAchievementSummary>> list(
            @PathVariable Long classId) {
        return Result.ok(personalAchievementService.list(classId));
    }

    @GetMapping("/objectives/{objectiveId}/students")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public Result<List<PersonalAchievementService.StudentPointAchievement>> objectiveStudents(
            @PathVariable Long classId,
            @PathVariable Long objectiveId) {
        return Result.ok(personalAchievementService.listObjectiveStudents(classId, objectiveId));
    }

    @GetMapping("/indicators/{indicatorId}/students")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public Result<List<PersonalAchievementService.StudentPointAchievement>> indicatorStudents(
            @PathVariable Long classId,
            @PathVariable Long indicatorId) {
        return Result.ok(personalAchievementService.listCourseIndicatorStudents(classId, indicatorId));
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public Result<PersonalAchievementService.StudentAchievementDetail> detail(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        return Result.ok(personalAchievementService.getDetail(classId, studentId));
    }

    @GetMapping("/excel")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public ResponseEntity<byte[]> export(@PathVariable Long classId) {
        byte[] excel = personalAchievementService.exportExcel(classId);
        String filename = URLEncoder.encode(
                "个人达成度数据-" + classId + ".xlsx",
                StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }
}
