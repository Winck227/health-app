package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SettingsUpdateRequest {
    private Boolean notifyEnabled;
    private Integer stepGoal;
    private String privacyLevel;

    public Boolean getNotifyEnabled() { return notifyEnabled; }
    public void setNotifyEnabled(Boolean notifyEnabled) { this.notifyEnabled = notifyEnabled; }
    @JsonProperty("notify_enabled")
    public void setNotifyEnabledSnake(Object value) { this.notifyEnabled = toBoolean(value); }
    @JsonProperty("notificationEnabled")
    public void setNotificationEnabled(Object value) { this.notifyEnabled = toBoolean(value); }
    public Integer getStepGoal() { return stepGoal; }
    public void setStepGoal(Integer stepGoal) { this.stepGoal = stepGoal; }
    @JsonProperty("step_goal")
    public void setStepGoalSnake(Integer stepGoal) { this.stepGoal = stepGoal; }
    @JsonProperty("dailyGoal")
    public void setDailyGoal(Integer dailyGoal) { this.stepGoal = dailyGoal; }
    @JsonProperty("dailySteps")
    public void setDailySteps(Integer dailySteps) { this.stepGoal = dailySteps; }
    public String getPrivacyLevel() { return privacyLevel; }
    public void setPrivacyLevel(String privacyLevel) { this.privacyLevel = privacyLevel; }
    @JsonProperty("privacy_level")
    public void setPrivacyLevelSnake(String privacyLevel) { this.privacyLevel = privacyLevel; }

    private Boolean toBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return null;
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text);
    }
}
