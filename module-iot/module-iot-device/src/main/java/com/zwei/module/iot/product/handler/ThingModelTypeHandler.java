package com.zwei.module.iot.product.handler;

import com.alibaba.fastjson2.JSON;
import com.zwei.module.iot.thing.domain.ThingModel;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ThingModel JSON Type Handler
 *
 * @Author: Jerriel
 * @CreateTime: 2026-01-28
 */
@MappedTypes(ThingModel.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class ThingModelTypeHandler extends BaseTypeHandler<ThingModel> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ThingModel parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, JSON.toJSONString(parameter));
    }

    @Override
    public ThingModel getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parse(json);
    }

    @Override
    public ThingModel getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parse(json);
    }

    @Override
    public ThingModel getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parse(json);
    }

    private ThingModel parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, ThingModel.class);
        } catch (Exception e) {
            // Log error or return empty object/null depending on requirement
            // For now, return null if parsing fails to avoid breaking flow
            return null;
        }
    }
}
