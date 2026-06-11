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
import com.zwei.iot.device.service.IProductTslService;
import com.zwei.iot.device.tsl.TslBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ProductTslServiceImpl implements IProductTslService {

    private static final String TSL_VERSION = "1.0";

    private final ProductMapper productMapper;
    private final DeviceMapper deviceMapper;
    private final SensorAttributeMapper attributeMapper;
    private final TslBuilder tslBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    public ProductTslServiceImpl(ProductMapper productMapper,
                                  DeviceMapper deviceMapper,
                                  SensorAttributeMapper attributeMapper,
                                  TslBuilder tslBuilder) {
        this.productMapper = productMapper;
        this.deviceMapper = deviceMapper;
        this.attributeMapper = attributeMapper;
        this.tslBuilder = tslBuilder;
    }

    @Override
    public ProductTsl getByProductKey(String productKey) {
        Product product = productMapper.selectByProductKey(productKey);
        if (product == null) {
            throw new ServiceException("产品物模型不存在: productKey=" + productKey);
        }
        if (product.getTslJson() == null) {
            throw new ServiceException("产品物模型TSL JSON为空: productKey=" + product.getProductKey());
        }
        return parseTsl(product.getTslJson());
    }

    @Override
    public ProductTsl getByDeviceId(Long deviceId) {
        Product product = productMapper.selectByDeviceId(deviceId);
        if (product == null) {
            throw new ServiceException("产品物模型不存在: deviceId=" + deviceId);
        }
        if (product.getTslJson() == null) {
            throw new ServiceException("产品物模型TSL JSON为空: deviceId=" + deviceId);
        }
        return parseTsl(product.getTslJson());
    }

    @Override
    @Transactional
    public void regenerate(Long deviceId) {
        Device device = deviceMapper.selectDeviceById(deviceId);
        if (device == null) {
            throw new ServiceException("设备不存在: id=" + deviceId);
        }

        List<SensorAttribute> allAttrs = attributeMapper.selectAttributeListByDeviceId(deviceId);

        ProductTsl tsl = tslBuilder.build(device.getCode(), allAttrs);
        String tslJson = toJson(tsl);

        Product existing = productMapper.selectByDeviceId(deviceId);
        if (existing != null) {
            Product updated = Product.builder()
                    .id(existing.getId())
                    .productKey(existing.getProductKey())
                    .deviceId(existing.getDeviceId())
                    .tslJson(tslJson)
                    .tslVersion(TSL_VERSION)
                    .build();
            productMapper.upsert(updated);
        } else {
            Product product = Product.builder()
                    .productKey(tsl.profile().productKey())
                    .deviceId(deviceId)
                    .tslJson(tslJson)
                    .tslVersion(TSL_VERSION)
                    .build();
            productMapper.insert(product);
        }
    }

    private ProductTsl parseTsl(String tslJson) {
        try {
            return objectMapper.readValue(tslJson, ProductTsl.class);
        } catch (JsonProcessingException e) {
            log.error("TSL JSON parse failed", e);
            throw new ServiceException("TSL JSON 解析失败");
        }
    }

    private String toJson(ProductTsl tsl) {
        try {
            return objectMapper.writeValueAsString(tsl);
        } catch (JsonProcessingException e) {
            log.error("TSL JSON serialize failed", e);
            throw new ServiceException("TSL JSON 序列化失败");
        }
    }
}
