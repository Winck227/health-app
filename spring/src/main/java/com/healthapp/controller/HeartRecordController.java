package com.healthapp.controller;

import com.healthapp.common.Result;
import com.healthapp.dto.HeartRecordRequest;
import com.healthapp.service.HeartRecordService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/heart")
public class HeartRecordController {
    private final HeartRecordService heartRecordService;

    public HeartRecordController(HeartRecordService heartRecordService) {
        this.heartRecordService = heartRecordService;
    }

    @GetMapping
    public Result<Map<String, Object>> list() {
        return Result.ok(heartRecordService.list());
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody HeartRecordRequest request) {
        return Result.ok(heartRecordService.create(request));
    }
}
