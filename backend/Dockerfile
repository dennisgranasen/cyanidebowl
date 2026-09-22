# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY backend ./backend
COPY api ./api
COPY cyanide-api ./cyanide-api
RUN mvn clean package -Dmaven.test.skip=true -DskipDocker -Pserver -pl api,cyanide-api,backend -am

FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache curl

WORKDIR /app
COPY --from=build /app/backend/target/*.jar warpscores-backend.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "warpscores-backend.jar"]