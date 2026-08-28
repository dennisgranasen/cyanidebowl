FROM eclipse-temurin:25-jre-alpine
LABEL org.opencontainers.image.title="Warp Scores Discord Bot"

RUN apk add --no-cache curl

HEALTHCHECK --interval=2m --timeout=15s --start-period=45s \
    CMD curl --retry 6 --max-time 5 --retry-delay 10 --retry-max-time 60 \
             --fail --silent --show-error http://localhost:8081/version.json

COPY discord-bot.jar warpscores-discord-bot.jar

EXPOSE 8081

ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "warpscores-discord-bot.jar"]
