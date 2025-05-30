#FROM openjdk:17-jdk-alpine
#MAINTAINER naytsyrhc <naytsyrhc@gmx.org>

#RUN apk add --update curl && \
#    rm -rf /var/cache/apk/*

#HEALTHCHECK --interval=2m --timeout=15s --start-period=45s \
#    CMD curl --retry 6 --max-time 5 --retry-delay 10 --retry-max-time 60 \
#            --fail --silent --show-error http://localhost:8080/version.json \
#          || bash -c 'kill --signal SIGTERM -1 && (sleep 10; kill --signal SIGKILL -1)'

#ADD backend.jar warpscores-backend.jar

#EXPOSE 8080

#CMD ["sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -Djava.awt.headless=true -jar warpscores-backend.jar"]

FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY ../../pom.xml .
COPY ../../backend ./backend
COPY ../../api ./api
COPY ../../cyanide-api ./cyanide-api
RUN mvn -f backend/pom.xml clean package -DskipTests -P server

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/backend/target/*.jar warpscores-backend.jar
EXPOSE 8080
CMD ["sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -Djava.awt.headless=true -jar warpscores-backend.jar"]