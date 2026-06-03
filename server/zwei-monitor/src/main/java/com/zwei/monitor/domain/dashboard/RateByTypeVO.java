package com.zwei.monitor.domain.dashboard;

import lombok.Data;

import java.util.List;

/**
 * 设备/传感器在线率/活跃率通用 VO。
 * <p>
 * 供设备在线率、设备活跃率、传感器在线率、传感器活跃率四个接口复用。
 */
@Data
public class RateByTypeVO {
    private int windowMinutes;
    private int total;
    private int online;       // MySQL 直查时表示在线，IoTDB 窗口检测时表示活跃
    private int offline;      // MySQL 直查时表示不在线，IoTDB 窗口检测时表示不活跃
    private double onlineRate;
    private List<TypeStat> byType;

    @Data
    public static class TypeStat {
        private long monitorTypeId;
        private String monitorTypeName;
        private int total;
        private int online;
        private int offline;
        private double onlineRate;
    }
}
