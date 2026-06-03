package com.zwei.monitor.controller;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.monitor.client.MqttHttpApiClient;
import com.zwei.monitor.domain.MqttClientInfo;
import com.zwei.monitor.domain.MqttClientPageResponse;
import com.zwei.monitor.domain.MqttSubscriptionInfo;
import com.zwei.monitor.service.MqttSessionEnrichService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MQTT 客户端管理接口
 * <p>
 * 提供客户端连接列表查询、详情查看（含订阅）、踢出等交互操作。
 */
@RestController
@RequestMapping("/api/v1/monitor/mqtt/clients")
public class MqttClientController {

    private final MqttHttpApiClient mqttHttpApiClient;
    private final MqttSessionEnrichService enrichService;

    public MqttClientController(MqttHttpApiClient mqttHttpApiClient,
                                MqttSessionEnrichService enrichService) {
        this.mqttHttpApiClient = mqttHttpApiClient;
        this.enrichService = enrichService;
    }

    /**
     * 分页查询当前连接的客户端列表（已富化设备信息）
     *
     * @param page  页码，默认 1
     * @param limit 每页大小，默认 20
     */
    @PreAuthorize("@ss.hasPermi('monitor:mqtt:list')")
    @GetMapping("/page")
    public AjaxResult getClients(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int limit) {
        MqttClientPageResponse raw = mqttHttpApiClient.getClients(page, limit);
        List<MqttClientInfo> enriched = enrichService.enrichBatch(raw.getList());
        raw.setList(enriched);
        return AjaxResult.success(raw);
    }

    /**
     * 获取指定客户端详情，包含订阅主题列表
     *
     * @param clientId MQTT clientId
     */
    @PreAuthorize("@ss.hasPermi('monitor:mqtt:list')")
    @GetMapping("/{clientId}")
    public AjaxResult getClientDetail(@PathVariable String clientId) {
        MqttClientInfo info = mqttHttpApiClient.getClientInfo(clientId);
        if (info == null) {
            return AjaxResult.error("客户端不存在或已离线: " + clientId);
        }
        enrichService.enrich(info);
        List<MqttSubscriptionInfo> subscriptions = mqttHttpApiClient.getClientSubscriptions(clientId);

        Map<String, Object> result = new HashMap<>();
        result.put("info", info);
        result.put("subscriptions", subscriptions);
        return AjaxResult.success(result);
    }

    /**
     * 踢出指定客户端（断开连接并清除会话）
     *
     * @param clientId MQTT clientId
     */
    @PreAuthorize("@ss.hasPermi('monitor:mqtt:kick')")
    @DeleteMapping("/{clientId}")
    public AjaxResult kickClient(@PathVariable String clientId) {
        boolean ok = mqttHttpApiClient.kickClient(clientId);
        if (ok) {
            return AjaxResult.success("已踢出客户端: " + clientId);
        }
        return AjaxResult.error("踢出失败，请确认 MQTT HTTP API 已启用且客户端在线");
    }

    /**
     * 批量踢出客户端
     *
     * @param clientIds MQTT clientId 列表
     */
    @PreAuthorize("@ss.hasPermi('monitor:mqtt:kick')")
    @DeleteMapping("/batch")
    public AjaxResult kickClients(@RequestBody List<String> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) {
            return AjaxResult.error("clientIds 不能为空");
        }
        Map<String, Object> result = new HashMap<>();
        int success = 0;
        int fail = 0;
        for (String clientId : clientIds) {
            if (mqttHttpApiClient.kickClient(clientId)) {
                success++;
            } else {
                fail++;
            }
        }
        result.put("success", success);
        result.put("fail", fail);
        result.put("total", clientIds.size());
        return AjaxResult.success(result);
    }
}
