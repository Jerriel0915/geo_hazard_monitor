package com.zwei.module.iot.product.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * 产品变更日志对象 product_change_log
 * 
 * @author linx
 * @date 2025-09-05
 */
@Data
public class ProductChangeLog {
    private static final long serialVersionUID = 1L;

    /** 主键id */
    private Long id;

    /** 产品ID */
    private Long productId;

    /** 产品密钥 */
    private String productKey;

    /** 操作类型：0-新增物模型 1-更新物模型 */
    private Integer operationType;

    /** 执行状态：0-待执行 1-执行成功 2-执行失败 */
    private Integer status;

    /** 错误信息 */
    private String errorMessage;

    /** 物模型内容 */
    private String tslContent;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date executeTime;
}