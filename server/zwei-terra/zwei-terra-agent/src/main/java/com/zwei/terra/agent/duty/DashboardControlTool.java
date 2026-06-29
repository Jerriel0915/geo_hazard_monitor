package com.zwei.terra.agent.duty;

import com.zwei.terra.core.tool.TerraTool;
import com.zwei.terra.core.tool.ToolMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 值守模式面板控制工具 — 让 Terra AI 能够控制 Dashboard 面板。
 *
 * <p>提供创建/销毁/更新面板、设置地图视图、更新 Terra 状态等能力。
 * AI 通过 ReAct Loop 调用这些工具方法，实现对值守看板的动态操控。</p>
 */
@Component
@TerraTool(name = "dashboard", description = "值守模式看板面板控制工具集", category = "duty")
@Slf4j
public class DashboardControlTool {

    @Autowired
    private TerraDutySessionManager sessionManager;

    /** 面板 ID 序列号 */
    private final AtomicInteger panelSeq = new AtomicInteger(0);

    // ==================== 面板生命周期 ====================

    @ToolMethod(description = "创建一个图表面板来展示数据图表。参数：" +
            "title(必填，面板标题)，chartType(可选，图表类型：line/bar/pie/scatter，默认line)，" +
            "data(必填，图表数据对象，包含 categories(X轴标签数组) 和 series(数据系列数组，每项含name和data数组))")
    public Map<String, Object> createChart(String title, String chartType, Object data) {
        String panelId = "chart-" + panelSeq.incrementAndGet();
        Map<String, Object> position = defaultPosition(6, 4);

        sessionManager.broadcast(DutyProtocol.createPanel(panelId, "chart", title, data, position));

        return result(panelId, "图表面板已创建: " + title);
    }

    @ToolMethod(description = "创建一个地图面板来展示设备/隐患点分布。参数：" +
            "title(必填，面板标题)，" +
            "markers(可选，标记点数组，每项含lat(纬度)、lng(经度)、name(名称)、state(状态: normal/watching/warning))，" +
            "center(可选，地图中心坐标，格式'纬度,经度')，zoom(可选，缩放级别，默认15)")
    public Map<String, Object> createMap(String title, Object markers, String center, Integer zoom) {
        String panelId = "map-" + panelSeq.incrementAndGet();

        Map<String, Object> data = new LinkedHashMap<>();
        if (markers != null) {
            data.put("markers", markers);
        }
        if (center != null) {
            data.put("center", center);
        }
        data.put("zoom", zoom != null ? zoom : 15);

        Map<String, Object> position = defaultPosition(6, 6);
        sessionManager.broadcast(DutyProtocol.createPanel(panelId, "map", title, data, position));

        return result(panelId, "地图面板已创建: " + title);
    }

    @ToolMethod(description = "创建一个表格面板来展示结构化数据。参数：" +
            "title(必填，面板标题)，" +
            "columns(必填，列定义数组，每项含key(字段名)和label(显示名))，" +
            "rows(必填，数据行数组)")
    public Map<String, Object> createTable(String title, Object columns, Object rows) {
        String panelId = "table-" + panelSeq.incrementAndGet();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("columns", columns);
        data.put("rows", rows);

        Map<String, Object> position = defaultPosition(8, 4);
        sessionManager.broadcast(DutyProtocol.createPanel(panelId, "table", title, data, position));

        return result(panelId, "表格面板已创建: " + title);
    }

    @ToolMethod(description = "创建一个视频面板来展示视频监控画面。参数：" +
            "title(必填，面板标题)，videoUrl(可选，视频流URL)，imageUrl(可选，视频快照URL)")
    public Map<String, Object> createVideo(String title, String videoUrl, String imageUrl) {
        String panelId = "video-" + panelSeq.incrementAndGet();

        Map<String, Object> data = new LinkedHashMap<>();
        if (videoUrl != null) data.put("videoUrl", videoUrl);
        if (imageUrl != null) data.put("imageUrl", imageUrl);

        Map<String, Object> position = defaultPosition(4, 4);
        sessionManager.broadcast(DutyProtocol.createPanel(panelId, "video", title, data, position));

        return result(panelId, "视频面板已创建: " + title);
    }

