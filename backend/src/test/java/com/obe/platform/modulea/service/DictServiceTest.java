package com.obe.platform.modulea.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.SysCollege;
import com.obe.platform.modulea.mapper.ClassStudentMapper;
import com.obe.platform.modulea.mapper.CourseMapper;
import com.obe.platform.modulea.mapper.StudentMapper;
import com.obe.platform.modulea.mapper.SysAdminClassMapper;
import com.obe.platform.modulea.mapper.SysCollegeMapper;
import com.obe.platform.modulea.mapper.SysDictSemesterMapper;
import com.obe.platform.modulea.mapper.SysMajorMapper;
import com.obe.platform.modulea.mapper.SysUserMapper;
import com.obe.platform.modulea.mapper.TeachingClassMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DictServiceTest {

    private final SysCollegeMapper collegeMapper = mock(SysCollegeMapper.class);
    private final SysMajorMapper majorMapper = mock(SysMajorMapper.class);
    private final SysDictSemesterMapper semesterMapper = mock(SysDictSemesterMapper.class);
    private final TeachingClassMapper teachingClassMapper = mock(TeachingClassMapper.class);
    private final CourseMapper courseMapper = mock(CourseMapper.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final ClassStudentMapper classStudentMapper = mock(ClassStudentMapper.class);
    private final StudentMapper studentMapper = mock(StudentMapper.class);
    private final SysAdminClassMapper adminClassMapper = mock(SysAdminClassMapper.class);

    private final DictService dictService = new DictService(
            collegeMapper,
            majorMapper,
            semesterMapper,
            teachingClassMapper,
            courseMapper,
            userMapper,
            classStudentMapper,
            studentMapper,
            adminClassMapper
    );

    @Test
    void deleteCollegeRejectsMissingCollege() {
        when(collegeMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> dictService.deleteCollege(99L))
                .isInstanceOf(BizException.class)
                .hasMessage("学院不存在");

        verify(collegeMapper, never()).deleteById(99L);
    }

    @Test
    void deleteCollegeRejectsCollegeWithMajors() {
        when(collegeMapper.selectById(12L)).thenReturn(new SysCollege());
        when(majorMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> dictService.deleteCollege(12L))
                .isInstanceOf(BizException.class)
                .hasMessage("该学院下存在专业，请先删除或调整专业后再删除学院");

        verify(collegeMapper, never()).deleteById(12L);
    }

    @Test
    void deleteCollegeDeletesEmptyCollege() {
        when(collegeMapper.selectById(12L)).thenReturn(new SysCollege());
        when(majorMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(studentMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        dictService.deleteCollege(12L);

        verify(collegeMapper).deleteById(12L);
    }
}
