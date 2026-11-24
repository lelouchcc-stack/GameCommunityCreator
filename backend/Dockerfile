# =============================================================================
# 后端镜像：多阶段构建
#   构建阶段：maven:3.9-eclipse-temurin-21 执行 mvn -B -DskipTests package
#   运行阶段：eclipse-temurin:21-jre 只带 JRE 与 jar，镜像更小
#
# 构建上下文为 backend/，因此只拷贝构建真正需要的文件（不把 target/ 带进镜像）。
#   docker build -t gcc-backend:latest backend/
# =============================================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# 先单独拷 pom.xml，依赖下载层可被缓存
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline || true

# 再拷源码并打包（跳过测试：测试由 CI / 本机 mvn test 负责，镜像构建只求快与稳）
COPY src ./src
RUN mvn -B -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# 时区与字符集：时间字段按 Asia/Shanghai 序列化，中文日志不乱码
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Spring Boot 插件产出的可执行 jar
COPY --from=build /build/target/*.jar /app/app.jar

EXPOSE 8080

# 用 shell 形式以便 JAVA_OPTS 生效
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
