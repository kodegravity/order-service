# =============================================================================
# STAGE 1 — BUILD
# =============================================================================
# Maven + Eclipse Temurin 17 image provides mvn on PATH and a full JDK.
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# ── Dependency cache layer ────────────────────────────────────────────────────
# Copy pom.xml first. Docker caches this layer; 'dependency:go-offline' is
# skipped on subsequent builds as long as pom.xml is unchanged — saves minutes.
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# ── Build ─────────────────────────────────────────────────────────────────────
COPY src ./src
# -DskipTests → CI/CD pipeline runs tests separately; skip them here.
RUN mvn package -DskipTests -B --no-transfer-progress


# =============================================================================
# STAGE 2 — RUNTIME
# =============================================================================
# Eclipse Temurin 17 JRE on Alpine (~80 MB). JRE-only = no compiler = smaller
# attack surface.
FROM eclipse-temurin:17-jre-alpine AS runtime

# Never run as root inside a container.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Only the fat JAR crosses the stage boundary — no Maven cache or source code.
COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

# Matches server.port in application.yml.
EXPOSE 8083

# JVM flags:
#   -XX:+UseContainerSupport      → read cgroup CPU/memory limits (Fargate critical)
#   -XX:MaxRAMPercentage=75.0     → cap heap at 75 % of container RAM
#   -XX:+ExitOnOutOfMemoryError   → hard-kill on OOM so ECS can restart the task
#   -Djava.security.egd=...urandom → non-blocking SecureRandom seed
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.lifecycle.timeout-per-shutdown-phase=30s"

# ECS polls this every 30 s. 60 s start-period gives Spring time to boot + connect to DB.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8083/actuator/health || exit 1

# Exec form so the JVM is PID 1 and receives SIGTERM for graceful shutdown.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
