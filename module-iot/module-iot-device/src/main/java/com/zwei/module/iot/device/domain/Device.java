package com.zwei.module.iot.device.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zwei.common.annotation.Excel;
import com.zwei.common.core.domain.BaseEntity;

/**
 * 设备基本信息对象 zw_device
 * 
 * @author zwei
 * @date 2025-09-05
 */
public class Device extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 数据ID（表主键，自增） */
    private Long id;

    /** 设备编号 */
    @Excel(name = "设备编号")
    private String sn;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String name;

    /** 设备类型 */
    @Excel(name = "设备类型")
    private String type;

    /** 所属产品id */
    @Excel(name = "所属产品id")
    private Long productId;

    /** 通信协议 */
    @Excel(name = "通信协议")
    private String commProtocol;

    /** 经度 */
    @Excel(name = "经度")
    private Long longitude;

    /** 纬度 */
    @Excel(name = "纬度")
    private Long latitude;

    /** 供电方式 */
    @Excel(name = "供电方式")
    private String powerSupply;

    /** 生产厂商 */
    @Excel(name = "生产厂商")
    private String manufacturer;

    /** 厂商电话 */
    @Excel(name = "厂商电话")
    private String suppierTel;

    /** 安装日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "安装日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date installData;

    /** 质保日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "质保日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date warrantyEnd;

    /** 维保人姓名 */
    @Excel(name = "维保人姓名")
    private String maintenanceName;

    /** 维保人电话 */
    @Excel(name = "维保人电话")
    private String maintenanceTel;

    /** 巡检频率 */
    @Excel(name = "巡检频率")
    private String inspectionCycle;

    /** 父设备id */
    @Excel(name = "父设备id")
    private Long parentId;

    /** 接入地址 */
    @Excel(name = "接入地址")
    private String gatewayIp;

    /** 端口号 */
    @Excel(name = "端口号")
    private Long gatewayPort;

    /** 账号 */
    @Excel(name = "账号")
    private String user;

    /** 密码 */
    @Excel(name = "密码")
    private String password;

    /** 安装位置 */
    @Excel(name = "安装位置")
    private String location;

    /** 设备安装附件 */
    @Excel(name = "设备安装附件")
    private String installAttach;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setSn(String sn) 
    {
        this.sn = sn;
    }

    public String getSn() 
    {
        return sn;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setType(String type) 
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }

    public void setProductId(Long productId) 
    {
        this.productId = productId;
    }

    public Long getProductId() 
    {
        return productId;
    }

    public void setCommProtocol(String commProtocol) 
    {
        this.commProtocol = commProtocol;
    }

    public String getCommProtocol() 
    {
        return commProtocol;
    }

    public void setLongitude(Long longitude) 
    {
        this.longitude = longitude;
    }

    public Long getLongitude() 
    {
        return longitude;
    }

    public void setLatitude(Long latitude) 
    {
        this.latitude = latitude;
    }

    public Long getLatitude() 
    {
        return latitude;
    }

    public void setPowerSupply(String powerSupply) 
    {
        this.powerSupply = powerSupply;
    }

    public String getPowerSupply() 
    {
        return powerSupply;
    }

    public void setManufacturer(String manufacturer) 
    {
        this.manufacturer = manufacturer;
    }

    public String getManufacturer() 
    {
        return manufacturer;
    }

    public void setSuppierTel(String suppierTel) 
    {
        this.suppierTel = suppierTel;
    }

    public String getSuppierTel() 
    {
        return suppierTel;
    }

    public void setInstallData(Date installData) 
    {
        this.installData = installData;
    }

    public Date getInstallData() 
    {
        return installData;
    }

    public void setWarrantyEnd(Date warrantyEnd) 
    {
        this.warrantyEnd = warrantyEnd;
    }

    public Date getWarrantyEnd() 
    {
        return warrantyEnd;
    }

    public void setMaintenanceName(String maintenanceName) 
    {
        this.maintenanceName = maintenanceName;
    }

    public String getMaintenanceName() 
    {
        return maintenanceName;
    }

    public void setMaintenanceTel(String maintenanceTel) 
    {
        this.maintenanceTel = maintenanceTel;
    }

    public String getMaintenanceTel() 
    {
        return maintenanceTel;
    }

    public void setInspectionCycle(String inspectionCycle) 
    {
        this.inspectionCycle = inspectionCycle;
    }

    public String getInspectionCycle() 
    {
        return inspectionCycle;
    }

    public void setParentId(Long parentId) 
    {
        this.parentId = parentId;
    }

    public Long getParentId() 
    {
        return parentId;
    }

    public void setGatewayIp(String gatewayIp) 
    {
        this.gatewayIp = gatewayIp;
    }

    public String getGatewayIp() 
    {
        return gatewayIp;
    }

    public void setGatewayPort(Long gatewayPort) 
    {
        this.gatewayPort = gatewayPort;
    }

    public Long getGatewayPort() 
    {
        return gatewayPort;
    }

    public void setUser(String user) 
    {
        this.user = user;
    }

    public String getUser() 
    {
        return user;
    }

    public void setPassword(String password) 
    {
        this.password = password;
    }

    public String getPassword() 
    {
        return password;
    }

    public void setLocation(String location) 
    {
        this.location = location;
    }

    public String getLocation() 
    {
        return location;
    }

    public void setInstallAttach(String installAttach) 
    {
        this.installAttach = installAttach;
    }

    public String getInstallAttach() 
    {
        return installAttach;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("sn", getSn())
            .append("name", getName())
            .append("type", getType())
            .append("productId", getProductId())
            .append("commProtocol", getCommProtocol())
            .append("longitude", getLongitude())
            .append("latitude", getLatitude())
            .append("powerSupply", getPowerSupply())
            .append("manufacturer", getManufacturer())
            .append("suppierTel", getSuppierTel())
            .append("installData", getInstallData())
            .append("warrantyEnd", getWarrantyEnd())
            .append("maintenanceName", getMaintenanceName())
            .append("maintenanceTel", getMaintenanceTel())
            .append("inspectionCycle", getInspectionCycle())
            .append("parentId", getParentId())
            .append("gatewayIp", getGatewayIp())
            .append("gatewayPort", getGatewayPort())
            .append("user", getUser())
            .append("password", getPassword())
            .append("location", getLocation())
            .append("installAttach", getInstallAttach())
            .toString();
    }
}
