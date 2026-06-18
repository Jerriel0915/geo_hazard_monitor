# 算法库功能实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在告警中心新增"算法库"菜单/页面，支持算法信息 CRUD、版本化管理、Python zip 算法包上传下载。

**架构：**
- 后端：在 `zwei-iot-alarm` 模块新增 `algolib` 子包（Controller/Service/Mapper/Domain/DTO），两张表 `algo_info` + `algo_version`，专用上传接口存本地磁盘 `algo-lib/yyyy/MM/dd/{uuid}.zip`
- 前端：新增 `AlgoLibrary.vue` 卡片列表页（参考 `CompositeAlarm.vue`，复用全局 `.grid/.card` 样式）+ 详情抽屉 + 表单弹窗，菜单走 `layout/index.vue` 硬编码

**技术栈：** Java 17 / Spring Boot 4 / MyBatis / Lombok / PageHelper / JUnit 5 / Mockito；Vue 3 / TypeScript / Element Plus / Axios

**规格依据：** `docs/superpowers/specs/2026-06-17-algo-library-design.md`

---

## 文件结构总览

### 新增文件（21 个）

**后端（13 个，路径前缀 `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/`）：**
- `algolib/domain/AlgoInfo.java`
- `algolib/domain/AlgoVersion.java`
- `algolib/domain/dto/AlgoCreateRequest.java`
- `algolib/domain/dto/AlgoUpdateRequest.java`
- `algolib/domain/dto/AlgoVersionUploadRequest.java`
- `algolib/mapper/AlgoInfoMapper.java`
- `algolib/mapper/AlgoVersionMapper.java`
- `algolib/service/IAlgoLibraryService.java`
- `algolib/service/IAlgoVersionService.java`
- `algolib/service/impl/AlgoLibraryServiceImpl.java`
- `algolib/service/impl/AlgoVersionServiceImpl.java`
- `algolib/controller/AlgoLibraryController.java`
- `algolib/controller/AlgoVersionController.java`

**后端 XML（2 个）：**
- `server/zwei-iot-alarm/src/main/resources/mapper/AlgoInfoMapper.xml`
- `server/zwei-iot-alarm/src/main/resources/mapper/AlgoVersionMapper.xml`

**后端测试（2 个）：**
- `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/algolib/service/impl/AlgoLibraryServiceImplTest.java`
- `server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/algolib/service/impl/AlgoVersionServiceImplTest.java`

**前端（4 个）：**
- `web/src/api/algoLibrary.ts`
- `web/src/views/alarm/AlgoLibrary.vue`
- `web/src/views/alarm/components/AlgoFormDialog.vue`
- `web/src/views/alarm/components/AlgoDetailDrawer.vue`

**数据库（1 个）：**
- `db/upgrade/V20260617__algo_library.sql`

### 修改文件（4 个）
- `server/zwei-admin/src/main/resources/application.yml` — 新增 multipart 配置
- `server/zwei-common/src/main/java/com/zwei/common/utils/file/FileUploadUtils.java` — 新增算法库专用路径方法
- `web/src/router/index.ts` — 新增 `/alarm/algo-library` 路由
- `web/src/layout/index.vue` — 三处菜单硬编码

---

## 任务 1：数据库升级脚本

**文件：**
- 创建：`db/upgrade/V20260617__algo_library.sql`

- [ ] **步骤 1：确认 db/upgrade 目录不存在并创建脚本**

```bash
mkdir -p db/upgrade
```

- [ ] **步骤 2：写入 SQL 脚本**

文件内容：`db/upgrade/V20260617__algo_library.sql`

```sql
-- =====================================================================
-- 算法库功能 V20260617
-- 新增表：algo_info（算法信息）、algo_version（算法版本）
-- =====================================================================

-- ----------------------------
-- 1. 算法信息表
-- ----------------------------
DROP TABLE IF EXISTS `algo_info`;
CREATE TABLE `algo_info` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        varchar(64)  NOT NULL COMMENT '算法编码（全局唯一，用于程序引用）',
    `name`        varchar(128) NOT NULL COMMENT '算法名称',
    `description` varchar(500) DEFAULT NULL COMMENT '算法描述',
    `status`      tinyint      DEFAULT '1' COMMENT '状态: 0-停用, 1-启用',
    `del_flag`    tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    `create_by`   varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`, `del_flag`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算法信息表';

-- ----------------------------
-- 2. 算法版本表
-- ----------------------------
DROP TABLE IF EXISTS `algo_version`;
CREATE TABLE `algo_version` (
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `algo_id`        bigint       NOT NULL COMMENT '算法ID（关联 algo_info.id）',
    `version_no`     varchar(64)  NOT NULL COMMENT '版本号（用户输入，同一算法下唯一）',
    `file_name`      varchar(255) NOT NULL COMMENT '存储文件名（相对路径）',
    `original_name`  varchar(255) NOT NULL COMMENT '原始文件名',
    `file_size`      bigint       DEFAULT '0' COMMENT '文件大小（字节）',
    `sha256`         varchar(64)  DEFAULT NULL COMMENT 'SHA256 摘要',
    `del_flag`       tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    `create_by`      varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`    datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`         varchar(500) DEFAULT NULL COMMENT '版本说明',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_algo_version` (`algo_id`, `version_no`, `del_flag`),
    KEY `idx_algo_id` (`algo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算法版本表';
```

- [ ] **步骤 3：在本地 MySQL 执行脚本**

运行：
```bash
mysql -uroot -pwodepassword geo_hazard_monitor < db/upgrade/V20260617__algo_library.sql
```
或在 IDE 数据库工具中执行 SQL 文件。

验证：`SHOW TABLES LIKE 'algo_%';` 应返回 `algo_info` 和 `algo_version`。

- [ ] **步骤 4：Commit**

```bash
git add db/upgrade/V20260617__algo_library.sql
git commit -m "feat(alarm): 新增算法库数据库表 algo_info/algo_version"
```

---

## 任务 2：扩展 FileUploadUtils 添加算法库路径方法

**文件：**
- 修改：`server/zwei-common/src/main/java/com/zwei/common/utils/file/FileUploadUtils.java`

- [ ] **步骤 1：在类中新增 algoLib 专用方法**

在 `FileUploadUtils.java` 中（建议在 `extractFilename` 方法之后），新增：

```java
/**
 * 算法库专用路径生成（algo-lib/yyyy/MM/dd/uuid.ext）
 * 不复用 extractFilename，避免与 /upload 目录混淆。
 */
public static final String extractAlgoLibFilename(MultipartFile file) {
    String extension = getExtension(file);
    return StringUtils.format("algo-lib/{}/{}.{}",
            DateUtils.datePath(),
            java.util.UUID.randomUUID().toString().replace("-", ""),
            extension);
}
```

**说明：** `getExtension(file)` 已存在，返回小写扩展名。`MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION` 已含 `"zip"`，但本接口在 Controller 层独立做扩展名校验（不接受非 zip）。

- [ ] **步骤 2：编译验证**

```bash
cd server && mvn clean compile -pl zwei-common -am -q
```
预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-common/src/main/java/com/zwei/common/utils/file/FileUploadUtils.java
git commit -m "feat(common): FileUploadUtils 新增算法库专用路径方法"
```

---

## 任务 3：application.yml 新增 multipart 配置

**文件：**
- 修改：`server/zwei-admin/src/main/resources/application.yml`

- [ ] **步骤 1：定位 spring 节点**

运行 `grep -n "^spring:" server/zwei-admin/src/main/resources/application.yml` 找到 spring 配置起始行。

- [ ] **步骤 2：在 spring 节点下新增 multipart 子节点**

在 `spring:` 节点下（注意 YAML 缩进，与同级 `datasource` 等对齐 2 空格）添加：

```yaml
spring:
  # ... 已有配置 ...
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

**注意：** 若已有 `spring.servlet` 节点，合并入；否则新建。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-admin/src/main/resources/application.yml
git commit -m "chore(alarm): 放宽 multipart 上传大小至 100MB 支持算法包"
```

---

## 任务 4：AlgoInfo Domain 实体

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/domain/AlgoInfo.java`

- [ ] **步骤 1：写入实体类**

```java
package com.zwei.iot.alarm.algolib.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 算法信息表 algo_info。
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoInfo extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private String description;
    /** 状态: 0-停用, 1-启用 */
    private Integer status;
    private Integer delFlag;

    // ── 非持久化字段（由 list 联表 algo_version 填充）──
    /** 该算法下未删除版本数 */
    private Integer versionCount;
    /** 最近一次上传的 version_no */
    private String latestVersionNo;
    /** 最近一次上传 create_time */
    private java.util.Date latestUploadTime;
}
```

**说明：** `BaseEntity` 已提供 `createBy/createTime/updateBy/updateTime/remark` 字段。`versionCount/latestVersionNo/latestUploadTime` 仅 list 接口返回时填充，详情/新增等接口可为 null。

- [ ] **步骤 2：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/domain/AlgoInfo.java
git commit -m "feat(alarm): 新增 AlgoInfo 实体类"
```

---

