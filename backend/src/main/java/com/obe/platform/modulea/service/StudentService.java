package com.obe.platform.modulea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.obe.platform.common.BizException;
import com.obe.platform.common.PageResult;
import com.obe.platform.modulea.entity.ClassStudent;
import com.obe.platform.modulea.entity.SysAdminClass;
import com.obe.platform.modulea.entity.Student;
import com.obe.platform.modulea.mapper.ClassStudentMapper;
import com.obe.platform.modulea.mapper.StudentMapper;
import com.obe.platform.modulea.mapper.SysAdminClassMapper;
import com.obe.platform.modulea.mapper.SysCollegeMapper;
import com.obe.platform.modulea.mapper.SysMajorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentMapper studentMapper;
    private final ClassStudentMapper classStudentMapper;
    private final SysAdminClassMapper adminClassMapper;
    private final SysCollegeMapper collegeMapper;
    private final SysMajorMapper majorMapper;

    public PageResult<Student> listStudents(long page, long size, String keyword,
                                             Long collegeId, Long majorId, Long adminClassId, Integer enrollmentYear) {
        var wrapper = new LambdaQueryWrapper<Student>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Student::getName, keyword)
                    .or().like(Student::getStudentNo, keyword));
        }
        if (collegeId != null) {
            wrapper.eq(Student::getCollegeId, collegeId);
        }
        if (majorId != null) {
            wrapper.eq(Student::getMajorId, majorId);
        }
        if (adminClassId != null) {
            wrapper.eq(Student::getAdminClassId, adminClassId);
        }
        if (enrollmentYear != null) {
            wrapper.eq(Student::getEnrollmentYear, enrollmentYear);
        }
        wrapper.orderByAsc(Student::getStudentNo);

        Page<Student> result = studentMapper.selectPage(new Page<>(page, size), wrapper);
        // Fill display names via join tables
        for (Student s : result.getRecords()) {
            if (s.getCollegeId() != null) {
                s.setCollegeName(collegeMapper.selectById(s.getCollegeId()).getName());
            }
            if (s.getMajorId() != null) {
                var major = majorMapper.selectById(s.getMajorId());
                if (major != null) s.setMajorName(major.getName());
            }
            if (s.getAdminClassId() != null) {
                var ac = adminClassMapper.selectById(s.getAdminClassId());
                if (ac != null) s.setAdminClassName(ac.getClassName());
            }
        }
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public Student createStudent(Student student) {
        studentMapper.insert(student);
        return student;
    }

    @Transactional
    public void updateStudent(Student student) {
        if (studentMapper.selectById(student.getId()) == null)
            throw new BizException("学生不存在");
        studentMapper.updateById(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        // Remove from class_student associations
        classStudentMapper.delete(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getStudentId, id));
        studentMapper.deleteById(id);
    }

    @Transactional
    public int importStudents(List<Student> students) {
        int imported = 0;
        for (Student student : students) {
            Long count = studentMapper.selectCount(
                    new LambdaQueryWrapper<Student>()
                            .eq(Student::getStudentNo, student.getStudentNo()));
            if (count == 0) {
                studentMapper.insert(student);
                imported++;
            }
        }
        return imported;
    }

    public List<Student> getStudentsByClass(Long classId) {
        List<ClassStudent> classStudents = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, classId));

        if (classStudents.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> studentIds = classStudents.stream()
                .map(ClassStudent::getStudentId)
                .collect(Collectors.toList());

        return studentMapper.selectBatchIds(studentIds);
    }
}
