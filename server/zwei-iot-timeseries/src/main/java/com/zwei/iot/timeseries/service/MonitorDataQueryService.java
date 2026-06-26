package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.common.utils.DateUtils;
import com.zwei.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.domain.dto.BoundDeviceVO;
import com.zwei.iot.hazardpoint.mapper.DeviceHazardPointMapper;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import com.zwei.iot.timeseries.config.MonitorQueryProperties;
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
 * 图表查询支持自动降采样：当区间估算点数超过阈值时自动切 GROUP BY 桶，避免全量物化导致 OOM。
 */
@Service
public class MonitorDataQueryService {
    private static final Logger log = LoggerFactory.getLogger(MonitorDataQueryService.class);
    private final DeviceHazardPointMapper deviceHazardPointMapper;
    private final HazardPointMapper hazardPointMapper;
    private final IDeviceSensorService deviceSensorService;
    private final IotdbTimeSeriesService iotdbTimeSeriesService;
    private final MonitorQueryProperties queryProperties;

    /**
     * 构造监测数据查询服务。
     *
     * @param deviceHazardPointMapper 设备隐患点绑定 Mapper
     * @param hazardPointMapper       隐患点 Mapper
     * @param deviceSensorService     设备传感器服务
     * @param iotdbTimeSeriesService  IoTDB 时序服务
     * @param queryProperties         查询性能配置（降采样阈值等）
     */
    @Autowired
    public MonitorDataQueryService(DeviceHazardPointMapper deviceHazardPointMapper,
                                   HazardPointMapper hazardPointMapper,
                                   IDeviceSensorService deviceSensorService,
                                   IotdbTimeSeriesService iotdbTimeSeriesService,
                                   MonitorQueryProperties queryProperties) {
        this.deviceHazardPointMapper = deviceHazardPointMapper;
        this.hazardPointMapper = hazardPointMapper;
        this.deviceSensorService = deviceSensorService;
        this.iotdbTimeSeriesService = iotdbTimeSeriesService;
        this.queryProperties = queryProperties;
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
     * <p>支持两种分页模式：</p>
     * <ul>
     *   <li><b>游标模式</b>：传 {@code cursor}（上一页最后一行时间戳）时走 keyset 游标路径，
     *       每个测点取 pageSize 行合并，内存 O(measurements × pageSize)</li>
     *   <li><b>页码模式</b>：传 {@code pageNum} 时保留 offset 路径但加合并行数上限守护，
     *       超出 maxMergeRows 时抛异常提示使用游标分页</li>
     * </ul>
     *
     * @param hazardPointId 隐患点ID
     * @param deviceId      设备ID，可空
     * @param sensorId      传感器ID，可空
     * @param attrCode      属性编码，可空
     * @param valueType     值类型，可空（当前未在分页查询中使用）
     * @param startTime     开始时间，可空
     * @param endTime       结束时间，可空
     * @param pageNum       页码
     * @param pageSize      每页条数
     * @param cursor        游标时间戳（毫秒），上一页最后一行时间，可空
     * @return 分页结果对象，游标模式时额外包含 {@code cursor} 字段
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
                                    int pageSize,
                                    Long cursor) {
        String hazardPointName = resolveHazardPointName(hazardPointId);
        Long startMillis = toMillis(startTime);
        Long endMillis = toMillis(endTime);
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 10);
        List<ResolvedMeasurement> measurements = resolveMeasurements(hazardPointName, hazardPointId, deviceId, sensorId, attrCode);
        List<MonitorDataVO> rows;
        long total;
        Long nextCursor = null;
        if (measurements.size() == 1) {
            // 单测点：IoTDB 直接分页
            ResolvedMeasurement m = measurements.get(0);
            if (cursor != null) {
                rows = new ArrayList<>();
                for (IotdbQueryRow r : iotdbTimeSeriesService.queryRangeCursor(
                        m.deviceId(), m.sensorCode(), m.attrCode(), startMillis, endMillis, cursor, safePageSize)) {
                    if (r.value() != null) {
                        rows.add(buildRow(m, r));
                        nextCursor = r.time();
                    }
                }
            } else {
                int offset = (safePageNum - 1) * safePageSize;
                rows = new ArrayList<>();
                for (IotdbQueryRow r : iotdbTimeSeriesService.queryRangePaged(
                        m.deviceId(), m.sensorCode(), m.attrCode(), startMillis, endMillis, safePageSize, offset)) {
                    if (r.value() != null) {
                        rows.add(buildRow(m, r));
                    }
                }
            }
            total = iotdbTimeSeriesService.countRange(m.deviceId(), m.sensorCode(), m.attrCode(), startMillis, endMillis);
        } else if (cursor != null) {
            PageRows pr = pageMultiCursor(measurements, startMillis, endMillis, safePageSize, cursor);
            rows = pr.rows;
            nextCursor = pr.nextCursor;
            total = pr.total;
        } else {
            PageRows pr = pageMultiOffset(measurements, startMillis, endMillis, safePageNum, safePageSize);
            rows = pr.rows;
            total = pr.total;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("rows", rows);
        data.put("pageNum", safePageNum);
        data.put("pageSize", safePageSize);
        if (nextCursor != null) {
            data.put("cursor", nextCursor);
        }
        return data;
    }

