# syntax=docker/dockerfile:1.7
# 后端镜像：多阶段构建（Maven 编译 -> JRE 运行）
# 构建上下文必须是项目根目录：
#   docker build -f docker/server.Dockerfile -t rental/server:1.0.0 .

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 用项目自带的阿里云镜像配置加速依赖下载
COPY rental-server/.mvn/settings.xml /root/.m2/settings.xml
COPY rental-server/pom.xml ./
COPY rental-server/src ./src

# 依赖仓库走 BuildKit 缓存：首次下载，之后构建秒级复用
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -q clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre-alpine

# 时区由 JVM 参数 -Duser.timezone 指定（JDK 自带 tzdb），
# 不额外装 tzdata，减少一层构建依赖
ENV TZ=Asia/Shanghai

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar /app/app.jar
RUN chown -R app:app /app
USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Shanghai"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
