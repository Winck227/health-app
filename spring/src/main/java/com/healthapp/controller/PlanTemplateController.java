package com.healthapp.controller;

import com.healthapp.common.Result;
import com.healthapp.service.PlanTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plan-templates")
public class PlanTemplateController {
    private final PlanTemplateService planTemplateService;

    public PlanTemplateController(PlanTemplateService planTemplateService) {
        this.planTemplateService = planTemplateService;
    }

    @GetMapping("/version")
    public Result<Map<String, Object>> version() {
        return Result.ok(planTemplateService.version());
    }

    @GetMapping
    public Result<List<Map<String, Object>>> templates() {
        return Result.ok(planTemplateService.templates());
    }
}
