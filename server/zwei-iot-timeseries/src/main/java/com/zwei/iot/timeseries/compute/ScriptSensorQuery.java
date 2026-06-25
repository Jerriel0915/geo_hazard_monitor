package com.zwei.iot.timeseries.compute;

import com.zwei.iot.timeseries.domain.SensorSnapshot;
import com.zwei.iot.timeseries.util.SensorDataQueryUtil;
import org.springframework.stereotype.Component;

/**
 * Groovy 脚本可调用的传感器数据查询实例外壳 — 委托 {@link SensorDataQueryUtil}。
 *
 * <p>异常策略: <b>吞噬</b> — 任何 RuntimeException 返回 null。
 * 计算属性求值在主链路 (MonitorIngestFacade.ingest) 上, 不能因 sensor 查询失败让整条消息失败。
 * 与 {@link ScriptCacheOps} 的"透传"策略相反。
 */
@Component
public class ScriptSensorQuery {

    public SensorSnapshot query(long deviceId, String sensorCode, long time, String attrCode) {
        try {
            return SensorDataQueryUtil.query(deviceId, sensorCode, time, attrCode);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
