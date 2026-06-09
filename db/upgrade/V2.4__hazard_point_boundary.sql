-- V2.4: 隐患点边界范围 — 存储 polygon / strikeCoords / strikeAngle
ALTER TABLE hazard_point
    ADD COLUMN boundary_coords JSON DEFAULT NULL COMMENT '边界范围数据: {"polygon":[[lat,lng],...],"strikeCoords":[[lat,lng],[lat,lng]],"strikeAngle":45.5}' AFTER strike;
