package com.healthapp.service;

import com.healthapp.common.BusinessException;
import com.healthapp.dto.HealthRecordRequest;
import com.healthapp.entity.HealthRecord;
import com.healthapp.entity.User;
import com.healthapp.mapper.HealthRecordMapper;
import com.healthapp.mapper.UserMapper;
import com.healthapp.security.UserContext;
import com.healthapp.util.DateTimeUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HealthRecordService {
    private final HealthRecordMapper healthRecordMapper;
    private final UserMapper userMapper;

    public HealthRecordService(HealthRecordMapper healthRecordMapper, UserMapper userMapper) {
        this.healthRecordMapper = healthRecordMapper;
        this.userMapper = userMapper;
    }

    // 健康档案页需要“最新一条 + 全部历史”两套视图，这里一次性组装返回。
    public Map<String, Object> getRecord() {
        Long userId = UserContext.getUserId();
        HealthRecord current = healthRecordMapper.latest(userId);
        List<Map<String, Object>> history = healthRecordMapper.listByUser(userId).stream().map(this::toDto).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("current", current == null ? null : toDto(current));
        result.put("history", history);
        return result;
    }

    // 录入时在后端统一校验身高体重范围，并补算 BMI / BMR，避免前端各自实现公式。
    public Map<String, Object> create(HealthRecordRequest request) {
        if (request.getHeight() == null || request.getWeight() == null)
            throw new BusinessException("身高和体重不能为空");
        if (request.getHeight().compareTo(new BigDecimal("50")) < 0
                || request.getHeight().compareTo(new BigDecimal("250")) > 0)
            throw new BusinessException("身高范围不正确");
        if (request.getWeight().compareTo(new BigDecimal("20")) < 0
                || request.getWeight().compareTo(new BigDecimal("300")) > 0)
            throw new BusinessException("体重范围不正确");
        Long userId = UserContext.getUserId();
        HealthRecord record = new HealthRecord();
        record.setUserId(userId);
        record.setHeight(request.getHeight());
        record.setWeight(request.getWeight());
        // BMI/BMR 都以后端结果为准，避免不同前端页面各自实现公式出现偏差。
        record.setBmi(calcBmi(request.getHeight(), request.getWeight()));
        record.setBmr(calcBmr(userMapper.findById(userId), request.getHeight(), request.getWeight()));
        record.setRecordTime(DateTimeUtil.parseDateTime(request.getRecordTime()));
        healthRecordMapper.insert(record);
        return getRecord();
    }

    public HealthRecord latest(Long userId) {
        return healthRecordMapper.latest(userId);
    }

    public Map<String, Object> toDto(HealthRecord record) {
        Map<String, Object> map = new HashMap<>();
        // 同时返回 camelCase 和 snake_case 用户编号，兼容移动端和后台表格消费方式。
        map.put("id", record.getId());
        map.put("userId", record.getUserId());
        map.put("user_id", record.getUserId());
        map.put("height", record.getHeight());
        map.put("weight", record.getWeight());
        map.put("bmi", record.getBmi());
        map.put("bmr", record.getBmr());
        map.put("recordTime", DateTimeUtil.format(record.getRecordTime()));
        map.put("createdAt", DateTimeUtil.format(record.getCreatedAt()));
        return map;
    }

    // BMI 统一按米为单位计算，并保留两位小数供前端直接展示。
    private BigDecimal calcBmi(BigDecimal heightCm, BigDecimal weightKg) {
        BigDecimal heightM = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
    }

    // BMR 采用常见 Mifflin-St Jeor 估算公式；年龄未知时用保守默认值兜底。
    private BigDecimal calcBmr(User user, BigDecimal height, BigDecimal weight) {
        int age = 25;
        if (user != null && user.getBirthday() != null) {
            age = Math.max(1, LocalDate.now().getYear() - user.getBirthday().getYear());
        }
        boolean female = user != null && "female".equalsIgnoreCase(user.getGender());
        BigDecimal bmr;
        if (female) {
            bmr = new BigDecimal("10").multiply(weight)
                    .add(new BigDecimal("6.25").multiply(height))
                    .subtract(new BigDecimal("5").multiply(new BigDecimal(age)))
                    .subtract(new BigDecimal("161"));
        } else {
            bmr = new BigDecimal("10").multiply(weight)
                    .add(new BigDecimal("6.25").multiply(height))
                    .subtract(new BigDecimal("5").multiply(new BigDecimal(age)))
                    .add(new BigDecimal("5"));
        }
        return bmr.setScale(2, RoundingMode.HALF_UP);
    }
}
