FROM openjdk:17-jdk-alpine
MAINTAINER naytsyrhc <naytsyrhc@gmx.org>

RUN apk add --update curl && \
    rm -rf /var/cache/apk/*

HEALTHCHECK --interval=60s --timeout=5s CMD curl -sSf http://localhost:8080/version.json || exit 1

ADD backend.jar warpscores-backend.jar

EXPOSE 8080

CMD ["sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -Djava.awt.headless=true -jar warpscores-backend.jar"]
