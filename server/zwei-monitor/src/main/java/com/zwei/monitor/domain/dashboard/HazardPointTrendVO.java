package com.zwei.monitor.domain.dashboard;

import lombok.Data;

import java.util.List;

/**
 * 隐患点增长趋势 VO。
 */
@Data
public class HazardPointTrendVO {
    private List<String> months;
    private List<Integer> counts;
    private List<Integer> cumulativeCounts;
}
