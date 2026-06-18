package com.zwei.iot.report.render;

public final class RiskAssessor {

    private RiskAssessor() {}

    public static Risk assess(int alarmTotal, int maxAlarmLevel, int trendUpCount, double onlineRatePct) {
        int score = 0;
        if (alarmTotal >= 30) score += 4;
        else if (alarmTotal >= 10) score += 3;
        else if (alarmTotal >= 3) score += 2;
        else if (alarmTotal >= 1) score += 1;

        score += Math.max(0, maxAlarmLevel);

        if (trendUpCount >= 3) score += 3;
        else if (trendUpCount >= 1) score += 1;

        if (onlineRatePct < 80) score += 1;
        if (onlineRatePct < 60) score += 1;

        String level;
        String color;
        if (score >= 9) { level = "极高"; color = "#ff4d4f"; }
        else if (score >= 6) { level = "高"; color = "#fa8c16"; }
        else if (score >= 3) { level = "中"; color = "#faad14"; }
        else { level = "低"; color = "#67c23a"; }
        return new Risk(level, color, score);
    }

    public record Risk(String level, String color, int score) {}
}
