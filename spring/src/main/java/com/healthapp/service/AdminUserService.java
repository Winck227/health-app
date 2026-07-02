package com.healthapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.common.BusinessException;
import com.healthapp.dto.PasswordUpdateRequest;
import com.healthapp.entity.User;
import com.healthapp.mapper.UserMapper;
import com.healthapp.security.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminUserService {
    // 服务说明：集中处理后台用户表 user 的查询、新增、修改、删除与密码重置。
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public AdminUserService(
            UserMapper userMapper,
            ObjectMapper objectMapper,
            BCryptPasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> users(String keyword, int page, int size) {
        requireAdmin();
        String from = "FROM `user` u";
        String where = userKeywordWhere(keyword);
        String select = "SELECT u.id, u.username, u.phone, u.password, u.nickname, u.gender, u.birthday, u.avatar, u.role, u.status, u.created_at, u.updated_at ";
        return pageQuery(select, from, where, keyword, "ORDER BY u.created_at DESC, u.id DESC", page, size);
    }

    @Transactional
    public Map<String, Object> createUser(JsonNode body) {
        requireAdmin();
        Map<String, Object> data = toMap(body);
        String username = text(data.get("username"));
        String password = text(data.get("password"));
        if (username.isBlank())
            throw new BusinessException("用户名不能为空");
        if (password.length() < 6)
            throw new BusinessException("密码至少 6 位");
        if (userMapper.countByUsername(username) > 0)
            throw new BusinessException("用户名已存在");
        String phone = emptyToNull(text(data.get("phone")));
        if (phone != null && userMapper.countByPhone(phone, -1L) > 0)
            throw new BusinessException("手机号已存在");
        // 后台创建用户统一在服务层做密码加密，避免调用方误把明文直接写进数据库。
        jdbcTemplate.update(
                "INSERT INTO `user` (username, phone, password, nickname, gender, birthday, avatar, role, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                username,
                phone,
                passwordEncoder.encode(password),
                text(data.get("nickname"), username),
                text(data.get("gender"), "unknown"),
                emptyToNull(text(data.get("birthday"))),
                emptyToNull(text(data.get("avatar"))),
                text(data.get("role"), "USER").toUpperCase(Locale.ROOT),
                toInt(data.get("status"), 1));
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return rowById("`user`", id);
    }

    @Transactional
    public Map<String, Object> updateUser(Long id, JsonNode body) {
        requireAdmin();
        requireLongId(id, "用户编号不能为空");
        if (userMapper.findById(id) == null)
            throw new BusinessException(404, "用户不存在");
        Map<String, Object> data = toMap(body);
        String phone = emptyToNull(text(data.get("phone")));
        // 手机号唯一性在更新时要排除自己，否则用户不改手机号也会被判重复。
        if (phone != null && userMapper.countByPhone(phone, id) > 0)
            throw new BusinessException("手机号已存在");
        jdbcTemplate.update(
                "UPDATE `user` SET phone=?, nickname=?, gender=?, birthday=?, avatar=?, role=?, status=? WHERE id=?",
                phone,
                text(data.get("nickname")),
                text(data.get("gender"), "unknown"),
                emptyToNull(text(data.get("birthday"))),
                emptyToNull(text(data.get("avatar"))),
                text(data.get("role"), "USER").toUpperCase(Locale.ROOT),
                toInt(data.get("status"), 1),
                id);
        return rowById("`user`", id);
    }

    @Transactional
    public Map<String, Object> deleteUser(Long id) {
        requireAdmin();
        requireLongId(id, "用户编号不能为空");
        int changed = jdbcTemplate.update("DELETE FROM `user` WHERE id=?", id);
        if (changed == 0)
            throw new BusinessException(404, "用户不存在");
        return Map.of("success", true, "deleted", id);
    }

    public Map<String, Object> resetPassword(Long userId, PasswordUpdateRequest request) {
        requireAdmin();
        if (userId == null)
            throw new BusinessException(400, "用户编号不能为空");
        String next = request == null ? "" : text(request.getNewPassword());
        if (next.length() < 6)
            throw new BusinessException("新密码至少 6 位");
        User user = userMapper.findById(userId);
        if (user == null)
            throw new BusinessException(404, "用户不存在");
        // 管理员重置密码不要求旧密码，但仍沿用统一的密码哈希策略。
        userMapper.updatePassword(userId, passwordEncoder.encode(next));
        return Map.of("success", true);
    }

    private String userKeywordWhere(String keyword) {
        if (text(keyword).isEmpty())
            return "";
        return "WHERE u.username LIKE ? OR u.phone LIKE ? OR u.nickname LIKE ?";
    }

    private Map<String, Object> pageQuery(String selectSql, String fromSql, String whereSql, String keyword,
            String orderBy, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(Math.min(size, 100), 1);
        int offset = (safePage - 1) * safeSize;
        List<Object> params = keywordParams(keyword);
        // 搜索条件为空时返回空参数列表，保证 count 和 list 两条 SQL 都能直接复用。
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + fromSql + " " + whereSql, Long.class,
                params.toArray());
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(safeSize);
        listParams.add(offset);
        List<Map<String, Object>> rows = jdbcTemplate
                .queryForList(selectSql + fromSql + " " + whereSql + " " + orderBy + " LIMIT ? OFFSET ?",
                        listParams.toArray())
                .stream()
                .map(this::normalizeDbRow)
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", rows);
        result.put("total", total == null ? 0 : total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    private List<Object> keywordParams(String keyword) {
        String key = text(keyword);
        if (key.isEmpty())
            return List.of();
        String like = "%" + key + "%";
        return List.of(like, like, like);
    }

    private Map<String, Object> rowById(String table, Long id) {
        if (id == null)
            return Map.of();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + table + " WHERE id=?", id);
        return rows.isEmpty() ? Map.of() : normalizeDbRow(rows.get(0));
    }

    private Map<String, Object> normalizeDbRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> {
            // JDBC 取出的时间对象统一转成前端可直接展示的字符串，减少后台表格额外格式化。
            if (value instanceof LocalDateTime time)
                normalized.put(key, time.toString().replace('T', ' '));
            else
                normalized.put(key, value);
        });
        return normalized;
    }

    private void requireAdmin() {
        Long userId = UserContext.getUserId();
        User user = userMapper.findById(userId);
        if (user == null || !"ADMIN".equalsIgnoreCase(text(user.getRole()))) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    private void requireLongId(Long id, String message) {
        if (id == null || id <= 0)
            throw new BusinessException(message);
    }

    private Map<String, Object> toMap(JsonNode body) {
        if (body == null || body.isNull())
            return new LinkedHashMap<>();
        return objectMapper.convertValue(body,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                });
    }

    private int toInt(Object value, int fallback) {
        if (value == null)
            return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String text(Object value, String fallback) {
        String result = text(value);
        return result.isEmpty() ? fallback : result;
    }

    private String emptyToNull(String value) {
        String text = text(value);
        return text.isEmpty() ? null : text;
    }
}
