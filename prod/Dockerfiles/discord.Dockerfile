FROM eclipse-temurin:17

WORKDIR /app

ADD https://github.com/kennarddh-mindustry/toast/releases/download/v0.0.15/toast-discord-0.0.15.jar bot.jar

ENTRYPOINT java -jar bot.jar
