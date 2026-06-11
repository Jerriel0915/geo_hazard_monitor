# TSL Product Model — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce TSL (Thing Specification Language) Product concept — each Device gets a 1:1 Product with auto-generated TSL JSON projection from existing sensor/attribute data.

**Architecture:** New `product` table stores TSL JSON alongside existing `device`/`device_sensor`/`sensor_attribute` tables. `TslBuilder` assembles TSL JSON from device code + flattened attribute list. `ProductTslServiceImpl.regenerate(deviceId)` is called in the same `@Transactional` boundary as sensor/attribute writes. TSL consumed internally via `IProductTslService`, no REST API exposure.

**Tech Stack:** Java 17, Spring Boot, MyBatis, MySQL JSON column, JUnit 5 + Mockito, Lombok, Jackson.

**File Structure:**

| File | Role |
|------|------|
| `domain/tsl/TslProfile.java` | `record` — TSL profile with productKey |
| `domain/tsl/TslDataSpecs.java` | `record` — dataType specs (min/max/unit/step/…) |
| `domain/tsl/TslDataType.java` | `record` — type + specs wrapper |
| `domain/tsl/TslProperty.java` | `record` — a single TSL property |
| `domain/tsl/TslEvent.java` | `record` — reserved event definition |
| `domain/tsl/TslService.java` | `record` — reserved service definition |
| `domain/tsl/ProductTsl.java` | `record` — top-level TSL document |
| `domain/Product.java` | entity — maps to `product` table |
| `mapper/ProductMapper.java` + `.xml` | MyBatis — CRUD + upsert for `product` |
| `tsl/TslBuilder.java` | `@Component` — pure function: deviceCode + attrs → ProductTsl |
| `service/IProductTslService.java` | interface — 3 methods |
| `service/impl/ProductTslServiceImpl.java` | `@Service` — regenerate + query, depends on 3 mappers + TslBuilder + ObjectMapper |
| `migration/ProductTslMigrationRunner.java` | `@Component` — ApplicationRunner for initial migration |
| `db/upgrade/v2.1_add_product_tsl.sql` | DDL — CREATE TABLE `product` |

---

- [ ] **Step 1: Create TslProfile record**

```java
package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TslProfile(@JsonProperty("productKey") String productKey) {
}
```

- [ ] **Step 2: Create TslDataSpecs record**

```java
package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TslDataSpecs(
        String min,
        String max,
        String unit,
        @JsonProperty("unitName") String unitName,
        String step,
        Integer size,
        Integer length,
        @JsonProperty("0") String value0,
        @JsonProperty("1") String value1,
        Item item) {

    public record Item(String type) {}
}
```

- [ ] **Step 3: Create TslDataType record**

```java
package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TslDataType(String type, TslDataSpecs specs) {
}
```

- [ ] **Step 4: Create TslProperty record**

```java
package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TslProperty(
        String identifier,
        String name,
        @JsonProperty("accessMode") String accessMode,
        Boolean required,
        @JsonProperty("dataType") TslDataType dataType) {
}
```

- [ ] **Step 5: Create TslEvent record (reserved)**

```java
package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TslEvent(
        String identifier,
        String name,
        String desc,
        String type,
        Boolean required,
        @JsonProperty("outputData") List<TslProperty> outputData,
        String method) {
}
```

- [ ] **Step 6: Create TslService record (reserved)**

```java
package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TslService(
        String identifier,
        String name,
        String desc,
        Boolean required,
        @JsonProperty("callType") String callType,
        @JsonProperty("inputData") List<TslProperty> inputData,
        @JsonProperty("outputData") List<TslProperty> outputData,
        String method) {
}
```

- [ ] **Step 7: Create ProductTsl wrapper record**

