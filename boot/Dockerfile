FROM openjdk:alpine

RUN addgroup deliverance && adduser -u 10000 -D -G deliverance deliverance
WORKDIR /home/dbbcbb3facade/
USER deliverance

HEALTHCHECK CMD curl -sSf http://localhost:8080/version.json

COPY target/boot.jar dbbcbb3facade.jar
EXPOSE 8080
CMD ["sh", "-c", "exec java -Denv=$ENVIRONMENT -Djava.security.egd=file:/dev/./urandom -Djava.awt.headless=true -jar dbbcbb3facade.jar"]
