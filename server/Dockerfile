# syntax=docker/dockerfile:1.7
# ==================== 阶段一：后端构建 ====================
# 多阶段构建：Maven 编译 + JRE 运行

# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 阿里云 Maven 镜像 — 国内拉 Maven Central 是慢的最大单点
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<'EOF'
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF

# 分层复制 POM — 只有 POM 变更才重新下载依赖
COPY pom.xml .
COPY zwei-common/pom.xml    zwei-common/
COPY zwei-framework/pom.xml zwei-framework/
COPY zwei-system/pom.xml    zwei-system/
COPY zwei-log/pom.xml       zwei-log/
COPY zwei-monitor/pom.xml   zwei-monitor/
COPY zwei-quartz/pom.xml    zwei-quartz/
COPY zwei-iot-monitor/pom.xml    zwei-iot-monitor/
COPY zwei-iot-device/pom.xml     zwei-iot-device/
COPY zwei-iot-timeseries/pom.xml zwei-iot-timeseries/
COPY zwei-iot-broker/pom.xml     zwei-iot-broker/
COPY zwei-iot-hazard/pom.xml     zwei-iot-hazard/
COPY zwei-iot-video/pom.xml      zwei-iot-video/
COPY zwei-iot-alarm/pom.xml      zwei-iot-alarm/
COPY zwei-admin/pom.xml    zwei-admin/

# BuildKit 缓存 ~/.m2 — 跨次构建复用依赖，二次构建几乎 0 秒
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:go-offline -B -q --no-transfer-progress

# 复制源码并构建
COPY . .

# -T 1C = 每 CPU 核心 1 线程并行编译 14 个模块
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn clean package -DskipTests -B -q --no-transfer-progress -T 1C

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre

# 安装必要工具（用于健康检查）
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /app/zwei-admin/target/*.jar app.jar

# 创建上传目录
RUN mkdir -p /app/uploads /app/logs

# 环境变量（可在 docker-compose 或 K8s 中覆盖）
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE="prod"
ENV ZWEI_LOG_RUNTIME_LEVELS="WARN,ERROR"
ENV ZWEI_LOG_RUNTIME_INFO_ENABLED="false"

EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -Dzwei.log.runtime-levels=$ZWEI_LOG_RUNTIME_LEVELS -Dzwei.log.runtime-info-enabled=$ZWEI_LOG_RUNTIME_INFO_ENABLED -jar app.jar"]
