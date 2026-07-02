package com.healthapp.service;

import com.healthapp.common.BusinessException;
import com.healthapp.dto.LoginRequest;
import com.healthapp.dto.PasswordUpdateRequest;
import com.healthapp.dto.RegisterRequest;
import com.healthapp.entity.User;
import com.healthapp.mapper.UserMapper;
import com.healthapp.mapper.UserSettingsMapper;
import com.healthapp.security.JwtUtil;
import com.healthapp.security.UserContext;
import com.healthapp.util.DateTimeUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    // 认证服务统一负责账号注册、登录、会话恢复与密码修改，前后台都复用这套能力。
    private final UserMapper userMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, UserSettingsMapper userSettingsMapper, BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.userSettingsMapper = userSettingsMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // 注册时同步创建默认设置，避免新账号首次进入设置页时拿到空记录。
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        String username = safe(request.getUsername());
        String password = safe(request.getPassword());
        if (username.isBlank()) throw new BusinessException("用户名不能为空");
        if (password.length() < 6) throw new BusinessException("密码至少 6 位");
        if (userMapper.countByUsername(username) > 0) throw new BusinessException("用户名已存在");
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(safe(request.getName()).isBlank() ? username : safe(request.getName()));
        user.setGender("unknown");
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        // 默认设置和用户主记录放在同一事务里，避免注册成功后设置表缺行。
        userSettingsMapper.ensureDefault(user.getId());
        return authPayload(user);
    }

    // 登录兼容用户名和手机号，两者都走同一套账号状态校验。
    public Map<String, Object> login(LoginRequest request) {
        String account = safe(request.getUsername());
        String password = safe(request.getPassword());
        if (account.isBlank() || password.isBlank()) throw new BusinessException("账号和密码不能为空");
        // 用户名和手机号共用同一个入口，减少 App 端额外判断当前输入的是哪种账号。
        User user = userMapper.findByUsernameOrPhone(account);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号不存在或已禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        return authPayload(user);
    }

    // 会话续期时优先复用现有 Bearer token，避免同一次登录频繁刷新令牌。
    public Map<String, Object> session(String authorization) {
        Long userId = UserContext.getUserId();
        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) throw new BusinessException(401, "用户不存在或已禁用");
        // 会话校验通过时尽量沿用现有 Bearer token，减少刷新页面时频繁签发新令牌。
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : jwtUtil.generateToken(userId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", token);
        payload.put("user", userDto(user));
        return payload;
    }

    public Map<String, Object> logout() {
        return Map.of("success", true);
    }

    // 修改密码前必须校验旧密码，防止仅凭登录态直接覆盖用户凭证。
    public void updatePassword(PasswordUpdateRequest request) {
        Long userId = UserContext.getUserId();
        User user = userMapper.findById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");
        if (!passwordEncoder.matches(safe(request.getOldPassword()), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        String next = safe(request.getNewPassword());
        if (next.length() < 6) throw new BusinessException("新密码至少 6 位");
        userMapper.updatePassword(userId, passwordEncoder.encode(next));
    }

    // 登录、注册、会话统一复用同一返回结构，减少前端兼容分支。
    private Map<String, Object> authPayload(User user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", jwtUtil.generateToken(user.getId()));
        payload.put("user", userDto(user));
        return payload;
    }

    // 同时保留新旧常用字段名，方便 App 和管理端直接复用用户对象。
    public Map<String, Object> userDto(User user) {
        Map<String, Object> map = new HashMap<>();
        // 同一份用户对象同时服务 App 和管理端，所以保留少量常用别名字段。
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", value(user.getNickname()));
        map.put("name", value(user.getNickname()));
        map.put("phone", value(user.getPhone()));
        map.put("gender", value(user.getGender(), "unknown"));
        map.put("birthday", DateTimeUtil.formatDate(user.getBirthday()));
        map.put("avatar", value(user.getAvatar()));
        map.put("role", value(user.getRole(), "USER"));
        map.put("status", user.getStatus() == null ? 1 : user.getStatus());
        return map;
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }
    private String value(String value) { return value == null ? "" : value; }
    private String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
}
