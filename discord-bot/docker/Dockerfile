FROM openjdk:17-jdk-slim
MAINTAINER naytsyrhc <naytsyrhc@gmx.org>

COPY ./discord-bot.jar warpscores-discord-bot.jar

EXPOSE 8081

CMD ["sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -Djava.awt.headless=true -jar warpscores-discord-bot.jar"]
