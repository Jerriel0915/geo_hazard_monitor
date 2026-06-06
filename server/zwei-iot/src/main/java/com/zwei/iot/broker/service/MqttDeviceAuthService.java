package com.zwei.iot.broker.service;

import com.zwei.common.event.DeviceOnlineEvent;
import com.zwei.common.utils.DateUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.iot.broker.component.MqttAuthFailureGuard;
import com.zwei.iot.broker.component.MqttDeviceSessionRegistry;
import com.zwei.iot.broker.config.MqttAuthCenterProperties;
import com.zwei.iot.broker.exception.MqttBusinessException;
import com.zwei.iot.broker.exception.MqttConnectionException;
import com.zwei.iot.broker.exception.MqttExceptionReporter;
import com.zwei.iot.broker.exception.MqttProtocolException;
import com.zwei.iot.broker.model.MqttDeviceSession;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceAuthLog;
import com.zwei.iot.device.service.IDeviceAuthQueryService;
import com.zwei.iot.device.service.DeviceAuthLogService;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Node;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MQTT 设备鉴权中心服务。
 * <p>
 * 统一处理设备 CONNECT 鉴权、发布主题准入、会话抢占、失败封禁、
 * 在线离线状态回写以及鉴权审计日志，作为 Broker 鉴权扩展点背后的核心业务实现。
 */
