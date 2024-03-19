FROM eclipse-temurin:17

WORKDIR /app

ADD https://github.com/kennarddh-mindustry/toast/releases/download/v0.0.49/toast-discord-0.0.49.jar bot.jar

# https://dzone.com/articles/gracefully-shutting-down-java-in-containers
ENTRYPOINT exec java -XX:+ExitOnOutOfMemoryError -jar bot.jar
