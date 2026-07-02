package com.healthapp.controller;

import com.healthapp.common.Result;
import com.healthapp.dto.HealthRecordRequest;
import com.healthapp.service.HealthRecordService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/health/record")
public class HealthRecordController {
    private final HealthRecordService healthRecordService;

    public HealthRecordController(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @GetMapping
    public Result<Map<String, Object>> getRecord() {
        return Result.ok(healthRecordService.getRecord());
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody HealthRecordRequest request) {
        return Result.ok(healthRecordService.create(request));
    }
}