```java
package com.zwei.iot.device.domain.tsl;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductTsl(
        String schema,
        TslProfile profile,
        List<TslProperty> properties,
        List<TslEvent> events,
        List<TslService> services) {

    public static final String SCHEMA_URL = "https://iot.example.com/tsl/v1";

    public static ProductTsl empty(String productKey) {
        return new ProductTsl(
                SCHEMA_URL,
                new TslProfile(productKey),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
```

- [ ] **Step 8: Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/tsl/
git commit -m "feat: add TSL domain value types (ProductTsl, TslProperty, TslEvent, TslService)"
```

---

### Task 2: Create Product entity

**Files:**
- Create: `server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/Product.java`

Follow existing patterns — extends BaseEntity, uses Lombok `@SuperBuilder`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Setter`, `@Getter`.

- [ ] **Step 1: Create Product entity**

```java
package com.zwei.iot.device.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Product extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String productKey;
    private Long deviceId;
    private String tslJson;
    private String tslVersion;
    private Integer delFlag;
}
```

- [ ] **Step 2: Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/domain/Product.java
git commit -m "feat: add Product entity for TSL product model"
```

---

### Task 3: Create ProductMapper (MyBatis)

**Files:**
- Create: `server/zwei-iot-device/src/main/java/com/zwei/iot/device/mapper/ProductMapper.java`
- Create: `server/zwei-iot-device/src/main/resources/mapper/iot/device/ProductMapper.xml`

- [ ] **Step 1: Create ProductMapper interface**

```java
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
```

- [ ] **Step 2: Create ProductMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.device.mapper.ProductMapper">

    <resultMap id="ProductResult" type="com.zwei.iot.device.domain.Product">
        <id     property="id"          column="id"/>
        <result property="productKey"  column="product_key"/>
        <result property="deviceId"    column="device_id"/>
        <result property="tslJson"     column="tsl_json"/>
        <result property="tslVersion"  column="tsl_version"/>
        <result property="createBy"    column="create_by"/>
        <result property="createTime"  column="create_time"/>
        <result property="updateBy"    column="update_by"/>
        <result property="updateTime"  column="update_time"/>
        <result property="delFlag"     column="del_flag"/>
    </resultMap>

    <sql id="selectProductVo">
        SELECT id, product_key, device_id, tsl_json, tsl_version,
               create_by, create_time, update_by, update_time, del_flag
        FROM product
    </sql>

    <select id="selectById" parameterType="Long" resultMap="ProductResult">
        <include refid="selectProductVo"/>
        WHERE id = #{id} AND del_flag = 0
    </select>

    <select id="selectByProductKey" parameterType="String" resultMap="ProductResult">
        <include refid="selectProductVo"/>
        WHERE product_key = #{productKey} AND del_flag = 0
    </select>

    <select id="selectByDeviceId" parameterType="Long" resultMap="ProductResult">
        <include refid="selectProductVo"/>
        WHERE device_id = #{deviceId} AND del_flag = 0
    </select>

    <insert id="insert" parameterType="com.zwei.iot.device.domain.Product"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO product (product_key, device_id, tsl_json, tsl_version,
                             create_by, create_time, update_by, update_time, del_flag)
        VALUES (#{productKey}, #{deviceId}, #{tslJson}, #{tslVersion},
                #{createBy}, NOW(), #{updateBy}, NOW(), 0)
    </insert>

    <insert id="upsert" parameterType="com.zwei.iot.device.domain.Product">
        INSERT INTO product (product_key, device_id, tsl_json, tsl_version,
                             create_by, create_time, update_by, update_time, del_flag)
        VALUES (#{productKey}, #{deviceId}, #{tslJson}, #{tslVersion},
                #{createBy}, NOW(), #{updateBy}, NOW(), 0)
        ON DUPLICATE KEY UPDATE
            tsl_json    = VALUES(tsl_json),
            tsl_version = VALUES(tsl_version),
            update_by   = VALUES(update_by),
            update_time = NOW()
    </insert>

    <update id="deleteByDeviceId" parameterType="Long">
        UPDATE product SET del_flag = 1, update_time = NOW()
        WHERE device_id = #{deviceId} AND del_flag = 0
    </update>

    <select id="countAll" resultType="int">
        SELECT COUNT(*) FROM product WHERE del_flag = 0
    </select>

</mapper>
```

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/mapper/ProductMapper.java
git add server/zwei-iot-device/src/main/resources/mapper/iot/device/ProductMapper.xml
git commit -m "feat: add ProductMapper with insert, upsert, and query methods"
```

---

### Task 4: Create TslBuilder (test-first)

**Files:**
- Create: `server/zwei-iot-device/src/main/java/com/zwei/iot/device/tsl/TslBuilder.java`
- Create: `server/zwei-iot-device/src/test/java/com/zwei/iot/device/tsl/TslBuilderTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.zwei.iot.device.tsl;

