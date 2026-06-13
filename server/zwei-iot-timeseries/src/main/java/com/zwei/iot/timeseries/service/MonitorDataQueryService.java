package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.common.utils.DateUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.domain.dto.BoundDeviceVO;
import com.zwei.iot.hazardpoint.mapper.DeviceHazardPointMapper;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import com.zwei.iot.timeseries.domain.ChartDataVO;
import com.zwei.iot.timeseries.domain.IotdbQueryRow;
import com.zwei.iot.timeseries.domain.MonitorDataVO;
import com.zwei.iot.timeseries.domain.ValueType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 监测数据查询服务。
 * <p>
 * IoTDB 查询聚合服务，将隐患点、设备、传感器元数据与时序查询结果组装为接口返回结构。
 */
@Service
public class MonitorDataQueryService {
    private final DeviceHazardPointMapper deviceHazardPointMapper;
    private final HazardPointMapper hazardPointMapper;
    private final IDeviceSensorService deviceSensorService;
    private final IotdbTimeSeriesService iotdbTimeSeriesService;

    /**
     * 构造监测数据查询服务。
     *
     * @param deviceHazardPointMapper 设备隐患点绑定 Mapper
     * @param hazardPointMapper       隐患点 Mapper
     * @param deviceSensorService     设备传感器服务
     * @param iotdbTimeSeriesService  IoTDB 时序服务
     */
    @Autowired
    public MonitorDataQueryService(DeviceHazardPointMapper deviceHazardPointMapper,
                                   HazardPointMapper hazardPointMapper,
                                   IDeviceSensorService deviceSensorService,
                                   IotdbTimeSeriesService iotdbTimeSeriesService) {
        this.deviceHazardPointMapper = deviceHazardPointMapper;
        this.hazardPointMapper = hazardPointMapper;
        this.deviceSensorService = deviceSensorService;
        this.iotdbTimeSeriesService = iotdbTimeSeriesService;
    }

    /**
     * 查询隐患点下各指标的最新监测值。
     *
     * @param hazardPointId 隐患点ID
     * @return 最新监测数据集合
     * @throws ServiceException 当隐患点ID为空或元数据解析失败时抛出
     */
    public List<MonitorDataVO> latest(Long hazardPointId) {
        String hazardPointName = resolveHazardPointName(hazardPointId);
        List<MonitorDataVO> rows = new ArrayList<>();
        for (ResolvedMeasurement measurement : resolveMeasurements(hazardPointName, hazardPointId, null, null, null)) {
            IotdbQueryRow latest = iotdbTimeSeriesService.queryLatest(
                    measurement.deviceId(),
                    measurement.sensorCode(),
                    measurement.attrCode()
            );
            if (latest == null || latest.value() == null) {
                continue;
            }
            rows.add(buildRow(measurement, latest));
        }
        return rows;
    }

