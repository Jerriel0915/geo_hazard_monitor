package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TslDataSpecs(
        String min,
        String max,
        String unit,
        @JsonProperty("unitName") String unitName,
        String step,
        Integer size,
        Integer length,
        @JsonProperty("0") String value0,
        @JsonProperty("1") String value1,
        Item item) {

    public record Item(String type) {}
}
