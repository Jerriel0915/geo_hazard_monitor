package com.zwei.iot.timeseries.service;

import com.alibaba.fastjson2.JSON;
import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.domain.ParsedMessageSnapshot;
import com.zwei.common.domain.PropertyValue;
import com.zwei.common.event.MonitorDataIngestedEvent;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.service.DeviceOnlineStatusService;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.timeseries.compute.LastMessageStore;
import com.zwei.iot.timeseries.config.MonitorIngestProperties;
import com.zwei.iot.timeseries.domain.StandardMeasurementPoint;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 监测数据异步消费者 — Redis Stream → IoTDB 落库。
 *
 * <h3>职责</h3>
 * <ol>
 *   <li>应用启动后自动创建 Redis Stream 消费组，单线程轮询消费</li>
 *   <li>基于 device_id + sensor_code + attr_code + data_time + payload_hash 幂等去重</li>
 *   <li>写入 IoTDB 成功后回写运维指标（device_online_status.last_report_at、device_sensor.last_report_time）</li>
 *   <li>写入失败 → 三段退避重试（可配置延迟秒数列表）→ 超限进入死信队列</li>
 * </ol>
 *
 * <h3>数据流</h3>
 * <pre>
 * MQTT PUBLISH → MonitorIngestFacade.ingest()
 *   → MonitorPayloadParser.parse() → List&lt;StandardMeasurementPoint&gt;
 *   → MonitorIngestStreamService.enqueue() → Redis Stream
 *   → [本消费者] processRecord() → IoTDB / 重试 / 死信
 * </pre>
 *
 * <h3>线程模型</h3>
 * 消费线程为单线程 daemon（monitor-ingest-consumer），通过 {@code volatile running} 控制启停。
 * 应用关闭时 {@code @PreDestroy} 触发优雅停止：设置 running=false → shutdownNow → awaitTermination(5s)。
 */
@Slf4j
@Service
public class MonitorIngestConsumerService {
    private final RedisTemplate<Object, Object> redisTemplate;
    private final MonitorIngestProperties properties;
    private final IotdbTimeSeriesService iotdbTimeSeriesService;
    private final MonitorIngestStreamService streamService;
    private final DeviceMapper deviceMapper;
    private final DeviceOnlineStatusService deviceOnlineStatusService;
    private final IDeviceSensorService deviceSensorService;
    private final ApplicationEventPublisher eventPublisher;
    private final LastMessageStore lastMessageStore;
    private final ExecutorService executorService;
    private volatile boolean running = true;