    /**
     * 分页查询隐患点下的历史监测数据。
     *
     * @param hazardPointId 隐患点ID
     * @param deviceId      设备ID，可空
     * @param sensorId      传感器ID，可空
     * @param attrCode      属性编码，可空
     * @param startTime     开始时间，可空
     * @param endTime       结束时间，可空
     * @param pageNum       页码
     * @param pageSize      每页条数
     * @return 分页结果对象
     * @throws ServiceException 当隐患点ID为空或查询失败时抛出
     */
    public Map<String, Object> page(Long hazardPointId,
                                    Long deviceId,
                                    Long sensorId,
                                    String attrCode,
                                    String valueType,
                                    String startTime,
                                    String endTime,
                                    int pageNum,
                                    int pageSize) {
        String hazardPointName = resolveHazardPointName(hazardPointId);
        ValueType vt = ValueType.fromCode(valueType);
        Long startMillis = toMillis(startTime);
        Long endMillis = toMillis(endTime);
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 10);
        List<ResolvedMeasurement> measurements = resolveMeasurements(hazardPointName, hazardPointId, deviceId, sensorId, attrCode);
        List<MonitorDataVO> rows;
        long total;
        if (measurements.size() == 1) {
            // 单测点：IoTDB 直接分页
            ResolvedMeasurement m = measurements.get(0);
            int offset = (safePageNum - 1) * safePageSize;
            rows = new ArrayList<>();
            for (IotdbQueryRow r : iotdbTimeSeriesService.queryRangePaged(
                    m.deviceId(), m.sensorCode(), m.attrCode(), startMillis, endMillis, safePageSize, offset)) {
                if (r.value() != null) {
                    rows.add(buildRow(m, r));
                }
            }
            total = iotdbTimeSeriesService.countRange(m.deviceId(), m.sensorCode(), m.attrCode(), startMillis, endMillis);
        } else {
            // 多测点：每个测点查询 limit = pageNum * pageSize，合并排序后截取
            int perMeasurementLimit = safePageNum * safePageSize;
            total = 0;
            List<MonitorDataVO> allRows = new ArrayList<>();
            for (ResolvedMeasurement m : measurements) {
                total += iotdbTimeSeriesService.countRange(m.deviceId(), m.sensorCode(), m.attrCode(), startMillis, endMillis);
                for (IotdbQueryRow r : iotdbTimeSeriesService.queryRangePaged(
                        m.deviceId(), m.sensorCode(), m.attrCode(), startMillis, endMillis, perMeasurementLimit, 0)) {
                    if (r.value() != null) {
                        allRows.add(buildRow(m, r));
                    }
                }
            }
            allRows.sort(Comparator.comparing(MonitorDataVO::dataTime, Comparator.reverseOrder()));
            int fromIndex = Math.min((safePageNum - 1) * safePageSize, allRows.size());
            int toIndex = Math.min(fromIndex + safePageSize, allRows.size());
            rows = allRows.subList(fromIndex, toIndex);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("rows", rows);
        data.put("pageNum", safePageNum);
        data.put("pageSize", safePageSize);
        return data;
    }

