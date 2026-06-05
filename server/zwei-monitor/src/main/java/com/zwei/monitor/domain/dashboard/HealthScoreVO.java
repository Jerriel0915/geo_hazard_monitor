package com.zwei.monitor.domain.dashboard;

import lombok.Data;

import java.util.List;

/**
 * 系统健康度评分 VO。
 * <p>
 * 综合资料完善率、设备在线率、设备正常率、告警响应率、边坡稳定率五个维度计算。
 */
@Data
public class HealthScoreVO {
    /** 综合健康度得分 (0-100) */
    private double overallScore;
    /** 各维度明细 */
    private List<HealthItem> items;

    @Data
    public static class HealthItem {
        private String name;
        private double value;
        private double weight;
        private String color;
        /** 数据来源：computed(真实计算) / placeholder(告警模块未就绪时的占位值) */
        private String dataSource;

        public static HealthItem of(String name, double value, double weight, String color, String dataSource) {
            HealthItem item = new HealthItem();
            item.name = name;
            item.value = value;
            item.weight = weight;
            item.color = color;
            item.dataSource = dataSource;
            return item;
        }
    }
}
