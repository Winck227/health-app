package com.healthapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthapp.common.Result;
import com.healthapp.dto.LoginRequest;
import com.healthapp.dto.PasswordUpdateRequest;
import com.healthapp.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return Result.ok(adminService.login(request));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(adminService.stats());
    }

    @GetMapping("/users")
    public Result<Map<String, Object>> users(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        return Result.ok(adminService.users(keyword, page, size));
    }

    @PostMapping("/users")
    public Result<Map<String, Object>> createUser(@RequestBody JsonNode body) {
        return Result.ok(adminService.createUser(body));
    }

    @PutMapping("/users/{id}")
    public Result<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody JsonNode body) {
        return Result.ok(adminService.updateUser(id, body));
    }

    @DeleteMapping("/users/{id}")
    public Result<Map<String, Object>> deleteUser(@PathVariable Long id) {
        return Result.ok(adminService.deleteUser(id));
    }

    @PutMapping("/user/{id}/password")
    public Result<Map<String, Object>> resetPassword(@PathVariable Long id, @RequestBody PasswordUpdateRequest request) {
        return Result.ok(adminService.resetPassword(id, request));
    }

    @GetMapping("/diet-records")
    public Result<Map<String, Object>> dietRecords(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        return Result.ok(adminService.listDietRecords(keyword, page, size));
    }

    @PostMapping("/diet-records")
    public Result<Map<String, Object>> createDietRecord(@RequestBody JsonNode body) {
        return Result.ok(adminService.createDietRecord(body));
    }

    @PutMapping("/diet-records/{id}")
    public Result<Map<String, Object>> updateDietRecord(@PathVariable Long id, @RequestBody JsonNode body) {
        return Result.ok(adminService.updateDietRecord(id, body));
    }

    @DeleteMapping("/diet-records/{id}")
    public Result<Map<String, Object>> deleteDietRecord(@PathVariable Long id) {
        return Result.ok(adminService.deleteTableRow("diet_record", id));
    }

    @GetMapping("/health-records")
    public Result<Map<String, Object>> healthRecords(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        return Result.ok(adminService.listHealthRecords(keyword, page, size));
    }

    @PostMapping("/health-records")
    public Result<Map<String, Object>> createHealthRecord(@RequestBody JsonNode body) {
        return Result.ok(adminService.createHealthRecord(body));
    }

    @PutMapping("/health-records/{id}")
    public Result<Map<String, Object>> updateHealthRecord(@PathVariable Long id, @RequestBody JsonNode body) {
        return Result.ok(adminService.updateHealthRecord(id, body));
    }

    @DeleteMapping("/health-records/{id}")
    public Result<Map<String, Object>> deleteHealthRecord(@PathVariable Long id) {
        return Result.ok(adminService.deleteTableRow("health_record", id));
    }

    @GetMapping("/heart-records")
    public Result<Map<String, Object>> heartRecords(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        return Result.ok(adminService.listHeartRecords(keyword, page, size));
    }

    @PostMapping("/heart-records")
    public Result<Map<String, Object>> createHeartRecord(@RequestBody JsonNode body) {
        return Result.ok(adminService.createHeartRecord(body));
    }

    @PutMapping("/heart-records/{id}")
    public Result<Map<String, Object>> updateHeartRecord(@PathVariable Long id, @RequestBody JsonNode body) {
        return Result.ok(adminService.updateHeartRecord(id, body));
    }

    @DeleteMapping("/heart-records/{id}")
    public Result<Map<String, Object>> deleteHeartRecord(@PathVariable Long id) {
        return Result.ok(adminService.deleteTableRow("heart_record", id));
    }

    @GetMapping("/user-plans")
    public Result<Map<String, Object>> userPlans(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        return Result.ok(adminService.listUserPlans(keyword, page, size));
    }

    @PostMapping("/user-plans")
    public Result<Map<String, Object>> createUserPlan(@RequestBody JsonNode body) {
        return Result.ok(adminService.createUserPlan(body));
    }

    @PutMapping("/user-plans/{id}")
    public Result<Map<String, Object>> updateUserPlan(@PathVariable Long id, @RequestBody JsonNode body) {
        return Result.ok(adminService.updateUserPlan(id, body));
    }

    @DeleteMapping("/user-plans/{id}")
    public Result<Map<String, Object>> deleteUserPlan(@PathVariable Long id) {
        return Result.ok(adminService.deleteTableRow("user_plan", id));
    }

    @GetMapping("/plan-checkins")
    public Result<Map<String, Object>> planCheckins(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        return Result.ok(adminService.listPlanCheckins(keyword, page, size));
    }

    @PostMapping("/plan-checkins")
    public Result<Map<String, Object>> createPlanCheckin(@RequestBody JsonNode body) {
        return Result.ok(adminService.createPlanCheckin(body));
    }

    @PutMapping("/plan-checkins/{id}")
    public Result<Map<String, Object>> updatePlanCheckin(@PathVariable Long id, @RequestBody JsonNode body) {
        return Result.ok(adminService.updatePlanCheckin(id, body));
    }

    @DeleteMapping("/plan-checkins/{id}")
    public Result<Map<String, Object>> deletePlanCheckin(@PathVariable Long id) {
        return Result.ok(adminService.deleteTableRow("plan_checkin", id));
    }

    @GetMapping("/article-favorites")
    public Result<Map<String, Object>> articleFavorites(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        return Result.ok(adminService.listArticleFavorites(keyword, page, size));
    }

    @PostMapping("/article-favorites")
    public Result<Map<String, Object>> createArticleFavorite(@RequestBody JsonNode body) {
        return Result.ok(adminService.createArticleFavorite(body));
    }

    @PutMapping("/article-favorites/{id}")
    public Result<Map<String, Object>> updateArticleFavorite(@PathVariable Long id, @RequestBody JsonNode body) {
        return Result.ok(adminService.updateArticleFavorite(id, body));
    }

    @DeleteMapping("/article-favorites/{id}")
    public Result<Map<String, Object>> deleteArticleFavorite(@PathVariable Long id) {
        return Result.ok(adminService.deleteTableRow("article_favorite", id));
    }

    @GetMapping("/user-settings")
    public Result<Map<String, Object>> userSettings(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        return Result.ok(adminService.listUserSettings(keyword, page, size));
    }

    @PostMapping("/user-settings")
    public Result<Map<String, Object>> createUserSettings(@RequestBody JsonNode body) {
        return Result.ok(adminService.createUserSettings(body));
    }

    @PutMapping("/user-settings/{id}")
    public Result<Map<String, Object>> updateUserSettings(@PathVariable Long id, @RequestBody JsonNode body) {
        return Result.ok(adminService.updateUserSettings(id, body));
    }

    @DeleteMapping("/user-settings/{id}")
    public Result<Map<String, Object>> deleteUserSettings(@PathVariable Long id) {
        return Result.ok(adminService.deleteTableRow("user_settings", id));
    }
}
