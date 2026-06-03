package com.zwei.monitor.controller;

import com.zwei.common.constant.CacheConstants;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.framework.web.domain.Server;
import com.zwei.monitor.client.MqttHttpApiClient;
import com.zwei.monitor.domain.MqttStatsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统监控总览接口
 * <p>
 * 聚合服务器健康、Redis 缓存、在线用户、MQTT 状态，
 * 为前端监控大屏提供一站式数据入口。
 */
@RestController
@RequestMapping("/api/v1/monitor")
public class MonitorOverviewController {

    private final MqttHttpApiClient mqttHttpApiClient;
    private final RedisCache redisCache;

    @Autowired
    public MonitorOverviewController(MqttHttpApiClient mqttHttpApiClient,
                                     RedisCache redisCache) {
        this.mqttHttpApiClient = mqttHttpApiClient;
        this.redisCache = redisCache;
    }

    /**
     * 获取系统监控总览
     * <p>
     * 一次性返回服务器硬件状态、JVM、Redis 概要、
     * 在线用户数和 MQTT 服务器核心指标。
     */
    @PreAuthorize("@ss.hasPermi('monitor:overview:list')")
    @GetMapping("/overview")
    public AjaxResult overview() throws Exception {
        Map<String, Object> result = new HashMap<>();

        // ---- 服务器健康 ----
        Server server = new Server();
        server.copyTo();
        result.put("server", server);

        // ---- Redis 概要 ----
        Map<String, Object> redisSummary = new HashMap<>();
        try {
            Collection<String> keys = redisCache.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
            redisSummary.put("onlineUserCount", keys != null ? keys.size() : 0);
        } catch (Exception e) {
            redisSummary.put("onlineUserCount", 0);
        }
        try {
            Collection<String> allKeys = redisCache.keys("*");
            redisSummary.put("totalKeys", allKeys != null ? allKeys.size() : 0);
        } catch (Exception e) {
            redisSummary.put("totalKeys", 0);
        }
        result.put("redis", redisSummary);

        // ---- 系统运行时间 ----
        long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        long uptimeMs = System.currentTimeMillis() - jvmStartTime;
        result.put("uptime", Map.of(
                "startTime", jvmStartTime,
                "uptimeMs", uptimeMs,
                "uptimeFormatted", formatUptime(uptimeMs)
        ));

        // ---- MQTT 状态 ----
        MqttStatsResponse mqttStats = mqttHttpApiClient.getStats();
        result.put("mqtt", mqttStats);

        return AjaxResult.success(result);
    }

    private String formatUptime(long uptimeMs) {
        long days = uptimeMs / 86400000;
        long hours = (uptimeMs % 86400000) / 3600000;
        long minutes = (uptimeMs % 3600000) / 60000;
        long seconds = (uptimeMs % 60000) / 1000;
        if (days > 0) {
            return String.format("%d天 %d小时 %d分", days, hours, minutes);
        }
        if (hours > 0) {
            return String.format("%d小时 %d分 %d秒", hours, minutes, seconds);
        }
        return String.format("%d分 %d秒", minutes, seconds);
    }
}
