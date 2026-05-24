package com.obe.platform.modulea.controller;

import com.obe.platform.common.PageResult;
import com.obe.platform.common.Result;
import com.obe.platform.modulea.entity.Course;
import com.obe.platform.modulea.entity.TeachingClass;
import com.obe.platform.modulea.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACADEMIC','DIRECTOR')")
    public Result<PageResult<Course>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(courseService.listCourses(page, size, majorId, keyword));
    }

    @PostMapping
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Course> create(@RequestBody Course course) {
        courseService.createCourse(course);
        return Result.ok(course);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Course course) {
        courseService.updateCourse(id, course);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return Result.ok();
    }

    @GetMapping("/{id}/classes")
    public Result<List<TeachingClass>> listClasses(@PathVariable Long id) {
        return Result.ok(courseService.listClasses(id));
    }

    @PostMapping("/{id}/classes")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> createClass(@PathVariable Long id, @RequestBody TeachingClass teachingClass) {
        teachingClass.setCourseId(id);
        courseService.createClass(teachingClass);
        return Result.ok();
    }
}