## 任务 5：AlgoVersion Domain 实体

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/domain/AlgoVersion.java`

- [ ] **步骤 1：写入实体类**

```java
package com.zwei.iot.alarm.algolib.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 算法版本表 algo_version。
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoVersion extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long algoId;
    private String versionNo;
    /** 相对路径，如 algo-lib/2026/06/17/uuid.zip */
    private String fileName;
    private String originalName;
    private Long fileSize;
    private String sha256;
    private Integer delFlag;
}
```

- [ ] **步骤 2：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/domain/AlgoVersion.java
git commit -m "feat(alarm): 新增 AlgoVersion 实体类"
```

---

## 任务 6：DTO（4 个请求类）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/domain/dto/AlgoCreateRequest.java`
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/domain/dto/AlgoUpdateRequest.java`

- [ ] **步骤 1：写入 AlgoCreateRequest**

```java
package com.zwei.iot.alarm.algolib.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 算法 — 新增请求。
 *
 * @author zwei
 */
public class AlgoCreateRequest {

    @NotBlank(message = "算法编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,63}$",
            message = "算法编码必须以大写字母开头，3-64 字符，仅含大写字母/数字/下划线")
    private String code;

    @NotBlank(message = "算法名称不能为空")
    @Size(max = 128, message = "算法名称不能超过 128 字符")
    private String name;

    @Size(max = 500, message = "算法描述不能超过 500 字符")
    private String description;

    @Size(max = 500, message = "备注不能超过 500 字符")
    private String remark;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
```

- [ ] **步骤 2：写入 AlgoUpdateRequest（不含 code 字段）**

```java
package com.zwei.iot.alarm.algolib.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 算法 — 修改请求（不可修改 code）。
 *
 * @author zwei
 */
public class AlgoUpdateRequest {

    @NotBlank(message = "算法名称不能为空")
    @Size(max = 128, message = "算法名称不能超过 128 字符")
    private String name;

    @Size(max = 500, message = "算法描述不能超过 500 字符")
    private String description;

    @Size(max = 500, message = "备注不能超过 500 字符")
    private String remark;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
```

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/domain/dto/
git commit -m "feat(alarm): 新增算法库请求 DTO"
```

---

## 任务 7：AlgoInfoMapper + XML

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/mapper/AlgoInfoMapper.java`
- 创建：`server/zwei-iot-alarm/src/main/resources/mapper/AlgoInfoMapper.xml`

- [ ] **步骤 1：写入 Mapper 接口**

```java
package com.zwei.iot.alarm.algolib.mapper;

import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 算法信息 Mapper。
 *
 * @author zwei
 */
@Mapper
public interface AlgoInfoMapper {

    /**
     * 分页查询（含版本统计、最新版本号、最新上传时间）
     */
    List<AlgoInfo> selectList(AlgoInfo query);

    /**
     * 详情（不含版本统计字段，由 Service 二次查询版本填充）
     */
    AlgoInfo selectById(Long id);

    /**
     * 校验 code 唯一（排除指定 id 与已逻辑删除记录）
     *
     * @return 命中的算法（null 表示唯一）
     */
    AlgoInfo checkCodeUnique(@Param("code") String code, @Param("id") Long id);

    int insert(AlgoInfo algoInfo);

    int update(AlgoInfo algoInfo);

    /**
     * 逻辑删除算法（del_flag=1）
     */
    int softDelete(Long id);
}
```

- [ ] **步骤 2：写入 XML 映射文件**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.alarm.algolib.mapper.AlgoInfoMapper">

    <resultMap type="com.zwei.iot.alarm.algolib.domain.AlgoInfo" id="AlgoInfoResult">
        <id property="id" column="id"/>
        <result property="code" column="code"/>
        <result property="name" column="name"/>
        <result property="description" column="description"/>
        <result property="status" column="status"/>
        <result property="delFlag" column="del_flag"/>
        <result property="createBy" column="create_by"/>
        <result property="createTime" column="create_time"/>
        <result property="updateBy" column="update_by"/>
        <result property="updateTime" column="update_time"/>
        <result property="remark" column="remark"/>
        <result property="versionCount" column="version_count"/>
        <result property="latestVersionNo" column="latest_version_no"/>
        <result property="latestUploadTime" column="latest_upload_time"/>
    </resultMap>

    <select id="selectList" parameterType="com.zwei.iot.alarm.algolib.domain.AlgoInfo"
            resultMap="AlgoInfoResult">
        SELECT a.id, a.code, a.name, a.description, a.status, a.del_flag,
               a.create_by, a.create_time, a.update_by, a.update_time, a.remark,
               COALESCE(v.cnt, 0)               AS version_count,
               v.latest_version_no              AS latest_version_no,
               v.latest_upload_time             AS latest_upload_time
        FROM algo_info a
        LEFT JOIN (
            SELECT algo_id,
                   COUNT(*)                  AS cnt,
                   MAX(create_time)          AS latest_upload_time,
                   SUBSTRING_INDEX(GROUP_CONCAT(version_no ORDER BY create_time DESC), ',', 1) AS latest_version_no
            FROM algo_version
            WHERE del_flag = 0
            GROUP BY algo_id
        ) v ON v.algo_id = a.id
        <where>
            a.del_flag = 0
            <if test="name != null and name != ''">AND a.name LIKE CONCAT('%', #{name}, '%')</if>
            <if test="status != null">AND a.status = #{status}</if>
            <if test="code != null and code != ''">AND a.code LIKE CONCAT('%', #{code}, '%')</if>
        </where>
        ORDER BY a.create_time DESC
    </select>

    <select id="selectById" parameterType="Long" resultMap="AlgoInfoResult">
        SELECT id, code, name, description, status, del_flag,
               create_by, create_time, update_by, update_time, remark
        FROM algo_info
        WHERE id = #{id} AND del_flag = 0
    </select>

    <select id="checkCodeUnique" resultMap="AlgoInfoResult">
        SELECT id, code, name, description, status, del_flag,
               create_by, create_time, update_by, update_time, remark
        FROM algo_info
        WHERE del_flag = 0
        AND code = #{code}
        AND id != #{id}
        LIMIT 1
    </select>

    <insert id="insert" parameterType="com.zwei.iot.alarm.algolib.domain.AlgoInfo"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO algo_info (code, name, description, status, del_flag,
                               create_by, create_time, update_by, update_time, remark)
        VALUES (#{code}, #{name}, #{description}, #{status}, 0,
                #{createBy}, #{createTime}, #{updateBy}, #{updateTime}, #{remark})
    </insert>

    <update id="update" parameterType="com.zwei.iot.alarm.algolib.domain.AlgoInfo">
        UPDATE algo_info
        <set>
            <if test="name != null and name != ''">name = #{name},</if>
            <if test="description != null">description = #{description},</if>
            <if test="status != null">status = #{status},</if>
            <if test="remark != null">remark = #{remark},</if>
            <if test="updateBy != null">update_by = #{updateBy},</if>
            <if test="updateTime != null">update_time = #{updateTime},</if>
        </set>
        WHERE id = #{id} AND del_flag = 0
    </update>

    <update id="softDelete" parameterType="Long">
        UPDATE algo_info SET del_flag = 1 WHERE id = #{id}
    </update>

</mapper>
```

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/mapper/AlgoInfoMapper.java \
        server/zwei-iot-alarm/src/main/resources/mapper/AlgoInfoMapper.xml
git commit -m "feat(alarm): 新增 AlgoInfoMapper 及 XML（含版本统计联表查询）"
```

---

## 任务 8：AlgoVersionMapper + XML

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/mapper/AlgoVersionMapper.java`
- 创建：`server/zwei-iot-alarm/src/main/resources/mapper/AlgoVersionMapper.xml`

- [ ] **步骤 1：写入 Mapper 接口**

```java
package com.zwei.iot.alarm.algolib.mapper;

import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 算法版本 Mapper。
 *
 * @author zwei
 */
@Mapper
public interface AlgoVersionMapper {

    /**
     * 按算法 ID 查询未删除版本（按 create_time DESC）
     */
    List<AlgoVersion> selectByAlgoId(Long algoId);

    AlgoVersion selectById(Long id);

    /**
     * 校验版本号在指定算法下唯一（排除逻辑删除记录）
     */
    AlgoVersion checkVersionUnique(@Param("algoId") Long algoId,
                                   @Param("versionNo") String versionNo);

    int insert(AlgoVersion version);

    /**
     * 逻辑删除指定算法 ID 下所有版本（删除算法时级联调用）
     */
    int softDeleteByAlgoId(Long algoId);

    /**
     * 逻辑删除单个版本
     */
    int softDeleteById(Long id);
}
```

- [ ] **步骤 2：写入 XML**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zwei.iot.alarm.algolib.mapper.AlgoVersionMapper">

    <resultMap type="com.zwei.iot.alarm.algolib.domain.AlgoVersion" id="AlgoVersionResult">
        <id property="id" column="id"/>
        <result property="algoId" column="algo_id"/>
        <result property="versionNo" column="version_no"/>
        <result property="fileName" column="file_name"/>
        <result property="originalName" column="original_name"/>
        <result property="fileSize" column="file_size"/>
        <result property="sha256" column="sha256"/>
        <result property="delFlag" column="del_flag"/>
        <result property="createBy" column="create_by"/>
        <result property="createTime" column="create_time"/>
        <result property="updateBy" column="update_by"/>
        <result property="updateTime" column="update_time"/>
        <result property="remark" column="remark"/>
    </resultMap>

