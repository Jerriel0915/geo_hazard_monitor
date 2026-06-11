package com.zwei.iot.device.tsl;

import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.tsl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class TslBuilder {

    private static final String ACCESS_MODE_READ = "r";
    private static final String DATA_TYPE_DOUBLE = "double";

    private String generateProductKey(String deviceCode) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(deviceCode.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("p_");
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    public ProductTsl build(String deviceCode, List<SensorAttribute> attributes) {
        if (deviceCode == null || deviceCode.isBlank()) {
            throw new IllegalArgumentException("deviceCode must not be null or blank");
        }

        String productKey = generateProductKey(deviceCode);
        List<TslProperty> properties = new ArrayList<>();

        if (attributes != null) {
            for (SensorAttribute attr : attributes) {
                TslProperty prop = toProperty(attr);
                if (prop != null) {
                    properties.add(prop);
                }
            }
        }

        return new ProductTsl(
                ProductTsl.SCHEMA_URL,
                new TslProfile(productKey),
                properties,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private TslProperty toProperty(SensorAttribute attr) {
        if (attr.getAttrCode() == null || attr.getAttrCode().isBlank()
                || attr.getAttrName() == null || attr.getAttrName().isBlank()) {
            log.warn("Skipping sensor attribute id={}: attrCode or attrName is null/blank", attr.getId());
            return null;
        }

        return new TslProperty(
                attr.getAttrCode(),
                attr.getAttrName(),
                ACCESS_MODE_READ,
                true,
                new TslDataType(DATA_TYPE_DOUBLE, buildSpecs(attr))
        );
    }

    private TslDataSpecs buildSpecs(SensorAttribute attr) {
        String min = attr.getRangeMin() != null ? attr.getRangeMin().toPlainString() : null;
        String max = attr.getRangeMax() != null ? attr.getRangeMax().toPlainString() : null;
        return new TslDataSpecs(min, max, attr.getUnit(), null, null, null, null, null, null, null);
    }
}
