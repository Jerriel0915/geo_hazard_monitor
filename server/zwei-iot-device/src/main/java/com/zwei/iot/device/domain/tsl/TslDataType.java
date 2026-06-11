package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TslDataType(String type, TslDataSpecs specs) {
}
