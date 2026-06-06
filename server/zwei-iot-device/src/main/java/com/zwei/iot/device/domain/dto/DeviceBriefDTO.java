package com.zwei.iot.device.domain.dto;

/**
 * 设备简要信息 DTO。
 * <p>
 * 仅包含跨模块（如 monitor）需要的非敏感字段，
 * 不暴露 authPassword 等内部字段。
 */
public class DeviceBriefDTO {

    private Long id;
    private String name;
    private String code;
    private Integer runStatus;
    private String lastAuthIp;
    private String lastAuthTime;
    private String hazardPointName;

    public DeviceBriefDTO() {
    }

    public DeviceBriefDTO(Long id, String name, String code, Integer runStatus,
                          String lastAuthIp, String lastAuthTime, String hazardPointName) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.runStatus = runStatus;
        this.lastAuthIp = lastAuthIp;
        this.lastAuthTime = lastAuthTime;
        this.hazardPointName = hazardPointName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(Integer runStatus) {
        this.runStatus = runStatus;
    }

    public String getLastAuthIp() {
        return lastAuthIp;
    }

    public void setLastAuthIp(String lastAuthIp) {
        this.lastAuthIp = lastAuthIp;
    }

    public String getLastAuthTime() {
        return lastAuthTime;
    }

    public void setLastAuthTime(String lastAuthTime) {
        this.lastAuthTime = lastAuthTime;
    }

    public String getHazardPointName() {
        return hazardPointName;
    }

    public void setHazardPointName(String hazardPointName) {
        this.hazardPointName = hazardPointName;
    }
}