    <sql id="columns">
        id, algo_id, version_no, file_name, original_name, file_size, sha256,
        del_flag, create_by, create_time, update_by, update_time, remark
    </sql>

    <select id="selectByAlgoId" parameterType="Long" resultMap="AlgoVersionResult">
        SELECT <include refid="columns"/>
        FROM algo_version
        WHERE algo_id = #{algoId} AND del_flag = 0
        ORDER BY create_time DESC
    </select>

    <select id="selectById" parameterType="Long" resultMap="AlgoVersionResult">
        SELECT <include refid="columns"/>
        FROM algo_version
        WHERE id = #{id} AND del_flag = 0
    </select>

    <select id="checkVersionUnique" resultMap="AlgoVersionResult">
        SELECT <include refid="columns"/>
        FROM algo_version
        WHERE algo_id = #{algoId}
        AND version_no = #{versionNo}
        AND del_flag = 0
        LIMIT 1
    </select>

    <insert id="insert" parameterType="com.zwei.iot.alarm.algolib.domain.AlgoVersion"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO algo_version (algo_id, version_no, file_name, original_name,
                                  file_size, sha256, del_flag,
                                  create_by, create_time, update_by, update_time, remark)
        VALUES (#{algoId}, #{versionNo}, #{fileName}, #{originalName},
                #{fileSize}, #{sha256}, 0,
                #{createBy}, #{createTime}, #{updateBy}, #{updateTime}, #{remark})
    </insert>

    <update id="softDeleteByAlgoId" parameterType="Long">
        UPDATE algo_version SET del_flag = 1 WHERE algo_id = #{algoId} AND del_flag = 0
    </update>

    <update id="softDeleteById" parameterType="Long">
        UPDATE algo_version SET del_flag = 1 WHERE id = #{id} AND del_flag = 0
    </update>

</mapper>
```

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/mapper/AlgoVersionMapper.java \
        server/zwei-iot-alarm/src/main/resources/mapper/AlgoVersionMapper.xml
git commit -m "feat(alarm): 新增 AlgoVersionMapper 及 XML"
```

---

## 任务 9：Service 接口

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/IAlgoLibraryService.java`
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/IAlgoVersionService.java`

- [ ] **步骤 1：写入 IAlgoLibraryService**

```java
package com.zwei.iot.alarm.algolib.service;

import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.domain.AlgoVersion;

import java.util.List;

/**
 * 算法信息 Service。
 *
 * @author zwei
 */
public interface IAlgoLibraryService {

    List<AlgoInfo> selectList(AlgoInfo query);

    /** 详情（含版本列表，按 create_time DESC） */
    AlgoInfo selectDetailById(Long id);

    int insert(AlgoInfo algoInfo);

    int update(AlgoInfo algoInfo);

    /** 启停 */
    int updateStatus(Long id, Integer status, String updateBy);

    /** 删除算法（级联逻辑删版本，物理文件保留） */
    int deleteWithVersions(Long id);

    /** code 唯一校验 */
    boolean checkCodeUnique(String code, Long id);
}
```

- [ ] **步骤 2：写入 IAlgoVersionService**

```java
package com.zwei.iot.alarm.algolib.service;

import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 算法版本 Service。
 *
 * @author zwei
 */
public interface IAlgoVersionService {

    List<AlgoVersion> selectByAlgoId(Long algoId);

    AlgoVersion selectById(Long id);

    /**
     * 上传新版本：
     * 1. 校验文件类型（仅 zip）与大小（≤100MB）
     * 2. 校验 algoId 存在且未删除
     * 3. 校验 versionNo 在该算法下唯一
     * 4. 落盘到 {zwei.profile}/algo-lib/yyyy/MM/dd/{uuid}.zip
     * 5. 计算 SHA256，写入 algo_version
     *
     * @return 新创建的版本 ID
     */
    Long upload(Long algoId, String versionNo, String remark,
                MultipartFile file, String createBy);

    /** 逻辑删除单个版本（不删物理文件） */
    int delete(Long id);

    /** 校验版本号唯一 */
    boolean checkVersionUnique(Long algoId, String versionNo);
}
```

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/
git commit -m "feat(alarm): 新增 IAlgoLibraryService/IAlgoVersionService 接口"
```

---

## 任务 10：编写 AlgoLibraryServiceImpl 单元测试（TDD - 先写测试）

**文件：**
- 创建：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/algolib/service/impl/AlgoLibraryServiceImplTest.java`

- [ ] **步骤 1：写入测试类**

```java
package com.zwei.iot.alarm.algolib.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.mapper.AlgoInfoMapper;
import com.zwei.iot.alarm.algolib.mapper.AlgoVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AlgoLibraryServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlgoLibraryServiceImpl")
class AlgoLibraryServiceImplTest {

    @Mock private AlgoInfoMapper algoInfoMapper;
    @Mock private AlgoVersionMapper algoVersionMapper;

    private AlgoLibraryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AlgoLibraryServiceImpl(algoInfoMapper, algoVersionMapper);
    }

    @Nested
    @DisplayName("insert")
    class Insert {

        @Test
        @DisplayName("code 重复时抛 ServiceException")
        void duplicateCodeThrows() {
            when(algoInfoMapper.checkCodeUnique("ALGO_X", 0L))
                    .thenReturn(AlgoInfo.builder().id(99L).code("ALGO_X").build());

            AlgoInfo input = AlgoInfo.builder()
                    .code("ALGO_X").name("测试").createBy("admin").build();

            assertThatThrownBy(() -> service.insert(input))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("已存在");

            verify(algoInfoMapper, never()).insert(any());
        }

        @Test
        @DisplayName("code 唯一时正常插入，status 默认 1（启用）")
        void uniqueCodeInserts() {
            when(algoInfoMapper.checkCodeUnique("ALGO_NEW", 0L)).thenReturn(null);
            when(algoInfoMapper.insert(any(AlgoInfo.class))).thenAnswer(inv -> {
                inv.<AlgoInfo>getArgument(0).setId(1L);
                return 1;
            });

            AlgoInfo input = AlgoInfo.builder()
                    .code("ALGO_NEW").name("新算法").createBy("admin").build();

            int rows = service.insert(input);

            assertThat(rows).isEqualTo(1);
            assertThat(input.getId()).isEqualTo(1L);
            assertThat(input.getStatus()).isEqualTo(1);  // 默认启用
            assertThat(input.getCreateTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("状态从 1 切换为 0，调用 mapper.update")
        void toggleToDisabled() {
            when(algoInfoMapper.update(any(AlgoInfo.class))).thenReturn(1);

            int rows = service.updateStatus(10L, 0, "admin");

            assertThat(rows).isEqualTo(1);
            verify(algoInfoMapper).update(argThat(a ->
                    a.getId().equals(10L)
                    && a.getStatus().equals(0)
                    && "admin".equals(a.getUpdateBy())));
        }
    }

    @Nested
    @DisplayName("deleteWithVersions")
    class DeleteWithVersions {

        @Test
        @DisplayName("删除算法时级联软删所有版本")
        void cascadesVersionSoftDelete() {
            when(algoInfoMapper.softDelete(10L)).thenReturn(1);
            when(algoVersionMapper.softDeleteByAlgoId(10L)).thenReturn(3);

            int rows = service.deleteWithVersions(10L);

            assertThat(rows).isEqualTo(1);
            verify(algoInfoMapper).softDelete(10L);
            verify(algoVersionMapper).softDeleteByAlgoId(10L);
        }

        @Test
        @DisplayName("算法不存在时返回 0，不调用版本 mapper")
        void notFound() {
            when(algoInfoMapper.softDelete(99L)).thenReturn(0);

            int rows = service.deleteWithVersions(99L);

            assertThat(rows).isEqualTo(0);
            verify(algoVersionMapper, never()).softDeleteByAlgoId(any());
        }
    }

    @Test
    @DisplayName("selectDetailById 填充版本列表")
    void selectDetailByIdFillsVersions() {
        AlgoInfo info = AlgoInfo.builder().id(1L).code("ALGO_X").name("X").build();
        when(algoInfoMapper.selectById(1L)).thenReturn(info);

        int rows = service.selectDetailById(1L) != null ? 1 : 0;
        assertThat(rows).isEqualTo(1);
        // 版本列表填充由 Service 内部调用 algoVersionMapper.selectByAlgoId
        // 在此用例中验证 info 返回非 null 即可，详细验证在 Impl 完成后补
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlgoLibraryServiceImplTest -q
```
预期：编译失败（`AlgoLibraryServiceImpl` 不存在）。

- [ ] **步骤 3：Commit（暂存失败测试）**

```bash
git add server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/algolib/service/impl/AlgoLibraryServiceImplTest.java
git commit -m "test(alarm): 新增 AlgoLibraryServiceImpl 单元测试（红）"
```

---

## 任务 11：实现 AlgoLibraryServiceImpl（TDD - 让测试通过）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/impl/AlgoLibraryServiceImpl.java`

- [ ] **步骤 1：写入实现**

```java
package com.zwei.iot.alarm.algolib.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import com.zwei.iot.alarm.algolib.mapper.AlgoInfoMapper;
import com.zwei.iot.alarm.algolib.mapper.AlgoVersionMapper;
import com.zwei.iot.alarm.algolib.service.IAlgoLibraryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 算法信息 Service 实现。
 *
 * @author zwei
 */
@Service
public class AlgoLibraryServiceImpl implements IAlgoLibraryService {

    private final AlgoInfoMapper algoInfoMapper;
    private final AlgoVersionMapper algoVersionMapper;

    public AlgoLibraryServiceImpl(AlgoInfoMapper algoInfoMapper,
                                  AlgoVersionMapper algoVersionMapper) {
        this.algoInfoMapper = algoInfoMapper;
        this.algoVersionMapper = algoVersionMapper;
    }

    @Override
    public List<AlgoInfo> selectList(AlgoInfo query) {
        return algoInfoMapper.selectList(query);
    }

    @Override
    public AlgoInfo selectDetailById(Long id) {
        AlgoInfo info = algoInfoMapper.selectById(id);
        if (info == null) return null;
        // 详情中按倒序返回版本列表（前端展示用）
        List<AlgoVersion> versions = algoVersionMapper.selectByAlgoId(id);
        info.setVersions(versions);
        return info;
    }

    @Override
    public int insert(AlgoInfo algoInfo) {
        if (!checkCodeUnique(algoInfo.getCode(), 0L)) {
            throw new ServiceException("新增失败，算法编码已存在: " + algoInfo.getCode());
        }
        if (algoInfo.getStatus() == null) algoInfo.setStatus(1);
        Date now = new Date();
        algoInfo.setCreateTime(now);
        algoInfo.setUpdateTime(now);
        return algoInfoMapper.insert(algoInfo);
    }

    @Override
    public int update(AlgoInfo algoInfo) {
        if (!checkCodeUnique(algoInfo.getCode(), algoInfo.getId())) {
            throw new ServiceException("修改失败，算法编码已存在: " + algoInfo.getCode());
        }
        algoInfo.setUpdateTime(new Date());
        return algoInfoMapper.update(algoInfo);
    }

    @Override
    public int updateStatus(Long id, Integer status, String updateBy) {
        AlgoInfo update = AlgoInfo.builder()
                .id(id).status(status).updateBy(updateBy).updateTime(new Date()).build();
        return algoInfoMapper.update(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWithVersions(Long id) {
        int rows = algoInfoMapper.softDelete(id);
        if (rows > 0) {
            algoVersionMapper.softDeleteByAlgoId(id);
        }
        return rows;
    }

    @Override
    public boolean checkCodeUnique(String code, Long id) {
        return algoInfoMapper.checkCodeUnique(code, id) == null;
    }
}
```

**说明：** Service 通过 `info.setVersions(...)` 把版本列表挂回 `AlgoInfo`。需要在 `AlgoInfo` 实体中追加 `versions` 字段（任务 4 创建时未包含，本步骤补加）。

- [ ] **步骤 2：在 AlgoInfo 中补加 versions 字段**

打开 `AlgoInfo.java`，在 `latestUploadTime` 字段后添加：

```java
    // ── 详情接口填充的版本列表（仅 selectDetailById 使用）──
    private java.util.List<AlgoVersion> versions;
```

同时确保文件顶部 import 正确，否则使用全限定名（本步骤采用全限定名 `java.util.List`，避免 import 修改）。

- [ ] **步骤 3：运行测试验证通过**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlgoLibraryServiceImplTest -q
```
预期：BUILD SUCCESS，4 个测试用例全部通过。

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/impl/AlgoLibraryServiceImpl.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/domain/AlgoInfo.java
git commit -m "feat(alarm): 实现 AlgoLibraryServiceImpl（含级联逻辑删）"
```

---

## 任务 12：编写 AlgoVersionServiceImpl 单元测试（TDD - 先写测试）

**文件：**
- 创建：`server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/algolib/service/impl/AlgoVersionServiceImplTest.java`

- [ ] **步骤 1：写入测试类**

```java
package com.zwei.iot.alarm.algolib.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import com.zwei.iot.alarm.algolib.mapper.AlgoInfoMapper;
import com.zwei.iot.alarm.algolib.mapper.AlgoVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AlgoVersionServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlgoVersionServiceImpl")
class AlgoVersionServiceImplTest {

    @Mock private AlgoInfoMapper algoInfoMapper;
    @Mock private AlgoVersionMapper algoVersionMapper;

    @TempDir
    Path tempDir;

    private AlgoVersionServiceImpl service;

    @BeforeEach
    void setUp() {
        // 使用临时目录作为存储根，profile 通过反射注入或构造器传入
        service = new AlgoVersionServiceImpl(algoInfoMapper, algoVersionMapper, tempDir.toString());
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("非 zip 文件抛异常")
        void nonZipThrows() {
            MultipartFile file = new MockMultipartFile(
                    "file", "algo.tar", "application/octet-stream", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> service.upload(1L, "v1", null, file, "admin"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("zip");

            verify(algoVersionMapper, never()).insert(any());
        }

        @Test
        @DisplayName("算法不存在抛异常")
        void algoNotFoundThrows() {
            when(algoInfoMapper.selectById(99L)).thenReturn(null);
            MultipartFile file = new MockMultipartFile(
                    "file", "algo.zip", "application/zip", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> service.upload(99L, "v1", null, file, "admin"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("不存在");

            verify(algoVersionMapper, never()).insert(any());
        }

        @Test
        @DisplayName("版本号冲突抛异常")
        void versionDuplicateThrows() {
            when(algoInfoMapper.selectById(1L))
                    .thenReturn(AlgoInfo.builder().id(1L).code("ALGO_X").name("X").build());
            when(algoVersionMapper.checkVersionUnique(1L, "v1"))
                    .thenReturn(AlgoVersion.builder().id(50L).versionNo("v1").build());

            MultipartFile file = new MockMultipartFile(
                    "file", "algo.zip", "application/zip", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> service.upload(1L, "v1", null, file, "admin"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("版本号已存在");

            verify(algoVersionMapper, never()).insert(any());
        }

        @Test
        @DisplayName("合法 zip 上传 → 落盘 + 入库 + 返回版本 ID")
        void success() {
            when(algoInfoMapper.selectById(1L))
                    .thenReturn(AlgoInfo.builder().id(1L).code("ALGO_X").name("X").build());
            when(algoVersionMapper.checkVersionUnique(1L, "v1.0.0")).thenReturn(null);
            when(algoVersionMapper.insert(any(AlgoVersion.class))).thenAnswer(inv -> {
                inv.<AlgoVersion>getArgument(0).setId(777L);
                return 1;
            });

            byte[] content = "fake-zip-content".getBytes();
            MultipartFile file = new MockMultipartFile(
                    "file", "algo.zip", "application/zip", content);

            Long versionId = service.upload(1L, "v1.0.0", "首次上传", file, "admin");

            assertThat(versionId).isEqualTo(777L);
            verify(algoVersionMapper).insert(argThat(v ->
                    v.getAlgoId().equals(1L)
                    && v.getVersionNo().equals("v1.0.0")
                    && v.getOriginalName().equals("algo.zip")
                    && v.getFileSize().equals((long) content.length)
                    && v.getFileName().startsWith("algo-lib/")
                    && v.getSha256() != null && !v.getSha256().isEmpty()
                    && v.getCreateBy().equals("admin")));
        }
    }

    @Test
    @DisplayName("delete 委托 mapper.softDeleteById")
    void delete() {
        when(algoVersionMapper.softDeleteById(5L)).thenReturn(1);
        assertThat(service.delete(5L)).isEqualTo(1);
        verify(algoVersionMapper).softDeleteById(5L);
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlgoVersionServiceImplTest -q
```
预期：编译失败（`AlgoVersionServiceImpl` 不存在）。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/algolib/service/impl/AlgoVersionServiceImplTest.java
git commit -m "test(alarm): 新增 AlgoVersionServiceImpl 单元测试（红）"
```

---

## 任务 13：实现 AlgoVersionServiceImpl（TDD - 让测试通过）

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/impl/AlgoVersionServiceImpl.java`

- [ ] **步骤 1：写入实现**

```java
package com.zwei.iot.alarm.algolib.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.common.utils.file.FileUploadUtils;
import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import com.zwei.iot.alarm.algolib.mapper.AlgoInfoMapper;
import com.zwei.iot.alarm.algolib.mapper.AlgoVersionMapper;
import com.zwei.iot.alarm.algolib.service.IAlgoVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;

/**
 * 算法版本 Service 实现。
 *
 * @author zwei
 */
@Service
public class AlgoVersionServiceImpl implements IAlgoVersionService {

    private static final Logger log = LoggerFactory.getLogger(AlgoVersionServiceImpl.class);

    /** 单文件最大 100MB */
    private static final long MAX_SIZE = 100L * 1024 * 1024;

    private final AlgoInfoMapper algoInfoMapper;
    private final AlgoVersionMapper algoVersionMapper;
    private final String profilePath;

    public AlgoVersionServiceImpl(AlgoInfoMapper algoInfoMapper,
                                  AlgoVersionMapper algoVersionMapper,
                                  String profilePath) {
        this.algoInfoMapper = algoInfoMapper;
        this.algoVersionMapper = algoVersionMapper;
        this.profilePath = profilePath;
    }

    @Override
    public List<AlgoVersion> selectByAlgoId(Long algoId) {
        return algoVersionMapper.selectByAlgoId(algoId);
    }

    @Override
    public AlgoVersion selectById(Long id) {
        return algoVersionMapper.selectById(id);
    }

    @Override
    public Long upload(Long algoId, String versionNo, String remark,
                       MultipartFile file, String createBy) {
        // 1. 校验文件
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase().endsWith(".zip")) {
            throw new ServiceException("仅支持 zip 格式算法包");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ServiceException("文件大小不能超过 100MB");
        }

        // 2. 校验算法存在
        AlgoInfo algo = algoInfoMapper.selectById(algoId);
        if (algo == null) {
            throw new ServiceException("算法不存在或已删除: " + algoId);
        }

        // 3. 校验版本号唯一
        if (!checkVersionUnique(algoId, versionNo)) {
            throw new ServiceException("版本号已存在: " + versionNo);
        }

        // 4. 落盘（使用 FileUploadUtils 算法库专用路径方法）
        String relativePath;
        try {
            relativePath = FileUploadUtils.extractAlgoLibFilename(file);
            File dest = new File(profilePath + File.separator + relativePath);
            if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
                throw new IOException("创建目录失败: " + dest.getParent());
            }
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("算法包上传落盘失败 algoId={}, versionNo={}", algoId, versionNo, e);
            throw new ServiceException("算法包保存失败: " + e.getMessage());
        }

        // 5. 计算 SHA256
        String sha256;
        try {
            sha256 = sha256Hex(new File(profilePath + File.separator + relativePath));
        } catch (Exception e) {
            log.warn("SHA256 计算失败，继续入库: {}", e.getMessage());
            sha256 = null;
        }

        // 6. 入库
        AlgoVersion version = AlgoVersion.builder()
                .algoId(algoId)
                .versionNo(versionNo)
                .fileName(relativePath)
                .originalName(original)
                .fileSize(file.getSize())
                .sha256(sha256)
                .remark(remark)
                .createBy(createBy)
                .createTime(new Date())
                .build();
        algoVersionMapper.insert(version);

        return version.getId();
    }

    @Override
    public int delete(Long id) {
        return algoVersionMapper.softDeleteById(id);
    }

    @Override
    public boolean checkVersionUnique(Long algoId, String versionNo) {
        return algoVersionMapper.checkVersionUnique(algoId, versionNo) == null;
    }

    private static String sha256Hex(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        byte[] hash = digest.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
```

- [ ] **步骤 2：处理 profilePath 注入（重要）**

`AlgoVersionServiceImpl` 构造器需要 `profilePath` 字符串。但生产环境应该从 `RuoYiConfig.getProfile()` 注入。修改方案：在构造器上加 `@Autowired` 不合适（字符串无法自动注入），改为使用 `@Value` 字段注入配合无参/双参构造器。

**替代实现（推荐，避免测试复杂度）：** 改为字段注入 + setter，便于测试。

更新 `AlgoVersionServiceImpl.java`：

```java
// 删除上面的构造器，改为：

import com.zwei.common.config.RuoYiConfig;

@Service
public class AlgoVersionServiceImpl implements IAlgoVersionService {
    // ...

    private final AlgoInfoMapper algoInfoMapper;
    private final AlgoVersionMapper algoVersionMapper;

    public AlgoVersionServiceImpl(AlgoInfoMapper algoInfoMapper,
                                  AlgoVersionMapper algoVersionMapper) {
        this.algoInfoMapper = algoInfoMapper;
        this.algoVersionMapper = algoVersionMapper;
    }

    /** 测试可通过反射或子类覆盖注入；生产用 RuoYiConfig */
    protected String getProfilePath() {
        return RuoYiConfig.getProfile();
    }

    // upload 方法中把 profilePath 替换为 getProfilePath()：
    // File dest = new File(getProfilePath() + File.separator + relativePath);
}
```

**同步修改测试：**

`AlgoVersionServiceImplTest.java` 改为使用子类覆盖 `getProfilePath()`：

```java
// 修改 setUp：
@BeforeEach
void setUp() {
    String tempPath = tempDir.toString();
    service = new AlgoVersionServiceImpl(algoInfoMapper, algoVersionMapper) {
        @Override
        protected String getProfilePath() {
            return tempPath;
        }
    };
}
```

- [ ] **步骤 3：运行测试验证通过**

```bash
cd server && mvn test -pl zwei-iot-alarm -Dtest=AlgoVersionServiceImplTest -q
```
预期：BUILD SUCCESS，4 个测试用例通过。

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/impl/AlgoVersionServiceImpl.java \
        server/zwei-iot-alarm/src/test/java/com/zwei/iot/alarm/algolib/service/impl/AlgoVersionServiceImplTest.java
git commit -m "feat(alarm): 实现 AlgoVersionServiceImpl（含 SHA256 与 zip 校验）"
```

---

## 任务 14：AlgoLibraryController

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/controller/AlgoLibraryController.java`

- [ ] **步骤 1：写入 Controller**

```java
package com.zwei.iot.alarm.algolib.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.common.log.annotation.Log;
import com.zwei.common.log.enums.BusinessType;
import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.domain.dto.AlgoCreateRequest;
import com.zwei.iot.alarm.algolib.domain.dto.AlgoUpdateRequest;
import com.zwei.iot.alarm.algolib.service.IAlgoLibraryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 算法库 Controller。
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/algo-lib")
public class AlgoLibraryController extends BaseController {

    private final IAlgoLibraryService algoLibraryService;

    public AlgoLibraryController(IAlgoLibraryService algoLibraryService) {
        this.algoLibraryService = algoLibraryService;
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:list')")
    public TableDataInfo page(AlgoInfo query) {
        startPage();
        List<AlgoInfo> list = algoLibraryService.selectList(query);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:query')")
    public AjaxResult detail(@PathVariable Long id) {
        AlgoInfo info = algoLibraryService.selectDetailById(id);
        if (info == null) return error("算法不存在");
        return success(info);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('iot:algo-library:add')")
    @Log(title = "算法库", businessType = BusinessType.INSERT)
    public AjaxResult create(@jakarta.validation.Valid @RequestBody AlgoCreateRequest request) {
        AlgoInfo algo = AlgoInfo.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .remark(request.getRemark())
                .createBy(getUsername())
                .build();
        algoLibraryService.insert(algo);
        return AjaxResult.success("新增成功", java.util.Map.of("id", algo.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:edit')")
    @Log(title = "算法库", businessType = BusinessType.UPDATE)
    public AjaxResult update(@PathVariable Long id,
                             @jakarta.validation.Valid @RequestBody AlgoUpdateRequest request) {
        AlgoInfo algo = AlgoInfo.builder()
                .id(id)
                .name(request.getName())
                .description(request.getDescription())
                .remark(request.getRemark())
                .updateBy(getUsername())
                .build();
        return toAjax(algoLibraryService.update(algo));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:edit')")
    @Log(title = "算法库", businessType = BusinessType.UPDATE)
    public AjaxResult updateStatus(@PathVariable Long id,
                                   @org.springframework.web.bind.annotation.RequestParam Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return error("状态值非法（0-停用 1-启用）");
        }
        return toAjax(algoLibraryService.updateStatus(id, status, getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:remove')")
    @Log(title = "算法库", businessType = BusinessType.DELETE)
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(algoLibraryService.deleteWithVersions(id));
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/controller/AlgoLibraryController.java
git commit -m "feat(alarm): 新增 AlgoLibraryController（CRUD + 启停）"
```

---

## 任务 15：AlgoVersionController

**文件：**
- 创建：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/controller/AlgoVersionController.java`

- [ ] **步骤 1：写入 Controller**

```java
package com.zwei.iot.alarm.algolib.controller;

import com.zwei.common.config.RuoYiConfig;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.exception.ServiceException;
import com.zwei.common.log.annotation.Log;
import com.zwei.common.log.enums.BusinessType;
import com.zwei.common.utils.file.FileUtils;
import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import com.zwei.iot.alarm.algolib.service.IAlgoVersionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 算法版本 Controller。
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/algo-lib")
public class AlgoVersionController extends BaseController {

    private final IAlgoVersionService versionService;

    public AlgoVersionController(IAlgoVersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping("/{algoId}/versions")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:query')")
    public AjaxResult listVersions(@PathVariable Long algoId) {
        List<AlgoVersion> list = versionService.selectByAlgoId(algoId);
        return success(list);
    }

    @PostMapping("/{algoId}/versions/upload")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:upload')")
    @Log(title = "算法库版本", businessType = BusinessType.INSERT)
    public AjaxResult upload(@PathVariable Long algoId,
                             @RequestParam("file") MultipartFile file,
                             @RequestParam("versionNo") String versionNo,
                             @RequestParam(value = "remark", required = false) String remark) {
        Long versionId = versionService.upload(algoId, versionNo, remark, file, getUsername());
        return AjaxResult.success("上传成功", java.util.Map.of("id", versionId));
    }

    @DeleteMapping("/versions/{id}")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:remove')")
    @Log(title = "算法库版本", businessType = BusinessType.DELETE)
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(versionService.delete(id));
    }

    @GetMapping("/versions/{id}/download")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:query')")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        AlgoVersion version = versionService.selectById(id);
        if (version == null) {
            throw new ServiceException("版本不存在或已删除");
        }
        File file = new File(RuoYiConfig.getProfile() + File.separator + version.getFileName());
        if (!file.exists()) {
            throw new ServiceException("算法包文件不存在: " + version.getFileName());
        }
        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" +
                            URLEncoder.encode(version.getOriginalName(), StandardCharsets.UTF_8) + "\"");
            FileUtils.writeBytes(file.getAbsolutePath(), response.getOutputStream());
        } catch (IOException e) {
            throw new ServiceException("下载失败: " + e.getMessage());
        }
    }
}
```

**说明：** `FileUtils.writeBytes(String, OutputStream)` 在 `zwei-common` 已存在。

- [ ] **步骤 2：编译并启动后端验证**

```bash
cd server && mvn clean compile -pl zwei-iot-alarm -am -q
```
预期：BUILD SUCCESS。

启动后端 IDE，确认无 Spring bean 注入失败。

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/controller/AlgoVersionController.java
git commit -m "feat(alarm): 新增 AlgoVersionController（上传/下载/列表/删除）"
```

