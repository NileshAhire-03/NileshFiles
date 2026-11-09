FROM node:22-alpine AS frontend
WORKDIR /build/frontend
COPY nilspath-ai/frontend/package*.json ./
RUN npm ci
COPY nilspath-ai/frontend/ ./
ENV VITE_API_URL=/api
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /build/backend
COPY nilspath-ai/backend/pom.xml ./
RUN mvn -B dependency:go-offline
COPY nilspath-ai/backend/src ./src
COPY --from=frontend /build/frontend/dist/ ./src/main/resources/static/
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system careerpath && useradd --system --gid careerpath careerpath
COPY --from=backend /build/backend/target/nilspath-api-1.0.0.jar /app/app.jar
USER careerpath
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
