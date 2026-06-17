package com.zwei.iot.report.render;

import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class QuarterlyReportRenderer implements ReportRenderer {

    @Override
    public ReportType type() { return ReportType.QUARTERLY; }

    @Override
    public String render(ReportContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(ReportHtmlBuilder.header(ctx));

        AlarmSummary summary = ctx.alarmSummary();
        int alarmTotal = summary == null ? 0 : summary.total();
        int maxLevel = summary == null ? 0 : summary.maxLevel();

        // 1. 季度风险总览
        sb.append(ReportHtmlBuilder.sectionTitle("1. 季度风险总览"));
        sb.append(ReportHtmlBuilder.openTable("指标", "数值"));
        sb.append(ReportHtmlBuilder.row("季度告警总数", String.valueOf(alarmTotal)));
        sb.append(ReportHtmlBuilder.row("最高告警级别",
            "<span style=\"color:" + ReportHtmlBuilder.levelColor(maxLevel) + ";\">"
            + ReportHtmlBuilder.levelName(maxLevel) + "</span>"));
        sb.append(ReportHtmlBuilder.closeTable());

        sb.append(ReportHtmlBuilder.sectionTitle("告警分布"));
        sb.append(ReportHtmlBuilder.openTable("月份", "告警次数"));
        Map<String, Integer> monthly = ctx.alarmMonthlyCount();
        if (monthly == null || monthly.isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无数据", "-"));
        } else {
            monthly.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(ReportHtmlBuilder.row(e.getKey(), String.valueOf(e.getValue()))));
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 2. 趋势分析
        sb.append(ReportHtmlBuilder.sectionTitle("2. 趋势分析"));
        sb.append(ReportHtmlBuilder.openTable("属性", "趋势方向", "斜率", "解读"));
        Map<String, String> dirs = ctx.trendDirections();
        Map<String, Double> slopes = ctx.trendSlopes();
        int upCount = 0;
        if (dirs == null || dirs.isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无数据", "", "", ""));
        } else {
            for (Map.Entry<String, String> e : dirs.entrySet()) {
                String symbol = symbolFor(e.getValue());
                if ("UP".equals(e.getValue())) upCount++;
                Double slope = slopes == null ? null : slopes.get(e.getKey());
                sb.append(ReportHtmlBuilder.row(
                    e.getKey(),
                    "<span style=\"font-size:14px;font-weight:bold;\">" + symbol + "</span>",
                    slope == null ? "-" : String.format("%.4f", slope),
                    interpret(e.getValue())
                ));
            }
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 3. 设备运行汇总
        sb.append(ReportHtmlBuilder.sectionTitle("3. 设备运行汇总"));
        sb.append(ReportHtmlBuilder.openTable("指标", "数值"));
        sb.append(ReportHtmlBuilder.row("设备总数", String.valueOf(ctx.deviceTotal())));
        sb.append(ReportHtmlBuilder.row("在线设备数", String.valueOf(ctx.deviceOnline())));
        sb.append(ReportHtmlBuilder.row("设备在线率", String.format("%.1f%%", ctx.onlineRatePct())));
        sb.append(ReportHtmlBuilder.closeTable());

        // 4. 告警级别分布
        sb.append(ReportHtmlBuilder.sectionTitle("4. 告警级别分布"));
        if (summary == null || summary.total() == 0) {
            sb.append(ReportHtmlBuilder.paragraph("本季度无告警分布数据。"));
        } else {
            sb.append(ReportHtmlBuilder.openTable("告警级别", "次数"));
            summary.levelCount().forEach((lvl, cnt) -> {
                String color = ReportHtmlBuilder.levelColor(lvl);
                sb.append(ReportHtmlBuilder.row(
                    "<span style=\"color:" + color + ";\">" + ReportHtmlBuilder.levelName(lvl) + "</span>",
                    String.valueOf(cnt)));
            });
            sb.append(ReportHtmlBuilder.closeTable());
        }

        // 5. 风险评估与建议
        sb.append(ReportHtmlBuilder.sectionTitle("5. 风险评估与建议"));
        RiskAssessor.Risk risk = RiskAssessor.assess(alarmTotal, maxLevel, upCount, ctx.onlineRatePct());
        sb.append(ReportHtmlBuilder.paragraph(
            "综合评级: <span style=\"color:" + risk.color() + ";font-weight:bold;font-size:16px;\">"
            + risk.level() + "</span> (评分 " + risk.score() + ")"));
        List<String> advice = new ArrayList<>();
        advice.add("本季度共发生告警 " + alarmTotal + " 次, 趋势上升指标 " + upCount + " 个, 设备在线率 "
            + String.format("%.1f", ctx.onlineRatePct()) + "%。");
        if ("极高".equals(risk.level()) || "高".equals(risk.level())) {
            advice.add("建议提高监测频率, 加强现场巡查, 准备应急预案。");
        } else if ("中".equals(risk.level())) {
            advice.add("建议持续关注, 适当加密巡查频次。");
        } else {
            advice.add("整体监测稳定, 建议维持现有方案。");
        }
        sb.append(ReportHtmlBuilder.bulletList(advice.toArray(new String[0])));

        return sb.toString();
    }

    private static String symbolFor(String dir) {
        return switch (dir == null ? "" : dir) {
            case "UP" -> "↑";
            case "DOWN" -> "↓";
            case "STABLE" -> "→";
            default -> "-";
        };
    }

    private static String interpret(String dir) {
        return switch (dir == null ? "" : dir) {
            case "UP" -> "数据呈上升趋势, 需关注";
            case "DOWN" -> "数据呈下降趋势";
            case "STABLE" -> "数据稳定";
            default -> "-";
        };
    }
}
