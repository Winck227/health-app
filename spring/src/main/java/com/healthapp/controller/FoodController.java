package com.healthapp.controller;

import com.healthapp.common.Result;
import com.healthapp.service.FoodService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/foods")
public class FoodController {
    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping("/version")
    public Result<Map<String, Object>> version() {
        return Result.ok(foodService.version());
    }

    @GetMapping
    public Result<Map<String, Object>> foods() {
        return Result.ok(foodService.foods());
    }
}
