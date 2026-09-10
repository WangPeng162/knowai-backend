# ============================================================
# KnowAI 后端镜像（多阶段构建）
#  stage 1: Maven 编译打包
#  stage 2: 仅带 JRE 运行（镜像更小、无构建工具、更安全）
# 注意：application-local.yml 被 .dockerignore 排除，
#       所有环境相关配置（DB/Redis/Qdrant/OSS/API Key）通过环境变量注入
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先只复制 pom 并预下载依赖：依赖不变时可复用缓存层，改代码不用重新下载
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 时区（日志时间与业务时间保持一致）
ENV TZ=Asia/Shanghai

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
