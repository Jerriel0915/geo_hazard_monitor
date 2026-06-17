package com.zwei.iot.timeseries.compute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 计算脚本在线测试请求。
 *
 * <p>后端会用 monitorTypeId 加载该类型下其他计算属性的脚本, 把目标 attrCode
 * 的脚本临时替换为请求中的 calcScript, 拼装后执行, 返回结果。
 */
public class CalcScriptTestRequest {

    @NotNull(message = "监测类型ID不能为空")
    private Long monitorTypeId;

    @NotBlank(message = "属性编码不能为空")
    private String attrCode;

    @NotBlank(message = "计算脚本不能为空")
    @Size(max = 65535, message = "计算脚本长度不能超过 64KB")
    private String calcScript;

    /** 模拟 curData, 例如 { "properties": { "displacement": 12.5 } } */
    private Map<String, Object> curData;

    /** 模拟 prevData, 可空 */
    private Map<String, Object> prevData;

    public Long getMonitorTypeId() { return monitorTypeId; }
    public void setMonitorTypeId(Long monitorTypeId) { this.monitorTypeId = monitorTypeId; }
    public String getAttrCode() { return attrCode; }
    public void setAttrCode(String attrCode) { this.attrCode = attrCode; }
    public String getCalcScript() { return calcScript; }
    public void setCalcScript(String calcScript) { this.calcScript = calcScript; }
    public Map<String, Object> getCurData() { return curData; }
    public void setCurData(Map<String, Object> curData) { this.curData = curData; }
    public Map<String, Object> getPrevData() { return prevData; }
    public void setPrevData(Map<String, Object> prevData) { this.prevData = prevData; }
}
