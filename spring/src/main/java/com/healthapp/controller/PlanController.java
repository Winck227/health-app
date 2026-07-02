package com.healthapp.controller;

import com.healthapp.common.Result;
import com.healthapp.dto.CheckinRequest;
import com.healthapp.dto.PlanCreateRequest;
import com.healthapp.service.PlanService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    // 计划列表返回当前用户的全部有效计划，前端再按页面需要拆成今日视图和历史视图。
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(planService.list());
    }

    // 创建接口同时兼容纯手填计划和基于模板快速创建两种入口。
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody PlanCreateRequest request) {
        return Result.ok(planService.create(request));
    }

    // 详情页只允许读取当前用户自己的计划，跨账号访问会被 Service 层拦住。
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(planService.detail(id));
    }

    // 删除采用软删除，避免历史打卡和统计数据被物理清空。
    @DeleteMapping("/{id}")
    public Result<Map<String, Object>> delete(@PathVariable Long id) {
        return Result.ok(planService.delete(id));
    }

    // 打卡请求体可为空，前端不填备注时也能直接完成今日打卡。
    @PostMapping("/{id}/checkin")
    public Result<Map<String, Object>> checkin(@PathVariable Long id, @RequestBody(required = false) CheckinRequest request) {
        return Result.ok(planService.checkin(id, request));
    }
}
