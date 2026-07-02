package com.healthapp.service;

import com.healthapp.common.BusinessException;
import com.healthapp.dto.DietRecordRequest;
import com.healthapp.entity.DietRecord;
import com.healthapp.mapper.DietRecordMapper;
import com.healthapp.security.UserContext;
import com.healthapp.util.DateTimeUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DietService {
    private final DietRecordMapper dietRecordMapper;

    public DietService(DietRecordMapper dietRecordMapper) {
        this.dietRecordMapper = dietRecordMapper;
    }

    // 饮食页需要完整日志和今日汇总热量，这里统一组装成一个返回包。
    public Map<String, Object> list() {
        Long userId = UserContext.getUserId();
        List<Map<String, Object>> logs = dietRecordMapper.listByUser(userId).stream().map(this::toDto).toList();
        LocalDate today = LocalDate.now();
        BigDecimal todayCalories = dietRecordMapper.sumCaloriesBetween(userId, today.atStartOfDay(),
                today.plusDays(1).atStartOfDay());
        Map<String, Object> summary = new HashMap<>();
        summary.put("todayCalories", todayCalories == null ? BigDecimal.ZERO : todayCalories);
        Map<String, Object> result = new HashMap<>();
        result.put("logs", logs);
        result.put("summary", summary);
        return result;
    }

    // 创建饮食记录时只强制校验餐次和食物名，热量与重量缺失则回退为 0。
    public Map<String, Object> create(DietRecordRequest request) {
        if (isBlank(request.getMealType()))
            throw new BusinessException("餐次不能为空");
        if (isBlank(request.getFoodName()))
            throw new BusinessException("食物名称不能为空");
        DietRecord record = new DietRecord();
        record.setUserId(UserContext.getUserId());
        record.setMealType(request.getMealType().trim());
        record.setFoodName(request.getFoodName().trim());
        record.setWeight(nonNegative(request.getWeight()));
        record.setCalories(nonNegative(request.getCalories()));
        // 记录时间允许前端缺省，统一在 DateTimeUtil 里走默认时间兜底。
        record.setRecordTime(DateTimeUtil.parseDateTime(request.getRecordTime()));
        dietRecordMapper.insert(record);
        return toDto(record);
    }

    // 删除只允许操作当前用户自己的记录，避免通过编号误删他人数据。
    public Map<String, Object> delete(Long id) {
        if (id == null)
            throw new BusinessException("记录编号不能为空");
        int count = dietRecordMapper.deleteByIdAndUser(id, UserContext.getUserId());
        if (count == 0)
            throw new BusinessException(404, "饮食记录不存在");
        return Map.of("success", true);
    }

    // 今日热量为空时统一回退 0，前端就不用再处理 null 值。
    public BigDecimal todayCalories(Long userId) {
        LocalDate today = LocalDate.now();
        BigDecimal value = dietRecordMapper.sumCaloriesBetween(userId, today.atStartOfDay(),
                today.plusDays(1).atStartOfDay());
        return value == null ? BigDecimal.ZERO : value;
    }

    // DTO 同时保留 userId 和 user_id，兼容 App 页面和管理端表格。
    public Map<String, Object> toDto(DietRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getId());
        map.put("userId", record.getUserId());
        map.put("user_id", record.getUserId());
        map.put("mealType", record.getMealType());
        map.put("foodName", record.getFoodName());
        map.put("weight", record.getWeight());
        map.put("calories", record.getCalories());
        map.put("recordTime", DateTimeUtil.format(record.getRecordTime()));
        map.put("createdAt", DateTimeUtil.format(record.getCreatedAt()));
        return map;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // 录入页允许重量或热量缺省，后端统一把负值和空值压成 0。
    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0)
            return BigDecimal.ZERO;
        return value;
    }
}
