package com.healthapp.service;

import com.healthapp.entity.HealthRecord;
import com.healthapp.entity.HeartRecord;
import com.healthapp.entity.User;
import com.healthapp.mapper.PlanCheckinMapper;
import com.healthapp.mapper.UserMapper;
import com.healthapp.mapper.UserPlanMapper;
import com.healthapp.security.UserContext;
import com.healthapp.util.DateTimeUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {
    private final UserMapper userMapper;
    private final AuthService authService;
    private final DietService dietService;
    private final HealthRecordService healthRecordService;
    private final HeartRecordService heartRecordService;
    private final UserPlanMapper userPlanMapper;
    private final PlanCheckinMapper planCheckinMapper;

    public DashboardService(UserMapper userMapper, AuthService authService, DietService dietService,
            HealthRecordService healthRecordService, HeartRecordService heartRecordService,
            UserPlanMapper userPlanMapper, PlanCheckinMapper planCheckinMapper) {
        this.userMapper = userMapper;
        this.authService = authService;
        this.dietService = dietService;
        this.healthRecordService = healthRecordService;
        this.heartRecordService = heartRecordService;
        this.userPlanMapper = userPlanMapper;
        this.planCheckinMapper = planCheckinMapper;
    }

    // 仪表盘把用户、健康、心率、饮食和计划数据拼成首页需要的一次性快照。
    public Map<String, Object> dashboard() {
        Long userId = UserContext.getUserId();
        User user = userMapper.findById(userId);
        HealthRecord health = healthRecordService.latest(userId);
        HeartRecord heart = heartRecordService.latest(userId);
        BigDecimal todayCalories = dietService.todayCalories(userId);
        var plans = userPlanMapper.listByUser(userId);
        long planCount = plans.size();
        // 今日打卡数按“每个计划今天是否至少打过一次”统计，避免重复打卡放大数字。
        long checked = plans.stream()
                .filter(plan -> planCheckinMapper.countByDate(plan.getId(), userId, LocalDate.now()) > 0)
                .count();
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("score", null);
        dashboard.put("scoreDelta", null);
        dashboard.put("heartRate", heart == null ? null : heart.getHeartRate());
        dashboard.put("heartDate", heart == null ? "" : DateTimeUtil.format(heart.getRecordTime()));
        dashboard.put("bmi", health == null ? null : health.getBmi());
        dashboard.put("weight", health == null ? null : health.getWeight());
        dashboard.put("todayCalories", todayCalories);
        dashboard.put("planCount", planCount);
        dashboard.put("todayCheckedInCount", checked);
        Map<String, Object> result = new HashMap<>();
        result.put("user", authService.userDto(user));
        result.put("dashboard", dashboard);
        return result;
    }
}
