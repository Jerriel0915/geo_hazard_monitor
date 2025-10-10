DROP TABLE IF EXISTS zw_iot_device;
CREATE TABLE zw_iot_device(
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据ID（表主键，自增）',
    `product_id` BIGINT COMMENT '所属产品id',
    `sn` VARCHAR(50) NOT NULL COMMENT '设备编号;clientId',
    `name` VARCHAR(100) NOT NULL COMMENT '设备名称',
    `type` VARCHAR(10) NOT NULL COMMENT '设备类型;直连设备/网关设备/网关子设备',
    `device_key` VARCHAR(50) COMMENT 'deviceKey',
    `device_secret` VARCHAR(100) COMMENT 'deviceSecret',
    `comm_protocol` VARCHAR(32) NOT NULL COMMENT '通信协议',
    `longitude` DECIMAL NOT NULL COMMENT '经度',
    `latitude` DECIMAL NOT NULL COMMENT '纬度',
    `power_supply` VARCHAR(30) COMMENT '供电方式',
    `manufacturer` VARCHAR(100) COMMENT '生产厂商',
    `parent_id` BIGINT COMMENT '父设备id',
    PRIMARY KEY (`id`)
) COMMENT '设备基本信息表';

CREATE UNIQUE INDEX `device_id` ON zw_iot_device (
    `sn` ASC
);

DROP TABLE IF EXISTS zw_iot_device_alive_log;
CREATE TABLE zw_iot_device_alive_log(
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id` BIGINT NOT NULL COMMENT '关联设备ID',
    `last_connect_time` DATETIME COMMENT '上线时间;用于判断mqtt连接状态',
    `last_disconnect_time` DATETIME COMMENT '下线时间;用于判断mqtt连接状态',
    PRIMARY KEY (`id`)
) COMMENT '状态日志';


DROP TABLE IF EXISTS zw_iot_product_tsl;
CREATE TABLE zw_iot_product_tsl(
    `id` NVARCHAR(128) NOT NULL COMMENT '',
    `product_id` NVARCHAR(128) COMMENT '所属产品ID',
    `tsl` TEXT COMMENT '物模型定义',
    PRIMARY KEY (`id`)
) COMMENT '产品物模型定义;遵循aliyun tsl规范';


DROP TABLE IF EXISTS zw_iot_device_status;
CREATE TABLE zw_iot_device_status(
    `device_id` NVARCHAR(128) COMMENT '设备ID',
    `status` INT DEFAULT 0 COMMENT '0离线1在线',
    `last_report_time` BIGINT COMMENT '最后上报数据时间',
    `last_connect_time` BIGINT COMMENT '最后上线时间',
    `last_offline_time` BIGINT COMMENT '最后主动离线时间',
    PRIMARY KEY (`device_id`)
) COMMENT '设备实时状态;设备实时状态表';

