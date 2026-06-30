package com.zwei.iot.device.mapper;

import com.zwei.iot.device.domain.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    Product selectById(@Param("id") Long id);

    Product selectByProductKey(@Param("productKey") String productKey);

    Product selectByDeviceId(@Param("deviceId") Long deviceId);

    int insert(Product product);

    int upsert(Product product);

    int deleteByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 批量根据设备ID列表逻辑删除 Product（释放 product_key 唯一约束）。
     *
     * @param deviceIds 设备ID列表
     * @return 影响行数
     */
    int deleteByDeviceIds(@Param("deviceIds") List<Long> deviceIds);

    int countAll();
}
