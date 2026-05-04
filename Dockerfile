FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY app/.mvn .mvn
COPY app/mvnw app/pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY app/src src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
