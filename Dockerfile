# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

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
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a volume for temporary files if needed normally, but crucial for some java apps
VOLUME /tmp

# Copy the built jar from the build stage
COPY --from=build /app/target/*-jar-with-dependencies.jar app.jar

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
