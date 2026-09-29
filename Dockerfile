# ==========================================
# Stage 1: Build Vue 3 Frontend
# ==========================================
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# ==========================================
# Stage 2: Build Spring Boot Application
# ==========================================
FROM eclipse-temurin:17-jdk-jammy AS backend-builder
WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

COPY src src
# Copy frontend production distribution to Spring Boot static resources
COPY --from=frontend-builder /app/frontend/dist src/main/resources/static

RUN ./mvnw clean package -DskipTests

# ==========================================
# Stage 3: Minimal Production JRE Runtime
# ==========================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create upload directories for persistent claim attachments and SQLite database
RUN mkdir -p /app/uploads /app/data

# Copy built artifact
COPY --from=backend-builder /app/target/demo-0.0.1-SNAPSHOT.war /app/app.war

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.war"]
