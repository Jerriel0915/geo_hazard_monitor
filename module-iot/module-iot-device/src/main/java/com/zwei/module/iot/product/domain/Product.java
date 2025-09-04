package com.zwei.module.iot.product.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zwei.common.annotation.Excel;
import com.zwei.common.core.domain.BaseEntity;

/**
 * 产品管理对象 zw_product
 * 
 * @author zwei
 * @date 2025-09-04
 */
public class Product extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 数据ID（表主键，自增） */
    private Long id;

    /** 产品编号 */
    @Excel(name = "产品编号")
    private String sn;

    /** 产品内容 */
    @Excel(name = "产品内容")
    private String content;

    /** 产品类型 */
    @Excel(name = "产品类型")
    private String type;

    /** 数据字段 */
    @Excel(name = "数据字段")
    private String fields;

    /** 备注 */
    @Excel(name = "备注")
    private String comment;

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

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setType(String type) 
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }

    public void setFields(String fields) 
    {
        this.fields = fields;
    }

    public String getFields() 
    {
        return fields;
    }

    public void setComment(String comment) 
    {
        this.comment = comment;
    }

    public String getComment() 
    {
        return comment;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("sn", getSn())
            .append("content", getContent())
            .append("type", getType())
            .append("fields", getFields())
            .append("comment", getComment())
            .toString();
    }
}