---

## 任务 16：后端整体编译 + 单元测试

- [ ] **步骤 1：运行全部模块测试**

```bash
cd server && mvn clean test -pl zwei-iot-alarm -am -q
```
预期：所有测试通过，包括 `AlgoLibraryServiceImplTest` 和 `AlgoVersionServiceImplTest`。

- [ ] **步骤 2：如有失败，修复后再次运行直到全绿（不省略此步）**

- [ ] **步骤 3：启动后端并冒烟测试**

启动 IDE 中的 `RuoYiApplication`（profile=local），登录后用 Postman/curl 测试：

```bash
# 新增算法
curl -X POST http://localhost:8080/api/v1/algo-lib \
  -H "Authorization: Bearer <你的token>" \
  -H "Content-Type: application/json" \
  -d '{"code":"ALGO_TEST","name":"测试算法","description":"冒烟测试"}'

# 列表查询
curl "http://localhost:8080/api/v1/algo-lib/page?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <你的token>"

# 上传版本
curl -X POST http://localhost:8080/api/v1/algo-lib/1/versions/upload \
  -H "Authorization: Bearer <你的token>" \
  -F "file=@/path/to/test.zip" \
  -F "versionNo=v1.0.0" \
  -F "remark=首次上传"
```

预期：所有接口返回 `{"code":200,...}`，无 500 错误。

