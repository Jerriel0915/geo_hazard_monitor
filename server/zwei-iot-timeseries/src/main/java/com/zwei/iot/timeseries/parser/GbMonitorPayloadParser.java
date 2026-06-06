package com.zwei.iot.timeseries.parser;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.timeseries.domain.StandardMeasurementPoint;
import com.zwei.iot.timeseries.support.MonitorTopic;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 国标字节流报文解析占位。
 * <p>
 * 待获取正式字节流样例后再补齐解析实现。
 */
@Component
public class GbMonitorPayloadParser implements MonitorPayloadParser {
    /**
     * 判断是否由当前解析器处理国标主题。
     *
     * @param topic 监测主题信息
     * @return 当主题来源为 {@code gb} 时返回 {@code true}
     */
    @Override
    public boolean supports(MonitorTopic topic) {
        return topic != null && Objects.equals("gb", topic.sourceType());
    }

    /**
     * 预留国标字节流报文解析实现。
     *
     * @param topic    监测主题信息
     * @param message  原始报文字节数组
     * @param metadata 传感器元数据
     * @return 当前版本不返回有效结果
     * @throws ServiceException 当前阶段固定抛出未实现异常
     */
    @Override
    public List<StandardMeasurementPoint> parse(MonitorTopic topic, byte[] message, SensorMetadata metadata) {
        // TODO 按国标字节流报文规范补充正式解析逻辑，当前版本先预留空实现。
        throw new ServiceException("国标字节流报文解析暂未实现");
    }
}
