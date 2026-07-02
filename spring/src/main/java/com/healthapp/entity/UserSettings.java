package com.healthapp.entity;

import java.time.LocalDateTime;

public class UserSettings {
    private Long id;
    private Long userId;
    private Integer notifyEnabled;
    private Integer stepGoal;
    private String privacyLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getNotifyEnabled() { return notifyEnabled; }
    public void setNotifyEnabled(Integer notifyEnabled) { this.notifyEnabled = notifyEnabled; }
    public Integer getStepGoal() { return stepGoal; }
    public void setStepGoal(Integer stepGoal) { this.stepGoal = stepGoal; }
    public String getPrivacyLevel() { return privacyLevel; }
    public void setPrivacyLevel(String privacyLevel) { this.privacyLevel = privacyLevel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
