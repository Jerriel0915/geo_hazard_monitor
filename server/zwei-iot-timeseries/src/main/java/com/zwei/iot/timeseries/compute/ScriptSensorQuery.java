package com.zwei.iot.timeseries.compute;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.timeseries.domain.SensorSnapshot;
import com.zwei.iot.timeseries.util.SensorDataQueryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Groovy 脚本可调用的传感器数据查询实例外壳 — 委托 {@link SensorDataQueryUtil}.
 *
 * <p>异常策略: <b>吞噬</b> — 任何 RuntimeException 返回 null。
 * 计算属性求值在主链路 (MonitorIngestFacade.ingest) 上, 不能因 sensor 查询失败让整条消息失败。
 * 与 {@link ScriptCacheOps} 的"透传"策略相反。
 *
 * <p>失败时 log.warn 记录 (e.toString(), 不打全栈避免日志洪水), 便于运维定位。
 *
 * <p>参数语义: 接受 {@code deviceCode} (字符串编码, 与 curData.deviceCode 同源),
 * 内部解析为 {@code deviceId} 供 IoTDB 路径使用。脚本作者无需感知数字主键。
 */
@Component
public class ScriptSensorQuery {

    private static final Logger log = LoggerFactory.getLogger(ScriptSensorQuery.class);

    private final DeviceMapper deviceMapper;

    @Autowired
    public ScriptSensorQuery(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    /**
     * 查询传感器在某时刻的数据快照。
     *
     * @param deviceCode  设备编码 (与 curData.deviceCode 同源)
     * @param sensorCode  传感器编码
     * @param time        查询时刻 (毫秒时间戳), 返回 {@code time <= 此值} 的最近一条
     * @param attrCode    属性编码; {@code null}/空串时查询全部业务属性
     * @return 数据快照; 设备未找到 / 无数据 / 异常时均返回 {@code null}
     */
    public SensorSnapshot query(String deviceCode, String sensorCode, long time, String attrCode) {
        try {
            Device dev = deviceMapper.selectDeviceByCode(deviceCode);
            if (dev == null || dev.getId() == null) {
                log.warn("sensor query: deviceCode not found, return null — devCode={} sensor={} attr={}",
                        deviceCode, sensorCode, attrCode);
                return null;
            }
            return SensorDataQueryUtil.query(dev.getId(), sensorCode, time, attrCode);
        } catch (RuntimeException e) {
            log.warn("sensor query failed, return null — devCode={} sensor={} attr={}: {}",
                    deviceCode, sensorCode, attrCode, e.toString());
            return null;
        }
    }
}
