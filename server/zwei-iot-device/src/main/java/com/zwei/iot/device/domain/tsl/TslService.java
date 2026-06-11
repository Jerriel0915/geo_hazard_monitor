package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TslService(
        String identifier,
        String name,
        String desc,
        Boolean required,
        @JsonProperty("callType") String callType,
        @JsonProperty("inputData") List<TslProperty> inputData,
        @JsonProperty("outputData") List<TslProperty> outputData,
        String method) {
}
