package com.healthapp.controller;

import com.healthapp.common.Result;
import com.healthapp.dto.PasswordUpdateRequest;
import com.healthapp.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserPasswordController {
    private final AuthService authService;

    public UserPasswordController(AuthService authService) {
        this.authService = authService;
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody PasswordUpdateRequest request) {
        authService.updatePassword(request);
        return Result.ok();
    }
}
