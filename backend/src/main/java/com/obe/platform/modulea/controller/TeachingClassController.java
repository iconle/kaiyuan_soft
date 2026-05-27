package com.obe.platform.modulea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.PageResult;
import com.obe.platform.common.Result;
import com.obe.platform.modulea.entity.Course;
import com.obe.platform.modulea.entity.Student;
import com.obe.platform.modulea.entity.TeachingClass;
import com.obe.platform.modulea.mapper.CourseMapper;
import com.obe.platform.modulea.mapper.TeachingClassMapper;
import com.obe.platform.modulea.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teaching-classes")
@RequiredArgsConstructor
public class TeachingClassController {

    private final DictService dictService;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseMapper courseMapper;

    /** Get all classes for the current teacher (for course switcher) */
    @GetMapping("/my-classes")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<List<TeachingClass>> myClasses() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<TeachingClass> classes = teachingClassMapper.selectList(
                new LambdaQueryWrapper<TeachingClass>().eq(TeachingClass::getTeacherId, userId));
        for (TeachingClass tc : classes) {
            Course course = courseMapper.selectById(tc.getCourseId());
            if (course != null) tc.setCourseName(course.getName());
        }
        return Result.ok(classes);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ACADEMIC','DIRECTOR')")
    public Result<PageResult<TeachingClass>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long semesterId) {
        return Result.ok(dictService.listClasses(page, size, courseId, semesterId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<TeachingClass> create(@RequestBody TeachingClass tc) {
        return Result.ok(dictService.createClass(tc));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> update(@PathVariable Long id, @RequestBody TeachingClass tc) {
        dictService.updateClass(id, tc);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> delete(@PathVariable Long id) {
        dictService.deleteClass(id);
        return Result.ok();
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ACADEMIC','TEACHER')")
    public Result<List<Student>> getStudents(@PathVariable Long id) {
        return Result.ok(dictService.getClassStudents(id));
    }

    @PostMapping("/{classId}/students/{studentId}")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> addStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        dictService.addClassStudent(classId, studentId);
        return Result.ok();
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> removeStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        dictService.removeClassStudent(classId, studentId);
        return Result.ok();
    }
}
