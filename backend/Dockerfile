# Multi-stage build for MongoDB CSFLE Backend
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /build

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

# Install curl and wget for healthcheck and downloading libraries
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl wget && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Download MongoDB Automatic Encryption Shared Library (mongo_crypt_v1.so)
# This is the modern replacement for mongocryptd and is required for CSFLE
# Version 8.0.5 is compatible with MongoDB Driver 5.2.1
# Detect architecture and download the appropriate version
RUN ARCH=$(uname -m) && \
    if [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then \
        echo "Downloading ARM64 version of mongo_crypt_shared..." && \
        wget -q https://downloads.mongodb.com/linux/mongo_crypt_shared_v1-linux-aarch64-enterprise-ubuntu2204-8.0.5.tgz && \
        tar -xzf mongo_crypt_shared_v1-linux-aarch64-enterprise-ubuntu2204-8.0.5.tgz && \
        mkdir -p /usr/local/lib && \
        cp lib/mongo_crypt_v1.so /usr/local/lib/ && \
        rm -rf mongo_crypt_shared_v1-linux-aarch64-enterprise-ubuntu2204-8.0.5.tgz lib; \
    else \
        echo "Downloading x86_64 version of mongo_crypt_shared..." && \
        wget -q https://downloads.mongodb.com/linux/mongo_crypt_shared_v1-linux-x86_64-enterprise-ubuntu2204-8.0.5.tgz && \
        tar -xzf mongo_crypt_shared_v1-linux-x86_64-enterprise-ubuntu2204-8.0.5.tgz && \
        mkdir -p /usr/local/lib && \
        cp lib/mongo_crypt_v1.so /usr/local/lib/ && \
        rm -rf mongo_crypt_shared_v1-linux-x86_64-enterprise-ubuntu2204-8.0.5.tgz lib; \
    fi

# Set environment variables for the shared library
ENV MONGOCRYPT_SHARED_LIB_PATH=/usr/local/lib/mongo_crypt_v1.so
ENV LD_LIBRARY_PATH=/usr/local/lib:$LD_LIBRARY_PATH

# Verify the library was installed
RUN ls -la /usr/local/lib/mongo_crypt_v1.so

# Copy the built JAR from builder stage
COPY --from=builder /build/target/fle-demo-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

