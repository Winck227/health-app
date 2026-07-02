package com.healthapp.controller;

import com.healthapp.common.Result;
import com.healthapp.dto.LoginRequest;
import com.healthapp.dto.RegisterRequest;
import com.healthapp.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 注册接口直接返回登录态，前端注册成功后无需再额外补一次登录请求。
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    // 登录兼容用户名和手机号，具体校验逻辑统一放在 AuthService。
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    // 会话恢复允许前端把现有 Authorization 头透传进来，避免无意义刷新 token。
    @GetMapping("/session")
    public Result<Map<String, Object>> session(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(authService.session(authorization));
    }

    // 当前版本的退出登录只负责通知前端清理本地会话，不做服务端黑名单。
    @PostMapping("/logout")
    public Result<Map<String, Object>> logout() {
        return Result.ok(authService.logout());
    }
}
