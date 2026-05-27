package com.obe.platform.modulea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.obe.platform.common.BizException;
import com.obe.platform.common.PageResult;
import com.obe.platform.modulea.entity.Course;
import com.obe.platform.modulea.entity.SysUser;
import com.obe.platform.modulea.entity.TeachingClass;
import com.obe.platform.modulea.mapper.CourseMapper;
import com.obe.platform.modulea.mapper.SysUserMapper;
import com.obe.platform.modulea.mapper.TeachingClassMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final SysUserMapper userMapper;

    public PageResult<Course> listCourses(long page, long size, Long majorId, String keyword) {
        var wrapper = new LambdaQueryWrapper<Course>();
        if (majorId != null) {
            wrapper.eq(Course::getMajorId, majorId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Course::getName, keyword)
                    .or().like(Course::getCode, keyword));
        }
        wrapper.orderByAsc(Course::getCode);

        Page<Course> result = courseMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public void createCourse(Course course) {
        courseMapper.insert(course);
    }

    @Transactional
    public void updateCourse(Long id, Course course) {
        if (courseMapper.selectById(id) == null) throw new BizException("课程不存在");
        course.setId(id);
        courseMapper.updateById(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course existing = courseMapper.selectById(id);
        if (existing == null) {
            throw new BizException("课程不存在");
        }
        courseMapper.deleteById(id);
    }

    public List<TeachingClass> listClasses(Long courseId) {
        List<TeachingClass> classes = teachingClassMapper.selectList(
                new LambdaQueryWrapper<TeachingClass>()
                        .eq(TeachingClass::getCourseId, courseId)
                        .orderByDesc(TeachingClass::getSemesterId));

        for (TeachingClass tc : classes) {
            Course course = courseMapper.selectById(tc.getCourseId());
            if (course != null) {
                tc.setCourseName(course.getName());
            }
            SysUser teacher = userMapper.selectById(tc.getTeacherId());
            if (teacher != null) {
                tc.setTeacherName(teacher.getRealName());
            }
        }

        return classes;
    }

    @Transactional
    public void createClass(TeachingClass teachingClass) {
        teachingClassMapper.insert(teachingClass);
    }
}
