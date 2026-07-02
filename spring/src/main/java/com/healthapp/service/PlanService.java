package com.healthapp.service;

import com.healthapp.common.BusinessException;
import com.healthapp.dto.CheckinRequest;
import com.healthapp.dto.PlanCreateRequest;
import com.healthapp.entity.PlanCheckin;
import com.healthapp.entity.UserPlan;
import com.healthapp.mapper.PlanCheckinMapper;
import com.healthapp.mapper.UserPlanMapper;
import com.healthapp.security.UserContext;
import com.healthapp.util.DateTimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlanService {
    private final UserPlanMapper userPlanMapper;
    private final PlanCheckinMapper planCheckinMapper;
    private final PlanTemplateService planTemplateService;

    public PlanService(UserPlanMapper userPlanMapper, PlanCheckinMapper planCheckinMapper, PlanTemplateService planTemplateService) {
        this.userPlanMapper = userPlanMapper;
        this.planCheckinMapper = planCheckinMapper;
        this.planTemplateService = planTemplateService;
    }

    public List<Map<String, Object>> list() {
        Long userId = UserContext.getUserId();
        return userPlanMapper.listByUser(userId).stream().map(plan -> toDto(plan, userId)).toList();
    }

    public Map<String, Object> detail(Long id) {
        Long userId = UserContext.getUserId();
        UserPlan plan = requirePlan(id, userId);
        return toDto(plan, userId);
    }

    @Transactional
    public Map<String, Object> create(PlanCreateRequest request) {
        Long userId = UserContext.getUserId();
        Map<String, Object> template = null;
        if (!isBlank(request.getTemplateId())) {
            template = planTemplateService.findTemplate(request.getTemplateId());
        }
        String name = text(request.getName(), template == null ? null : String.valueOf(template.get("name")));
        if (isBlank(name)) throw new BusinessException("计划名称不能为空");
        Integer days = request.getDays();
        if (days == null && template != null) days = toInt(template.get("days"), 7);
        if (days == null || days < 1) days = 7;
        UserPlan plan = new UserPlan();
        plan.setUserId(userId);
        plan.setTitle(name);
        plan.setCategory(text(request.getCategory(), template == null ? "习惯" : String.valueOf(template.get("category"))));
        plan.setTargetDays(days);
        plan.setGoalText(text(request.getGoalText(), template == null ? "" : String.valueOf(template.get("goalText"))));
        plan.setTheme(text(request.getTheme(), template == null ? "green" : String.valueOf(template.get("theme"))));
        plan.setProgress(0);
        plan.setStatus("active");
        plan.setStartDate(LocalDate.now());
        plan.setEndDate(LocalDate.now().plusDays(days - 1L));
        userPlanMapper.insert(plan);
        return toDto(plan, userId);
    }

    public Map<String, Object> delete(Long id) {
        Long userId = UserContext.getUserId();
        int count = userPlanMapper.softDelete(id, userId);
        if (count == 0) throw new BusinessException(404, "计划不存在");
        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Object> checkin(Long id, CheckinRequest request) {
        Long userId = UserContext.getUserId();
        UserPlan plan = requirePlan(id, userId);
        LocalDate today = LocalDate.now();
        boolean already = planCheckinMapper.countByDate(id, userId, today) > 0;
        if (!already) {
            PlanCheckin checkin = new PlanCheckin();
            checkin.setPlanId(id);
            checkin.setUserId(userId);
            checkin.setCheckinDate(today);
            checkin.setRemark(request == null ? null : request.getRemark());
            planCheckinMapper.insert(checkin);
        }
        refreshProgress(plan);
        UserPlan latest = requirePlan(id, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("alreadyCheckedIn", already);
        result.put("plan", toDto(latest, userId));
        return result;
    }

    public UserPlan requirePlan(Long id, Long userId) {
        if (id == null) throw new BusinessException("计划编号不能为空");
        UserPlan plan = userPlanMapper.findByIdAndUser(id, userId);
        if (plan == null) throw new BusinessException(404, "计划不存在");
        return plan;
    }

    private void refreshProgress(UserPlan plan) {
        Long userId = plan.getUserId();
        int completed = planCheckinMapper.countByPlanAndUser(plan.getId(), userId);
        int target = Math.max(1, plan.getTargetDays() == null ? 1 : plan.getTargetDays());
        int percent = Math.min(100, (int)Math.round(completed * 100.0 / target));
        String status = percent >= 100 ? "completed" : "active";
        userPlanMapper.updateProgressAndStatus(plan.getId(), userId, percent, status);
    }

    public Map<String, Object> toDto(UserPlan plan, Long userId) {
        List<LocalDate> dates = planCheckinMapper.listCheckinDates(plan.getId(), userId);
        int completed = dates.size();
        int target = Math.max(1, plan.getTargetDays() == null ? 1 : plan.getTargetDays());
        int percent = Math.min(100, (int)Math.round(completed * 100.0 / target));
        LocalDate last = planCheckinMapper.lastCheckinDate(plan.getId(), userId);
        Map<String, Object> map = new HashMap<>();
        map.put("id", plan.getId());
        map.put("userId", userId);
        map.put("user_id", userId);
        map.put("name", plan.getTitle());
        map.put("title", plan.getTitle());
        map.put("category", plan.getCategory());
        map.put("days", target);
        map.put("targetDays", target);
        map.put("completedDays", completed);
        map.put("percent", percent);
        map.put("progress", percent);
        map.put("goalText", plan.getGoalText());
        map.put("theme", plan.getTheme());
        map.put("status", plan.getStatus());
        map.put("todayCheckedIn", dates.contains(LocalDate.now()));
        map.put("lastCheckinDate", DateTimeUtil.formatDate(last));
        map.put("checkinDates", dates.stream().map(DateTimeUtil::formatDate).toList());
        map.put("startDate", DateTimeUtil.formatDate(plan.getStartDate()));
        map.put("endDate", DateTimeUtil.formatDate(plan.getEndDate()));
        return map;
    }

    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private String text(String first, String fallback) { return isBlank(first) ? (fallback == null ? "" : fallback) : first.trim(); }
    private int toInt(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return fallback; }
    }
}
