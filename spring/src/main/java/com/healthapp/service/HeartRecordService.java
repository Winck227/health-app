package com.healthapp.service;

import com.healthapp.common.BusinessException;
import com.healthapp.dto.HeartRecordRequest;
import com.healthapp.entity.HeartRecord;
import com.healthapp.mapper.HeartRecordMapper;
import com.healthapp.security.UserContext;
import com.healthapp.util.DateTimeUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HeartRecordService {
    private final HeartRecordMapper heartRecordMapper;

    public HeartRecordService(HeartRecordMapper heartRecordMapper) {
        this.heartRecordMapper = heartRecordMapper;
    }

    // 心率页同时需要当前值、最新记录和历史趋势，这里统一组装一份返回包。
    public Map<String, Object> list() {
        Long userId = UserContext.getUserId();
        HeartRecord latest = heartRecordMapper.latest(userId);
        List<Map<String, Object>> history = heartRecordMapper.listByUser(userId).stream().map(this::toDto).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("currentBpm", latest == null ? null : latest.getHeartRate());
        result.put("latest", latest == null ? null : toDto(latest));
        result.put("history", history);
        return result;
    }

    // 心率区间校验统一放后端，避免不同采集入口出现不一致的合法范围。
    public Map<String, Object> create(HeartRecordRequest request) {
        Integer bpm = request.getHeartRate();
        if (bpm == null || bpm < 30 || bpm > 220)
            throw new BusinessException("心率范围不正确");
        HeartRecord record = new HeartRecord();
        record.setUserId(UserContext.getUserId());
        record.setHeartRate(bpm);
        // 未显式指定测量方式时默认记为手动录入，兼容旧页面调用。
        record.setMeasureType(isBlank(request.getMeasureType()) ? "manual" : request.getMeasureType().trim());
        record.setRecordTime(DateTimeUtil.parseDateTime(request.getRecordTime()));
        heartRecordMapper.insert(record);
        return toDto(record);
    }

    public HeartRecord latest(Long userId) {
        return heartRecordMapper.latest(userId);
    }

    public Map<String, Object> toDto(HeartRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getId());
        map.put("userId", record.getUserId());
        map.put("user_id", record.getUserId());
        map.put("heartRate", record.getHeartRate());
        map.put("measureType", record.getMeasureType());
        map.put("recordTime", DateTimeUtil.format(record.getRecordTime()));
        map.put("createdAt", DateTimeUtil.format(record.getCreatedAt()));
        return map;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
