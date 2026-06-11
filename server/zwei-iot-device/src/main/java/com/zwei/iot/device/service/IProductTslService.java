package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.tsl.ProductTsl;

public interface IProductTslService {

    ProductTsl getByProductKey(String productKey);

    ProductTsl getByDeviceId(Long deviceId);

    void regenerate(Long deviceId);
}
