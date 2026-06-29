# ========================================
# Stage 1: Build
# ========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY app/pom.xml ./

RUN mvn dependency:go-offline -q

COPY app/src ./src

RUN mvn package -DskipTests -q

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

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
