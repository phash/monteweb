# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

# Runtime stage
FROM eclipse-temurin:25-jre-alpine AS runtime

LABEL org.opencontainers.image.title="MonteWeb Backend" \
      org.opencontainers.image.description="MonteWeb School Intranet — Spring Boot API" \
      org.opencontainers.image.vendor="MonteWeb"

WORKDIR /app

RUN addgroup -S monteweb && adduser -S monteweb -G monteweb
USER monteweb

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
