# 后端镜像：多阶段构建。
#
#   构建阶段 maven:3.9-eclipse-temurin-21 —— **必须是 JDK 21**：JDK 25 下 Lombok 注解处理器
#   不生效，编译会报一堆"找不到符号"（本机 JAVA_HOME 指向 JDK 25 时踩过）。
#   运行阶段 JRE 21 Alpine，非 root 用户，上传目录挂卷。
#
# 镜像里不跑测试（-Dmaven.test.skip=true）：跑测试请在本机或 CI 执行 `mvn test`
# （TemplateResourceTest 要读 classpath 里的模板资源，UploadServiceImplTest 要写临时目录），
# 没必要让每次镜像构建都拖上这一段时间。

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /src

# 先只拷 pom 预热依赖：源码改动不会让依赖层缓存失效。
# 结尾的 `|| true` 是有意的——某些插件在 go-offline 阶段解析不到不影响后续 package，
# 真缺依赖的话下面那次 package 会明确报错，不会被这里掩盖。
COPY backend/pom.xml ./backend/pom.xml
RUN mvn -B -q -f backend/pom.xml -Dmaven.test.skip=true dependency:go-offline || true

COPY backend/src ./backend/src
RUN mvn -B -f backend/pom.xml -Dmaven.test.skip=true package


FROM eclipse-temurin:21-jre-alpine

# tzdata：Alpine 基础镜像不带时区库，不装的话 TZ=Asia/Shanghai 会被忽略、日志与
# create_time 全按 UTC 走。Java 也靠它解析 TZ 环境变量。
RUN apk add --no-cache tzdata \
 && addgroup -S gcc \
 && adduser -S -G gcc -h /app gcc \
 && mkdir -p /app /data/uploads \
 && chown -R gcc:gcc /app /data/uploads

WORKDIR /app

COPY --from=build /src/backend/target/lelcc-backend-0.0.1-SNAPSHOT.jar /app/app.jar
RUN chown gcc:gcc /app/app.jar

# 端口、上传目录、图片对外基址的默认值与 application.yaml 对齐；
# 连接数据库、JWT 密钥这些没有合理默认值的项一律由 compose 传环境变量（见 docker-compose.yml）。
ENV TZ=Asia/Shanghai \
    SERVER_PORT=9331 \
    UPLOAD_PROVIDER=local \
    UPLOAD_DIR=/data/uploads \
    UPLOAD_PUBLIC_BASE_URL=/static \
    JAVA_OPTS="-Xms256m -Xmx512m"

USER gcc

EXPOSE 9331

# 健康检查打的是 /app/site/config：它要读库，所以"健康"同时意味着数据库连得上——
# compose 里 web 靠这个健康状态决定什么时候起 nginx。
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=12 \
    CMD wget -qO- "http://127.0.0.1:${SERVER_PORT}/app/site/config" >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
