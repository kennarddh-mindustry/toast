FROM eclipse-temurin:17

WORKDIR /app

ADD https://github.com/kennarddh-mindustry/toast/releases/download/v0.0.35/toast-discord-0.0.35.jar bot.jar

ENTRYPOINT java -XX:+ExitOnOutOfMemoryError -jar bot.jar