    /**
     * 多测点 keyset 游标分页：每个测点取 pageSize 行，合并排序后取前 pageSize。
     *
     * @return 分页结果（rows + cursor + total），内存 O(measurements × pageSize)
     */
    private PageRows pageMultiCursor(List<ResolvedMeasurement> measurements,
                                      Long startMillis, Long endMillis,
                                      int safePageSize, Long cursor) {
        record CursorRow(ResolvedMeasurement measurement, IotdbQueryRow row) implements Comparable<CursorRow> {
            @Override
            public int compareTo(CursorRow o) {
                return Long.compare(o.row.time(), this.row.time()); // 降序
            }
        }
        List<CursorRow> rawRows = new ArrayList<>(measurements.size() * safePageSize);
        for (ResolvedMeasurement m : measurements) {
            for (IotdbQueryRow r : iotdbTimeSeriesService.queryRangeCursor(
                    m.deviceId(), m.sensorCode(), m.attrCode(), startMillis, endMillis, cursor, safePageSize)) {
                if (r.value() != null) {
                    rawRows.add(new CursorRow(m, r));
                }
            }
        }
        rawRows.sort(null); // 使用 CursorRow.compareTo 自然排序（降序）
        int toIndex = Math.min(safePageSize, rawRows.size());
        List<MonitorDataVO> rows = new ArrayList<>(toIndex);
        for (CursorRow cr : rawRows.subList(0, toIndex)) {
            rows.add(buildRow(cr.measurement(), cr.row()));
        }
        Long nextCursor = rows.isEmpty() ? null : rawRows.get(toIndex - 1).row().time();
        long total = 0;
        for (ResolvedMeasurement m : measurements) {
            total += iotdbTimeSeriesService.countRange(m.deviceId(), m.sensorCode(), m.attrCode(), startMillis, endMillis);
        }
        return new PageRows(rows, total, nextCursor);
    }

    /**
     * 多测点 offset 分页：保留旧行为但加合并行数上限守护。
     *
     * @throws ServiceException 当 pageNum × pageSize 超过 maxMergeRows 时抛出
     */
    private PageRows pageMultiOffset(List<ResolvedMeasurement> measurements,
                                      Long startMillis, Long endMillis,
                                      int safePageNum, int safePageSize) {
        int perMeasurementLimit = safePageNum * safePageSize;
        if (perMeasurementLimit > queryProperties.getMaxMergeRows()) {
            throw new ServiceException("查询结果过多，请缩小筛选范围或使用游标分页");
        }
        long total = 0;
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
        return new PageRows(allRows.subList(fromIndex, toIndex), total, null);
    }

