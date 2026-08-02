# syntax=docker/dockerfile:1
#
# ADR-022: built by GitHub Actions (.github/workflows/deploy-api.yml), pushed to ghcr.io, and pulled
# by Azure Container Apps -- not built directly on the Container Apps host. Uses Spring Boot's layered
# jar support so unchanged dependency layers are cached across builds; only the application layer
# (usually a few KB) needs re-pulling on most deploys.

# --- Build stage --------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Cache Gradle dependency resolution separately from source changes.
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --version

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Split the fat jar into layers (dependencies / spring-boot-loader / snapshot-dependencies /
# application) so Docker can cache everything except the actual application code between builds.
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination extracted

# --- Runtime stage -------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Run as a non-root user -- the base image ships a "ubuntu" user, but a dedicated one keeps this
# explicit and independent of upstream image changes.
RUN groupadd --system helix && useradd --system --gid helix --home-dir /app helix

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER helix
EXPOSE 8080

# Azure Container Apps sets PORT via the ingress target port configured in Terraform (8080, matching
# server.port's default) -- not injected as an env var the app needs to read itself.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