    /**
     * 构造监测数据流消费者。
     *
     * @param redisTemplate          Redis 模板
     * @param properties             接入缓冲配置
     * @param iotdbTimeSeriesService IoTDB 时序服务
     * @param streamService          Stream 写入服务
     * @param deviceMapper           设备 Mapper
     * @param deviceOnlineStatusService 设备在线状态服务
     * @param eventPublisher         Spring 事件发布器（发布 {@link MonitorDataIngestedEvent}）
     */
    @Autowired
    public MonitorIngestConsumerService(RedisTemplate<Object, Object> redisTemplate,
                                        MonitorIngestProperties properties,
                                        IotdbTimeSeriesService iotdbTimeSeriesService,
                                        MonitorIngestStreamService streamService,
                                        DeviceMapper deviceMapper,
                                        DeviceOnlineStatusService deviceOnlineStatusService,
                                        IDeviceSensorService deviceSensorService,
                                        ApplicationEventPublisher eventPublisher,
                                        LastMessageStore lastMessageStore) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.iotdbTimeSeriesService = iotdbTimeSeriesService;
        this.streamService = streamService;
        this.deviceMapper = deviceMapper;
        this.deviceOnlineStatusService = deviceOnlineStatusService;
        this.deviceSensorService = deviceSensorService;
        this.eventPublisher = eventPublisher;
        this.lastMessageStore = lastMessageStore;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "monitor-ingest-consumer");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 应用启动完成后初始化消费组并启动消费线程。
     * <p>
     * 使用 {@link ApplicationReadyEvent} 替代 {@code @PostConstruct}，
     * 确保 Redis 连接池等基础设施已完全就绪后再执行初始化。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.isEnabled()) {
            log.warn("监测数据消费已禁用（iot.monitor-ingest.enabled=false），MQTT 数据将仅入栈 Redis Stream 不落库 IoTDB");
            return;
        }
        initConsumerGroup();
        executorService.submit(this::consume);
        log.info("监测数据消费已启动。stream={}, group={}, consumer={}",
                properties.getStreamKey(), properties.getConsumerGroup(), properties.getConsumerName());
    }

    /**
     * 停止消费线程并等待资源释放。
     *
     * @throws InterruptedException 当等待线程池停止时被中断
     */
    @PreDestroy
    public void stop() throws InterruptedException {
        running = false;
        executorService.shutdownNow();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * 持续轮询 Redis Stream 并消费消息。
     */
    private void consume() {
        while (running) {
            try {
                @SuppressWarnings("unchecked")
                List<MapRecord<Object, Object, Object>> records = (List<MapRecord<Object, Object, Object>>) (List<?>) redisTemplate.opsForStream().read(
                        Consumer.from(properties.getConsumerGroup(), properties.getConsumerName()),
                        StreamReadOptions.empty()
                                .count(properties.getPollBatchSize())
                                .block(Duration.ofMillis(properties.getPollBlockMs())),
                        StreamOffset.create(properties.getStreamKey(), ReadOffset.lastConsumed())
                );
                if (records == null || records.isEmpty()) {
                    continue;
                }
                for (MapRecord<Object, Object, Object> record : records) {
                    processRecord(record);
                }
            } catch (Exception e) {
                if (!running) {
                    log.debug("消费线程已停止");
                    break;
                }
                log.error("消费监测数据流失败", e);
            }
        }
    }

    /**
     * 处理单条 Stream 消息。
     *
     * <p>核心消费逻辑——阶段顺序：</p>
     * <ol>
     *   <li><b>反序列化</b>：从 Stream record 还原 StandardMeasurementPoint</li>
     *   <li><b>幂等去重</b>：基于 Redis SETNX，重复消息直接 ACK 跳过</li>
     *   <li><b>IoTDB 写入</b>：惰性创建时序 schema + 写入测点</li>
     *   <li><b>运维指标回写</b>：更新 device_online_status.last_report_at + device_sensor.last_report_time</li>
     *   <li><b>失败重试</b>：三段退避 → 超限进入死信队列</li>
     * </ol>
     *
     * @param record Stream 记录
     */
    private void processRecord(MapRecord<Object, Object, Object> record) {
        String payload = String.valueOf(record.getValue().get("payload"));
        int retryCount = Integer.parseInt(String.valueOf(record.getValue().getOrDefault("retryCount", "0")));
        String payloadType = String.valueOf(record.getValue().getOrDefault("payloadType", "STANDARD_POINT"));

        if ("PARSED_MESSAGE".equals(payloadType)) {
            processParsedMessage(record, payload, retryCount);
            return;
        }

        StandardMeasurementPoint point = JSON.parseObject(payload, StandardMeasurementPoint.class);
        try {
            // ── 阶段1: 幂等去重 ──
            // 去重键 = device_id:sensor_code:attr_code:data_time:payload_hash
            // 利用 Redis SETNX 原子操作保证 TTL 窗口内同一条数据只落库一次
            if (isDuplicate(point)) {
                ack(record);
                return;
            }
            // ── 阶段2: IoTDB 时序写入 ──
            // writePoints 内部惰性建表：首次写入自动创建 aligned timeseries + 质量码列
            iotdbTimeSeriesService.writePoints(List.of(point));
            // 累计监测次数 (+1)
            redisTemplate.opsForValue().increment("stats:total:monitor:count");
            // ── 阶段3: 运维指标回写 ──
            // 三个维度：device_online_status（实时在线状态）、device_sensor（传感器活跃率）、device（兼容保留）
            deviceOnlineStatusService.updateLastReportAt(point.deviceId());
            if (point.sensorId() != null) {
                String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date(point.dataTime()));
                deviceSensorService.updateLastReportTime(point.sensorId(), now);
            }
            // 同步回写设备主表 lastReportTime（保留兼容，后续可逐步移除）
            deviceMapper.updateDevice(Device.builder()
                    .id(point.deviceId())
                    .lastReportTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(new java.util.Date(point.dataTime())))
                    .build());
            log.info("时序数据落库成功 deviceId={} sensorCode={} attrCode={} attrName={} value={} {} dataTime={}",
                    point.deviceId(), point.sensorCode(), point.attrCode(), point.attrName(),
                    point.value(), point.unit() != null ? point.unit() : "",
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(point.dataTime())));
            // 包装为单属性 ParsedMessage 后发布事件（统一契约，与 PARSED_MESSAGE 路径走同一发布方法）
            // STANDARD_POINT 是非 sys 报文格式，不参与告警评估，prevSnapshot 传 null
            publishIngestedEvent(point.deviceId(), point.sensorId(),
                    wrapPointAsParsedMessage(point), null);
            ack(record);
        } catch (Exception e) {
            // ── 阶段4: 退避重试 / 死信 ──
            // 三段退避（默认 3s/9s/27s），超限进入死信队列供人工排查
            if (retryCount >= properties.getRetryDelaysSeconds().size()) {
                streamService.enqueueDeadLetter(point, e.getMessage());
                ack(record);
                return;
            }
            long delaySeconds = properties.getRetryDelaysSeconds().get(retryCount);
            sleep(delaySeconds);
            // 重试次数 +1 后重新入队，等待下一轮消费
            record.getValue().put("retryCount", String.valueOf(retryCount + 1));
            redisTemplate.opsForStream().add(MapRecord.create(properties.getStreamKey(), record.getValue()));
            ack(record);
        }
    }

    /**
     * Handle ParsedMessage-type records -- adapt to StandardMeasurementPoint then write to IoTDB.
     */
    private void processParsedMessage(MapRecord<Object, Object, Object> record, String payload, int retryCount) {
        ParsedMessage parsed =
                JSON.parseObject(payload, ParsedMessage.class);
        try {
            List<StandardMeasurementPoint> points = adapt(parsed);
            if (points.isEmpty()) {
                ack(record);
                return;
            }
            // Idempotent dedup -- use first point's payloadHash
            if (isDuplicate(points.get(0))) {
                ack(record);
                return;
            }
            iotdbTimeSeriesService.writePoints(points);
            // 累计监测次数 (+N)
            redisTemplate.opsForValue().increment("stats:total:monitor:count", points.size());
            // Operational metrics callback
            for (StandardMeasurementPoint pt : points) {
                deviceOnlineStatusService.updateLastReportAt(pt.deviceId());
                if (pt.sensorId() != null) {
                    String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(new java.util.Date(pt.dataTime()));
                    deviceSensorService.updateLastReportTime(pt.sensorId(), now);
                }
                deviceMapper.updateDevice(Device.builder()
                        .id(pt.deviceId())
                        .lastReportTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(new java.util.Date(pt.dataTime())))
                        .build());
            }
            log.info("ParsedMessage ingested: deviceCode={} sensorCode={} properties={}",
                    parsed.deviceCode(), parsed.sensorCode(), points.size());
            // ── prev snapshot: get 必须在 put 之前, 此时 store 里是上一条 ──
            Long deviceId = points.get(0).deviceId();
            Long sensorId = points.get(0).sensorId();
            ParsedMessageSnapshot prevSnapshot = lastMessageStore.get(deviceId, parsed.sensorCode());
            // ── 推进 store: 当前条覆盖上一条 ──
            Map<String, Object> currentProps = new LinkedHashMap<>();
            for (PropertyValue pv : parsed.properties()) {
                if (pv.value() != null) currentProps.put(pv.identifier(), pv.value());
            }
            lastMessageStore.put(deviceId, parsed.sensorCode(),
                    new ParsedMessageSnapshot(parsed.deviceCode(), parsed.sensorCode(),
                            parsed.dataTime(), currentProps));
            // ── 发布携带 prevSnapshot 的事件 ──
            publishIngestedEvent(deviceId, sensorId, parsed, prevSnapshot);
            ack(record);
        } catch (Exception e) {
            if (retryCount >= properties.getRetryDelaysSeconds().size()) {
                streamService.enqueueDeadLetter(parsed.deviceCode(), payload, e.getMessage());
                ack(record);
                return;
            }
            long delaySeconds = properties.getRetryDelaysSeconds().get(retryCount);
            sleep(delaySeconds);
            record.getValue().put("retryCount", String.valueOf(retryCount + 1));
            redisTemplate.opsForStream().add(MapRecord.create(properties.getStreamKey(), record.getValue()));
            ack(record);
        }
    }

    /**
     * 发布监测数据入库事件，通知告警引擎评估。
     *
     * <p>携带完整的 ParsedMessage 数据包（全部属性），一次报文对应一次事件。
     * 同步发布 — 由 Spring 事件机制按订阅者线程模型决定执行线程
     * (alarm 引擎 {@code @EventListener} 默认同步，会在本消费线程内执行；
     * 如需异步可加 {@code @Async}，本工程当前同步满足告警时延要求)。
     *
     * @param deviceId     已解析的设备 ID（来自 adapt 阶段，避免下游重复查 DB）
     * @param sensorId     已解析的传感器 ID
     * @param msg          已成功写入 IoTDB 的完整报文
     * @param prevSnapshot 同设备+传感器上一条报文的精简快照; null 表示首次上报或缓存失效
     */
    private void publishIngestedEvent(Long deviceId, Long sensorId, ParsedMessage msg,
                                      ParsedMessageSnapshot prevSnapshot) {
        try {
            eventPublisher.publishEvent(new MonitorDataIngestedEvent(
                    deviceId, sensorId, msg.deviceCode(), msg.sensorCode(), msg.sourceType(),
                    msg.receiveTime(), msg.payloadHash(), msg.properties(),
                    msg.dataTime(), prevSnapshot));
            log.info("发布 MonitorDataIngestedEvent: deviceCode={} sensorCode={} properties={} hasPrev={}",
                    msg.deviceCode(), msg.sensorCode(), msg.properties().size(),
                    prevSnapshot != null);
        } catch (Exception e) {
            // 事件发布失败不影响入库主流程
            log.warn("发布 MonitorDataIngestedEvent 失败 deviceCode={} sensorCode={}: {}",
                    msg.deviceCode(), msg.sensorCode(), e.getMessage());
        }
    }

    /**
     * 将单点 StandardMeasurementPoint 包装为只含 1 个属性的 ParsedMessage。
     *
     * <p>用于 STANDARD_POINT 路径（已无生产调用方）与 PARSED_MESSAGE 路径走同一发布方法。
     * deviceCode 通过 deviceMapper 反查（失败时置 null，事件容错）。
     */
    private ParsedMessage wrapPointAsParsedMessage(StandardMeasurementPoint point) {
        String deviceCode = null;
        try {
            Device dev = deviceMapper.selectDeviceById(point.deviceId());
            if (dev != null) {
                deviceCode = dev.getCode();
            }
        } catch (Exception ignored) {
            // deviceCode 仅用于日志/审计，反查失败不影响主流程
        }
        return new ParsedMessage(
                deviceCode,
                point.sensorCode(),
                point.sourceType(),
                point.dataTime(),
                point.receiveTime(),
                point.payloadHash(),
                List.of(new PropertyValue(
                        point.attrCode(),
                        point.attrName(),
                        point.unit(),
                        point.value(),
                        point.quality())));
    }

    /**
     * Adapt ParsedMessage to List of StandardMeasurementPoint.
     *
     * deviceCode → deviceId lookup is done in the consumer, not the parser.
     */
    private List<StandardMeasurementPoint> adapt(ParsedMessage msg) {
        Long deviceId = resolveDeviceId(msg.deviceCode());
        Long sensorId = resolveSensorId(deviceId, msg.sensorCode());
        return msg.properties().stream()
                .filter(p -> p.value() != null)
                .map(p -> StandardMeasurementPoint.builder()
                        .deviceId(deviceId)
                        .sensorCode(msg.sensorCode())
                        .sensorId(sensorId)
                        .attrCode(p.identifier())
                        .attrName(p.name())
                        .unit(p.unit())
                        .dataTime(msg.dataTime())
                        .value(p.value())
                        .quality(p.quality() != null ? p.quality() : 0)
                        .reportTime(msg.dataTime())
                        .receiveTime(msg.receiveTime())
                        .sourceType(msg.sourceType())
                        .payloadHash(msg.payloadHash())
                        .build())
                .toList();
    }

    private Long resolveDeviceId(String deviceCode) {
        Device dev = deviceMapper.selectDeviceByCode(deviceCode);
        return dev != null ? dev.getId() : -1L;
    }

    private Long resolveSensorId(Long deviceId, String sensorCode) {
        DeviceSensor sensor = deviceSensorService.selectSensorByDeviceIdAndCode(deviceId, sensorCode);
        return sensor != null ? sensor.getId() : -1L;
    }

    /**
     * 判断当前时序点是否为重复消息。
     *
     * @param point 标准化时序点
     * @return 已存在时返回 {@code true}
     */
    private boolean isDuplicate(StandardMeasurementPoint point) {
        String key = properties.getDedupeKeyPrefix()
                + point.deviceId() + ":" + point.sensorCode() + ":" + point.attrCode() + ":" + point.dataTime() + ":" + point.payloadHash();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(properties.getDedupeTtlSeconds()));
        return Boolean.FALSE.equals(success);
    }

    /**
     * 确认消费并删除已处理记录。
     *
     * @param record Stream 记录
     */
    private void ack(MapRecord<Object, Object, Object> record) {
        RecordId recordId = record.getId();
        redisTemplate.opsForStream().acknowledge(properties.getStreamKey(), properties.getConsumerGroup(), recordId);
        redisTemplate.opsForStream().delete(properties.getStreamKey(), recordId);
    }

    /**
     * 初始化 Redis Stream 消费组，含重试机制。
     * <p>
     * ApplicationReadyEvent 触发后 Redis 连接池可能仍在预热，
     * 最多重试 3 次，间隔递增（2s / 4s）。
     * 消费组已存在（BUSYGROUP）直接视为成功，不进入重试。
     */
    private void initConsumerGroup() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                redisTemplate.opsForStream().createGroup(properties.getStreamKey(),
                        ReadOffset.latest(), properties.getConsumerGroup());
                log.info("消费组初始化成功。stream={}, group={}",
                        properties.getStreamKey(), properties.getConsumerGroup());
                return;
            } catch (Exception e) {
                // BUSYGROUP：消费组已存在，属正常运行态，无需重试。
                if (isConsumerGroupExists(e)) {
                    log.debug("消费组已存在，跳过创建。stream={}, group={}",
                            properties.getStreamKey(), properties.getConsumerGroup());
                    return;
                }
                if (attempt < 3) {
                    long delay = (long) Math.pow(2, attempt);
                    log.debug("创建消费组失败（第 {} 次），{}s 后重试。stream={}, group={}",
                            attempt, delay, properties.getStreamKey(), properties.getConsumerGroup(), e);
                    sleep(delay);
                } else {
                    log.warn("消费组初始化最终失败。stream={}, group={}",
                            properties.getStreamKey(), properties.getConsumerGroup(), e);
                }
            }
        }
    }

    /**
     * 判断异常是否为消费组已存在的预期错误（Redis BUSYGROUP）。
     */
    private boolean isConsumerGroupExists(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains("BUSYGROUP")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * 按配置延迟当前线程，用于简单退避重试。
     *
     * @param delaySeconds 延迟秒数
     */
    private void sleep(long delaySeconds) {
        try {
            TimeUnit.SECONDS.sleep(delaySeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
