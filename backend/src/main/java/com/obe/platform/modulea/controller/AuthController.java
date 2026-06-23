package com.obe.platform.modulea.controller;

import com.obe.platform.common.Result;
import com.obe.platform.modulea.dto.CurrentUserResponse;
import com.obe.platform.modulea.dto.LoginRequest;
import com.obe.platform.modulea.dto.LoginResponse;
import com.obe.platform.modulea.dto.PasswordChangeRequest;
import com.obe.platform.modulea.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(request);
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<CurrentUserResponse> getCurrentUser() {
        return Result.ok(authService.getCurrentUser());
    }
}
