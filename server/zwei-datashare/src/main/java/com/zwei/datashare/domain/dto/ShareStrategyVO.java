package com.zwei.datashare.domain.dto;

import com.zwei.datashare.domain.ShareStrategy;
import com.zwei.datashare.enums.ScopeType;
import com.zwei.datashare.enums.ShareMethod;
import com.zwei.datashare.enums.StrategyStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShareStrategyVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private ShareMethod method;
    private String methodLabel;
    private String address;
    private String topic;
    private ScopeType scopeType;
    private String scopeTypeLabel;
    private String frequency;
    private StrategyStatus status;
    private String statusLabel;
    private Integer successCount;
    private LocalDateTime lastRunTime;
    private String lastRunStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ShareStrategyVO fromEntity(ShareStrategy entity) {
        if (entity == null) {
            return null;
        }
        ShareStrategyVO vo = new ShareStrategyVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setMethod(entity.getMethod());
        vo.setMethodLabel(entity.getMethod() != null ? entity.getMethod().getLabel() : null);
        vo.setAddress(entity.getAddress());
        vo.setTopic(entity.getTopic());
        vo.setScopeType(entity.getScopeType());
        vo.setScopeTypeLabel(entity.getScopeType() != null ? entity.getScopeType().getLabel() : null);
        vo.setFrequency(entity.getCron());
        vo.setStatus(entity.getStatus());
        vo.setStatusLabel(entity.getStatus() != null ? entity.getStatus().getLabel() : null);
        vo.setSuccessCount(entity.getSuccessCount());
        vo.setLastRunTime(entity.getLastRunTime());
        vo.setLastRunStatus(entity.getLastRunStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}