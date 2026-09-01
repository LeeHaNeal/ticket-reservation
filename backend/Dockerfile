# ---- 1단계: 빌드 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 의존성만 먼저 복사해서 캐싱 (소스만 바뀌면 의존성 재다운로드 안 하도록)
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

# ---- 2단계: 실행 ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
