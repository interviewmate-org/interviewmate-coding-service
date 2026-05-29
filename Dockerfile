
# -------- Build stage --------
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only pom.xml first (for dependency caching)
COPY pom.xml ./

# Download dependencies
RUN mvn dependency:go-offline

# Copy source code
COPY src/ src/

# Build the JAR
RUN mvn clean package -DskipTests -B

# -------- Runtime stage --------
FROM bellsoft/liberica-runtime-container:jre-21-slim-musl
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appuser && adduser -S -G appuser appuser

# Copy the compiled JAR from the build stage
COPY --from=build /workspace/target/coding-service-0.0.1-SNAPSHOT.jar /app/coding-service.jar


# Expose the service port (8085)
EXPOSE 8085

# Switch to the non-root user
USER appuser

# Entrypoint with container-friendly JVM flags
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/coding-service.jar"]
