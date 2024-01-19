FROM openjdk:alpine

RUN addgroup warpscore && adduser -u 10000 -D -G warpscore warpscore
WORKDIR /home/warpscore/
USER warpscore

HEALTHCHECK CMD curl -sSf http://localhost:8080/version.json

COPY target/${JAR_FILE} warpscore.jar
EXPOSE 8080
CMD ["sh", "-c", "exec java -Denv=${ENVIRONMENT} -Djava.security.egd=file:/dev/./urandom -Djava.awt.headless=true -jar warpscore.jar"]
