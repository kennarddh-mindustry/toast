FROM eclipse-temurin:17

WORKDIR /app

ADD https://github.com/kennarddh-mindustry/toast/releases/download/v0.0.9/toast-discord-0.0.9.jar bot.jar

ENTRYPOINT java -XX:MinRAMPercentage=20 -XX:MaxRAMPercentage=95 -jar bot.jar
