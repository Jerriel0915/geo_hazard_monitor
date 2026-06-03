package com.zwei.monitor.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zwei.monitor.config.MqttHttpApiProperties;
import com.zwei.monitor.domain.MqttClientInfo;
import com.zwei.monitor.domain.MqttClientPageResponse;
import com.zwei.monitor.domain.MqttStatsResponse;
import com.zwei.monitor.domain.MqttSubscriptionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * mica-mqtt HTTP API 客户端。
 * <p>
 * 通过内部 HTTP 调用 mica-mqtt 管理接口（默认端口 18083），
 * 获取服务器统计、客户端列表、订阅详情，并支持踢出客户端等管理操作。
 * <p>
 * 参数与返回格式约定见 {@code docs/MqttHttpApi.md}。
 */
@Slf4j
@Component
public class MqttHttpApiClient {

    private final RestTemplate restTemplate;
    private final MqttHttpApiProperties properties;

    public MqttHttpApiClient(@Qualifier("mqttHttpRestTemplate") RestTemplate restTemplate,
                             MqttHttpApiProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    // ==================== 统计信息 ====================

    /**
     * 获取 MQTT 服务器统计信息。
     *
     * @return 统计结果；不可用时返回 {@code unavailable} 标记
     */
    public MqttStatsResponse getStats() {
        if (!properties.isEnable()) {
            log.debug("MQTT HTTP API 未启用 (mqtt.server.http-listener.enable=false)，跳过 stats 查询");
            return MqttStatsResponse.unavailable();
        }
        try {
            String json = restTemplate.getForObject("/stats", String.class);
            log.debug("MQTT /stats HTTP 状态码=200, body 长度={}", json != null ? json.length() : 0);
            return parseStats(json);
        } catch (RestClientException e) {
            log.warn("无法获取 MQTT 统计信息: {}", e.getMessage());
            return MqttStatsResponse.unavailable();
        }
    }

    // ==================== 客户端管理 ====================

    /**
     * 分页查询当前连接的客户端列表。
     *
     * @param page  页码（从 1 开始）
     * @param limit 每页大小
     * @return 分页客户端列表
     */
    public MqttClientPageResponse getClients(int page, int limit) {
        if (!properties.isEnable()) {
            return emptyPage(page, limit);
        }
        try {
            String json = restTemplate.getForObject(
                    "/clients?_page={page}&_limit={limit}",
                    String.class, page, limit
            );
            return parseClientPage(json, page, limit);
        } catch (RestClientException e) {
            log.warn("无法获取 MQTT 客户端列表: {}", e.getMessage());
            return emptyPage(page, limit);
        }
    }

    /**
     * 获取单个客户端详情。
     *
     * @param clientId MQTT clientId
     * @return 客户端详情；未命中时返回 {@code null}
     */
    public MqttClientInfo getClientInfo(String clientId) {
        if (!properties.isEnable()) {
            return null;
        }
        try {
            String json = restTemplate.getForObject(
                    "/clients/info?clientId={clientId}",
                    String.class, clientId
            );
            return parseClientInfo(json);
        } catch (RestClientException e) {
            log.warn("无法获取 MQTT 客户端详情 clientId={}: {}", clientId, e.getMessage());
            return null;
        }
    }

    /**
     * 获取客户端的主题订阅列表。
     *
     * @param clientId MQTT clientId
     * @return 订阅列表
     */
    public List<MqttSubscriptionInfo> getClientSubscriptions(String clientId) {
        if (!properties.isEnable()) {
            return Collections.emptyList();
        }
        try {
            String json = restTemplate.getForObject(
                    "/client/subscriptions?clientId={clientId}",
                    String.class, clientId
            );
            return parseSubscriptions(json);
        } catch (RestClientException e) {
            log.warn("无法获取 MQTT 客户端订阅 clientId={}: {}", clientId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 踢出指定客户端（断开连接并清除会话）。
     *
     * @param clientId MQTT clientId
     * @return 是否成功
     */
    public boolean kickClient(String clientId) {
        if (!properties.isEnable()) {
            return false;
        }
        try {
            String json = restTemplate.postForObject(
                    "/clients/delete?clientId={clientId}",
                    null, String.class, clientId
            );
            JSONObject result = JSON.parseObject(json);
            return result != null && result.getIntValue("code") == 1;
        } catch (RestClientException e) {
            log.warn("踢出 MQTT 客户端失败 clientId={}: {}", clientId, e.getMessage());
            return false;
        }
    }

    // ==================== 解析逻辑 ====================

    @SuppressWarnings("unchecked")
    private MqttStatsResponse parseStats(String json) {
        log.debug("MQTT /stats raw response: {}", json);
        JSONObject root = JSON.parseObject(json);
        MqttStatsResponse r = new MqttStatsResponse();
        r.setUpstream(true);
        if (root == null) {
            log.warn("MQTT /stats 响应解析为 null，raw={}", json);
            r.setUpstream(false);
            return r;
        }
        int code = root.getIntValue("code");
        if (code != 1) {
            log.warn("MQTT /stats 返回非成功码 code={}, raw={}", code, json);
            r.setUpstream(false);
            return r;
        }
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            r.setUpstream(false);
            return r;
        }

        // connections
        JSONObject connJson = data.getJSONObject("connections");
        if (connJson != null) {
            MqttStatsResponse.Connections conn = new MqttStatsResponse.Connections();
            conn.setAccepted(connJson.getLongValue("accepted"));
            conn.setClosed(connJson.getLongValue("closed"));
            conn.setSize(connJson.getLongValue("size"));
            r.setConnections(conn);
        }

        // messages
        JSONObject msgJson = data.getJSONObject("messages");
        if (msgJson != null) {
            MqttStatsResponse.Messages msg = new MqttStatsResponse.Messages();
            msg.setHandledPackets(msgJson.getLongValue("handledPackets"));
            msg.setHandledBytes(msgJson.getLongValue("handledBytes"));
            msg.setReceivedPackets(msgJson.getLongValue("receivedPackets"));
            msg.setReceivedBytes(msgJson.getLongValue("receivedBytes"));
            msg.setSendPackets(msgJson.getLongValue("sendPackets"));
            msg.setSendBytes(msgJson.getLongValue("sendBytes"));
            msg.setBytesPerTcpReceive(msgJson.getDoubleValue("bytesPerTcpReceive"));
            msg.setPacketsPerTcpReceive(msgJson.getDoubleValue("packetsPerTcpReceive"));
            r.setMessages(msg);
        }

        // nodes
        JSONObject nodesJson = data.getJSONObject("nodes");
        if (nodesJson != null) {
            MqttStatsResponse.Nodes nodes = new MqttStatsResponse.Nodes();
            nodes.setClientNodes(nodesJson.getIntValue("clientNodes"));
            nodes.setConnections(nodesJson.getIntValue("connections"));
            nodes.setUsers(nodesJson.getIntValue("users"));
            r.setNodes(nodes);
        }

        Long st = data.getLong("startTime");
        if (st == null) st = root.getLong("startTime");
        r.setStartTime(st);
        return r;
    }

    private MqttClientPageResponse parseClientPage(String json, int page, int limit) {
        JSONObject root = JSON.parseObject(json);
        MqttClientPageResponse r = new MqttClientPageResponse();
        r.setPageNumber(page);
        r.setPageSize(limit);
        if (root == null || root.getIntValue("code") != 1) {
            return r;
        }
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            return r;
        }
        r.setTotalRow(data.getIntValue("totalRow"));
        r.setPageNumber(data.getIntValue("pageNumber", page));
        r.setPageSize(data.getIntValue("pageSize", limit));
        JSONArray list = data.getJSONArray("list");
        List<MqttClientInfo> clients = new ArrayList<>();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                JSONObject item = list.getJSONObject(i);
                if (item != null) {
                    clients.add(mapClientInfo(item));
                }
            }
        }
        r.setList(clients);
        return r;
    }

