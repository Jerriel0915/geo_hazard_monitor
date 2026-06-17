package com.zwei.iot.timeseries.compute;

import com.zwei.iot.monitor.domain.MonitorContent;

/**
 * 计算属性(来自 monitor_content WHERE field_type='computed')。
 *
 * <p>由 {@link ComputedAttributeRegistry} 从字典层加载并缓存, 供
 * {@link ComputedAttributeEvaluator} 拼装脚本执行。
 *
 * @param id         monitor_content.id
 * @param monitorTypeId 所属监测类型 ID
 * @param code       属性编码(必须合法 Java 标识符, 用作 Groovy 函数名 calc_{code})
 * @param name       属性名称(中文)
 * @param unit       单位
 * @param calcScript Groovy 脚本片段(非空)
 * @param sortOrder  排序号(决定求值顺序)
 */
public record ComputedAttribute(
        Long id,
        Long monitorTypeId,
        String code,
        String name,
        String unit,
        String calcScript,
        Integer sortOrder
) {
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
}
