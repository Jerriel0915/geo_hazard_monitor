package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TslProperty(
        String identifier,
        String name,
        @JsonProperty("accessMode") String accessMode,
        Boolean required,
        @JsonProperty("dataType") TslDataType dataType) {
}
