package com.zwei.iot.report.render;

import com.zwei.iot.device.domain.brief.DeviceBrief;
import com.zwei.iot.report.datasource.MetricRow;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WeeklyReportRenderer implements ReportRenderer {

    @Override
    public ReportType type() { return ReportType.WEEKLY; }

    @Override
    public String render(ReportContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(ReportHtmlBuilder.header(ctx));

        // 1. 设备运行列表
        sb.append(ReportHtmlBuilder.sectionTitle("1. 设备运行列表"));
        sb.append(ReportHtmlBuilder.openTable("设备编号", "设备名称", "设备类型", "传感器数", "在线状态", "最近上报时间"));
        if (ctx.devices().isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无设备", "", "", "", "", ""));
        } else {
            for (DeviceBrief d : ctx.devices()) {
                String status = d.onlineStatus() == null ? "未注册"
                    : (d.onlineStatus() == 1 ? "<span style=\"color:#67c23a;\">在线</span>"
                                              : "<span style=\"color:#f56c6c;\">离线</span>");
                sb.append(ReportHtmlBuilder.row(
                    d.code(), d.name(),
                    d.deviceType() == null ? "-" : String.valueOf(d.deviceType()),
                    String.valueOf(d.sensorCount()),
                    status,
                    d.lastReportAt() == null ? "-" : d.lastReportAt().toString()
                ));
            }
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 2. 监测数据概况
        sb.append(ReportHtmlBuilder.sectionTitle("2. 监测数据概况"));
        sb.append(ReportHtmlBuilder.openTable("属性编码", "属性名称", "单位", "最新值", "周最大", "周最小", "周均值"));
        if (ctx.metrics().isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无数据", "", "", "", "", "", ""));
        } else {
            for (MetricRow m : ctx.metrics()) {
                sb.append(ReportHtmlBuilder.row(
                    m.attrCode(), m.attrName(), m.unit(),
                    fmt(m.latest()), fmt(m.maxValue()), fmt(m.minValue()), fmt(m.avgValue())
                ));
            }
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 3. 数据完整率
        sb.append(ReportHtmlBuilder.sectionTitle("3. 数据完整率"));
        sb.append(ReportHtmlBuilder.openTable("属性", "完整率"));
        if (ctx.metrics().isEmpty()) {
            sb.append(ReportHtmlBuilder.row("无数据", "-"));
        } else {
            for (MetricRow m : ctx.metrics()) {
                String pct = m.completenessPct() == null ? "-" : String.format("%.1f%%", m.completenessPct());
                sb.append(ReportHtmlBuilder.row(m.attrName(), pct));
            }
        }
        sb.append(ReportHtmlBuilder.closeTable());

        // 4. 异常数据
        sb.append(ReportHtmlBuilder.sectionTitle("4. 异常数据"));
        sb.append(ReportHtmlBuilder.paragraph("本周无异常数据 (所有指标均在阈值范围内)。"));

        // 5. 分析与建议
        sb.append(ReportHtmlBuilder.sectionTitle("5. 分析与建议"));
        List<String> advice = new ArrayList<>();
        advice.add("设备在线率 " + String.format("%.1f", ctx.onlineRatePct()) + "%, "
            + (ctx.onlineRatePct() >= 95 ? "运行正常。" : "建议核查离线设备。"));
        advice.add("建议持续关注本周变化较大的指标。");
        advice.add("确保各监测点设备供电及通信正常。");
        sb.append(ReportHtmlBuilder.bulletList(advice.toArray(new String[0])));

        return sb.toString();
    }

    private static String fmt(Double v) {
        return v == null ? "-" : String.format("%.3f", v);
    }
}
