package com.obe.platform.modulea.controller;

import com.obe.platform.common.PageResult;
import com.obe.platform.common.Result;
import com.obe.platform.modulea.entity.Student;
import com.obe.platform.modulea.entity.SysAdminClass;
import com.obe.platform.modulea.entity.SysCollege;
import com.obe.platform.modulea.entity.SysDictSemester;
import com.obe.platform.modulea.entity.SysMajor;
import com.obe.platform.modulea.entity.TeachingClass;
import com.obe.platform.modulea.service.AdminClassImportService;
import com.obe.platform.modulea.service.DictService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;
    private final AdminClassImportService adminClassImportService;

    // ========== 学院 ==========

    @GetMapping("/colleges")
    public Result<List<SysCollege>> listColleges() {
        return Result.ok(dictService.listColleges());
    }

    @PostMapping("/colleges")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> createCollege(@RequestBody Map<String, String> body) {
        dictService.createCollege(body.get("name"));
        return Result.ok();
    }

    @PutMapping("/colleges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateCollege(@PathVariable Long id, @RequestBody Map<String, String> body) {
        dictService.updateCollege(id, body.get("name"));
        return Result.ok();
    }

    @DeleteMapping("/colleges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCollege(@PathVariable Long id) {
        dictService.deleteCollege(id);
        return Result.ok();
    }

    // ========== 专业 ==========

    @GetMapping("/majors")
    public Result<PageResult<SysMajor>> listMajors(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long collegeId) {
        return Result.ok(dictService.listMajors(page, size, collegeId));
    }

    @PostMapping("/majors")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> createMajor(@RequestBody SysMajor major) {
        dictService.createMajor(major);
        return Result.ok();
    }

    @PutMapping("/majors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateMajor(@PathVariable Long id, @RequestBody SysMajor major) {
        major.setId(id);
        dictService.updateMajor(major);
        return Result.ok();
    }

    // ========== 学年学期 ==========

    @GetMapping("/semesters")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC','DIRECTOR','TEACHER')")
    public Result<List<SysDictSemester>> listSemesters() {
        return Result.ok(dictService.listSemesters());
    }

    @PostMapping("/semesters")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> createSemester(@RequestBody SysDictSemester semester) {
        dictService.createSemester(semester);
        return Result.ok();
    }

    @DeleteMapping("/semesters/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteSemester(@PathVariable Long id) {
        dictService.deleteSemester(id);
        return Result.ok();
    }

    // ========== 行政班级 ==========

    @GetMapping("/admin-classes")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<PageResult<SysAdminClass>> listAdminClasses(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long majorId) {
        return Result.ok(dictService.listAdminClasses(page, size, majorId));
    }

    @PostMapping("/admin-classes")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<SysAdminClass> createAdminClass(@RequestBody SysAdminClass ac) {
        return Result.ok(dictService.createAdminClass(ac));
    }

    @GetMapping("/admin-classes/import-template")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public ResponseEntity<byte[]> downloadAdminClassTemplate() {
        byte[] data = adminClassImportService.generateTemplate();
        String filename = URLEncoder.encode("行政班级导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/admin-classes/import")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<Integer> importAdminClasses(@RequestParam("file") MultipartFile file) {
        return Result.ok(adminClassImportService.importAdminClasses(file));
    }

    @PutMapping("/admin-classes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<Void> updateAdminClass(@PathVariable Long id, @RequestBody SysAdminClass ac) {
        dictService.updateAdminClass(id, ac);
        return Result.ok();
    }

    @DeleteMapping("/admin-classes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<Void> deleteAdminClass(@PathVariable Long id) {
        dictService.deleteAdminClass(id);
        return Result.ok();
    }

    @GetMapping("/admin-classes/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<List<Student>> getAdminClassStudents(@PathVariable Long id) {
        return Result.ok(dictService.getAdminClassStudents(id));
    }

    @PostMapping("/admin-classes/{classId}/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<Void> addAdminClassStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        dictService.addAdminClassStudent(classId, studentId);
        return Result.ok();
    }

    @DeleteMapping("/admin-classes/{classId}/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<Void> removeAdminClassStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        dictService.removeAdminClassStudent(classId, studentId);
        return Result.ok();
    }
}
