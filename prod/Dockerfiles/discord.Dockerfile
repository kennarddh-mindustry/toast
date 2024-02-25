FROM eclipse-temurin:17

WORKDIR /app

ADD https://github.com/kennarddh-mindustry/toast/releases/download/v0.0.16/toast-discord-0.0.16.jar bot.jar

ENTRYPOINT java -jar bot.jar
