package com.zwei.terra.agent.tool;

import com.zwei.terra.core.tool.TerraTool;
import com.zwei.terra.core.tool.ToolMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 系统数据查询工具 — 让 Terra AI 能够查询知微系统的实时业务数据。
 * <p>
 * 通过 JdbcTemplate 直接查询数据库，保持 terra 模块与其他业务模块的完全隔离。
 */
@Component
@TerraTool(name = "system.query", description = "知微系统数据查询工具集", category = "system")
@Slf4j
public class SystemQueryTool {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== 设备相关 ====================

    @ToolMethod(description = "查询设备统计信息，包括设备总数、在线数、离线数、按监测类型分组的统计。不需要任何参数。")
    public Map<String, Object> deviceStat() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 总数
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM device WHERE del_flag = '0'", Integer.class);
        result.put("total", total != null ? total : 0);

        // 在线/离线
        Integer online = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM device_online_status WHERE status = 'online'", Integer.class);
        result.put("online", online != null ? online : 0);
        result.put("offline", Math.max(0, (total != null ? total : 0) - (online != null ? online : 0)));

        // 按类型分组
        List<Map<String, Object>> byType = jdbcTemplate.queryForList(
            "SELECT mt.type_name AS type, COUNT(d.id) AS count " +
            "FROM device d LEFT JOIN monitor_type mt ON d.monitor_type_id = mt.id " +
            "WHERE d.del_flag = '0' GROUP BY d.monitor_type_id, mt.type_name ORDER BY count DESC");
        result.put("byType", byType);

        return result;
    }

    @ToolMethod(description = "查询设备列表。参数：keyword(可选，设备名称或编号关键词)，limit(可选，返回条数，默认10)")
    public List<Map<String, Object>> deviceList(String keyword, Integer limit) {
        int lim = limit != null ? Math.min(limit, 50) : 10;
        String pattern = "%" + (keyword != null ? keyword : "") + "%";

        return jdbcTemplate.queryForList(
            "SELECT d.id, d.device_name, d.device_code, d.status, d.last_report_time, " +
            "mt.type_name AS monitor_type, hp.name AS hazard_point_name " +
            "FROM device d " +
            "LEFT JOIN monitor_type mt ON d.monitor_type_id = mt.id " +
            "LEFT JOIN device_hazard_point dhp ON d.id = dhp.device_id " +
            "LEFT JOIN hazard_point hp ON dhp.hazard_point_id = hp.id " +
            "WHERE d.del_flag = '0' AND (d.device_name LIKE ? OR d.device_code LIKE ?) " +
            "ORDER BY d.id DESC LIMIT ?",
            pattern, pattern, lim);
    }

    // ==================== 隐患点相关 ====================

    @ToolMethod(description = "查询隐患点统计信息，包括隐患点总数、正常数、预警数、按地区或级别分组。不需要任何参数。")
    public Map<String, Object> hazardPointStat() {
        Map<String, Object> result = new LinkedHashMap<>();

        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM hazard_point WHERE del_flag = '0'", Integer.class);
        result.put("total", total != null ? total : 0);

        // 按状态分组
        List<Map<String, Object>> byStatus = jdbcTemplate.queryForList(
            "SELECT status, COUNT(*) AS count FROM hazard_point " +
            "WHERE del_flag = '0' GROUP BY status ORDER BY count DESC");
        result.put("byStatus", byStatus);

        return result;
    }

    @ToolMethod(description = "查询隐患点列表。参数：keyword(可选，隐患点名称关键词)，limit(可选，返回条数，默认10)")
    public List<Map<String, Object>> hazardPointList(String keyword, Integer limit) {
        int lim = limit != null ? Math.min(limit, 50) : 10;
        String pattern = "%" + (keyword != null ? keyword : "") + "%";

        return jdbcTemplate.queryForList(
            "SELECT id, name, code, status, longitude, latitude, description " +
            "FROM hazard_point WHERE del_flag = '0' AND name LIKE ? " +
            "ORDER BY id DESC LIMIT ?",
            pattern, lim);
    }

    // ==================== 告警相关 ====================

    @ToolMethod(description = "查询告警统计信息。参数：days(可选，统计最近N天的数据，默认7天)")
    public Map<String, Object> alarmStat(Integer days) {
        int d = days != null ? Math.min(days, 365) : 7;
        Map<String, Object> result = new LinkedHashMap<>();

        // 总数
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM alarm_record WHERE create_time >= DATE_SUB(NOW(), INTERVAL ? DAY)",
            Integer.class, d);
        result.put("total", total != null ? total : 0);

        // 待处理
        Integer pending = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM alarm_record WHERE status = '0' AND create_time >= DATE_SUB(NOW(), INTERVAL ? DAY)",
            Integer.class, d);
        result.put("pending", pending != null ? pending : 0);

        // 按级别分组
        List<Map<String, Object>> byLevel = jdbcTemplate.queryForList(
            "SELECT alarm_level AS level, COUNT(*) AS count FROM alarm_record " +
            "WHERE create_time >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
            "GROUP BY alarm_level ORDER BY alarm_level", d);
        result.put("byLevel", byLevel);

        return result;
    }

    // ==================== 传感器相关 ====================

    @ToolMethod(description = "查询某设备下的传感器列表。参数：deviceId(必填，设备ID)")
    public List<Map<String, Object>> sensorList(Long deviceId) {
        return jdbcTemplate.queryForList(
            "SELECT ds.id, ds.sensor_no, ds.sensor_name, ds.monitor_content_id, " +
            "mc.content_name AS monitor_content, ds.last_report_time, ds.status " +
            "FROM device_sensor ds " +
            "LEFT JOIN monitor_content mc ON ds.monitor_content_id = mc.id " +
            "WHERE ds.device_id = ? AND ds.del_flag = '0' ORDER BY ds.sensor_no",
            deviceId);
    }

    // ==================== 系统总览 ====================

    @ToolMethod(description = "获取系统总览统计数据，包括设备、隐患点、告警的关键指标。不需要任何参数。")
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 设备
        Integer deviceTotal = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM device WHERE del_flag = '0'", Integer.class);
        Integer deviceOnline = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM device_online_status WHERE status = 'online'", Integer.class);
        result.put("deviceTotal", deviceTotal != null ? deviceTotal : 0);
        result.put("deviceOnline", deviceOnline != null ? deviceOnline : 0);

        // 隐患点
        Integer hpTotal = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM hazard_point WHERE del_flag = '0'", Integer.class);
        result.put("hazardPointTotal", hpTotal != null ? hpTotal : 0);

        // 今日告警
        Integer todayAlarm = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM alarm_record WHERE DATE(create_time) = CURDATE()",
            Integer.class);
        result.put("todayAlarm", todayAlarm != null ? todayAlarm : 0);

        // 待处理告警
        Integer pendingAlarm = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM alarm_record WHERE status = '0'", Integer.class);
        result.put("pendingAlarm", pendingAlarm != null ? pendingAlarm : 0);

        return result;
    }
}
