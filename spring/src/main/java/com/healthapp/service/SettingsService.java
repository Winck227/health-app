package com.healthapp.service;

import com.healthapp.dto.SettingsUpdateRequest;
import com.healthapp.entity.UserSettings;
import com.healthapp.mapper.UserSettingsMapper;
import com.healthapp.security.UserContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SettingsService {
    private final UserSettingsMapper settingsMapper;

    public SettingsService(UserSettingsMapper settingsMapper) {
        this.settingsMapper = settingsMapper;
    }

    // 首次进入设置页时自动补默认记录，前端始终能拿到完整配置。
    public Map<String, Object> getSettings() {
        Long userId = UserContext.getUserId();
        settingsMapper.ensureDefault(userId);
        return toDto(settingsMapper.findByUserId(userId));
    }

    // 这里只更新显式传入的字段，避免单个表单误覆盖其他设置项。
    public Map<String, Object> update(SettingsUpdateRequest request) {
        Long userId = UserContext.getUserId();
        settingsMapper.ensureDefault(userId);
        Integer notify = request.getNotifyEnabled() == null ? null : (request.getNotifyEnabled() ? 1 : 0);
        Integer goal = request.getStepGoal();
        if (goal != null) {
            if (goal < 1000) goal = 1000;
            if (goal > 50000) goal = 50000;
        }
        String privacy = request.getPrivacyLevel();
        if (privacy != null && !(privacy.equals("public") || privacy.equals("partial") || privacy.equals("private"))) {
            privacy = "partial";
        }
        if (notify != null || goal != null || privacy != null) {
            settingsMapper.updatePartial(userId, notify, goal, privacy);
        }
        return toDto(settingsMapper.findByUserId(userId));
    }

    // 同时输出 camelCase 和下划线字段，兼容新旧页面以及管理端表格读取。
    public Map<String, Object> toDto(UserSettings settings) {
        Map<String, Object> map = new HashMap<>();
        Long userId = settings == null ? null : settings.getUserId();
        boolean notify = settings == null || settings.getNotifyEnabled() == null || settings.getNotifyEnabled() == 1;
        int goal = settings == null || settings.getStepGoal() == null ? 8000 : settings.getStepGoal();
        String privacy = settings == null || settings.getPrivacyLevel() == null ? "partial" : settings.getPrivacyLevel();
        map.put("userId", userId);
        map.put("user_id", userId);
        map.put("notifyEnabled", notify);
        map.put("notify_enabled", notify ? 1 : 0);
        map.put("stepGoal", goal);
        map.put("step_goal", goal);
        map.put("privacyLevel", privacy);
        map.put("privacy_level", privacy);
        return map;
    }
}
