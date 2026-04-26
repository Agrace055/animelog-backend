# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Cache dependency resolution layer
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Build application
COPY src ./src
RUN mvn package -q -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/target/animelog-backend-0.1.0.jar /app/animelog-backend.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/animelog-backend.jar", "--spring.profiles.active=prod"]
