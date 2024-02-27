package com.github.kennarddh.mindustry.toast.common.discovery

import com.github.kennarddh.mindustry.toast.common.Server
import com.github.kennarddh.mindustry.toast.common.verify.discord.VerifyDiscordRedis
import io.github.domgew.kedis.KedisClient
import io.github.domgew.kedis.KedisConfiguration
import io.github.domgew.kedis.arguments.SetOptions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DiscoveryRedis {
    lateinit var client: KedisClient
        private set

    suspend fun init() {
        client = KedisClient.newClient(
            configuration = KedisConfiguration(
                endpoint = KedisConfiguration.Endpoint.HostPort(
                    host = System.getenv("DISCOVERY_REDIS_HOST"),
                    port = System.getenv("DISCOVERY_REDIS_PORT").toInt(),
                ),
                authentication = KedisConfiguration.Authentication.NoAutoAuth,
                connectionTimeoutMillis = 20000,
            ),
        )

        client.connect()
        client.auth(
            System.getenv("DISCOVERY_REDIS_USERNAME"),
            System.getenv("DISCOVERY_REDIS_PASSWORD")
        )
    }

    suspend fun post(server: Server, payload: DiscoveryPayload) {
        val encodedPayload = Json.encodeToString(payload)

        client.set(
            server.name,
            encodedPayload,
            options = SetOptions(
                previousKeyHandling = SetOptions.PreviousKeyHandling.OVERRIDE,
                expire = SetOptions.ExpireOption.ExpiresInSeconds(10),
            ),
        )
    }

    suspend fun get(server: Server): DiscoveryPayload? {
        val payloadEncoded = client.get(server.name) ?: return null

        val payload = Json.decodeFromString<DiscoveryPayload>(payloadEncoded)

        return payload
    }

    suspend fun close() {
        VerifyDiscordRedis.client.closeSuspended()
    }
}