import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.tsl.ProductTsl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TslBuilderTest {

    private final TslBuilder builder = new TslBuilder();

    @Test
    @DisplayName("builds TSL with properties from sensor attributes")
    void build_withAttributes_returnsCompleteTsl() {
        SensorAttribute attr = SensorAttribute.builder()
                .id(10L).sensorId(5L)
                .attrCode("rainfall_hour").attrName("小时降雨量")
                .unit("mm").rangeMin(new BigDecimal("0")).rangeMax(new BigDecimal("500.00"))
                .build();

        ProductTsl result = builder.build("DEV001", List.of(attr));

        assertThat(result.profile().productKey()).startsWith("p_");
        assertThat(result.properties()).hasSize(1);
        assertThat(result.properties().get(0).identifier()).isEqualTo("rainfall_hour");
        assertThat(result.properties().get(0).name()).isEqualTo("小时降雨量");
        assertThat(result.properties().get(0).accessMode()).isEqualTo("r");
        assertThat(result.properties().get(0).required()).isTrue();
        assertThat(result.properties().get(0).dataType().specs().min()).isEqualTo("0");
        assertThat(result.properties().get(0).dataType().specs().max()).isEqualTo("500.00");
        assertThat(result.properties().get(0).dataType().specs().unit()).isEqualTo("mm");
        assertThat(result.events()).isEmpty();
        assertThat(result.services()).isEmpty();
    }

    @Test
    @DisplayName("builds TSL with empty properties when no attributes")
    void build_noAttributes_returnsEmptyProperties() {
        ProductTsl result = builder.build("DEV002", Collections.emptyList());
        assertThat(result.properties()).isEmpty();
    }

    @Test
    @DisplayName("productKey is stable — same input yields same key")
    void build_productKey_isStable() {
        SensorAttribute attr = SensorAttribute.builder()
                .id(1L).attrCode("t").attrName("t").build();
        ProductTsl t1 = builder.build("DEV003", List.of(attr));
        ProductTsl t2 = builder.build("DEV003", List.of(attr));
        assertThat(t1.profile().productKey()).isEqualTo(t2.profile().productKey());
    }

    @Test
    @DisplayName("null range values produce null specs fields")
    void build_nullRanges_producesNullSpecs() {
        SensorAttribute attr = SensorAttribute.builder()
                .id(1L).attrCode("temp").attrName("温度").unit("°C").build();

        ProductTsl result = builder.build("DEV", List.of(attr));

        assertThat(result.properties().get(0).dataType().specs().min()).isNull();
        assertThat(result.properties().get(0).dataType().specs().max()).isNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd server && mvn test -pl zwei-iot-device -Dtest=TslBuilderTest -DfailIfNoTests=false`
Expected: COMPILATION ERROR (TslBuilder class not found)

- [ ] **Step 3: Implement TslBuilder**

```java
package com.zwei.iot.device.tsl;

import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.tsl.*;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class TslBuilder {

    String generateProductKey(String deviceCode) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(deviceCode.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("p_");
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    public ProductTsl build(String deviceCode, List<SensorAttribute> attributes) {
        String productKey = generateProductKey(deviceCode);
        List<TslProperty> properties = new ArrayList<>();

        if (attributes != null) {
            for (SensorAttribute attr : attributes) {
                properties.add(toProperty(attr));
            }
        }

        return new ProductTsl(
                ProductTsl.SCHEMA_URL,
                new TslProfile(productKey),
                properties,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private TslProperty toProperty(SensorAttribute attr) {
        return new TslProperty(
                attr.getAttrCode(),
                attr.getAttrName(),
                "r",
                true,
                new TslDataType("double", buildSpecs(attr))
        );
    }

    private TslDataSpecs buildSpecs(SensorAttribute attr) {
        String min = attr.getRangeMin() != null ? attr.getRangeMin().toPlainString() : null;
        String max = attr.getRangeMax() != null ? attr.getRangeMax().toPlainString() : null;
        return new TslDataSpecs(min, max, attr.getUnit(), null, null, null, null, null, null, null);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd server && mvn test -pl zwei-iot-device -Dtest=TslBuilderTest`
Expected: PASS (4/4 tests)

- [ ] **Step 5: Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/tsl/TslBuilder.java
git add server/zwei-iot-device/src/test/java/com/zwei/iot/device/tsl/TslBuilderTest.java
git commit -m "feat: add TslBuilder that generates TSL from device code and attributes"
```

---

### Task 5: Create IProductTslService + ProductTslServiceImpl (test-first)

**Files:**
- Create: `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IProductTslService.java`
- Create: `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/ProductTslServiceImpl.java`
- Create: `server/zwei-iot-device/src/test/java/com/zwei/iot/device/service/impl/ProductTslServiceImplTest.java`

- [ ] **Step 1: Create IProductTslService interface**

```java
package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.tsl.ProductTsl;

public interface IProductTslService {

    ProductTsl getByProductKey(String productKey);

    ProductTsl getByDeviceId(Long deviceId);

    void regenerate(Long deviceId);
}
```

- [ ] **Step 2: Write the failing test**

```java
package com.zwei.iot.device.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.Product;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.tsl.ProductTsl;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.ProductMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.tsl.TslBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductTslServiceImplTest {

    @Mock private ProductMapper productMapper;
    @Mock private DeviceMapper deviceMapper;
    @Mock private DeviceSensorMapper sensorMapper;
    @Mock private SensorAttributeMapper attributeMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TslBuilder tslBuilder;
    private ProductTslServiceImpl service;

    @BeforeEach
    void setUp() {
        tslBuilder = new TslBuilder();
        service = new ProductTslServiceImpl(
                productMapper, deviceMapper, sensorMapper, attributeMapper,
                tslBuilder, objectMapper);
    }

    @Test
    @DisplayName("regenerate: new device → inserts Product with TSL JSON")
    void regenerate_newDevice_insertsProduct() throws JsonProcessingException {
        Device device = Device.builder().id(1L).code("DEV001").name("Test").build();
        DeviceSensor sensor = DeviceSensor.builder().id(5L).deviceId(1L).build();
        SensorAttribute attr = SensorAttribute.builder()
                .id(10L).sensorId(5L).attrCode("temp").attrName("温度")
                .unit("°C").rangeMin(new BigDecimal("-40")).rangeMax(new BigDecimal("85"))
                .build();

        when(deviceMapper.selectDeviceById(1L)).thenReturn(device);
        when(sensorMapper.selectSensorListByDeviceId(1L)).thenReturn(List.of(sensor));
        when(attributeMapper.selectAttributeListBySensorId(5L)).thenReturn(List.of(attr));
        when(productMapper.selectByDeviceId(1L)).thenReturn(null);

        service.regenerate(1L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getProductKey()).startsWith("p_");
        assertThat(saved.getDeviceId()).isEqualTo(1L);
        assertThat(saved.getTslVersion()).isEqualTo("1.0");

        ProductTsl parsed = objectMapper.readValue(saved.getTslJson(), ProductTsl.class);
        assertThat(parsed.properties()).hasSize(1);
        assertThat(parsed.properties().get(0).identifier()).isEqualTo("temp");
    }

    @Test
    @DisplayName("regenerate: existing product → upserts")
    void regenerate_existingProduct_upserts() {
        Device device = Device.builder().id(1L).code("DEV001").name("Test").build();
        Product existing = Product.builder()
                .id(100L).productKey("p_abc").deviceId(1L).build();

        when(deviceMapper.selectDeviceById(1L)).thenReturn(device);
        when(sensorMapper.selectSensorListByDeviceId(1L)).thenReturn(List.of());
        when(productMapper.selectByDeviceId(1L)).thenReturn(existing);

        service.regenerate(1L);

        verify(productMapper, never()).insert(any());
        verify(productMapper).upsert(any(Product.class));
    }

    @Test
    @DisplayName("regenerate: device not found → throws ServiceException")
    void regenerate_deviceNotFound_throws() {
        when(deviceMapper.selectDeviceById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.regenerate(99L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("设备不存在");
    }

    @Test
    @DisplayName("getByDeviceId: returns parsed ProductTsl")
    void getByDeviceId_returnsTsl() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(ProductTsl.empty("p_test"));
        Product product = Product.builder()
                .id(1L).productKey("p_test").deviceId(1L).tslJson(json).build();

        when(productMapper.selectByDeviceId(1L)).thenReturn(product);

        ProductTsl result = service.getByDeviceId(1L);
        assertThat(result.profile().productKey()).isEqualTo("p_test");
        assertThat(result.properties()).isEmpty();
    }

    @Test
    @DisplayName("getByDeviceId: no product → throws ServiceException")
    void getByDeviceId_noProduct_throws() {
        when(productMapper.selectByDeviceId(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.getByDeviceId(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("产品物模型不存在");
    }

    @Test
    @DisplayName("regenerate collects attributes from all sensors")
    void regenerate_collectsAttributesAcrossMultipleSensors() throws JsonProcessingException {
        Device device = Device.builder().id(1L).code("DEV001").name("Multi").build();
        DeviceSensor s1 = DeviceSensor.builder().id(1L).deviceId(1L).build();
        DeviceSensor s2 = DeviceSensor.builder().id(2L).deviceId(1L).build();
        SensorAttribute a1 = SensorAttribute.builder()
                .id(10L).sensorId(1L).attrCode("attr1").attrName("属性1").unit("m").build();
        SensorAttribute a2 = SensorAttribute.builder()
                .id(20L).sensorId(2L).attrCode("attr2").attrName("属性2").unit("°C").build();

        when(deviceMapper.selectDeviceById(1L)).thenReturn(device);
        when(sensorMapper.selectSensorListByDeviceId(1L)).thenReturn(List.of(s1, s2));
        when(attributeMapper.selectAttributeListBySensorId(1L)).thenReturn(List.of(a1));
        when(attributeMapper.selectAttributeListBySensorId(2L)).thenReturn(List.of(a2));
        when(productMapper.selectByDeviceId(1L)).thenReturn(null);

        service.regenerate(1L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(captor.capture());
        ProductTsl parsed = objectMapper.readValue(captor.getValue().getTslJson(), ProductTsl.class);
        assertThat(parsed.properties()).hasSize(2);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `cd server && mvn test -pl zwei-iot-device -Dtest=ProductTslServiceImplTest -DfailIfNoTests=false`
Expected: COMPILATION ERROR (ProductTslServiceImpl not found)

- [ ] **Step 4: Implement ProductTslServiceImpl**

```java
package com.zwei.iot.device.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.Product;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.tsl.ProductTsl;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.ProductMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.IProductTslService;
import com.zwei.iot.device.tsl.TslBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ProductTslServiceImpl implements IProductTslService {

    private static final String TSL_VERSION = "1.0";

    private final ProductMapper productMapper;
    private final DeviceMapper deviceMapper;
    private final DeviceSensorMapper sensorMapper;
    private final SensorAttributeMapper attributeMapper;
    private final TslBuilder tslBuilder;
    private final ObjectMapper objectMapper;

    @Autowired
    public ProductTslServiceImpl(ProductMapper productMapper,
                                  DeviceMapper deviceMapper,
                                  DeviceSensorMapper sensorMapper,
                                  SensorAttributeMapper attributeMapper,
                                  TslBuilder tslBuilder,
                                  ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.deviceMapper = deviceMapper;
        this.sensorMapper = sensorMapper;
        this.attributeMapper = attributeMapper;
        this.tslBuilder = tslBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProductTsl getByProductKey(String productKey) {
        Product product = productMapper.selectByProductKey(productKey);
        if (product == null) {
            throw new ServiceException("产品物模型不存在: productKey=" + productKey);
        }
        return parseTsl(product.getTslJson());
    }

    @Override
    public ProductTsl getByDeviceId(Long deviceId) {
        Product product = productMapper.selectByDeviceId(deviceId);
        if (product == null) {
            throw new ServiceException("产品物模型不存在: deviceId=" + deviceId);
        }
        return parseTsl(product.getTslJson());
    }

    @Override
    public void regenerate(Long deviceId) {
        Device device = deviceMapper.selectDeviceById(deviceId);
        if (device == null) {
            throw new ServiceException("设备不存在: id=" + deviceId);
        }

        List<DeviceSensor> sensors = sensorMapper.selectSensorListByDeviceId(deviceId);
        List<SensorAttribute> allAttrs = new ArrayList<>();
        if (sensors != null) {
            for (DeviceSensor sensor : sensors) {
                List<SensorAttribute> attrs =
                        attributeMapper.selectAttributeListBySensorId(sensor.getId());
                if (attrs != null) {
                    allAttrs.addAll(attrs);
                }
            }
        }

        ProductTsl tsl = tslBuilder.build(device.getCode(), allAttrs);
        String tslJson = toJson(tsl);

        Product existing = productMapper.selectByDeviceId(deviceId);
        if (existing != null) {
            existing.setTslJson(tslJson);
            existing.setTslVersion(TSL_VERSION);
            productMapper.upsert(existing);
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
            throw new ServiceException("TSL JSON 解析失败", e);
        }
    }

    private String toJson(ProductTsl tsl) {
        try {
            return objectMapper.writeValueAsString(tsl);
        } catch (JsonProcessingException e) {
            throw new ServiceException("TSL JSON 序列化失败", e);
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd server && mvn test -pl zwei-iot-device -Dtest=ProductTslServiceImplTest`
Expected: PASS (6/6 tests)

- [ ] **Step 6: Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/IProductTslService.java
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/ProductTslServiceImpl.java
git add server/zwei-iot-device/src/test/java/com/zwei/iot/device/service/impl/ProductTslServiceImplTest.java
git commit -m "feat: add IProductTslService + ProductTslServiceImpl with regenerate logic"
```

---

### Task 6: Wire TSL regeneration into DeviceSensorServiceImpl

**Files:**
- Modify: `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceSensorServiceImpl.java`

Add `IProductTslService` as a constructor dependency and call `regenerate(deviceId)` at the end of `insertSensor`, `updateSensor`, and `deleteSensorById`.

- [ ] **Step 1: Add IProductTslService to constructor**

In `DeviceSensorServiceImpl.java`, add the field:

```java
private final IProductTslService productTslService;
```

And add it to the constructor parameter list and assignment:

```java
@Autowired
public DeviceSensorServiceImpl(DeviceMapper deviceMapper,
                               DeviceSensorMapper sensorMapper,
                               SensorAttributeMapper attributeMapper,
                               IMonitorTypeService monitorTypeService,
                               IMonitorContentService monitorContentService,
                               ITimeSeriesSchemaService timeSeriesSchemaService,
                               IProductTslService productTslService) {
    // ... existing assignments ...
    this.productTslService = productTslService;
}
```

- [ ] **Step 2: Add regenerate call in insertSensor**

At the end of `insertSensor`, after `timeSeriesSchemaService.createSensorSchema(...)`, add:

```java
productTslService.regenerate(sensor.getDeviceId());
```

- [ ] **Step 3: Add regenerate call in updateSensor**

At the end of `updateSensor`, after the attribute update loop and before `return rows;`, add:

```java
productTslService.regenerate(existing.getDeviceId());
```

- [ ] **Step 4: Add regenerate call in deleteSensorById**

At the end of `deleteSensorById`, after `sensorMapper.deleteSensorById(id)`, add:

```java
// Query deviceId before the sensor is logically deleted for regenerate
DeviceSensor sensor = sensorMapper.selectSensorById(id);
if (sensor != null) {
    productTslService.regenerate(sensor.getDeviceId());
}
```

Wait — `deleteSensorById` deletes attrs first, then the sensor. After delete, `selectSensorById` may return a deleted sensor (logical delete adds `#DEL#` suffix). The existing code returns the delete count but not the sensor. Let me check the current implementation again.

Looking at the current `deleteSensorById`:
```java
public int deleteSensorById(Long id) {
    attributeMapper.deleteAttributeBySensorId(id);
    return sensorMapper.deleteSensorById(id);
}
```

The deviceId is not returned. I need to query the sensor first (before deletion) to get deviceId. Let me adjust:

```java
@Override
@Transactional
public int deleteSensorById(Long id) {
    DeviceSensor sensor = sensorMapper.selectSensorById(id);
    if (sensor == null) {
        throw new ServiceException("传感器不存在");
    }
    attributeMapper.deleteAttributeBySensorId(id);
    int rows = sensorMapper.deleteSensorById(id);
    productTslService.regenerate(sensor.getDeviceId());
    return rows;
}
```

- [ ] **Step 5: Add import**

Ensure `import com.zwei.iot.device.service.IProductTslService;` is present.

- [ ] **Step 6: Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceSensorServiceImpl.java
git commit -m "feat: wire TSL regeneration into DeviceSensorServiceImpl create/update/delete"
```

---

### Task 7: Wire TSL regeneration into DeviceServiceImpl (copy) and DeviceRegistryServiceImpl (register)

**Files:**
- Modify: `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceServiceImpl.java`
- Modify: `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceRegistryServiceImpl.java`

- [ ] **Step 1: Wire into DeviceServiceImpl.copyDevice**

Locate the `copyDevice` method in `DeviceServiceImpl`. At the end, after the new device and its sensors are persisted, add:

```java
productTslService.regenerate(newDevice.getId());
```

Add `IProductTslService productTslService` to the constructor and field declarations.

- [ ] **Step 2: Wire into DeviceRegistryServiceImpl.register**

Locate the `register` method in `DeviceRegistryServiceImpl`. At the end, after the device and sensors are created and IoTDB schema is pre-created, add:

```java
productTslService.regenerate(device.getId());
```

Add `IProductTslService productTslService` to the constructor and field declarations.

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceServiceImpl.java
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/impl/DeviceRegistryServiceImpl.java
git commit -m "feat: wire TSL regeneration into device copy and self-registration"
```

---

### Task 8: DB upgrade script + Migration runner

**Files:**
- Create: `db/upgrade/v2.1_add_product_tsl.sql`
- Create: `server/zwei-iot-device/src/main/java/com/zwei/iot/device/migration/ProductTslMigrationRunner.java`

- [ ] **Step 1: Create DB upgrade script (DDL only)**

```sql
-- v2.1: Add product table for TSL thing model
-- Run: mysql -u root -p geo_hazard_monitor < db/upgrade/v2.1_add_product_tsl.sql

CREATE TABLE IF NOT EXISTS `product` (
  `id`            bigint       NOT NULL AUTO_INCREMENT,
  `product_key`   varchar(64)  NOT NULL COMMENT '产品唯一标识，由device.code哈希生成',
  `device_id`     bigint       NOT NULL COMMENT '关联设备ID，当前1:1',
  `tsl_json`      json         NOT NULL COMMENT '完整TSL JSON（properties/events/services）',
  `tsl_version`   varchar(32)  DEFAULT '1.0' COMMENT 'TSL版本号',
  `create_by`     varchar(64)  DEFAULT NULL,
  `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP,
  `update_by`     varchar(64)  DEFAULT NULL,
  `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag`      tinyint      DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_key` (`product_key`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_product_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品物模型表';
```

- [ ] **Step 2: Create ProductTslMigrationRunner**

```java
package com.zwei.iot.device.migration;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.ProductMapper;
import com.zwei.iot.device.service.IProductTslService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1) // Run early but after DB and MyBatis are ready
@Slf4j
public class ProductTslMigrationRunner implements ApplicationRunner {

    private final ProductMapper productMapper;
    private final DeviceMapper deviceMapper;
    private final IProductTslService productTslService;

    @Autowired
    public ProductTslMigrationRunner(ProductMapper productMapper,
                                      DeviceMapper deviceMapper,
                                      IProductTslService productTslService) {
        this.productMapper = productMapper;
        this.deviceMapper = deviceMapper;
        this.productTslService = productTslService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int existingProducts = productMapper.countAll();
        if (existingProducts > 0) {
            log.info("Product TSL migration already completed ({} products exist), skipping.", existingProducts);
            return;
        }

        List<Device> devices = deviceMapper.selectDeviceAll();
        if (devices == null || devices.isEmpty()) {
            log.info("No devices found, skipping TSL migration.");
            return;
        }

        log.info("Starting TSL migration for {} devices...", devices.size());
        int migrated = 0;
        int failed = 0;

        for (Device device : devices) {
            try {
                productTslService.regenerate(device.getId());
                migrated++;
            } catch (Exception e) {
                log.error("Failed to migrate TSL for device id={} code={}: {}",
                        device.getId(), device.getCode(), e.getMessage());
                failed++;
            }
        }

        log.info("TSL migration complete: {} migrated, {} failed out of {} devices.",
                migrated, failed, devices.size());
    }
}
```

- [ ] **Step 3: Run full test suite to verify no regressions**

Run: `cd server && mvn test -pl zwei-iot-device`
Expected: All existing tests + new tests PASS

- [ ] **Step 4: Commit**

```bash
git add db/upgrade/v2.1_add_product_tsl.sql
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/migration/ProductTslMigrationRunner.java
git commit -m "feat: add DB upgrade script and migration runner for product TSL"
```

---

### Task 9: Build verification

- [ ] **Step 1: Full compile**

Run: `cd server && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all device module tests**

Run: `cd server && mvn test -pl zwei-iot-device`
Expected: All tests PASS

- [ ] **Step 3: Final commit if any cleanup needed**

Only if there are formatting or minor fixes from the build verification step.

---

## Verification Checklist

- [ ] `TslBuilder.generateProductKey("DEV001")` produces stable `"p_"` + 12 hex chars
- [ ] `ProductTslServiceImpl.regenerate(deviceId)` inserts new Product when none exists
- [ ] `ProductTslServiceImpl.regenerate(deviceId)` upserts when Product already exists
- [ ] Creating/deleting a sensor triggers TSL regeneration in same transaction
- [ ] ProductTslMigrationRunner skips when products already exist
- [ ] ProductTslMigrationRunner handles empty device table gracefully
- [ ] All existing device/sensor CRUD tests still pass
- [ ] TSL JSON serialization produces valid JSON matching the spec format
- [ ] `mvn clean compile` passes across all modules
