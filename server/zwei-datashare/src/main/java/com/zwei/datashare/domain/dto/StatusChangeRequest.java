package com.zwei.datashare.domain.dto;

import com.zwei.datashare.enums.StrategyStatus;
import lombok.Data;

/**
 * 状态变更请求
 */
@Data
public class StatusChangeRequest {
    private StrategyStatus status;
}
