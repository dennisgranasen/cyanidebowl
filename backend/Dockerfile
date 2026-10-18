# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
ARG TARGETARCH
COPY pom.xml .
COPY backend ./backend
COPY api ./api
COPY cyanide-api ./cyanide-api
RUN case "$TARGETARCH" in \
      amd64) copilot_runtime_classifier=linux-x64 ;; \
      arm64) copilot_runtime_classifier=linux-arm64 ;; \
      *) echo "Unsupported target architecture for Copilot runtime: $TARGETARCH" >&2; exit 1 ;; \
    esac && \
    mvn clean package -Dmaven.test.skip=true -DskipDocker -Pserver \
      -Dcopilot.runtime.classifier="$copilot_runtime_classifier" \
      -pl api,cyanide-api,backend -am

FROM eclipse-temurin:25-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/backend/target/*.jar warpscores-backend.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "warpscores-backend.jar"]