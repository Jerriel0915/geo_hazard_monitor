
DROP TABLE IF EXISTS zw_iot_product;
CREATE TABLE zw_iot_product(
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据ID（表主键，自增）',
    `product_key` VARCHAR(32) NOT NULL COMMENT 'productKey;协议接入时topic使用',
    `name` VARCHAR(50) NOT NULL COMMENT '产品名称',
    `node_type` INT NOT NULL COMMENT '设备类型;0:直连设备,1:网关,2:传感器',
    `remarks` VARCHAR(255) COMMENT '描述',
    PRIMARY KEY (`id`)
) COMMENT '产品';


DROP TABLE IF EXISTS zw_iot_product_tsl;
CREATE TABLE zw_iot_product_tsl(
    `product_id` NVARCHAR(128) NOT NULL COMMENT '所属产品ID',
    `tsl` TEXT COMMENT '物模型定义;TSL JSON',
    PRIMARY KEY (`product_id`)
) COMMENT '产品物模型定义;遵循aliyun tsl规范';

