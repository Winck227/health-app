package com.healthapp.controller;

import com.healthapp.common.Result;
import com.healthapp.dto.ProfileUpdateRequest;
import com.healthapp.service.ProfileAvatarService;
import com.healthapp.service.ProfileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final ProfileAvatarService profileAvatarService;

    public ProfileController(ProfileService profileService, ProfileAvatarService profileAvatarService) {
        this.profileService = profileService;
        this.profileAvatarService = profileAvatarService;
    }

    // 个人资料页读取统一用户 DTO，前端不需要分别请求头像和基本信息。
    @GetMapping
    public Result<Map<String, Object>> profile() {
        return Result.ok(profileService.currentProfile());
    }

    // 基本资料更新和头像上传拆开处理，避免 multipart 和 JSON 表单互相干扰。
    @PutMapping
    public Result<Map<String, Object>> update(@RequestBody ProfileUpdateRequest request) {
        return Result.ok(profileService.updateProfile(request));
    }

    /**
     * 头像上传接口。
     * 前端 uni.uploadFile 必须使用 name: 'file'，请求地址为 POST /api/profile/avatar。
     */
    @PostMapping(value = {"/avatar", "/avatar/"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(profileAvatarService.uploadAvatar(file));
    }

    /**
     * 清除头像接口。
     */
    @DeleteMapping(value = {"/avatar", "/avatar/"})
    public Result<Map<String, Object>> clearAvatar() {
        return Result.ok(profileAvatarService.clearAvatar());
    }
}
