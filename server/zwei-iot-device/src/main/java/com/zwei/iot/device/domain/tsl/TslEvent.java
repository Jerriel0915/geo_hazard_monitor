package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TslEvent(
        String identifier,
        String name,
        String desc,
        String type,
        Boolean required,
        @JsonProperty("outputData") List<TslProperty> outputData,
        String method) {
}