    /**
     * 查询单个监测指标的图表数据与统计值。
     *
     * @param hazardPointId 隐患点ID
     * @param deviceId      设备ID，可空
     * @param sensorId      传感器ID，可空
     * @param attrCode      属性编码，可空
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @return 图表数据对象
     * @throws ServiceException 当未找到可查询指标时抛出
     */
    public List<ChartDataVO> chart(Long hazardPointId,
                                    Long deviceId,
                                    Long sensorId,
                                    String attrCode,
                                    String valueType,
                                    String startTime,
                                    String endTime) {
        String hazardPointName = resolveHazardPointName(hazardPointId);
        List<ResolvedMeasurement> measurements = resolveMeasurements(hazardPointName, hazardPointId, deviceId, sensorId, attrCode);
        if (measurements.isEmpty()) {
            throw new ServiceException("未找到可查询的监测指标");
        }
        ValueType vt = ValueType.fromCode(valueType);
        Long startMillis = toMillis(startTime);
        Long endMillis = toMillis(endTime);
        List<ChartDataVO> series = new ArrayList<>();
        for (ResolvedMeasurement measurement : measurements) {
            List<IotdbQueryRow> rows = iotdbTimeSeriesService.queryRange(
                    measurement.deviceId(),
                    measurement.sensorCode(),
                    measurement.attrCode(),
                    startMillis,
                    endMillis,
                    vt
            );
            List<String> labels = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            double max = Double.NEGATIVE_INFINITY;
            double min = Double.POSITIVE_INFINITY;
            double sum = 0D;
            for (IotdbQueryRow row : rows) {
                if (row.value() == null) {
                    continue;
                }
                labels.add(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, new Date(row.time())));
                values.add(row.value());
                max = Math.max(max, row.value());
                min = Math.min(min, row.value());
                sum += row.value();
            }
            String seriesName = measurement.attrName()
                    + (measurement.sensorName() != null ? " (" + measurement.sensorName() + ")" : "");
            series.add(new ChartDataVO(
                    seriesName,
                    measurement.deviceName(),
                    measurement.sensorName(),
                    labels,
                    values,
                    measurement.unit(),
                    measurement.attrName(),
                    values.isEmpty() ? null : max,
                    values.isEmpty() ? null : min,
                    values.isEmpty() ? null : sum / values.size()
            ));
        }
        return series;
    }

    /**
     * 根据隐患点和筛选条件解析可查询的测点指标。
     *
     * @param hazardPointId 隐患点ID
     * @param deviceId      设备ID，可空
     * @param sensorId      传感器ID，可空
     * @param attrCode      属性编码，可空
     * @return 可查询指标集合
     * @throws ServiceException 当隐患点ID为空时抛出
     */
    private List<ResolvedMeasurement> resolveMeasurements(String hazardPointName, Long hazardPointId, Long deviceId, Long sensorId, String attrCode) {
        if (hazardPointId == null) {
            throw new ServiceException("隐患点ID不能为空");
        }
        List<BoundDeviceVO> boundDevices = deviceHazardPointMapper.selectBoundDevicesByHazardPointId(hazardPointId);
        List<ResolvedMeasurement> measurements = new ArrayList<>();
        for (BoundDeviceVO boundDevice : boundDevices) {
            if (deviceId != null && !deviceId.equals(boundDevice.getDeviceId())) {
                continue;
            }
            List<DeviceSensor> sensors = deviceSensorService.selectSensorListByDeviceId(boundDevice.getDeviceId());
            for (DeviceSensor sensor : sensors) {
                if (sensorId != null && !sensorId.equals(sensor.getId())) {
                    continue;
                }
                if (sensor.getAttrList() == null) {
                    continue;
                }
                for (SensorAttribute attribute : sensor.getAttrList()) {
                    if (StringUtils.isNotBlank(attrCode) && !attrCode.equals(attribute.getAttrCode())) {
                        continue;
                    }
                    measurements.add(new ResolvedMeasurement(
                            hazardPointName,
                            hazardPointId,
                            boundDevice.getDeviceId(),
                            boundDevice.getDeviceName(),
                            sensor.getId(),
                            sensor.getSensorCode(),
                            sensor.getSensorName(),
                            attribute.getAttrCode(),
                            attribute.getAttrName(),
                            attribute.getUnit()
                    ));
                }
            }
        }
        return measurements;
    }

    /**
     * 将时序查询结果组装为接口输出行。
     *
     * @param measurement 已解析的指标元数据
     * @param row         IoTDB 查询结果
     * @return 接口输出行
     */
    private MonitorDataVO buildRow(ResolvedMeasurement measurement, IotdbQueryRow row) {
        return new MonitorDataVO(
                measurement.hazardPointId(),
                measurement.hazardPointName(),
                measurement.deviceId(),
                measurement.deviceName(),
                measurement.sensorId(),
                measurement.sensorName(),
                measurement.attrCode(),
                measurement.attrName(),
                row.value(),
                measurement.unit(),
                DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, new Date(row.time())),
                row.quality(),
                row.quality() == null || row.quality() == 0 ? "正常" : "异常"
        );
    }

    /**
     * 解析隐患点名称。
     *
     * @param hazardPointId 隐患点ID
     * @return 隐患点名称；不存在时返回空字符串
     */
    private String resolveHazardPointName(Long hazardPointId) {
        HazardPoint hazardPoint = hazardPointMapper.selectHazardPointById(hazardPointId);
        return hazardPoint != null ? hazardPoint.getName() : "";
    }

    /**
     * 将时间字符串转换为毫秒时间戳。
     *
     * @param text 时间字符串
     * @return 毫秒时间戳；为空时返回 {@code null}
     */
    private Long toMillis(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return DateUtils.parseDate(text).getTime();
    }

    private record ResolvedMeasurement(String hazardPointName,
                                       Long hazardPointId,
                                       Long deviceId,
                                       String deviceName,
                                       Long sensorId,
                                       String sensorCode,
                                       String sensorName,
                                       String attrCode,
                                       String attrName,
                                       String unit) {
    }
}
