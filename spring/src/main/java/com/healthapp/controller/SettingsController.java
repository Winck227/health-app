package com.healthapp.controller;

import com.healthapp.common.Result;
import com.healthapp.dto.SettingsUpdateRequest;
import com.healthapp.service.SettingsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        return Result.ok(settingsService.getSettings());
    }

    @PutMapping
    public Result<Map<String, Object>> update(@RequestBody SettingsUpdateRequest request) {
        return Result.ok(settingsService.update(request));
    }
}
