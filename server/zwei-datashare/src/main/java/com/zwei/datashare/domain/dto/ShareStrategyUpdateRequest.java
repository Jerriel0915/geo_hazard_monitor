package com.zwei.datashare.domain.dto;

import com.zwei.datashare.enums.ScopeType;
import com.zwei.datashare.enums.ShareMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareStrategyUpdateRequest {
    private String name;
    private String description;
    private ShareMethod method;
    private String address;
    private String topic;
    private String username;
    private String password;
    private String params;
    private ScopeType scopeType;
    private String scopeIds;
    private String cron;

}
