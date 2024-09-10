FROM openjdk:17-jdk-alpine
MAINTAINER naytsyrhc <naytsyrhc@gmx.org>

RUN apk add --update curl && \
    rm -rf /var/cache/apk/*

HEALTHCHECK --interval=60s --timeout=5s CMD curl -sSf http://localhost:8081/version.json || exit 1

ADD discord-bot.jar warpscores-discord-bot.jar

EXPOSE 8081

CMD ["sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -Djava.awt.headless=true -jar warpscores-discord-bot.jar"]
