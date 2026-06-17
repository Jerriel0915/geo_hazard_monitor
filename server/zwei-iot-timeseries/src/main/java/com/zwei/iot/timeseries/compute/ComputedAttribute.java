package com.zwei.iot.timeseries.compute;

import com.zwei.iot.monitor.domain.MonitorContent;

import java.util.Objects;

/**
 * 计算属性(来自 monitor_content WHERE field_type='computed')。
 *
 * <p>由 {@link ComputedAttributeRegistry} 从字典层加载并缓存, 供
 * {@link ComputedAttributeEvaluator} 拼装脚本执行。
 *
 * <p>注意: 使用常规 POJO 而非 Java record, 以兼容 FastJSON2 Redis 缓存序列化。
 */
public class ComputedAttribute {

    private final Long id;
    private final Long monitorTypeId;
    private final String code;
    private final String name;
    private final String unit;
    private final String calcScript;
    private final Integer sortOrder;

    public ComputedAttribute(Long id, Long monitorTypeId, String code,
                             String name, String unit, String calcScript,
                             Integer sortOrder) {
        this.id = id;
        this.monitorTypeId = monitorTypeId;
        this.code = code;
        this.name = name;
        this.unit = unit;
        this.calcScript = calcScript;
        this.sortOrder = sortOrder;
    }

    public static ComputedAttribute from(MonitorContent mc) {
        if (mc.getCode() == null || !mc.getCode().matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException(
                    "非法 attrCode (必须以字母开头, 只含字母数字下划线): " + mc.getCode());
        }
        return new ComputedAttribute(
                mc.getId(),
                mc.getMonitorTypeId(),
                mc.getCode(),
                mc.getName(),
                mc.getUnit(),
                mc.getCalcScript(),
                mc.getSortOrder() == null ? 0 : mc.getSortOrder()
        );
    }

    public Long id() { return id; }
    public Long monitorTypeId() { return monitorTypeId; }
    public String code() { return code; }
    public String name() { return name; }
    public String unit() { return unit; }
    public String calcScript() { return calcScript; }
    public Integer sortOrder() { return sortOrder; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComputedAttribute that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(monitorTypeId, that.monitorTypeId)
                && Objects.equals(code, that.code)
                && Objects.equals(name, that.name)
                && Objects.equals(unit, that.unit)
                && Objects.equals(calcScript, that.calcScript)
                && Objects.equals(sortOrder, that.sortOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, monitorTypeId, code, name, unit, calcScript, sortOrder);
    }

    @Override
    public String toString() {
        return "ComputedAttribute{" +
                "id=" + id +
                ", monitorTypeId=" + monitorTypeId +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