    /** 分页查询的内部结果封装。 */
    private record PageRows(List<MonitorDataVO> rows, long total, Long nextCursor) {}

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
                                    String endTime,
                                    String granularity) {
        String hazardPointName = resolveHazardPointName(hazardPointId);
        List<ResolvedMeasurement> measurements = resolveMeasurements(hazardPointName, hazardPointId, deviceId, sensorId, attrCode);
        if (measurements.isEmpty()) {
            throw new ServiceException("未找到可查询的监测指标");
        }
        ValueType vt = ValueType.fromCode(valueType);
        Long startMillis = toMillis(startTime);
        Long endMillis = toMillis(endTime);
        long rangeMs = (startMillis != null && endMillis != null) ? (endMillis - startMillis) : 0L;
        boolean userAggregated = vt.isAggregated();
        boolean needDownsample = false;
        String downsampleInterval = null;
        // 用户主动指定降采样粒度
        boolean userDownsample = granularity != null && !"auto".equals(granularity);
        if (userDownsample) {
            if ("raw".equals(granularity)) {
                needDownsample = false;
            } else {
                needDownsample = true;
                downsampleInterval = granularity;
            }
        } else if (!userAggregated && rangeMs > 0) {
            long estimatedPoints = (long) (rangeMs / 1000.0 * queryProperties.getDownsampleEstimateHz());
            if (estimatedPoints > queryProperties.getMaxChartPoints()) {
                needDownsample = true;
                downsampleInterval = queryProperties.computeDownsampleInterval(rangeMs, queryProperties.getMaxChartPoints());
            }
        }
        List<ChartDataVO> series = new ArrayList<>();
        for (ResolvedMeasurement measurement : measurements) {
            List<IotdbQueryRow> rows;
            boolean sampled = false;
            String intervalUsed = null;
            if (userAggregated) {
                rows = iotdbTimeSeriesService.queryRange(
                        measurement.deviceId(),
                        measurement.sensorCode(),
                        measurement.attrCode(),
                        startMillis,
                        endMillis,
                        vt
                );
            } else if (needDownsample) {
                rows = iotdbTimeSeriesService.queryRangeDownsampled(
                        measurement.deviceId(),
                        measurement.sensorCode(),
                        measurement.attrCode(),
                        startMillis,
                        endMillis,
                        downsampleInterval
                );
                sampled = true;
                intervalUsed = downsampleInterval;
            } else if (rangeMs > 0) {
                long estimated = (long) (rangeMs / 1000.0 * queryProperties.getDownsampleEstimateHz());
                int wouldBeSlices = (int) Math.ceil((double) estimated / queryProperties.getMaxPointsPerSlice());
                if (estimated > queryProperties.getMaxAutoSlicePoints()
                        && wouldBeSlices <= queryProperties.getMaxSlices()) {
                    rows = queryRangeBySlices(
                            measurement.deviceId(),
                            measurement.sensorCode(),
                            measurement.attrCode(),
                            startMillis, endMillis,
                            estimated
                    );
                } else {
                    rows = iotdbTimeSeriesService.queryRangeWithLimit(
                            measurement.deviceId(),
                            measurement.sensorCode(),
                            measurement.attrCode(),
                            startMillis, endMillis,
                            queryProperties.getRawLimitCap()
                    );
                }
            } else {
                rows = iotdbTimeSeriesService.queryRangeWithLimit(
                        measurement.deviceId(),
                        measurement.sensorCode(),
                        measurement.attrCode(),
                        startMillis, endMillis,
                        queryProperties.getRawLimitCap()
                );
            }
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
                    values.isEmpty() ? null : sum / values.size(),
                    sampled,
                    intervalUsed,
                    values.size()
            ));
        }
        return series;
    }

    /**
     * 大数据量自动时间切片查询：将时间范围切分为多个子区间，每个子区间独立查询后合并。
     * <p>避免单次查询返回海量数据导致 OOM。切片粒度由 {@code maxPointsPerSlice} 控制。</p>
     *
     * @param deviceId   设备ID
     * @param sensorCode 传感器编码
     * @param attrCode   属性编码
     * @param startMs    开始时间毫秒
     * @param endMs      结束时间毫秒
     * @param estimated  估算总点数
     * @return 合并后的查询结果（自然时间升序，因为切片按时间递增顺序查询）
     */
    private List<IotdbQueryRow> queryRangeBySlices(
            long deviceId, String sensorCode, String attrCode,
            long startMs, long endMs, long estimated) {
        long rangeMs = endMs - startMs;
        int slices = Math.max(2, (int) Math.ceil((double) estimated / queryProperties.getMaxPointsPerSlice()));
        long sliceMs = rangeMs / slices;
        // 最小切片粒度不低于 10 秒，避免无限切片
        if (sliceMs < 10_000L) {
            sliceMs = 10_000L;
        }
        List<IotdbQueryRow> allRows = new ArrayList<>();
        for (long s = startMs; s < endMs; s += sliceMs) {
            long e = Math.min(s + sliceMs, endMs);
            List<IotdbQueryRow> sliceRows = iotdbTimeSeriesService.queryRangeWithLimit(
                    deviceId, sensorCode, attrCode, s, e, queryProperties.getRawLimitCap());
            if (sliceRows != null) {
                allRows.addAll(sliceRows);
            }
        }
        log.debug("时间切片查询: deviceId={}, range={}ms, estimated={}, slices={}, merged={} rows",
                deviceId, rangeMs, estimated, (int) Math.ceil((double) rangeMs / sliceMs), allRows.size());
        return allRows;
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
