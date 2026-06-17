package com.zwei.iot.report.render;

import com.zwei.iot.device.domain.brief.AlarmEvent;
import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.report.datasource.MetricRow;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MonthlyReportRenderer implements ReportRenderer {

    @Override
    public ReportType type() { return ReportType.MONTHLY; }

    @Override
    public String render(ReportContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(ReportHtmlBuilder.header(ctx));

        // 1. 设备运行汇总
        sb.append(ReportHtmlBuilder.sectionTitle("1. 设备运行汇总"));
        sb.append(ReportHtmlBuilder.openTable("指标", "数值"));
        sb.append(ReportHtmlBuilder.row("设备总数", String.valueOf(ctx.deviceTotal())));
        sb.append(ReportHtmlBuilder.row("在线设备数", String.valueOf(ctx.deviceOnline())));
        sb.append(ReportHtmlBuilder.row("离线设备数", String.valueOf(ctx.deviceOffline())));
        sb.append(ReportHtmlBuilder.row("设备在线率", String.format("%.1f%%", ctx.onlineRatePct())));
        sb.append(ReportHtmlBuilder.closeTable());

        // 2. 监测数据汇总
        sb.append(ReportHtmlBuilder.sectionTitle("2. 监测数据汇总"));
        sb.append(ReportHtmlBuilder.openTable("属性", "单位", "月最大", "月最小", "月均值", "月累计变化量"));
        if (ctx.metrics().isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无数据", "", "", "", "", ""));
        } else {
            for (MetricRow m : ctx.metrics()) {
                sb.append(ReportHtmlBuilder.row(
                    m.attrName(), m.unit(),
                    fmt(m.maxValue()), fmt(m.minValue()), fmt(m.avgValue()), fmt(m.sumValue())
                ));
            }
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 3. 风险情况
        sb.append(ReportHtmlBuilder.sectionTitle("3. 风险情况"));
        AlarmSummary sum = ctx.alarmSummary();
        if (sum == null || sum.total() == 0) {
            sb.append(ReportHtmlBuilder.paragraph("本月无告警记录。"));
        } else {
            sb.append(ReportHtmlBuilder.openTable("指标", "数值"));
            sb.append(ReportHtmlBuilder.row("告警总数", String.valueOf(sum.total())));
            String maxColor = ReportHtmlBuilder.levelColor(sum.maxLevel());
            String maxName = ReportHtmlBuilder.levelName(sum.maxLevel());
            sb.append(ReportHtmlBuilder.row("最高告警级别",
                "<span style=\"color:" + maxColor + ";font-weight:bold;\">" + maxName + "</span>"));
            sb.append(ReportHtmlBuilder.row("待处理告警数", String.valueOf(sum.pendingCount())));
            sb.append(ReportHtmlBuilder.closeTable());

            sb.append(ReportHtmlBuilder.openTable("告警级别", "次数"));
            Map<Integer, Integer> levelCount = sum.levelCount();
            for (int lvl = 4; lvl >= 1; lvl--) {
                int cnt = levelCount.getOrDefault(lvl, 0);
                if (cnt > 0) {
                    String color = ReportHtmlBuilder.levelColor(lvl);
                    sb.append(ReportHtmlBuilder.row(
                        "<span style=\"color:" + color + ";\">" + ReportHtmlBuilder.levelName(lvl) + "</span>",
                        String.valueOf(cnt)));
                }
            }
            sb.append(ReportHtmlBuilder.closeTable());
        }

        // 4. 关键事件
        sb.append(ReportHtmlBuilder.sectionTitle("4. 关键事件 (Top 10)"));
        sb.append(ReportHtmlBuilder.openTable("时间", "级别", "类型", "设备", "描述", "状态"));
        List<AlarmEvent> events = ctx.alarmTopEvents();
        if (events == null || events.isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无告警事件", "", "", "", "", ""));
        } else {
            for (AlarmEvent e : events) {
                String color = ReportHtmlBuilder.levelColor(e.alarmLevel());
                String levelText = e.alarmLevelText() != null ? e.alarmLevelText()
                    : ReportHtmlBuilder.levelName(e.alarmLevel());
                String statusText = e.statusName() != null ? e.statusName() : statusName(e.status());
                sb.append(ReportHtmlBuilder.row(
                    e.firstTriggerTime() == null ? "-" : e.firstTriggerTime().toString(),
                    "<span style=\"color:" + color + ";\">" + levelText + "</span>",
                    e.alarmType() == null ? "-" : e.alarmType(),
                    e.deviceName() == null ? "-" : e.deviceName(),
                    e.alarmMessage() == null ? "-" : e.alarmMessage(),
                    statusText
                ));
            }
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 5. 分析与建议
        sb.append(ReportHtmlBuilder.sectionTitle("5. 分析与建议"));
        List<String> advice = new ArrayList<>();
        if (sum != null && sum.total() > 0) {
            advice.add("本月共发生告警 " + sum.total() + " 次, 最高级别 "
                + ReportHtmlBuilder.levelName(sum.maxLevel()) + ", 建议核查高风险告警。");
        } else {
            advice.add("本月无告警记录, 监测数据稳定。");
        }
        advice.add("设备在线率 " + String.format("%.1f", ctx.onlineRatePct()) + "%。");
        advice.add("建议下月继续按既定监测方案执行, 关注雨季期间数据变化。");
        sb.append(ReportHtmlBuilder.bulletList(advice.toArray(new String[0])));

        return sb.toString();
    }

    private static String statusName(int s) {
        return switch (s) {
            case 1 -> "待处理";
            case 2 -> "处理中";
            case 3 -> "已销警";
            case 4 -> "误报";
            default -> "-";
        };
    }

    private static String fmt(Double v) {
        return v == null ? "-" : String.format("%.3f", v);
    }
}
