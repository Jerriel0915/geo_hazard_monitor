package com.zwei.iot.device.tsl;

import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.tsl.ProductTsl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TslBuilderTest {

    private final TslBuilder builder = new TslBuilder();

    @Test
    @DisplayName("builds TSL with properties from sensor attributes")
    void build_withAttributes_returnsCompleteTsl() {
        SensorAttribute attr = SensorAttribute.builder()
                .id(10L).sensorId(5L)
                .attrCode("rainfall_hour").attrName("小时降雨量")
                .unit("mm").rangeMin(new BigDecimal("0")).rangeMax(new BigDecimal("500.00"))
                .build();

        ProductTsl result = builder.build("DEV001", List.of(attr));

        assertThat(result.profile().productKey()).startsWith("p_");
        assertThat(result.properties()).hasSize(1);
        assertThat(result.properties().get(0).identifier()).isEqualTo("rainfall_hour");
        assertThat(result.properties().get(0).name()).isEqualTo("小时降雨量");
        assertThat(result.properties().get(0).accessMode()).isEqualTo("r");
        assertThat(result.properties().get(0).required()).isTrue();
        assertThat(result.properties().get(0).dataType().specs().min()).isEqualTo("0");
        assertThat(result.properties().get(0).dataType().specs().max()).isEqualTo("500.00");
        assertThat(result.properties().get(0).dataType().specs().unit()).isEqualTo("mm");
        assertThat(result.events()).isEmpty();
        assertThat(result.services()).isEmpty();
    }

    @Test
    @DisplayName("builds TSL with empty properties when no attributes")
    void build_noAttributes_returnsEmptyProperties() {
        ProductTsl result = builder.build("DEV002", Collections.emptyList());
        assertThat(result.properties()).isEmpty();
    }

    @Test
    @DisplayName("productKey is stable — same input yields same key")
    void build_productKey_isStable() {
        SensorAttribute attr = SensorAttribute.builder()
                .id(1L).attrCode("t").attrName("t").build();
        ProductTsl t1 = builder.build("DEV003", List.of(attr));
        ProductTsl t2 = builder.build("DEV003", List.of(attr));
        assertThat(t1.profile().productKey()).isEqualTo(t2.profile().productKey());
    }

    @Test
    @DisplayName("null range values produce null specs fields")
    void build_nullRanges_producesNullSpecs() {
        SensorAttribute attr = SensorAttribute.builder()
                .id(1L).attrCode("temp").attrName("温度").unit("°C").build();

        ProductTsl result = builder.build("DEV", List.of(attr));

        assertThat(result.properties().get(0).dataType().specs().min()).isNull();
        assertThat(result.properties().get(0).dataType().specs().max()).isNull();
    }

    @Test
    @DisplayName("null deviceCode throws IllegalArgumentException")
    void build_nullDeviceCode_throws() {
        assertThatThrownBy(() -> builder.build(null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deviceCode");
    }

    @Test
    @DisplayName("blank deviceCode throws IllegalArgumentException")
    void build_blankDeviceCode_throws() {
        assertThatThrownBy(() -> builder.build("  ", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deviceCode");
    }

    @Test
    @DisplayName("attribute with null attrCode is skipped")
    void build_nullAttrCode_skipped() {
        SensorAttribute attr = SensorAttribute.builder()
                .id(1L).attrCode(null).attrName("温度").build();

        ProductTsl result = builder.build("DEV", List.of(attr));
        assertThat(result.properties()).isEmpty();
    }

    @Test
    @DisplayName("attribute with null attrName is skipped")
    void build_nullAttrName_skipped() {
        SensorAttribute attr = SensorAttribute.builder()
                .id(1L).attrCode("temp").attrName(null).build();

        ProductTsl result = builder.build("DEV", List.of(attr));
        assertThat(result.properties()).isEmpty();
    }

    @Test
    @DisplayName("different device codes produce different product keys")
    void build_differentCodes_differentKeys() {
        ProductTsl t1 = builder.build("DEV001", List.of());
        ProductTsl t2 = builder.build("DEV002", List.of());
        assertThat(t1.profile().productKey())
                .isNotEqualTo(t2.profile().productKey());
    }
}
