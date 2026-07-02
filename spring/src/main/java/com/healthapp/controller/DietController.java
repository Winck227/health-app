package com.healthapp.controller;

import com.healthapp.common.Result;
import com.healthapp.dto.DietRecordRequest;
import com.healthapp.service.DietService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/diet")
public class DietController {
    private final DietService dietService;

    public DietController(DietService dietService) {
        this.dietService = dietService;
    }

    @GetMapping
    public Result<Map<String, Object>> list() {
        return Result.ok(dietService.list());
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody DietRecordRequest request) {
        return Result.ok(dietService.create(request));
    }

    @DeleteMapping("/{id}")
    public Result<Map<String, Object>> delete(@PathVariable Long id) {
        return Result.ok(dietService.delete(id));
    }
}
