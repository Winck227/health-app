package com.healthapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthapp.common.BusinessException;
import com.healthapp.dto.LoginRequest;
import com.healthapp.dto.PasswordUpdateRequest;
import com.healthapp.entity.User;
import com.healthapp.mapper.DietRecordMapper;
import com.healthapp.mapper.HealthRecordMapper;
import com.healthapp.mapper.UserMapper;
import com.healthapp.mapper.UserPlanMapper;
import com.healthapp.security.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("MM/dd");

    private final UserMapper userMapper;
    private final DietRecordMapper dietRecordMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final UserPlanMapper userPlanMapper;
    private final AuthService authService;
    private final AdminUserService adminUserService;
    private final AdminTableService adminTableService;
    private final JdbcTemplate jdbcTemplate;

    public AdminService(
            UserMapper userMapper,
            DietRecordMapper dietRecordMapper,
            HealthRecordMapper healthRecordMapper,
            UserPlanMapper userPlanMapper,
            AuthService authService,
            AdminUserService adminUserService,
            AdminTableService adminTableService,
            JdbcTemplate jdbcTemplate) {
        this.userMapper = userMapper;
        this.dietRecordMapper = dietRecordMapper;
        this.healthRecordMapper = healthRecordMapper;
        this.userPlanMapper = userPlanMapper;
        this.authService = authService;
        this.adminUserService = adminUserService;
        this.adminTableService = adminTableService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> login(LoginRequest request) {
        Map<String, Object> payload = authService.login(request);
        Object userObject = payload.get("user");
        if (!(userObject instanceof Map<?, ?> userMap) || !"ADMIN".equalsIgnoreCase(text(userMap.get("role")))) {
            throw new BusinessException(403, "当前账号不是管理员，不能登录后台管理端");
        }
        return payload;
    }

    public Map<String, Object> stats() {
        requireAdmin();
        List<User> users = userMapper.selectAll();
        Map<LocalDate, Long> trendCounts = users.stream()
                .filter(user -> user.getCreatedAt() != null)
                .collect(Collectors.groupingBy(user -> user.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<Map<String, Object>> userTrend = new ArrayList<>();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", date.format(LABEL_FORMAT));
            point.put("value", trendCounts.getOrDefault(date, 0L));
            userTrend.add(point);
        }

        List<Map<String, Object>> cards = List.of(
                card("users", "用户总数", userMapper.countAll()),
                card("diet", "饮食记录", dietRecordMapper.countAll()),
                card("health", "健康记录", healthRecordMapper.countAll()),
                card("plans", "计划总数", userPlanMapper.countAll()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cards", cards);
        result.put("userTrend", userTrend);
        result.put("latest", latestMetrics());
        return result;
    }

    public Map<String, Object> users(String keyword, int page, int size) {
        return adminUserService.users(keyword, page, size);
    }

    public Map<String, Object> createUser(JsonNode body) {
        return adminUserService.createUser(body);
    }

    public Map<String, Object> updateUser(Long id, JsonNode body) {
        return adminUserService.updateUser(id, body);
    }

    public Map<String, Object> deleteUser(Long id) {
        return adminUserService.deleteUser(id);
    }

    public Map<String, Object> resetPassword(Long userId, PasswordUpdateRequest request) {
        return adminUserService.resetPassword(userId, request);
    }

    public Map<String, Object> listDietRecords(String keyword, int page, int size) {
        return adminTableService.listDietRecords(keyword, page, size);
    }

    public Map<String, Object> createDietRecord(JsonNode body) {
        return adminTableService.createDietRecord(body);
    }

    public Map<String, Object> updateDietRecord(Long id, JsonNode body) {
        return adminTableService.updateDietRecord(id, body);
    }

    public Map<String, Object> listHealthRecords(String keyword, int page, int size) {
        return adminTableService.listHealthRecords(keyword, page, size);
    }

    public Map<String, Object> createHealthRecord(JsonNode body) {
        return adminTableService.createHealthRecord(body);
    }

    public Map<String, Object> updateHealthRecord(Long id, JsonNode body) {
        return adminTableService.updateHealthRecord(id, body);
    }

    public Map<String, Object> listHeartRecords(String keyword, int page, int size) {
        return adminTableService.listHeartRecords(keyword, page, size);
    }

    public Map<String, Object> createHeartRecord(JsonNode body) {
        return adminTableService.createHeartRecord(body);
    }

    public Map<String, Object> updateHeartRecord(Long id, JsonNode body) {
        return adminTableService.updateHeartRecord(id, body);
    }

    public Map<String, Object> listUserPlans(String keyword, int page, int size) {
        return adminTableService.listUserPlans(keyword, page, size);
    }

    public Map<String, Object> createUserPlan(JsonNode body) {
        return adminTableService.createUserPlan(body);
    }

    public Map<String, Object> updateUserPlan(Long id, JsonNode body) {
        return adminTableService.updateUserPlan(id, body);
    }

    public Map<String, Object> listPlanCheckins(String keyword, int page, int size) {
        return adminTableService.listPlanCheckins(keyword, page, size);
    }

    public Map<String, Object> createPlanCheckin(JsonNode body) {
        return adminTableService.createPlanCheckin(body);
    }

    public Map<String, Object> updatePlanCheckin(Long id, JsonNode body) {
        return adminTableService.updatePlanCheckin(id, body);
    }

    public Map<String, Object> listArticleFavorites(String keyword, int page, int size) {
        return adminTableService.listArticleFavorites(keyword, page, size);
    }

    public Map<String, Object> createArticleFavorite(JsonNode body) {
        return adminTableService.createArticleFavorite(body);
    }

    public Map<String, Object> updateArticleFavorite(Long id, JsonNode body) {
        return adminTableService.updateArticleFavorite(id, body);
    }

    public Map<String, Object> listUserSettings(String keyword, int page, int size) {
        return adminTableService.listUserSettings(keyword, page, size);
    }

    public Map<String, Object> createUserSettings(JsonNode body) {
        return adminTableService.createUserSettings(body);
    }

    public Map<String, Object> updateUserSettings(Long id, JsonNode body) {
        return adminTableService.updateUserSettings(id, body);
    }

    public Map<String, Object> deleteTableRow(String table, Long id) {
        return adminTableService.deleteTableRow(table, id);
    }

    private Map<String, Object> latestMetrics() {
        Map<String, Object> latest = new LinkedHashMap<>();
        latest.put("latestDiet", firstValue("SELECT food_name FROM diet_record ORDER BY record_time DESC, id DESC LIMIT 1"));
        latest.put("latestHeart", firstValue("SELECT heart_rate FROM heart_record ORDER BY record_time DESC, id DESC LIMIT 1"));
        latest.put("latestPlan", firstValue("SELECT title FROM user_plan ORDER BY created_at DESC, id DESC LIMIT 1"));
        return latest;
    }

    private Object firstValue(String sql) {
        List<Object> values = jdbcTemplate.queryForList(sql, Object.class);
        return values.isEmpty() ? null : values.get(0);
    }

    private void requireAdmin() {
        Long userId = UserContext.getUserId();
        User user = userMapper.findById(userId);
        if (user == null || !"ADMIN".equalsIgnoreCase(text(user.getRole()))) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    private Map<String, Object> card(String key, String label, long value) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("key", key);
        card.put("label", label);
        card.put("value", value);
        return card;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
