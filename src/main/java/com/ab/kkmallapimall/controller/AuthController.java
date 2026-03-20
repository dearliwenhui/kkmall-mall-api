package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.dto.request.LoginRequest;
import com.ab.kkmallapimall.dto.request.RegisterRequest;
import com.ab.kkmallapimall.dto.response.LoginResponse;
import com.ab.kkmallapimall.dto.response.UserInfoVO;
import com.ab.kkmallapimall.service.AuthService;
import com.ab.kkmallapimall.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户注册、登录、登出等认证相关接口")
@RestController
@RequestMapping("/api/mall/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success("注册成功", null);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo() {
        Long userId = securityUtils.getCurrentUserId();
        UserInfoVO userInfo = authService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT是无状态的，客户端删除Token即可
        return Result.success("退出成功", null);
    }
}
