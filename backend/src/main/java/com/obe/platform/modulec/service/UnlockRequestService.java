package com.obe.platform.modulec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.BizException;
import com.obe.platform.modulea.entity.SysUser;
import com.obe.platform.modulea.entity.TeachingClass;
import com.obe.platform.modulea.mapper.SysUserMapper;
import com.obe.platform.modulea.mapper.TeachingClassMapper;
import com.obe.platform.modulec.entity.ScoreSheet;
import com.obe.platform.modulec.entity.ScoreUnlockRequest;
import com.obe.platform.modulec.mapper.ScoreSheetMapper;
import com.obe.platform.modulec.mapper.ScoreUnlockRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UnlockRequestService {

    private final ScoreUnlockRequestMapper requestMapper;
    private final ScoreSheetMapper scoreSheetMapper;
    private final SysUserMapper userMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseCalcService courseCalcService;

    /** Teacher: submit an unlock request */
    @Transactional
    public ScoreUnlockRequest submitRequest(Long classId, Long requesterId, String reason) {
        ScoreSheet sheet = scoreSheetMapper.selectOne(
                new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, classId));
        if (sheet == null) throw new BizException("成绩单不存在");
        if (!"LOCKED".equals(sheet.getStatus())) throw new BizException("成绩单未锁定，无需申请勘误");

        // Check for existing pending request
        ScoreUnlockRequest existing = requestMapper.selectOne(
                new LambdaQueryWrapper<ScoreUnlockRequest>()
                        .eq(ScoreUnlockRequest::getSheetId, sheet.getId())
                        .eq(ScoreUnlockRequest::getStatus, "PENDING"));
        if (existing != null) throw new BizException("该成绩单已有待审批的勘误申请，请等待审批");

        ScoreUnlockRequest req = new ScoreUnlockRequest();
        req.setSheetId(sheet.getId());
        req.setClassId(classId);
        req.setRequesterId(requesterId);
        req.setReason(reason);
        req.setStatus("PENDING");
        req.setCreatedAt(LocalDateTime.now());
        requestMapper.insert(req);
        return req;
    }

    /** List requests for ACADEMIC review — shows all requests */
    public List<ScoreUnlockRequest> listRequestsForRole(String roleCode) {
        var wrapper = new LambdaQueryWrapper<ScoreUnlockRequest>();
        wrapper.orderByDesc(ScoreUnlockRequest::getCreatedAt);
        List<ScoreUnlockRequest> list = requestMapper.selectList(wrapper);
        for (ScoreUnlockRequest r : list) {
            SysUser requester = userMapper.selectById(r.getRequesterId());
            if (requester != null) r.setRequesterName(requester.getRealName());
            TeachingClass tc = teachingClassMapper.selectById(r.getClassId());
            if (tc != null) r.setClassName(tc.getClassName());
        }
        return list;
    }

    /** Teacher: get my requests for a class */
    public List<ScoreUnlockRequest> getRequestsByClass(Long classId, Long teacherId) {
        ScoreSheet sheet = scoreSheetMapper.selectOne(
                new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, classId));
        if (sheet == null) return List.of();
        List<ScoreUnlockRequest> list = requestMapper.selectList(
                new LambdaQueryWrapper<ScoreUnlockRequest>()
                        .eq(ScoreUnlockRequest::getSheetId, sheet.getId())
                        .eq(ScoreUnlockRequest::getRequesterId, teacherId)
                        .orderByDesc(ScoreUnlockRequest::getCreatedAt));
        for (ScoreUnlockRequest r : list) {
            if (r.getReviewerId() != null) {
                SysUser reviewer = userMapper.selectById(r.getReviewerId());
                // reviewer name will be set via transient field
            }
        }
        return list;
    }

    /** Teacher: cancel my pending request */
    @Transactional
    public void cancelRequest(Long requestId, Long requesterId) {
        ScoreUnlockRequest req = requestMapper.selectById(requestId);
        if (req == null) throw new BizException("勘误申请不存在");
        if (!requesterId.equals(req.getRequesterId())) throw new BizException("只能撤销自己的申请");
        if (!"PENDING".equals(req.getStatus())) throw new BizException("该申请已处理，无法撤销");
        requestMapper.deleteById(requestId);
    }

    /** Academic: agree that correction is needed */
    @Transactional
    public void approveRequest(Long requestId, Long reviewerId) {
        ScoreUnlockRequest req = requestMapper.selectById(requestId);
        if (req == null) throw new BizException("勘误申请不存在");
        if (!"PENDING".equals(req.getStatus())) throw new BizException("该申请已处理");

        req.setStatus("APPROVED");
        req.setReviewerId(reviewerId);
        req.setReviewedAt(LocalDateTime.now());
        requestMapper.updateById(req);
    }

    /** Academic: final approval — actually unlock the sheet */
    @Transactional
    public void unlockApprovedRequest(Long requestId, Long reviewerId) {
        ScoreUnlockRequest req = requestMapper.selectById(requestId);
        if (req == null) throw new BizException("勘误申请不存在");
        if (!"APPROVED".equals(req.getStatus())) throw new BizException("该申请尚未通过审核");

        ScoreSheet sheet = scoreSheetMapper.selectById(req.getSheetId());
        if (sheet == null) throw new BizException("成绩单不存在");
        if (!"LOCKED".equals(sheet.getStatus())) throw new BizException("成绩单已被解锁，无需重复操作");

        courseCalcService.unlockSheet(req.getSheetId());

        try {
            req.setStatus("UNLOCKED");
            req.setReviewerId(reviewerId);
            req.setReviewedAt(LocalDateTime.now());
            requestMapper.updateById(req);
        } catch (Exception e) {
            requestMapper.deleteById(requestId);
        }
    }

    /** Academic: reject request at any stage */
    @Transactional
    public void rejectRequest(Long requestId, Long reviewerId, String roleCode) {
        ScoreUnlockRequest req = requestMapper.selectById(requestId);
        if (req == null) throw new BizException("勘误申请不存在");
        if (!"PENDING".equals(req.getStatus()) && !"APPROVED".equals(req.getStatus())) {
            throw new BizException("该申请已处理");
        }

        req.setStatus("REJECTED");
        req.setReviewerId(reviewerId);
        req.setReviewedAt(LocalDateTime.now());
        requestMapper.updateById(req);
    }
}
