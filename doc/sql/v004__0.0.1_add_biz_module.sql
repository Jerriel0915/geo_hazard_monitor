DROP TABLE IF EXISTS zw_biz_monitoring_object;
CREATE TABLE zw_biz_monitoring_object(
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '',
    `name` VARCHAR(120) NOT NULL COMMENT '监测对象名称',
    `code` VARCHAR(120) NOT NULL COMMENT '监测对象编号',
    `type` VARCHAR(120) NOT NULL COMMENT '监测对象类型;边坡，桥梁，河流，堤坝等',
    `center_lng` FLOAT COMMENT '中心位置经度',
    `center_lat` FLOAT COMMENT '中心位置纬度',
    `monitor_polygon` TEXT COMMENT '对象范围;经纬坐标围成的多边形',
    `administra_region` VARCHAR(12) COMMENT '行政区划;存储一个行政区划的编码，例如510183001002',
    `address` VARCHAR(255) COMMENT '详细地址',
    `description` VARCHAR(255) COMMENT '概况',
    PRIMARY KEY (`id`)
) COMMENT '监测对象基本信息表';




DROP TABLE IF EXISTS zw_biz_monitoring_point;
CREATE TABLE zw_biz_monitoring_point(
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '',
    `object_id` BIGINT NOT NULL COMMENT '所属对象',
    `no` VARCHAR(64) COMMENT '编号',
    `name` VARCHAR(200) COMMENT '名称',
    `lng` FLOAT COMMENT '经度',
    `lat` FLOAT COMMENT '纬度',
    PRIMARY KEY (`id`)
) COMMENT '监测点位(测站点)';


DROP TABLE IF EXISTS zwei.zw_biz_point_device_mapping;
CREATE TABLE zwei.zw_biz_point_device_mapping(
    `point_id` BIGINT NOT NULL COMMENT '',
    `device_id` BIGINT NOT NULL COMMENT '',
    PRIMARY KEY (`point_id`,`device_id`)
) COMMENT '测点与设备关联关系';

