package com.zwei.iot.device.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.Product;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.tsl.ProductTsl;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.ProductMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.tsl.TslBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductTslServiceImplTest {

    @Mock private ProductMapper productMapper;
    @Mock private DeviceMapper deviceMapper;
    @Mock private SensorAttributeMapper attributeMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TslBuilder tslBuilder;
    private ProductTslServiceImpl service;

    @BeforeEach
    void setUp() {
        tslBuilder = new TslBuilder();
        service = new ProductTslServiceImpl(
                productMapper, deviceMapper, attributeMapper, tslBuilder);
    }

    @Test
    @DisplayName("regenerate: new device -> inserts Product with TSL JSON")
    void regenerate_newDevice_insertsProduct() throws JsonProcessingException {
        Device device = Device.builder().id(1L).code("DEV001").name("Test").build();
        SensorAttribute attr = SensorAttribute.builder()
                .id(10L).sensorId(5L).attrCode("temp").attrName("温度")
                .unit("°C").rangeMin(new BigDecimal("-40")).rangeMax(new BigDecimal("85"))
                .build();

        when(deviceMapper.selectDeviceById(1L)).thenReturn(device);
        when(attributeMapper.selectAttributeListByDeviceId(1L)).thenReturn(List.of(attr));
        when(productMapper.selectByDeviceId(1L)).thenReturn(null);

        service.regenerate(1L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getProductKey()).startsWith("p_");
        assertThat(saved.getDeviceId()).isEqualTo(1L);
        assertThat(saved.getTslVersion()).isEqualTo("1.0");

        ProductTsl parsed = objectMapper.readValue(saved.getTslJson(), ProductTsl.class);
        assertThat(parsed.properties()).hasSize(1);
        assertThat(parsed.properties().get(0).identifier()).isEqualTo("temp");
    }

    @Test
    @DisplayName("regenerate: existing product -> upserts")
    void regenerate_existingProduct_upserts() {
        Device device = Device.builder().id(1L).code("DEV001").name("Test").build();
        Product existing = Product.builder()
                .id(100L).productKey("p_abc").deviceId(1L).build();

        when(deviceMapper.selectDeviceById(1L)).thenReturn(device);
        when(attributeMapper.selectAttributeListByDeviceId(1L)).thenReturn(List.of());
        when(productMapper.selectByDeviceId(1L)).thenReturn(existing);

        service.regenerate(1L);

        verify(productMapper, never()).insert(any());
        verify(productMapper).upsert(any(Product.class));
    }

    @Test
    @DisplayName("regenerate: device not found -> throws ServiceException")
    void regenerate_deviceNotFound_throws() {
        when(deviceMapper.selectDeviceById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.regenerate(99L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("设备不存在");
    }

    @Test
    @DisplayName("getByDeviceId: returns parsed ProductTsl")
    void getByDeviceId_returnsTsl() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(ProductTsl.empty("p_test"));
        Product product = Product.builder()
                .id(1L).productKey("p_test").deviceId(1L).tslJson(json).build();

        when(productMapper.selectByDeviceId(1L)).thenReturn(product);

        ProductTsl result = service.getByDeviceId(1L);
        assertThat(result.profile().productKey()).isEqualTo("p_test");
        assertThat(result.properties()).isEmpty();
    }

    @Test
    @DisplayName("getByDeviceId: no product -> throws ServiceException")
    void getByDeviceId_noProduct_throws() {
        when(productMapper.selectByDeviceId(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.getByDeviceId(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("产品物模型不存在");
    }

    @Test
    @DisplayName("regenerate: batch query collects attributes across all sensors")
    void regenerate_collectsAttributesAcrossMultipleSensors() throws JsonProcessingException {
        Device device = Device.builder().id(1L).code("DEV001").name("Multi").build();
        SensorAttribute a1 = SensorAttribute.builder()
                .id(10L).sensorId(1L).attrCode("attr1").attrName("属性1").unit("m").build();
        SensorAttribute a2 = SensorAttribute.builder()
                .id(20L).sensorId(2L).attrCode("attr2").attrName("属性2").unit("°C").build();

        when(deviceMapper.selectDeviceById(1L)).thenReturn(device);
        when(attributeMapper.selectAttributeListByDeviceId(1L)).thenReturn(List.of(a1, a2));
        when(productMapper.selectByDeviceId(1L)).thenReturn(null);

        service.regenerate(1L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(captor.capture());
        ProductTsl parsed = objectMapper.readValue(captor.getValue().getTslJson(), ProductTsl.class);
        assertThat(parsed.properties()).hasSize(2);
    }
}