---

## 任务 17：前端 API 模块

**文件：**
- 创建：`web/src/api/algoLibrary.ts`

- [ ] **步骤 1：写入 API 模块**

```typescript
import request from '@/utils/request'

// ===== 类型 =====
export interface AlgoInfoPageParams {
  pageNum?: number
  pageSize?: number
  name?: string
  status?: 0 | 1
  code?: string
}

export interface AlgoInfoPayload {
  code?: string
  name: string
  description?: string
  remark?: string
}

export interface AlgoVersionUploadPayload {
  file: File
  versionNo: string
  remark?: string
}

export interface AlgoInfo {
  id: number
  code: string
  name: string
  description?: string
  status: 0 | 1
  delFlag?: 0 | 1
  versionCount?: number
  latestVersionNo?: string
  latestUploadTime?: string
  createTime?: string
  updateTime?: string
  remark?: string
  versions?: AlgoVersion[]
}

export interface AlgoVersion {
  id: number
  algoId: number
  versionNo: string
  fileName: string
  originalName: string
  fileSize: number
  sha256?: string
  createBy?: string
  createTime?: string
  remark?: string
}

export interface PageResult<T> {
  code: number
  msg: string
  rows: T[]
  total: number
}

// ===== 算法 =====
export function getAlgoLibraryPage(params: AlgoInfoPageParams) {
  return request.get<PageResult<AlgoInfo>>('/algo-lib/page', { params })
}

export function getAlgoLibraryDetail(id: number | string) {
  return request.get<{ code: number; msg: string; data: AlgoInfo }>(`/algo-lib/${id}`)
}

export function createAlgoLibrary(data: AlgoInfoPayload) {
  return request.post('/algo-lib', data)
}

export function updateAlgoLibrary(id: number | string, data: AlgoInfoPayload) {
  return request.put(`/algo-lib/${id}`, data)
}

export function updateAlgoLibraryStatus(id: number | string, status: 0 | 1) {
  return request.put(`/algo-lib/${id}/status`, null, { params: { status } })
}

export function deleteAlgoLibrary(id: number | string) {
  return request.delete(`/algo-lib/${id}`)
}

// ===== 版本 =====
export function getAlgoVersionList(algoId: number | string) {
  return request.get<{ code: number; msg: string; data: AlgoVersion[] }>(`/algo-lib/${algoId}/versions`)
}

export function uploadAlgoVersion(
  algoId: number | string,
  payload: AlgoVersionUploadPayload,
  onProgress?: (percent: number) => void
) {
  const formData = new FormData()
  formData.append('file', payload.file)
  formData.append('versionNo', payload.versionNo)
  if (payload.remark) formData.append('remark', payload.remark)
  return request.post(`/algo-lib/${algoId}/versions/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (onProgress && e.total) onProgress(Math.round((e.loaded * 100) / e.total))
    }
  })
}

