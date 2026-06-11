package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TslProfile(@JsonProperty("productKey") String productKey) {
}