@Service
@Slf4j
public class MqttDeviceAuthService {
    /**
     * 设备账号固定为 6 位大写字母或数字。
     */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Z0-9]{6}$");
    /**
     * 设备密码固定为 8 位字母数字组合。
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[A-Za-z0-9]{8}$");
    /**
     * 平台通用 JSON 上报主题。设备标识使用 deviceCode 与订阅主题保持一致。
     */
    private static final Pattern SYS_TOPIC_PATTERN = Pattern.compile("^sys/v1/(?<deviceCode>[A-Za-z0-9_-]{1,64})/(?<sensorNo>[A-Za-z0-9_-]{1,64})/updata$");
    /**
     * 国标兼容主题。当前鉴权中心只做 topic 级别准入，不解析报文体。
     */
    private static final Pattern GB_TOPIC_PATTERN = Pattern.compile("^gb/v1/(?<deviceCode>[A-Za-z0-9_-]{1,64})/(?<sensorNo>[A-Za-z0-9_-]{1,64})/updata$");
    private static final String PROTOCOL_MQTT = "MQTT";
    private static final int AUTH_STATUS_ENABLED = 1;
    private static final int DEVICE_RUN_STATUS_RUNNING = 1;
    private static final int DEVICE_RUN_STATUS_STOPPED = 2;
    private static final int AUTH_SUCCESS = 1;
    private static final int AUTH_FAILED = 0;

    private final IDeviceAuthQueryService deviceAuthQueryService;
    private final DeviceAuthLogService deviceAuthLogService;
    private final MqttDeviceSessionRegistry sessionRegistry;
    private final MqttAuthFailureGuard failureGuard;
    private final MqttAuthCenterProperties properties;
    private final ObjectProvider<MqttServer> mqttServerProvider;
    private final MqttExceptionReporter mqttExceptionReporter;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public MqttDeviceAuthService(IDeviceAuthQueryService deviceAuthQueryService,
                                 DeviceAuthLogService deviceAuthLogService,
                                 MqttDeviceSessionRegistry sessionRegistry,
                                 MqttAuthFailureGuard failureGuard,
                                 MqttAuthCenterProperties properties,
                                 ObjectProvider<MqttServer> mqttServerProvider,
                                 MqttExceptionReporter mqttExceptionReporter,
                                 ApplicationEventPublisher eventPublisher) {
        this.deviceAuthQueryService = deviceAuthQueryService;
        this.deviceAuthLogService = deviceAuthLogService;
        this.sessionRegistry = sessionRegistry;
        this.failureGuard = failureGuard;
        this.properties = properties;
        this.mqttServerProvider = mqttServerProvider;
        this.mqttExceptionReporter = mqttExceptionReporter;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 执行设备连接鉴权。
     * <p>
     * 处理顺序为：输入归一化 -> 格式校验 -> 失败封禁判断 -> 设备账号校验 ->
     * 在线状态回写 -> 会话注册与旧连接挤占 -> 鉴权审计记录。
     *
     * @param context  当前连接上下文
     * @param uniqueId Broker 内唯一 ID
     * @param clientId 客户端 ID
     * @param username 设备认证账号
     * @param password 设备认证密码
     * @return {@code true} 表示允许连接建立
     */
    public boolean authenticate(ChannelContext context, String uniqueId, String clientId, String username, String password) {
        // 优先使用 Broker 侧唯一标识，避免 clientId 被二次加工后导致会话键不一致。
        String normalizedClientId = normalizeClientId(uniqueId, clientId);
        String normalizedUsername = normalize(username);
        String normalizedPassword = normalize(password);
        String clientIp = resolveClientIp(context);
        var baseContext = mqttExceptionReporter.context(normalizedClientId)
                .clientId(normalizedClientId)
                .putAttribute("username", normalizedUsername)
                .putAttribute("ip", clientIp)
                .build();

        if (normalizedClientId == null) {
            return mqttExceptionReporter.rejectWithWarn(new MqttProtocolException.MalformedPacket(baseContext, "clientId为空"));
        }
        if (!isUsernameValid(normalizedUsername)) {
            return mqttExceptionReporter.rejectWithWarn(new MqttConnectionException.AuthenticationFailed(baseContext, "用户名格式非法"));
        }
        // 密码格式错误直接拒绝，并记入失败次数，避免无意义访问继续穿透到业务流程。
        if (!isPasswordValid(normalizedPassword)) {
            Device device = deviceAuthQueryService.findByAuthUsername(normalizedUsername);
            logFailure(device, normalizedUsername, normalizedClientId, clientIp, "PASSWORD_FORMAT_INVALID");
            failureGuard.recordFailure(normalizedUsername);
            return false;
        }
        // 被临时封禁的账号不再继续访问数据库校验，直接返回剩余封禁信息。
        if (failureGuard.isBlocked(normalizedUsername)) {
            Device blockedDevice = deviceAuthQueryService.findByAuthUsername(normalizedUsername);
            long remaining = failureGuard.getRemainingBlockSeconds(normalizedUsername);
            logFailure(blockedDevice, normalizedUsername, normalizedClientId, clientIp, "AUTH_TEMP_BLOCKED_" + remaining + "S");
            return false;
        }

        Device device = deviceAuthQueryService.findByAuthUsername(normalizedUsername);
        if (device == null) {
            mqttExceptionReporter.rejectWithWarn(new MqttConnectionException.AuthenticationFailed(baseContext, "设备不存在"));
            failureGuard.recordFailure(normalizedUsername);
            return false;
        }
        if (properties.isEnforceMqttProtocol() && !isProtocolAllowed(device.getProtocolType())) {
            logFailure(device, normalizedUsername, normalizedClientId, clientIp, "PROTOCOL_NOT_ALLOWED");
            failureGuard.recordFailure(normalizedUsername);
            return false;
        }
        if (!Objects.equals(device.getAuthStatus(), AUTH_STATUS_ENABLED)) {
            logFailure(device, normalizedUsername, normalizedClientId, clientIp, "ACCOUNT_DISABLED");
            failureGuard.recordFailure(normalizedUsername);
            return false;
        }
        if (!Objects.equals(normalizedPassword, normalize(device.getAuthPassword()))) {
            logFailure(device, normalizedUsername, normalizedClientId, clientIp, "PASSWORD_NOT_MATCH");
            failureGuard.recordFailure(normalizedUsername);
            return false;
        }

        // 鉴权通过后优先回写设备最近一次接入信息，便于后台排查连接来源。
        LocalDateTime now = LocalDateTime.now();
        deviceAuthQueryService.updateDevice(Device.builder()
                .id(device.getId())
                .lastAuthTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, DateUtils.toDate(now)))
                .lastAuthIp(clientIp)
                .runStatus(DEVICE_RUN_STATUS_RUNNING)
                .build());

        MqttDeviceSession session = new MqttDeviceSession(
                device.getId(),
                device.getCode(),
                device.getAuthUsername(),
                normalizedClientId,
                clientIp,
                now
        );
        bindContext(context, session);
        // 会话注册返回旧连接信息，用于实现单设备单活跃连接。
        MqttDeviceSession previous = sessionRegistry.register(session);
        if (previous != null && !Objects.equals(previous.clientId(), normalizedClientId) && properties.isDisconnectPreviousClient()) {
            disconnectPreviousClient(device.getId(), previous.clientId());
        }
        saveAuthLog(device.getId(), normalizedUsername, normalizedClientId, clientIp, AUTH_SUCCESS, null);
        failureGuard.reset(normalizedUsername);
        eventPublisher.publishEvent(new DeviceOnlineEvent(device.getId(), normalizedClientId, clientIp));
        log.info("[MQTT-AUTH] Authentication success. deviceId:{}, clientId:{}, ip:{}", device.getId(), normalizedClientId, clientIp);
        return true;
    }

    /**
     * 校验设备消息发布权限。
     * <p>
     * 发布权限基于三个维度同时校验：topic 格式合法、当前 clientId 已完成鉴权、
     * topic 中的设备 ID 与测点编号必须与当前设备真实归属一致。
     *
     * @param context  当前连接上下文
     * @param clientId 当前连接 clientId
     * @param topic    发布主题
     * @param qoS      发布 QoS
     * @param retain   是否为保留消息
     * @return {@code true} 表示允许发布
     */
    public boolean hasPublishPermission(ChannelContext context, String clientId, String topic, MqttQoS qoS, boolean retain) {
        String normalizedClientId = normalizeClientId(null, clientId);
        PublishTarget publishTarget = parsePublishTarget(topic);
        if (publishTarget == null) {
            return mqttExceptionReporter.rejectWithWarn(new MqttBusinessException.InvalidTopic(
                    mqttExceptionReporter.context(normalizedClientId, topic, qoS).build(),
                    "发布主题非法"
            ));
        }
        Optional<MqttDeviceSession> sessionOptional = sessionRegistry.getByClientId(normalizedClientId);
        if (sessionOptional.isEmpty()) {
            return mqttExceptionReporter.rejectWithWarn(new MqttBusinessException.PermissionDenied(
                    mqttExceptionReporter.context(normalizedClientId, topic, qoS).build(),
                    "未建立鉴权会话，禁止发布"
            ));
        }
        MqttDeviceSession session = sessionOptional.get();
        if (!Objects.equals(session.deviceCode(), publishTarget.deviceCode())) {
            return mqttExceptionReporter.rejectWithWarn(new MqttBusinessException.PermissionDenied(
                    mqttExceptionReporter.context(normalizedClientId, topic, qoS)
                            .putAttribute("authedDeviceCode", session.deviceCode())
                            .putAttribute("topicDeviceCode", publishTarget.deviceCode())
                            .build(),
                    "设备与主题不匹配，禁止发布"
            ));
        }

        // 传感器存在性与启用状态校验统一由 MonitorMetadataService 在数据接入阶段负责，
        // 避免同一条消息在发布准入和元数据加载时对 device_sensor 表重复查询。
        return true;
    }

    /**
     * 处理设备上线事件。
     *
     * @param context  当前连接上下文
     * @param clientId 当前连接 clientId
     * @param username 设备认证账号
     */
    public void handleClientOnline(ChannelContext context, String clientId, String username) {
        String clientIp = resolveClientIp(context);
        sessionRegistry.getByClientId(clientId).ifPresentOrElse(session -> {
            Device update = new Device();
            update.setId(session.deviceId());
            update.setRunStatus(DEVICE_RUN_STATUS_RUNNING);
            if (StringUtils.isNotBlank(clientIp)) {
                update.setLastAuthIp(clientIp);
            }
            deviceAuthQueryService.updateDevice(update);
        }, () -> log.debug("[MQTT-AUTH] Online event ignored because no authenticated session was found. clientId:{}, username:{}",
                clientId, username));
    }

    /**
     * 处理设备离线事件。
     * <p>
     * 优先按 clientId 清理当前活跃会话；若会话已被挤占或提前释放，
     * 再回退到 username 维度补写离线状态。
     *
     * @param context  当前连接上下文
     * @param clientId 当前连接 clientId
     * @param username 设备认证账号
     * @param reason   离线原因
     */
    public void handleClientOffline(ChannelContext context, String clientId, String username, String reason) {
        Optional<MqttDeviceSession> removedSession = sessionRegistry.removeByClientId(clientId);
        if (removedSession.isPresent()) {
            Device update = new Device();
            update.setId(removedSession.get().deviceId());
            update.setRunStatus(DEVICE_RUN_STATUS_STOPPED);
            deviceAuthQueryService.updateDevice(update);
            return;
        }
        Device device = deviceAuthQueryService.findByAuthUsername(normalize(username));
        if (device != null) {
            Device update = new Device();
            update.setId(device.getId());
            update.setRunStatus(DEVICE_RUN_STATUS_STOPPED);
            deviceAuthQueryService.updateDevice(update);
            log.debug("[MQTT-AUTH] Offline event fallback by username. clientId:{}, username:{}, reason:{}",
                    clientId, username, reason);
        }
    }

    /**
     * 主动断开旧连接，落实单设备单活跃连接策略。
     *
     * @param deviceId         设备主键
     * @param previousClientId 旧连接 clientId
     */
    private void disconnectPreviousClient(Long deviceId, String previousClientId) {
        MqttServer mqttServer = mqttServerProvider.getIfAvailable();
        if (mqttServer == null) {
            mqttExceptionReporter.rejectWithWarn(new MqttConnectionException.BrokerUnavailable(
                    mqttExceptionReporter.context(previousClientId).putAttribute("deviceId", deviceId).build(),
                    "Broker实例不可用，无法断开旧连接"
            ));
            return;
        }
        boolean disconnected = mqttServer.disconnect(previousClientId);
        log.info("[MQTT-AUTH] Disconnect previous client. deviceId:{}, previousClientId:{}, success:{}",
                deviceId, previousClientId, disconnected);
    }

    /**
     * 将鉴权结果写回到连接上下文，便于后续事件和权限校验复用。
     *
     * @param context 当前连接上下文
     * @param session 已认证会话
     */
    private void bindContext(ChannelContext context, MqttDeviceSession session) {
        if (context == null) {
            return;
        }
        context.setUserId(String.valueOf(session.deviceId()));
        context.setToken(session.authUsername());
        context.setBsId(session.clientId());
    }

    /**
     * 统一记录鉴权失败日志，并在已识别设备时补写审计记录。
     *
     * @param device   命中的设备实体，可能为空
     * @param username 设备认证账号
     * @param clientId 当前连接 clientId
     * @param clientIp 来源 IP
     * @param reason   失败原因编码
     */
    private void logFailure(Device device, String username, String clientId, String clientIp, String reason) {
        if (device != null) {
            saveAuthLog(device.getId(), username, clientId, clientIp, AUTH_FAILED, reason);
        }
        mqttExceptionReporter.rejectWithWarn(new MqttConnectionException.AuthenticationFailed(
                mqttExceptionReporter.context(clientId)
                        .putAttribute("username", username)
                        .putAttribute("ip", clientIp)
                        .putAttribute("reason", reason)
                        .putAttribute("deviceId", device == null ? null : device.getId())
                        .build(),
                "设备鉴权失败"
        ));
    }

    /**
     * 保存鉴权审计日志。
     *
     * @param deviceId      设备主键
     * @param username      设备认证账号
     * @param clientId      当前连接 clientId
     * @param clientIp      来源 IP
     * @param result        鉴权结果
     * @param failureReason 失败原因，成功场景为空
     */
    private void saveAuthLog(Long deviceId, String username, String clientId, String clientIp, int result, String failureReason) {
        if (deviceId == null || username == null) {
            return;
        }
        DeviceAuthLog authLog = new DeviceAuthLog();
        authLog.setDeviceId(deviceId);
        authLog.setAuthUsername(username);
        authLog.setAuthResult(result);
        authLog.setClientId(limitLength(clientId, 128));
        authLog.setClientIp(limitLength(clientIp, 64));
        authLog.setFailureReason(limitLength(failureReason, 255));
        deviceAuthLogService.save(authLog);
    }

    /**
     * 解析发布主题中的设备编码与测点编号。
     *
     * @param topic 发布主题
     * @return 解析结果；主题不匹配时返回 {@code null}
     */
    private PublishTarget parsePublishTarget(String topic) {
        Matcher sysMatcher = SYS_TOPIC_PATTERN.matcher(topic == null ? "" : topic);
        if (sysMatcher.matches()) {
            return new PublishTarget(sysMatcher.group("deviceCode"), sysMatcher.group("sensorNo"));
        }
        Matcher gbMatcher = GB_TOPIC_PATTERN.matcher(topic == null ? "" : topic);
        if (gbMatcher.matches()) {
            return new PublishTarget(gbMatcher.group("deviceCode"), gbMatcher.group("sensorNo"));
        }
        return null;
    }

    /**
     * 判断设备协议是否允许走 MQTT 鉴权。
     *
     * @param protocolType 设备协议类型
     * @return {@code true} 表示允许接入
     */
    private boolean isProtocolAllowed(String protocolType) {
        String normalized = normalize(protocolType);
        return normalized == null || StringUtils.equals(PROTOCOL_MQTT, normalized);
    }

    /**
     * 校验设备账号格式。
     *
     * @param username 设备认证账号
     * @return {@code true} 表示格式合法
     */
    private boolean isUsernameValid(String username) {
        return username != null && USERNAME_PATTERN.matcher(username.toUpperCase(Locale.ROOT)).matches();
    }

    /**
     * 校验设备密码格式。
     *
     * @param password 设备认证密码
     * @return {@code true} 表示格式合法
     */
    private boolean isPasswordValid(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 归一化 clientId，优先使用 Broker 传入的 uniqueId。
     *
     * @param uniqueId Broker 内唯一 ID
     * @param clientId 原始客户端 ID
     * @return 归一化后的 clientId
     */
    private String normalizeClientId(String uniqueId, String clientId) {
        String normalizedUniqueId = normalize(uniqueId);
        return normalizedUniqueId != null ? normalizedUniqueId : normalize(clientId);
    }

    /**
     * 去除首尾空格，并把空串统一转换为 {@code null}。
     *
     * @param value 原始字符串
     * @return 归一化后的值
     */
    private String normalize(String value) {
        return StringUtils.trimToNull(value);
    }

    /**
     * 解析客户端真实 IP，优先取代理节点地址。
     *
     * @param context 当前连接上下文
     * @return 客户端 IP
     */
    private String resolveClientIp(ChannelContext context) {
        if (context == null) {
            return null;
        }
        Node proxyNode = context.getProxyClientNode();
        if (proxyNode != null && proxyNode.getIp() != null) {
            return proxyNode.getIp();
        }
        Node clientNode = context.getClientNode();
        return clientNode == null ? null : clientNode.getIp();
    }

    /**
     * 截断超长字段，避免审计日志写入超过数据库字段长度。
     *
     * @param value     原始值
     * @param maxLength 最大长度
     * @return 截断后的值
     */
    private String limitLength(String value, int maxLength) {
        return StringUtils.substring(value, 0, maxLength);
    }

    /**
     * 主题解析出的设备与测点标识。
     */
    private record PublishTarget(String deviceCode, String sensorNo) {
    }
}
