package com.zwei.common.core.domain.model;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量主键请求
 */
public class BatchIdRequest
{
    @NotEmpty(message = "ids不能为空")
    private List<Long> ids;

    public List<Long> getIds()
    {
        return ids;
    }

    public void setIds(List<Long> ids)
    {
        this.ids = ids;
    }
}
