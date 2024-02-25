FROM eclipse-temurin:17

WORKDIR /app

ADD https://github.com/Anuken/Mindustry/releases/download/v146/server-release.jar server-release.jar
ADD https://github.com/xpdustry/kotlin-runtime/releases/download/v3.1.1-k.1.9.22/kotlin-runtime.jar config/mods/kotlin-runtime.jar

ADD https://github.com/kennarddh-mindustry/genesis/releases/download/v3.0.0-beta.11/genesis-core-3.0.0-beta.11.jar config/mods/genesis-core-3.0.0-beta.11.jar
ADD https://github.com/kennarddh-mindustry/genesis/releases/download/v3.0.0-beta.11/genesis-standard-3.0.0-beta.11.jar config/mods/genesis-standard-3.0.0-beta.11.jar

ADD https://github.com/kennarddh-mindustry/toast/releases/download/v0.0.16/toast-core-0.0.16.jar config/mods/toast-core-0.0.16.jar

# For ss command. ss command is used for healthcheck
RUN apt update && apt install -y iproute2

ENTRYPOINT java -jar server-release.jar
