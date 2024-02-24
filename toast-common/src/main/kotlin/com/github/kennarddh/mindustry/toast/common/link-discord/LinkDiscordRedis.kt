package com.github.kennarddh.mindustry.toast.common.discovery

import io.github.domgew.kedis.KedisClient
import io.github.domgew.kedis.KedisConfiguration

object LinkDiscordRedis {
    lateinit var client: KedisClient
        private set

    suspend fun init() {
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

        client.connect()
    }

    suspend fun set(userID: Int, code: String) {
        client.set(userID.toString(), code)
    }

    suspend fun get(userID: Int): String? {
        return client.get(userID.toString())
    }
}