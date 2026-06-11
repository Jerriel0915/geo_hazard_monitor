package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductTsl(
        String schema,
        TslProfile profile,
        List<TslProperty> properties,
        List<TslEvent> events,
        List<TslService> services) {

    public static final String SCHEMA_URL = "https://iot.example.com/tsl/v1";

    public static ProductTsl empty(String productKey) {
        return new ProductTsl(
                SCHEMA_URL,
                new TslProfile(productKey),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
