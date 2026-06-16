package com.obe.platform.modulea.controller;

import com.obe.platform.common.PageResult;
import com.obe.platform.common.Result;
import com.obe.platform.modulea.entity.Student;
import com.obe.platform.modulea.service.StudentImportService;
import com.obe.platform.modulea.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentImportService studentImportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<PageResult<Student>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Long adminClassId,
            @RequestParam(required = false) Integer enrollmentYear) {
        return Result.ok(studentService.listStudents(page, size, keyword, collegeId, majorId, adminClassId, enrollmentYear));
    }

    @GetMapping("/class/{classId}")
    public Result<List<Student>> listByClass(@PathVariable Long classId) {
        return Result.ok(studentService.getStudentsByClass(classId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Student> create(@RequestBody Student student) {
        return Result.ok(studentService.createStudent(student));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        studentService.updateStudent(student);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.ok();
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Integer> importStudents(@RequestBody List<Student> students) {
        int imported = studentService.importStudents(students);
        return Result.ok(imported);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC')")
    public byte[] downloadTemplate() throws Exception {
        return studentImportService.generateTemplate();
    }

    @PostMapping("/import-excel")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Integer> importExcel(@RequestParam("file") MultipartFile file) {
        int imported = studentImportService.importStudents(file);
        return Result.ok(imported);
    }
}
