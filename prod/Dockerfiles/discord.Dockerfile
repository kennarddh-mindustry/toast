FROM eclipse-temurin:17

WORKDIR /app

ADD https://github.com/kennarddh-mindustry/toast/releases/download/v0.0.34/toast-discord-0.0.34.jar bot.jar

ENTRYPOINT java -XX:+ExitOnOutOfMemoryError -jar bot.jar
