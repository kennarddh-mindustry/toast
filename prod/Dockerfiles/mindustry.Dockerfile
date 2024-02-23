FROM eclipse-temurin:17

WORKDIR /app

ADD https://github.com/Anuken/Mindustry/releases/download/v146/server-release.jar server-release.jar
ADD https://github.com/xpdustry/kotlin-runtime/releases/download/v3.1.1-k.1.9.22/kotlin-runtime.jar config/mods/kotlin-runtime.jar

ADD https://github.com/kennarddh-mindustry/genesis/releases/download/v3.0.0-beta.8/genesis-core-3.0.0-beta.8.jar config/mods/genesis-core-3.0.0-beta.8.jar
ADD https://github.com/kennarddh-mindustry/genesis/releases/download/v3.0.0-beta.8/genesis-standard-3.0.0-beta.8.jar config/mods/genesis-standard-3.0.0-beta.8.jar

ADD https://github.com/kennarddh-mindustry/toast/releases/download/v0.0.8/toast-core-0.0.8.jar config/mods/toast-core-0.0.8.jar

ENTRYPOINT java -XX:MinRAMPercentage=20 -XX:MaxRAMPercentage=95 -jar server-release.jar