    private MqttClientInfo parseClientInfo(String json) {
        JSONObject root = JSON.parseObject(json);
        if (root == null || root.getIntValue("code") != 1) {
            return null;
        }
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            return null;
        }
        return mapClientInfo(data);
    }

    private MqttClientInfo mapClientInfo(JSONObject o) {
        MqttClientInfo info = new MqttClientInfo();
        info.setClientId(o.getString("clientId"));
        info.setUsername(o.getString("username"));
        info.setConnected(o.getBooleanValue("connected", true));
        info.setIpAddress(o.getString("ipAddress"));
        info.setPort(o.getIntValue("port"));
        info.setProtoName(o.getString("protoName"));
        info.setProtoVer(o.getIntValue("protoVer"));
        info.setCreatedAt(o.getLongValue("createdAt"));
        info.setConnectedAt(o.getLongValue("connectedAt"));
        return info;
    }

    private List<MqttSubscriptionInfo> parseSubscriptions(String json) {
        JSONObject root = JSON.parseObject(json);
        if (root == null || root.getIntValue("code") != 1) {
            return Collections.emptyList();
        }
        JSONArray array = root.getJSONArray("data");
        if (array == null) {
            return Collections.emptyList();
        }
        List<MqttSubscriptionInfo> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            MqttSubscriptionInfo sub = new MqttSubscriptionInfo();
            sub.setClientId(item.getString("clientId"));
            sub.setTopicFilter(item.getString("topicFilter"));
            sub.setMqttQoS(item.getIntValue("mqttQoS"));
            list.add(sub);
        }
        return list;
    }

    private MqttClientPageResponse emptyPage(int page, int limit) {
        MqttClientPageResponse r = new MqttClientPageResponse();
        r.setPageNumber(page);
        r.setPageSize(limit);
        r.setTotalRow(0);
        r.setList(Collections.emptyList());
        return r;
    }
}
