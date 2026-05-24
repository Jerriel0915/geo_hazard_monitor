# ==================== 阶段一：后端构建 ====================
# 多阶段构建：减少最终镜像体积

# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制依赖文件，先下载依赖（利用 Docker 缓存层）
COPY pom.xml .
COPY zwei-common/pom.xml zwei-common/
COPY zwei-framework/pom.xml zwei-framework/
COPY zwei-system/pom.xml zwei-system/
COPY zwei-log/pom.xml zwei-log/
COPY zwei-quartz/pom.xml zwei-quartz/
COPY zwei-generator/pom.xml zwei-generator/
COPY zwei-iot/pom.xml zwei-iot/
COPY zwei-admin/pom.xml zwei-admin/

RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY . .
RUN mvn clean package -DskipTests -B

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
