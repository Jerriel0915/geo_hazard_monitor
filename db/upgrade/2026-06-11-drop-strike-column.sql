-- 删除冗余 strike 列（走向角度已可从前端 boundary_coords.strikeLine 计算）
ALTER TABLE hazard_point DROP COLUMN strike;
