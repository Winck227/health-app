package com.healthapp.dto;

import java.math.BigDecimal;

public class HealthRecordRequest {
    private BigDecimal height;
    private BigDecimal weight;
    private String recordTime;
    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public String getRecordTime() { return recordTime; }
    public void setRecordTime(String recordTime) { this.recordTime = recordTime; }
}
