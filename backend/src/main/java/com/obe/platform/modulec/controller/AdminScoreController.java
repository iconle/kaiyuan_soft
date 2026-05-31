package com.obe.platform.modulec.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.platform.common.Result;
import com.obe.platform.modulea.entity.TeachingClass;
import com.obe.platform.modulea.mapper.TeachingClassMapper;
import com.obe.platform.modulec.entity.ScoreSheet;
import com.obe.platform.modulec.entity.ScoreUnlockRequest;
import com.obe.platform.modulec.mapper.ScoreSheetMapper;
import com.obe.platform.modulec.service.CourseCalcService;
import com.obe.platform.modulec.service.UnlockRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminScoreController {

    private final ScoreSheetMapper scoreSheetMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final UnlockRequestService unlockRequestService;
    private final CourseCalcService courseCalcService;

    private String currentRoleCode() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().findFirst().map(GrantedAuthority::getAuthority)
                .orElse("").replace("ROLE_", "");
    }

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** Emergency direct unlock — bypasses work order flow (ACADEMIC only) */
    @PostMapping("/scores/{sheetId}/unlock")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> directUnlock(@PathVariable Long sheetId) {
        courseCalcService.unlockSheet(sheetId);
        return Result.ok();
    }

    @GetMapping("/scores")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<List<Map<String, Object>>> listSheets() {
        List<ScoreSheet> sheets = scoreSheetMapper.selectList(
                new LambdaQueryWrapper<ScoreSheet>().orderByDesc(ScoreSheet::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ScoreSheet s : sheets) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("classId", s.getClassId());
            item.put("status", s.getStatus());
            item.put("lockedAt", s.getLockedAt());
            item.put("lockedBy", s.getLockedBy());
            TeachingClass tc = teachingClassMapper.selectById(s.getClassId());
            item.put("className", tc != null ? tc.getClassName() : String.valueOf(s.getClassId()));
            result.add(item);
        }
        return Result.ok(result);
    }

    @GetMapping("/unlock-requests")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<List<ScoreUnlockRequest>> listRequests() {
        return Result.ok(unlockRequestService.listRequestsForRole(currentRoleCode()));
    }

    /** Academic: first-level review — agree correction is needed */
    @PostMapping("/unlock-requests/{id}/approve")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> approveRequest(@PathVariable Long id) {
        unlockRequestService.approveRequest(id, currentUserId());
        return Result.ok();
    }

    /** Academic: final review — unlock the sheet */
    @PostMapping("/unlock-requests/{id}/unlock")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> unlockApprovedRequest(@PathVariable Long id) {
        unlockRequestService.unlockApprovedRequest(id, currentUserId());
        return Result.ok();
    }

    /** Academic: reject request */
    @PostMapping("/unlock-requests/{id}/reject")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> rejectRequest(@PathVariable Long id) {
        unlockRequestService.rejectRequest(id, currentUserId(), currentRoleCode());
        return Result.ok();
    }
}
