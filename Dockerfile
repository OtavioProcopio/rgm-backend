# ========================================
# Stage 1: Build
# ========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY app/pom.xml ./
COPY app/src ./src

RUN mvn package -DskipTests -q \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=30

# ========================================
# Stage 2: Runtime
# ========================================
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN chown -R app:app /app

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
