package com.zwei.iot.alarm.domain.dto;

public class StrategyTestRunRequest {
    private String mockSensorCode;
    private Long mockDataTime;

    public String getMockSensorCode() { return mockSensorCode; }
    public void setMockSensorCode(String mockSensorCode) { this.mockSensorCode = mockSensorCode; }
    public Long getMockDataTime() { return mockDataTime; }
    public void setMockDataTime(Long mockDataTime) { this.mockDataTime = mockDataTime; }
}
