FROM openjdk:17-jdk-slim
MAINTAINER naytsyrhc <naytsyrhc@gmx.org>

HEALTHCHECK CMD curl -sSf http://localhost:8080/public/version.json

# COPY ./backend.jar warpscores-backend.jar
ADD backend.jar warpscores-backend.jar

EXPOSE 8080

CMD ["sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -Djava.awt.headless=true -jar warpscores-backend.jar"]
