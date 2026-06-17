package com.zwei.iot.report.render;

import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;

/**
 * HTML 拼接工具 (内联样式, PDF 截图友好)。
 */
public final class ReportHtmlBuilder {

    private static final String STYLE_TABLE = "border-collapse:collapse;width:100%;text-align:center;font-size:12px;";
    private static final String STYLE_TH = "background:#f0f5ff;padding:6px;border:1px solid #ddd;";
    private static final String STYLE_TD = "padding:6px;border:1px solid #ddd;";

    private ReportHtmlBuilder() {}

    public static String header(ReportContext ctx) {
        ReportType t = ctx.type();
        String title = "地质灾害监测" + t.desc();
        return "<h2 style=\"text-align:center;color:#1f2d3d;margin-bottom:4px;\">" + title + "</h2>"
            + "<div style=\"border-bottom:2px solid #1f2d3d;margin:8px 0 16px;\"></div>"
            + "<p style=\"margin:4px 0;\"><strong>报告周期：</strong>" + ctx.period().start() + " 至 " + ctx.period().end() + "</p>"
            + "<p style=\"margin:4px 0;\"><strong>隐患点：</strong>" + ctx.hazardPoint().code() + " " + ctx.hazardPoint().name() + "</p>"
            + (ctx.hazardPoint().longitude() != null
                ? "<p style=\"margin:4px 0;\"><strong>隐患点位置：</strong>经度 " + ctx.hazardPoint().longitude() + ", 纬度 " + ctx.hazardPoint().latitude() + "</p>"
                : "")
            + "<p style=\"margin:4px 0;\"><strong>生成时间：</strong>" + ctx.generatedAt() + "</p>"
            + "<div style=\"height:12px;\"></div>";
    }

    public static String sectionTitle(String text) {
        return "<h3 style=\"color:#1f2d3d;border-left:4px solid #409eff;padding-left:8px;margin:16px 0 8px;\">"
            + text + "</h3>";
    }

    public static String openTable(String... headers) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table style=\"").append(STYLE_TABLE).append("\">");
        sb.append("<thead><tr>");
        for (String h : headers) {
            sb.append("<th style=\"").append(STYLE_TH).append("\">").append(h).append("</th>");
        }
        sb.append("</tr></thead><tbody>");
        return sb.toString();
    }

    public static String row(String... cells) {
        StringBuilder sb = new StringBuilder("<tr>");
        for (String c : cells) {
            sb.append("<td style=\"").append(STYLE_TD).append("\">").append(c == null ? "-" : c).append("</td>");
        }
        return sb.append("</tr>").toString();
    }

    public static String closeTable() { return "</tbody></table>"; }

    public static String paragraph(String text) {
        return "<p style=\"margin:8px 0;line-height:1.6;\">" + text + "</p>";
    }

    public static String bulletList(String... items) {
        StringBuilder sb = new StringBuilder("<ul style=\"margin:8px 0;\">");
        for (String i : items) sb.append("<li>").append(i).append("</li>");
        return sb.append("</ul>").toString();
    }

    public static String levelColor(int level) {
        return switch (level) {
            case 4 -> "#ff4d4f";
            case 3 -> "#fa8c16";
            case 2 -> "#faad14";
            case 1 -> "#1890ff";
            default -> "#909399";
        };
    }

    public static String levelName(int level) {
        return switch (level) {
            case 4 -> "红色";
            case 3 -> "橙色";
            case 2 -> "黄色";
            case 1 -> "蓝色";
            default -> "无";
        };
    }
}
