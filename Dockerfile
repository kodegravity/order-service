# =============================================================================
# STAGE 1 — BUILD
# =============================================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# ── Dependency cache layer ────────────────────────────────────────────────────
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# ── Build ─────────────────────────────────────────────────────────────────────
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress


# =============================================================================
# STAGE 2 — RUNTIME
# =============================================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

# ── RDS SSL certificate ───────────────────────────────────────────────────────
# AWS RDS requires SSL ('sslmode=require' in the JDBC URL).
# The global bundle covers all AWS regions and RDS/Aurora engine versions.
# Downloaded at build time so the image is self-contained — no runtime curl needed.
RUN apk add --no-cache wget \
 && wget -qO /usr/local/share/ca-certificates/global-bundle.pem \
      https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem \
 && update-ca-certificates

# ── Non-root user ─────────────────────────────────────────────────────────────
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8083

ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.lifecycle.timeout-per-shutdown-phase=30s"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
