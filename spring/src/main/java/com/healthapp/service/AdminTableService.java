package com.healthapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.common.BusinessException;
import com.healthapp.entity.User;
import com.healthapp.mapper.UserMapper;
import com.healthapp.security.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminTableService {
    // 服务说明：只处理管理员端 8 张数据库表的通用增删改查，避免 AdminService 继续膨胀。
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public AdminTableService(UserMapper userMapper, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> listDietRecords(String keyword, int page, int size) {
        requireAdmin();
        return joinedPage("diet_record", "d", "d.id, d.user_id, u.username, u.phone, u.nickname, d.meal_type, d.food_name, d.weight, d.calories, d.record_time, d.created_at", keyword, "ORDER BY d.record_time DESC, d.id DESC", page, size);
    }

    public Map<String, Object> createDietRecord(JsonNode body) {
        requireAdmin();
        Map<String, Object> data = toMap(body);
        jdbcTemplate.update("INSERT INTO diet_record (user_id, meal_type, food_name, weight, calories, record_time) VALUES (?, ?, ?, ?, ?, ?)",
            requireUserId(data), text(data.get("meal_type"), text(data.get("mealType"), "早餐")), text(data.get("food_name"), text(data.get("foodName"))),
            toBigDecimal(data.get("weight"), BigDecimal.ZERO), toBigDecimal(data.get("calories"), BigDecimal.ZERO), dateTimeText(data.get("record_time")));
        return rowByLastId("diet_record");
    }

    public Map<String, Object> updateDietRecord(Long id, JsonNode body) {
        requireAdmin();
        requireLongId(id, "饮食记录编号不能为空");
        Map<String, Object> data = toMap(body);
        jdbcTemplate.update("UPDATE diet_record SET user_id=?, meal_type=?, food_name=?, weight=?, calories=?, record_time=? WHERE id=?",
            requireUserId(data), text(data.get("meal_type"), text(data.get("mealType"), "早餐")), text(data.get("food_name"), text(data.get("foodName"))),
            toBigDecimal(data.get("weight"), BigDecimal.ZERO), toBigDecimal(data.get("calories"), BigDecimal.ZERO), dateTimeText(data.get("record_time")), id);
        return rowById("diet_record", id);
    }

    public Map<String, Object> listHealthRecords(String keyword, int page, int size) {
        requireAdmin();
        return joinedPage("health_record", "h", "h.id, h.user_id, u.username, u.phone, u.nickname, h.height, h.weight, h.bmi, h.bmr, h.record_time, h.created_at", keyword, "ORDER BY h.record_time DESC, h.id DESC", page, size);
    }

    public Map<String, Object> createHealthRecord(JsonNode body) {
        requireAdmin();
        Map<String, Object> data = toMap(body);
        BigDecimal height = toBigDecimal(data.get("height"), BigDecimal.ZERO);
        BigDecimal weight = toBigDecimal(data.get("weight"), BigDecimal.ZERO);
        // 管理端允许直接录入 BMI/BMR；未传时再由后端兜底计算，兼容新旧表单。
        BigDecimal bmi = data.get("bmi") == null ? calcBmi(height, weight) : toBigDecimal(data.get("bmi"), BigDecimal.ZERO);
        BigDecimal bmr = data.get("bmr") == null ? calcBmr(height, weight) : toBigDecimal(data.get("bmr"), BigDecimal.ZERO);
        jdbcTemplate.update("INSERT INTO health_record (user_id, height, weight, bmi, bmr, record_time) VALUES (?, ?, ?, ?, ?, ?)",
            requireUserId(data), height, weight, bmi, bmr, dateTimeText(data.get("record_time")));
        return rowByLastId("health_record");
    }

    public Map<String, Object> updateHealthRecord(Long id, JsonNode body) {
        requireAdmin();
        requireLongId(id, "健康记录编号不能为空");
        Map<String, Object> data = toMap(body);
        BigDecimal height = toBigDecimal(data.get("height"), BigDecimal.ZERO);
        BigDecimal weight = toBigDecimal(data.get("weight"), BigDecimal.ZERO);
        BigDecimal bmi = data.get("bmi") == null ? calcBmi(height, weight) : toBigDecimal(data.get("bmi"), BigDecimal.ZERO);
        BigDecimal bmr = data.get("bmr") == null ? calcBmr(height, weight) : toBigDecimal(data.get("bmr"), BigDecimal.ZERO);
        jdbcTemplate.update("UPDATE health_record SET user_id=?, height=?, weight=?, bmi=?, bmr=?, record_time=? WHERE id=?",
            requireUserId(data), height, weight, bmi, bmr, dateTimeText(data.get("record_time")), id);
        return rowById("health_record", id);
    }

    public Map<String, Object> listHeartRecords(String keyword, int page, int size) {
        requireAdmin();
        return joinedPage("heart_record", "h", "h.id, h.user_id, u.username, u.phone, u.nickname, h.heart_rate, h.measure_type, h.record_time, h.created_at", keyword, "ORDER BY h.record_time DESC, h.id DESC", page, size);
    }

    public Map<String, Object> createHeartRecord(JsonNode body) {
        requireAdmin();
        Map<String, Object> data = toMap(body);
        int heartRate = toInt(data.get("heart_rate"), toInt(data.get("heartRate"), 0));
        if (heartRate < 30 || heartRate > 220) throw new BusinessException("心率值应在 30~220 之间");
        jdbcTemplate.update("INSERT INTO heart_record (user_id, heart_rate, measure_type, record_time) VALUES (?, ?, ?, ?)",
            requireUserId(data), heartRate, text(data.get("measure_type"), text(data.get("measureType"), "manual")), dateTimeText(data.get("record_time")));
        return rowByLastId("heart_record");
    }

    public Map<String, Object> updateHeartRecord(Long id, JsonNode body) {
        requireAdmin();
        requireLongId(id, "心率记录编号不能为空");
        Map<String, Object> data = toMap(body);
        int heartRate = toInt(data.get("heart_rate"), toInt(data.get("heartRate"), 0));
        if (heartRate < 30 || heartRate > 220) throw new BusinessException("心率值应在 30~220 之间");
        jdbcTemplate.update("UPDATE heart_record SET user_id=?, heart_rate=?, measure_type=?, record_time=? WHERE id=?",
            requireUserId(data), heartRate, text(data.get("measure_type"), text(data.get("measureType"), "manual")), dateTimeText(data.get("record_time")), id);
        return rowById("heart_record", id);
    }

    public Map<String, Object> listUserPlans(String keyword, int page, int size) {
        requireAdmin();
        return joinedPage("user_plan", "p", "p.id, p.user_id, u.username, u.phone, u.nickname, p.title, p.category, p.target_days, p.goal_text, p.theme, p.progress, p.status, p.start_date, p.end_date, p.created_at, p.updated_at", keyword, "ORDER BY p.created_at DESC, p.id DESC", page, size);
    }

    public Map<String, Object> createUserPlan(JsonNode body) {
        requireAdmin();
        Map<String, Object> data = toMap(body);
        // 这里同时兼容 App 端计划字段和管理端字段，避免录入入口一多就维护两套写库逻辑。
        jdbcTemplate.update("INSERT INTO user_plan (user_id, title, category, target_days, goal_text, theme, progress, status, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            requireUserId(data), text(data.get("title"), text(data.get("plan_name"), "健康计划")), text(data.get("category"), "习惯"),
            toInt(data.get("target_days"), toInt(data.get("duration_day"), 7)), text(data.get("goal_text"), text(data.get("goalText"))), text(data.get("theme"), "green"),
            toInt(data.get("progress"), toInt(data.get("current_day"), 0)), text(data.get("status"), "active"), dateText(data.get("start_date"), todayText()), emptyToNull(dateText(data.get("end_date"), "")));
        return rowByLastId("user_plan");
    }

    public Map<String, Object> updateUserPlan(Long id, JsonNode body) {
        requireAdmin();
        requireLongId(id, "用户计划编号不能为空");
        Map<String, Object> data = toMap(body);
        jdbcTemplate.update("UPDATE user_plan SET user_id=?, title=?, category=?, target_days=?, goal_text=?, theme=?, progress=?, status=?, start_date=?, end_date=? WHERE id=?",
            requireUserId(data), text(data.get("title"), text(data.get("plan_name"), "健康计划")), text(data.get("category"), "习惯"),
            toInt(data.get("target_days"), toInt(data.get("duration_day"), 7)), text(data.get("goal_text"), text(data.get("goalText"))), text(data.get("theme"), "green"),
            toInt(data.get("progress"), toInt(data.get("current_day"), 0)), text(data.get("status"), "active"), dateText(data.get("start_date"), todayText()), emptyToNull(dateText(data.get("end_date"), "")), id);
        return rowById("user_plan", id);
    }

    public Map<String, Object> listPlanCheckins(String keyword, int page, int size) {
        requireAdmin();
        return joinedPage("plan_checkin", "c", "c.id, c.plan_id, c.user_id, u.username, u.phone, u.nickname, c.checkin_date, c.remark, c.created_at", keyword, "ORDER BY c.checkin_date DESC, c.id DESC", page, size);
    }

    public Map<String, Object> createPlanCheckin(JsonNode body) {
        requireAdmin();
        Map<String, Object> data = toMap(body);
        jdbcTemplate.update("INSERT INTO plan_checkin (plan_id, user_id, checkin_date, remark) VALUES (?, ?, ?, ?)",
            requireLong(data.get("plan_id"), "计划编号不能为空"), requireUserId(data), dateText(data.get("checkin_date"), todayText()), text(data.get("remark"), text(data.get("checkin_content"))));
        return rowByLastId("plan_checkin");
    }

    public Map<String, Object> updatePlanCheckin(Long id, JsonNode body) {
        requireAdmin();
        requireLongId(id, "打卡记录编号不能为空");
        Map<String, Object> data = toMap(body);
        jdbcTemplate.update("UPDATE plan_checkin SET plan_id=?, user_id=?, checkin_date=?, remark=? WHERE id=?",
            requireLong(data.get("plan_id"), "计划编号不能为空"), requireUserId(data), dateText(data.get("checkin_date"), todayText()), text(data.get("remark"), text(data.get("checkin_content"))), id);
        return rowById("plan_checkin", id);
    }

    public Map<String, Object> listArticleFavorites(String keyword, int page, int size) {
        requireAdmin();
        return joinedPage("article_favorite", "f", "f.id, f.user_id, u.username, u.phone, u.nickname, f.article_title, f.article_url, f.source, f.summary, f.favorite_time, f.created_at", keyword, "ORDER BY f.favorite_time DESC, f.id DESC", page, size);
    }

    public Map<String, Object> createArticleFavorite(JsonNode body) {
        requireAdmin();
        Map<String, Object> data = toMap(body);
        jdbcTemplate.update("INSERT INTO article_favorite (user_id, article_title, article_url, source, summary, favorite_time) VALUES (?, ?, ?, ?, ?, ?)",
            requireUserId(data), text(data.get("article_title"), text(data.get("title"))), text(data.get("article_url"), text(data.get("url"))), text(data.get("source"), "健康知识"), text(data.get("summary")), dateTimeText(data.get("favorite_time")));
        return rowByLastId("article_favorite");
    }

    public Map<String, Object> updateArticleFavorite(Long id, JsonNode body) {
        requireAdmin();
        requireLongId(id, "收藏记录编号不能为空");
        Map<String, Object> data = toMap(body);
        jdbcTemplate.update("UPDATE article_favorite SET user_id=?, article_title=?, article_url=?, source=?, summary=?, favorite_time=? WHERE id=?",
            requireUserId(data), text(data.get("article_title"), text(data.get("title"))), text(data.get("article_url"), text(data.get("url"))), text(data.get("source"), "健康知识"), text(data.get("summary")), dateTimeText(data.get("favorite_time")), id);
        return rowById("article_favorite", id);
    }

    public Map<String, Object> listUserSettings(String keyword, int page, int size) {
        requireAdmin();
        return joinedPage("user_settings", "s", "s.id, s.user_id, u.username, u.phone, u.nickname, s.notify_enabled, s.step_goal, s.privacy_level, s.created_at, s.updated_at", keyword, "ORDER BY s.updated_at DESC, s.id DESC", page, size);
    }

    public Map<String, Object> createUserSettings(JsonNode body) {
        requireAdmin();
        Map<String, Object> data = toMap(body);
        jdbcTemplate.update("INSERT INTO user_settings (user_id, notify_enabled, step_goal, privacy_level) VALUES (?, ?, ?, ?)",
            requireUserId(data), toInt(data.get("notify_enabled"), 1), toInt(data.get("step_goal"), 8000), text(data.get("privacy_level"), "partial"));
        return rowByLastId("user_settings");
    }

    public Map<String, Object> updateUserSettings(Long id, JsonNode body) {
        requireAdmin();
        requireLongId(id, "设置记录编号不能为空");
        Map<String, Object> data = toMap(body);
        jdbcTemplate.update("UPDATE user_settings SET user_id=?, notify_enabled=?, step_goal=?, privacy_level=? WHERE id=?",
            requireUserId(data), toInt(data.get("notify_enabled"), 1), toInt(data.get("step_goal"), 8000), text(data.get("privacy_level"), "partial"), id);
        return rowById("user_settings", id);
    }

    public Map<String, Object> deleteTableRow(String table, Long id) {
        requireAdmin();
        requireLongId(id, "记录编号不能为空");
        if (!List.of("diet_record", "health_record", "heart_record", "user_plan", "plan_checkin", "article_favorite", "user_settings").contains(table)) {
            throw new BusinessException(400, "不允许删除该表数据");
        }
        int changed = jdbcTemplate.update("DELETE FROM " + table + " WHERE id=?", id);
        if (changed == 0) throw new BusinessException(404, "记录不存在");
        return Map.of("success", true, "deleted", id);
    }

    private Map<String, Object> joinedPage(String table, String alias, String columns, String keyword, String orderBy, int page, int size) {
        // 所有业务表列表都统一左联用户表，后台列表页就能直接搜用户信息并展示用户名/手机号。
        String from = "FROM " + table + " " + alias + " LEFT JOIN `user` u ON " + alias + ".user_id = u.id";
        String where = userKeywordWhere(keyword);
        return pageQuery("SELECT " + columns + " ", from, where, keyword, orderBy, page, size);
    }

    private List<Object> keywordParams(String keyword) {
        String search = "%" + text(keyword) + "%";
        return List.of(search, search, search);
    }

    private String userKeywordWhere(String keyword) {
        if (text(keyword).isEmpty()) return "";
        return "WHERE CAST(u.id AS CHAR) LIKE ? OR u.username LIKE ? OR u.phone LIKE ?";
    }

    private Map<String, Object> pageQuery(String selectSql, String fromSql, String whereSql, String keyword, String orderBy, int page, int size) {
        int currentPage = Math.max(1, page);
        int pageSize = Math.max(1, Math.min(size, 200));
        List<Object> params = whereSql == null || whereSql.isBlank() ? new ArrayList<>() : new ArrayList<>(keywordParams(keyword));
        // 总数和列表查询共用同一组 where 条件，避免分页数据和总数统计不一致。
        long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + fromSql + " " + whereSql, Long.class, params.toArray());
        int offset = (currentPage - 1) * pageSize;
        List<Object> rowParams = new ArrayList<>(params);
        rowParams.add(pageSize);
        rowParams.add(offset);
        List<Map<String, Object>> items = jdbcTemplate.queryForList(selectSql + fromSql + " " + whereSql + " " + orderBy + " LIMIT ? OFFSET ?", rowParams.toArray()).stream().map(this::normalizeDbRow).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", currentPage);
        result.put("size", pageSize);
        return result;
    }

    private Map<String, Object> rowByLastId(String table) {
        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM " + table, Long.class);
        return rowById(table, id);
    }

    private Map<String, Object> rowById(String table, Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + table + " WHERE id=?", id).stream().map(this::normalizeDbRow).toList();
        if (rows.isEmpty()) throw new BusinessException(404, "记录不存在");
        return rows.get(0);
    }

    private Map<String, Object> normalizeDbRow(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        row.forEach((key, value) -> result.put(key, value == null ? "" : value));
        // 管理端前端还保留部分旧字段名，这里补别名可以减少页面适配判断。
        if (result.containsKey("title") && !result.containsKey("plan_name")) result.put("plan_name", result.get("title"));
        if (result.containsKey("target_days") && !result.containsKey("duration_day")) result.put("duration_day", result.get("target_days"));
        if (result.containsKey("progress") && !result.containsKey("current_day")) result.put("current_day", result.get("progress"));
        if (result.containsKey("remark") && !result.containsKey("checkin_content")) result.put("checkin_content", result.get("remark"));
        return result;
    }

    private void requireAdmin() {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BusinessException(401, "未登录或登录已过期");
        User user = userMapper.findById(userId);
        if (user == null || !"ADMIN".equalsIgnoreCase(text(user.getRole()))) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    private void requireLongId(Long id, String message) {
        if (id == null || id <= 0) throw new BusinessException(message);
    }

    private Long requireUserId(Map<String, Object> data) {
        return requireLong(firstNonNull(data.get("user_id"), data.get("userId")), "用户编号不能为空");
    }

    private Long requireLong(Object value, String message) {
        Long number = toLong(value, null);
        if (number == null || number <= 0) throw new BusinessException(message);
        return number;
    }

    private BigDecimal calcBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null || heightCm.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        BigDecimal meter = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(meter.multiply(meter), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcBmr(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null) return BigDecimal.ZERO;
        // 当前采用简化版男性 BMR 公式，只做后台展示兜底，不用于医学诊断。
        return weightKg.multiply(BigDecimal.TEN)
            .add(heightCm.multiply(BigDecimal.valueOf(6.25)))
            .subtract(BigDecimal.valueOf(125))
            .add(BigDecimal.valueOf(5))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> toMap(JsonNode body) {
        if (body == null || body.isNull() || !body.isObject()) return new LinkedHashMap<>();
        return objectMapper.convertValue(body, new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    private BigDecimal toBigDecimal(Object value, BigDecimal fallback) {
        if (value == null) return fallback;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(String.valueOf(value)); } catch (Exception e) { return fallback; }
    }

    private int toInt(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return fallback; }
    }

    private Long toLong(Object value, Long fallback) {
        if (value == null) return fallback;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception e) { return fallback; }
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String text(Object value, String fallback) {
        String text = text(value);
        return text.isEmpty() ? fallback : text;
    }

    private String emptyToNull(String value) {
        String text = text(value);
        return text.isEmpty() ? null : text;
    }

    private String todayText() {
        return LocalDate.now().toString();
    }

    private String nowText() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String dateText(Object value, String fallback) {
        String text = text(value);
        if (text.isEmpty()) return fallback;
        return text.length() >= 10 ? text.substring(0, 10) : text;
    }

    private String dateTimeText(Object value) {
        String text = text(value);
        if (text.isEmpty()) return nowText();
        // 兼容 date、datetime 和前端常见的 ISO 字符串格式。
        if (text.length() == 10) return text + " 00:00:00";
        return text.replace('T', ' ');
    }

}