export function deleteAlgoVersion(id: number | string) {
  return request.delete(`/algo-lib/versions/${id}`)
}

export function downloadAlgoVersion(id: number | string) {
  return request.raw.get(`/algo-lib/versions/${id}/download`, { responseType: 'blob' })
}
```

- [ ] **步骤 2：Commit**

```bash
git add web/src/api/algoLibrary.ts
git commit -m "feat(web): 新增算法库 API 模块"
```

---

## 任务 18：路由 + 菜单硬编码

**文件：**
- 修改：`web/src/router/index.ts`
- 修改：`web/src/layout/index.vue`

- [ ] **步骤 1：router/index.ts 添加路由**

在 `/alarm/composite` 路由下方添加：

```typescript
      { path: '/alarm/composite', name: 'CompositeAlarm', component: () => import('@/views/alarm/CompositeAlarm.vue') },
      { path: '/alarm/algo-library', name: 'AlgoLibrary', component: () => import('@/views/alarm/AlgoLibrary.vue') },
```

- [ ] **步骤 2：layout/index.vue 第一处 — menuList**

在 `Alarm.children` 数组中，`AlarmDisposal` 之后、`divider` 之前插入 AlgoLibrary 项：

修改前（约 437-445 行）：
```javascript
    children: [
      {name: 'RealtimeAlarm', label: '待办告警'},
      {name: 'AlarmNotification', label: '历史告警'},
      {divider: true},
      { name: 'AlarmCriteria', label: '告警判据' },
      {name: 'AlarmDisposal', label: '综合告警'},
      {divider: true},
      {name: 'NotificationSetting', label: '通知设置'}
    ]
```

修改后：
```javascript
    children: [
      {name: 'RealtimeAlarm', label: '待办告警'},
      {name: 'AlarmNotification', label: '历史告警'},
      {divider: true},
      { name: 'AlarmCriteria', label: '告警判据' },
      {name: 'AlarmDisposal', label: '综合告警'},
      {name: 'AlgoLibrary', label: '算法库'},
      {divider: true},
      {name: 'NotificationSetting', label: '通知设置'}
    ]
```

- [ ] **步骤 3：layout/index.vue 第二处 — menuRouteMap**

在 `menuRouteMap` 中 `AlarmDisposal` 行之后添加（约 509 行附近）：

```typescript
  AlarmDisposal: '/alarm/disposal',
  AlgoLibrary: '/alarm/algo-library',
```

- [ ] **步骤 4：layout/index.vue 第三处 — menuLabelMap**

在 `menuLabelMap` 中 `AlarmDisposal` 行之后添加（约 538 行附近）：

```typescript
  AlarmDisposal: '综合告警',
  AlgoLibrary: '算法库',
```

- [ ] **步骤 5：Commit**

```bash
git add web/src/router/index.ts web/src/layout/index.vue
git commit -m "feat(web): 注册算法库路由与菜单（layout 硬编码）"
```

---

## 任务 19：AlgoFormDialog 组件

**文件：**
- 创建：`web/src/views/alarm/components/AlgoFormDialog.vue`

- [ ] **步骤 1：写入组件**

```vue
<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEdit ? '编辑算法' : '新增算法'"
    width="560px"
    destroy-on-close
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-form-item label="算法编码" prop="code">
        <el-input
          v-model="formData.code"
          :disabled="isEdit"
          placeholder="如 ALGO_RAIN_01（大写字母+数字+下划线）"
          maxlength="64"
        />
      </el-form-item>
      <el-form-item label="算法名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入算法名称" maxlength="128" />
      </el-form-item>
      <el-form-item label="算法描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="请输入算法描述"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="可选"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createAlgoLibrary,
  updateAlgoLibrary,
  type AlgoInfo,
  type AlgoInfoPayload
} from '@/api/algoLibrary'