    @ToolMethod(description = "创建一个图片面板。参数：" +
            "title(必填，面板标题)，imageUrl(必填，图片URL)")
    public Map<String, Object> createImage(String title, String imageUrl) {
        String panelId = "image-" + panelSeq.incrementAndGet();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("imageUrl", imageUrl);

        Map<String, Object> position = defaultPosition(4, 3);
        sessionManager.broadcast(DutyProtocol.createPanel(panelId, "image", title, data, position));

        return result(panelId, "图片面板已创建: " + title);
    }

    @ToolMethod(description = "创建一个内嵌网页面板。参数：" +
            "title(必填，面板标题)，url(必填，要嵌入的网页URL)")
    public Map<String, Object> createIframe(String title, String url) {
        String panelId = "iframe-" + panelSeq.incrementAndGet();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", url);

        Map<String, Object> position = defaultPosition(8, 5);
        sessionManager.broadcast(DutyProtocol.createPanel(panelId, "iframe", title, data, position));

        return result(panelId, "网页面板已创建: " + title);
    }

    @ToolMethod(description = "销毁指定面板。参数：panelId(必填，面板ID)")
    public Map<String, Object> destroyPanel(String panelId) {
        sessionManager.broadcast(DutyProtocol.destroyPanel(panelId));
        return result(panelId, "面板已销毁: " + panelId);
    }

    @ToolMethod(description = "清除所有面板。不需要参数。")
    public Map<String, Object> clearAllPanels() {
        sessionManager.broadcast(DutyProtocol.clearAllPanels());
        panelSeq.set(0);
        return result("all", "所有面板已清除");
    }

    // ==================== 面板操作 ====================

    @ToolMethod(description = "更新面板数据。参数：panelId(必填，面板ID)，data(必填，新的数据对象)")
    public Map<String, Object> updatePanelData(String panelId, Object data) {
        sessionManager.broadcast(DutyProtocol.setPanelData(panelId, data));
        return result(panelId, "面板数据已更新: " + panelId);
    }

    @ToolMethod(description = "地图导航到指定坐标。参数：panelId(必填，地图面板ID)，lat(必填，纬度)，lng(必填，经度)，zoom(可选，缩放级别，默认15)")
    public Map<String, Object> mapNavigate(String panelId, Double lat, Double lng, Integer zoom) {
        sessionManager.broadcast(DutyProtocol.mapSetView(panelId,
                lat != null ? lat : 0, lng != null ? lng : 0, zoom != null ? zoom : 15));
        return result(panelId, "地图已导航到: " + lat + "," + lng);
    }

    // ==================== Terra 状态 ====================

    @ToolMethod(description = "更新值守助手头像状态。参数：" +
            "state(必填，状态：normal=正常巡检/info=需要关注/caution=需要注意/warning=发出警告/critical=紧急情况)，" +
            "message(可选，状态描述)")
    public Map<String, Object> setTerraState(String state, String message) {
        sessionManager.broadcast(DutyProtocol.terraState(state, message));
        return result("state", "Terra 状态已更新: " + state);
    }

    @ToolMethod(description = "弹出告警提示。参数：" +
            "level(必填，告警级别：attention/warning/critical)，" +
            "title(必填，告警标题)，description(必填，告警描述)")
    public Map<String, Object> showAlert(String level, String title, String description) {
        sessionManager.broadcast(DutyProtocol.alert(level, title, description));
        return result("alert", "告警已弹出: " + title);
    }

    // ==================== 内部方法 ====================

    private Map<String, Object> result(String id, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("message", message);
        return result;
    }

    private Map<String, Object> defaultPosition(int w, int h) {
        Map<String, Object> pos = new LinkedHashMap<>();
        pos.put("x", 1);
        pos.put("y", 1);
        pos.put("w", w);
        pos.put("h", h);
        return pos;
    }
}
