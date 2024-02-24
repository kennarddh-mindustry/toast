package com.github.kennarddh.mindustry.toast.common.discovery

import io.github.domgew.kedis.KedisClient
import io.github.domgew.kedis.KedisConfiguration
import io.github.domgew.kedis.arguments.SetOptions
import kotlin.time.Duration.Companion.minutes

object VerifyDiscordRedis {
    lateinit var client: KedisClient
        private set

    suspend fun init() {
        client = KedisClient.newClient(
            configuration = KedisConfiguration(
                endpoint = KedisConfiguration.Endpoint.HostPort(
                    host = System.getenv("VERIFY_DISCORD_REDIS_HOST"),
                    port = System.getenv("VERIFY_DISCORD_REDIS_PORT").toInt(),
                ),
                authentication = KedisConfiguration.Authentication.NoAutoAuth,
                connectionTimeoutMillis = 20000,
            ),
        )

        client.connect()
    }

    suspend fun set(userID: Int, pin: Int) {
        client.set(
            userID.toString(), pin.toString(),
            options = SetOptions(
                previousKeyHandling = SetOptions.PreviousKeyHandling.OVERRIDE,
                expire = SetOptions.ExpireOption.ExpiresInSeconds(5.minutes.inWholeSeconds),
            ),
        )
    }

    suspend fun get(userID: Int): Int? {
        return client.get(userID.toString())?.toInt()
    }
}