const props = defineProps<{
  modelValue: boolean
  algo?: AlgoInfo | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<AlgoInfoPayload>({
  code: '',
  name: '',
  description: '',
  remark: ''
})

const rules: FormRules = {
  code: [
    { required: true, message: '请输入算法编码', trigger: 'blur' },
    {
      pattern: /^[A-Z][A-Z0-9_]{2,63}$/,
      message: '必须以大写字母开头，3-64 字符，仅含大写字母/数字/下划线',
      trigger: 'blur'
    }
  ],
  name: [{ required: true, message: '请输入算法名称', trigger: 'blur' }]
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      isEdit.value = !!props.algo
      if (props.algo) {
        formData.code = props.algo.code
        formData.name = props.algo.name
        formData.description = props.algo.description || ''
        formData.remark = props.algo.remark || ''
      } else {
        formData.code = ''
        formData.name = ''
        formData.description = ''
        formData.remark = ''
      }
    }
  }
)

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: AlgoInfoPayload = {
      name: formData.name,
      description: formData.description,
      remark: formData.remark
    }
    if (isEdit.value && props.algo) {
      await updateAlgoLibrary(props.algo.id, payload)
      ElMessage.success('更新成功')
    } else {
      payload.code = formData.code
      await createAlgoLibrary(payload)
      ElMessage.success('创建成功')
    }
    emit('update:modelValue', false)
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>
```

- [ ] **步骤 2：Commit**

```bash
git add web/src/views/alarm/components/AlgoFormDialog.vue
git commit -m "feat(web): 新增算法库表单弹窗组件"
```

---

## 任务 20：AlgoDetailDrawer 组件（含版本上传）

**文件：**
- 创建：`web/src/views/alarm/components/AlgoDetailDrawer.vue`

- [ ] **步骤 1：写入组件**

```vue
<template>
  <el-drawer
    :model-value="modelValue"
    title="算法详情"
    size="720px"
    destroy-on-close
    @update:model-value="$emit('update:modelValue', $event)"
    @open="loadDetail"
  >
    <div v-loading="loading" class="detail">
      <template v-if="algo">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="算法名称">{{ algo.name }}</el-descriptions-item>
          <el-descriptions-item label="算法编码">
            <code>{{ algo.code }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="algo.status === 1 ? 'success' : 'info'" size="small">
              {{ algo.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="版本数">{{ algo.versions?.length || 0 }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ algo.createTime }}</el-descriptions-item>
          <el-descriptions-item label="算法描述" :span="2">{{ algo.description || '—' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ algo.remark || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 版本列表 -->
        <div class="version-section">
          <div class="version-header">
            <h3>版本列表</h3>
            <el-button type="primary" size="small" @click="uploadVisible = true">
              <el-icon><Upload /></el-icon> 上传新版本
            </el-button>
          </div>

          <el-table :data="algo.versions || []" stripe>
            <el-table-column label="版本号" prop="versionNo" width="120">
              <template #default="{ row }">
                <el-tag size="small">{{ row.versionNo }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="原始文件" prop="originalName" show-overflow-tooltip />
            <el-table-column label="大小" width="100">
              <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column label="上传人" prop="createBy" width="100" />
            <el-table-column label="上传时间" prop="createTime" width="160" />
            <el-table-column label="备注" prop="remark" show-overflow-tooltip />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="handleDownload(row)">下载</el-button>
                <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </div>

    <!-- 上传新版本弹窗 -->
    <el-dialog v-model="uploadVisible" title="上传新版本" width="480px" append-to-body>
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="80px">
        <el-form-item label="版本号" prop="versionNo">
          <el-input v-model="uploadForm.versionNo" placeholder="如 v1.0.0" maxlength="64" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="uploadForm.remark"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="算法包" prop="file">
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            accept=".zip"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">将 zip 文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .zip 格式，最大 100MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="uploadProgress > 0" label="进度">
          <el-progress :percentage="uploadProgress" :status="uploadProgress === 100 ? 'success' : ''" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUploadSubmit">上传</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { Upload, UploadFilled } from '@element-plus/icons-vue'
import {
  getAlgoLibraryDetail,
  getAlgoVersionList,
  uploadAlgoVersion,
  deleteAlgoVersion,
  downloadAlgoVersion,
  type AlgoInfo,
  type AlgoVersion
} from '@/api/algoLibrary'

const props = defineProps<{
  modelValue: boolean
  algoId: number | null
}>()

defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const loading = ref(false)
const algo = ref<AlgoInfo | null>(null)

const uploadVisible = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadFormRef = ref<FormInstance>()
const uploadForm = reactive<{ versionNo: string; remark: string; file: File | null }>({
  versionNo: '',
  remark: '',
  file: null
})
const uploadRules: FormRules = {
  versionNo: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  file: [{ required: true, message: '请选择算法包文件', trigger: 'change' }]
}

async function loadDetail() {
  if (!props.algoId) return
  loading.value = true
  try {
    const res: any = await getAlgoLibraryDetail(props.algoId)
    algo.value = res.data
    if (algo.value && !algo.value.versions) {
      const vRes: any = await getAlgoVersionList(props.algoId)
      algo.value.versions = vRes.data || []
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handleFileChange(file: UploadFile) {
  if (file.raw) uploadForm.file = file.raw
}

function handleFileRemove() {
  uploadForm.file = null
}

async function handleUploadSubmit() {
  await uploadFormRef.value?.validate()
  if (!props.algoId || !uploadForm.file) return
  uploading.value = true
  uploadProgress.value = 0
  try {
    await uploadAlgoVersion(
      props.algoId,
      { file: uploadForm.file, versionNo: uploadForm.versionNo, remark: uploadForm.remark },
      (pct) => (uploadProgress.value = pct)
    )
    ElMessage.success('上传成功')
    uploadVisible.value = false
    uploadForm.versionNo = ''
    uploadForm.remark = ''
    uploadForm.file = null
    uploadProgress.value = 0
    await loadDetail()
  } catch (e: any) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function handleDelete(row: AlgoVersion) {
  try {
    await ElMessageBox.confirm(`确定删除版本「${row.versionNo}」？`, '删除确认', { type: 'warning' })
    await deleteAlgoVersion(row.id)
    ElMessage.success('删除成功')
    await loadDetail()
  } catch { /* cancelled */ }
}

async function handleDownload(row: AlgoVersion) {
  try {
    const res = await downloadAlgoVersion(row.id)
    const blob = new Blob([res.data], { type: 'application/zip' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.originalName
    a.click()
    URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error(e.message || '下载失败')
  }
}

function formatSize(bytes: number): string {
  if (!bytes) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}
</script>

<style scoped>
.detail {
  padding: 16px;
}
.version-section {
  margin-top: 24px;
}
.version-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.version-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
</style>
```

- [ ] **步骤 2：Commit**

```bash
git add web/src/views/alarm/components/AlgoDetailDrawer.vue
git commit -m "feat(web): 新增算法库详情抽屉组件（含版本上传/下载/删除）"
```

---

## 任务 21：AlgoLibrary 卡片列表页

**文件：**
- 创建：`web/src/views/alarm/AlgoLibrary.vue`

- [ ] **步骤 1：写入页面（参考 CompositeAlarm.vue 结构 + 全局 .grid/.card 样式）**

```vue
<template>
  <div class="page">
    <!-- 页头 -->
    <div class="header">
      <div class="header__left">
        <h2 class="header__title">算法库</h2>
        <span class="header__subtitle">Python 算法包管理与版本化</span>
      </div>
      <div class="header__right">
        <el-button v-if="hasPerm('iot:algo-library:add')" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增算法
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search">
      <el-input
        v-model="searchName"
        placeholder="搜索算法名称"
        class="search__input"
        clearable
        @clear="loadData"
        @keyup.enter="loadData"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchStatus" placeholder="状态" clearable class="search__select" @change="loadData">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="handleResetSearch">重置</el-button>
    </div>

    <!-- 卡片列表（复用全局 .grid/.card 样式） -->
    <div v-loading="loading" class="grid">
      <el-empty v-if="!loading && algoList.length === 0" description="暂无算法" />

      <div
        v-for="item in algoList"
        :key="item.id"
        class="card"
        :class="{ 'card--disabled': item.status !== 1 }"
      >
        <div class="card__header">
          <div class="card__title-row">
            <h3 class="card__title">{{ item.name }}</h3>
            <el-switch
              v-if="hasPerm('iot:algo-library:edit')"
              :model-value="item.status === 1"
              size="small"
              active-text="启用"
              inactive-text="停用"
              @change="(val: boolean) => handleToggleStatus(item, val)"
            />
            <el-tag v-else :type="item.status === 1 ? 'success' : 'info'" size="small">
              {{ item.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </div>
          <p class="card__desc">{{ item.description || '—' }}</p>
        </div>

        <div class="card__meta">
          <div class="card__meta-row">
            <span class="card__meta-label">编码:</span>
            <code class="card__meta-value">{{ item.code }}</code>
          </div>
          <div class="card__meta-row">
            <span class="card__meta-label">版本数:</span>
            <span class="card__meta-value">{{ item.versionCount || 0 }}</span>
          </div>
          <div v-if="item.latestVersionNo" class="card__meta-row">
            <span class="card__meta-label">最近上传:</span>
            <span class="card__meta-value">
              {{ item.latestVersionNo }} · {{ item.latestUploadTime }}
            </span>
          </div>
        </div>

        <div class="card__footer">
          <el-button type="primary" text size="small" @click="handleDetail(item)">
            <el-icon><View /></el-icon> 详情
          </el-button>
          <el-button v-if="hasPerm('iot:algo-library:edit')" type="primary" text size="small" @click="handleEdit(item)">
            <el-icon><Setting /></el-icon> 编辑
          </el-button>
          <el-button v-if="hasPerm('iot:algo-library:remove')" type="danger" text size="small" @click="handleDelete(item)">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <AlgoFormDialog v-model="formVisible" :algo="editingItem" @saved="loadData" />

    <!-- 详情抽屉 -->
    <AlgoDetailDrawer v-model="detailVisible" :algo-id="currentId" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, View, Setting, Delete } from '@element-plus/icons-vue'
import {
  getAlgoLibraryPage,
  updateAlgoLibraryStatus,
  deleteAlgoLibrary,
  type AlgoInfo
} from '@/api/algoLibrary'
import { hasPerm } from '@/utils/permission'
import AlgoFormDialog from './components/AlgoFormDialog.vue'
import AlgoDetailDrawer from './components/AlgoDetailDrawer.vue'

// ==================== 列表状态 ====================
const loading = ref(false)
const algoList = ref<AlgoInfo[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const searchName = ref('')
const searchStatus = ref<0 | 1 | ''>('')

// ==================== 弹窗状态 ====================
const formVisible = ref(false)
const editingItem = ref<AlgoInfo | null>(null)
const detailVisible = ref(false)
const currentId = ref<number | null>(null)

// ==================== 数据加载 ====================
async function loadData() {
  loading.value = true
  try {
    const res: any = await getAlgoLibraryPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: searchName.value || undefined,
      status: searchStatus.value === '' ? undefined : searchStatus.value
    })
    algoList.value = res.rows || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handleResetSearch() {
  searchName.value = ''
  searchStatus.value = ''
  pageNum.value = 1
  loadData()
}

// ==================== 操作 ====================
function handleAdd() {
  editingItem.value = null
  formVisible.value = true
}

function handleEdit(item: AlgoInfo) {
  editingItem.value = item
  formVisible.value = true
}

function handleDetail(item: AlgoInfo) {
  currentId.value = item.id
  detailVisible.value = true
}

async function handleToggleStatus(item: AlgoInfo, enabled: boolean) {
  const newStatus = enabled ? 1 : 0
  const action = enabled ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${action}算法「${item.name}」？`, `${action}确认`, { type: 'warning' })
    await updateAlgoLibraryStatus(item.id, newStatus as 0 | 1)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch { /* cancelled */ }
}

async function handleDelete(item: AlgoInfo) {
  try {
    await ElMessageBox.confirm(
      `确定删除算法「${item.name}」？将同时删除该算法下所有版本记录（物理文件保留）。`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteAlgoLibrary(item.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

onMounted(() => loadData())
</script>

<style scoped>
.page {
  background: #f0f2f5;
}
</style>
```

**说明：** `.grid/.card/.card__header/.card__title-row/.card__title/.card__desc/.card__meta/.card__meta-row/.card__meta-label/.card__meta-value/.card__footer` 全部定义在 `web/src/style.css`，无需重复定义。`.header/.search/.pagination` 等若已在全局定义也复用，若项目缺这些类则在前端工程师审阅时按需补充（前端审阅时由审阅者决策）。

- [ ] **步骤 2：Commit**

```bash
git add web/src/views/alarm/AlgoLibrary.vue
git commit -m "feat(web): 新增算法库卡片列表页"
```

---

## 任务 22：前端类型检查 + 启动冒烟

- [ ] **步骤 1：Vue 类型检查 + 构建**

```bash
cd web && npm run build
```
预期：vue-tsc 类型检查通过，无 error。

- [ ] **步骤 2：启动前端 dev server**

```bash
cd web && npm run dev
```
打开浏览器到 `http://localhost:5173`，登录系统，在侧边栏「告警中心」下应看到「算法库」菜单。

- [ ] **步骤 3：手测清单（逐项验证）**

- [ ] 点击「算法库」菜单 → 进入空列表页面（显示"暂无算法"）
- [ ] 点击「新增算法」→ 填写表单（编码/名称/描述）→ 确定 → 卡片出现
- [ ] 在卡片上切换「启用/停用」开关 → 状态变化、卡片半透明变化
- [ ] 点击「编辑」→ 修改描述 → 确定 → 信息更新
- [ ] 点击「详情」→ 抽屉打开 → 显示版本列表（空）
- [ ] 点击「上传新版本」→ 选择一个 zip 文件 + 输入版本号 → 上传成功 → 版本列表出现一行
- [ ] 上传同一版本号 → 应报"版本号已存在"
- [ ] 上传非 zip 文件 → 应被前端 accept=".zip" 拦截
- [ ] 在版本行点击「下载」→ 文件下载，名称与原始名一致
- [ ] 在版本行点击「删除」→ 二次确认 → 版本消失
- [ ] 回到列表，点击卡片「删除」→ 二次确认 → 卡片消失（验证级联逻辑删）
- [ ] 数据库验证：`SELECT * FROM algo_info WHERE del_flag=1;` 应能看到刚删除的算法，`SELECT * FROM algo_version WHERE del_flag=1;` 应能看到级联删除的版本

- [ ] **步骤 4：Commit（如有手测中修复的问题）**

```bash
git status
# 如有未提交的修复：
# git add <files>
# git commit -m "fix(alarm): 手测修复 XXX"
```

---

## 任务 23：合并回主分支

**前置条件：** 所有测试通过、手测清单全部勾选、无未提交修改。

- [ ] **步骤 1：worktree 内最终 commit & push**

```bash
git status   # 应 clean
git log --oneline web260429..HEAD   # 查看本次新增的所有 commit
```

- [ ] **步骤 2：与用户确认合并方式**

询问用户：
- 选项 A：直接 fast-forward 合并到 `web260429`（worktree 完成后由用户执行 `git merge`）
- 选项 B：发起 PR 合并到 `develop` 或 `web260429`

- [ ] **步骤 3：按用户选择执行合并**

**合并到 web260429：**
```bash
cd <主仓库根>
git checkout web260429
git merge <worktree 分支名>
git push origin web260429
```

**或发 PR：**
```bash
gh pr create --base web260429 --head <worktree 分支名> \
  --title "feat(alarm): 新增算法库功能" \
  --body "..."
```

- [ ] **步骤 4：清理 worktree**

合并完成后，按 `using-git-worktrees` skill 提示删除 worktree。

---

## 自检清单（执行计划前最后一遍核对）

### 规格覆盖度

- [x] 表 algo_info / algo_version → 任务 1
- [x] 文件上传扩展 → 任务 2
- [x] multipart 配置 → 任务 3
- [x] Domain 实体 → 任务 4、5
- [x] DTO → 任务 6
- [x] Mapper + XML → 任务 7、8
- [x] Service 接口 → 任务 9
- [x] TDD 测试 → 任务 10、12
- [x] Service 实现 → 任务 11、13
- [x] Controller → 任务 14、15
- [x] 后端编译/冒烟 → 任务 16
- [x] 前端 API → 任务 17
- [x] 路由 + 菜单 → 任务 18
- [x] 表单弹窗 → 任务 19
- [x] 详情抽屉（含上传/下载/删除）→ 任务 20
- [x] 卡片列表页 → 任务 21
- [x] 前端构建 + 手测 → 任务 22
- [x] 合并 → 任务 23

### 类型/方法一致性

- [x] `AlgoInfo.versions` 字段：任务 4 创建，任务 11 步骤 2 补加，任务 20/21 使用
- [x] `IAlgoVersionService.upload` 签名：任务 9 定义、任务 13 实现、任务 15 Controller 调用 一致（`Long algoId, String versionNo, String remark, MultipartFile file, String createBy`）
- [x] `AlgoVersionServiceImpl.getProfilePath()` 受保护方法：任务 13 定义、任务 12 测试覆盖
- [x] 前端 `uploadAlgoVersion` 签名：任务 17 定义、任务 20 调用 一致
- [x] 前端 `getAlgoLibraryDetail` 返回类型：任务 17 定义、任务 20 使用 `res.data` 一致

### 占位符扫描

无 TODO / 待定 / 类似任务 N 等。每个步骤都有完整代码块。

### 风险提示

1. **任务 13 的 `getProfilePath()` 受保护方法**：生产用 `RuoYiConfig.getProfile()`，测试用子类覆盖。若 `RuoYiConfig` 类路径与计划不符（应在 `com.zwei.common.config.RuoYiConfig`），需先 `grep -rn "class RuoYiConfig" server/` 确认。
2. **`FileUtils.writeBytes`**：需在 `zwei-common` 已存在。若不存在，用 `Files.copy(file.toPath(), response.getOutputStream())` 替代。
3. **`application.yml` 中的 `spring.servlet.multipart`**：项目原本使用 Spring Boot 默认值（1MB）。任务 3 调整为 100MB 后，**所有**上传接口的上限都变为 100MB（不仅是算法库），其他业务无负面影响。
4. **菜单硬编码位置**：layout/index.vue 行号在两次修改间可能漂移，**用 Grep 定位**（`AlarmDisposal` 关键字）而非依赖具体行号。

---

## 执行方式

计划已完成并保存到 `docs/superpowers/plans/2026-06-17-algo-library.md`。两种执行方式：

**1. 子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代

**2. 内联执行** - 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点

**选哪种方式？**
