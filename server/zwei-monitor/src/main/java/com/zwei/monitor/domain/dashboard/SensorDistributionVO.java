package com.zwei.monitor.domain.dashboard;

import lombok.Data;

import java.util.List;

/**
 * 传感器按监测类型分布 VO。
 */
@Data
public class SensorDistributionVO {
    private List<TypeCount> list;

    @Data
    public static class TypeCount {
        private long monitorTypeId;
        private String monitorTypeName;
        private int sensorCount;
    }
}
