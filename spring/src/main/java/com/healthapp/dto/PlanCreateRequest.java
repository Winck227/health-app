package com.healthapp.dto;

public class PlanCreateRequest {
    private String templateId;
    private String name;
    private String category;
    private Integer days;
    private String goalText;
    private String theme;

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public String getGoalText() { return goalText; }
    public void setGoalText(String goalText) { this.goalText = goalText; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
}
