package com.healthapp.service;

import com.healthapp.common.BusinessException;
import com.healthapp.dto.ProfileUpdateRequest;
import com.healthapp.entity.User;
import com.healthapp.mapper.UserMapper;
import com.healthapp.security.UserContext;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ProfileService {
    private final UserMapper userMapper;
    private final AuthService authService;

    public ProfileService(UserMapper userMapper, AuthService authService) {
        this.userMapper = userMapper;
        this.authService = authService;
    }

    // 个人资料页直接复用统一用户 DTO，避免昵称、头像等字段重复组装。
    public Map<String, Object> currentProfile() {
        User user = requireCurrentUser();
        return authService.userDto(user);
    }

    // 更新资料时允许局部修改，未显式传入的字段继续沿用数据库原值。
    public Map<String, Object> updateProfile(ProfileUpdateRequest request) {
        User user = requireCurrentUser();
        if (request == null) {
            return authService.userDto(user);
        }
        String nextPhone = trimToNull(request.getPhone());
        if (nextPhone != null && userMapper.countByPhone(nextPhone, user.getId()) > 0) {
            throw new BusinessException("手机号已被使用");
        }
        String nickname = trimToNull(request.getNickname());
        if (nickname == null) nickname = trimToNull(request.getName());
        user.setNickname(nickname == null ? user.getNickname() : nickname);
        user.setPhone(nextPhone);
        user.setGender(trimToDefault(request.getGender(), "unknown"));
        user.setBirthday(request.getBirthday());

        // 只有请求体显式携带 avatar 时才更新头像，避免“保存资料”误清空已上传头像。
        if (request.getAvatar() != null) {
            user.setAvatar(trimToDefault(request.getAvatar(), ""));
        }
        userMapper.updateProfile(user);
        return authService.userDto(userMapper.findById(user.getId()));
    }

    // 所有个人资料操作都以当前登录用户为准，不接受前端指定 userId。
    private User requireCurrentUser() {
        User user = userMapper.findById(UserContext.getUserId());
        if (user == null) throw new BusinessException(401, "用户不存在");
        return user;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String trimToDefault(String value, String fallback) {
        String text = trimToNull(value);
        return text == null ? fallback : text;
    }
}
