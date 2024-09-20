FROM openjdk:17-jdk-alpine
MAINTAINER naytsyrhc <naytsyrhc@gmx.org>

RUN apk add --update curl && \
    rm -rf /var/cache/apk/*

HEALTHCHECK --interval=2m --timeout=15s --start-period=45s \
    CMD curl --retry 6 --max-time 5 --retry-delay 10 --retry-max-time 60 \
             --fail --silent --show-error http://localhost:8081/version.json \
          || bash -c 'kill --signal SIGTERM -1 && (sleep 10; kill --signal SIGKILL -1)'

ADD discord-bot.jar warpscores-discord-bot.jar

EXPOSE 8081

CMD ["sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -Djava.awt.headless=true -jar warpscores-discord-bot.jar"]
