package com.github.kennarddh.mindustry.toast.common.discovery

import io.github.domgew.kedis.KedisClient
import io.github.domgew.kedis.KedisConfiguration

object LinkDiscordRedis {
    lateinit var client: KedisClient
        private set

    fun init() {
        client = KedisClient.newClient(
            configuration = KedisConfiguration(
                endpoint = KedisConfiguration.Endpoint.HostPort(
                    host = System.getenv("LINK_DISCORD_REDIS_HOST"),
                    port = System.getenv("LINK_DISCORD_REDIS_PORT").toInt(),
                ),
                authentication = KedisConfiguration.Authentication.NoAutoAuth,
                connectionTimeoutMillis = 20000,
            ),
        )
    }
}