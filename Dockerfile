# Build State
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Run State
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy built jar
COPY --from=builder /app/target/*.jar app.jar

# Spring Boot port
EXPOSE 8080

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]