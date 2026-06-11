package com.zwei.iot.device.mapper;

import com.zwei.iot.device.domain.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductMapper {

    Product selectById(@Param("id") Long id);

    Product selectByProductKey(@Param("productKey") String productKey);

    Product selectByDeviceId(@Param("deviceId") Long deviceId);

    int insert(Product product);

    int upsert(Product product);

    int deleteByDeviceId(@Param("deviceId") Long deviceId);

    int countAll();
}
