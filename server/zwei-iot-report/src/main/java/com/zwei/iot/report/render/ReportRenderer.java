package com.zwei.iot.report.render;

import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.domain.ReportType;

/**
 * 报告渲染策略 (Spring 自动注入所有实现)。
 */
public interface ReportRenderer {

    ReportType type();

    /**
     * 将 ctx 渲染为完整 HTML 字符串。
     * HTML 内联样式, 不引用外部 CSS / 字体 (html2canvas 截图友好)。
     */
    String render(ReportContext ctx);
}
