FROM eclipse-temurin:17

WORKDIR /app

ADD https://github.com/kennarddh-mindustry/toast/releases/download/v0.0.13/toast-discord-0.0.13.jar bot.jar

ENTRYPOINT java -jar bot.jar
