# Stage 1: Build the application
# Using non-Alpine for native library compatibility
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
# Download dependencies first to cache them
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Stage 2: Run the application
# Using non-Alpine Debian-based image for Argon2 native library compatibility
# This ensures glibc is available for JNA/native libraries on ARM64
FROM eclipse-temurin:21-jre

# Install any missing system libraries that might be needed
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libc6 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Create a volume for temporary files if needed normally, but crucial for some java apps
VOLUME /tmp

# Copy the built jar from the build stage
COPY --from=build /app/target/*-jar-with-dependencies.jar app.jar

# Copy email templates needed at runtime (EmailUtil reads them from the filesystem)
COPY --from=build /app/src/main/Security/Resources /app/src/main/Security/Resources

# Expose the application port
EXPOSE 7000
# Expose the debug port
EXPOSE 5005

# Environment variables that can be overridden
ENV PORT=7000
ENV JAVA_OPTS=""

# Run the application with debug agent enabled by default (can be controlled via JAVA_OPTS if preferred, but explicit is easier here)
# address=*:5005 allows connection from outside the container
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar"]
