package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.timers.annotations.TimerTask
import com.github.kennarddh.mindustry.toast.common.discovery.DiscoveryPayload
import com.github.kennarddh.mindustry.toast.common.discovery.DiscoveryRedis
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import io.github.domgew.kedis.arguments.SetOptions
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mindustry.Vars
import mindustry.gen.Groups
import java.net.URL

class DiscoveryHandler : Handler() {
    lateinit var serverStart: Instant

    override suspend fun onInit() {
        serverStart = Clock.System.now()
    }

    @TimerTask(1f, 5f)
    suspend fun onUpdateDiscovery() {
        val uptime = Clock.System.now() - serverStart

        val publicIP = URL("http://checkip.amazonaws.com").readText().trim('\n')

        val host = "${publicIP}:${ToastVars.port}"

        val payload = DiscoveryPayload(
            Clock.System.now(),
            Groups.player.map { it.name }.toTypedArray(),
            if (Vars.state.serverTps == -1) 60 else Vars.state.serverTps,
            uptime,
            Vars.state.map.name(),
            host
        )

        val encodedPayload = Json.encodeToString(payload)

        DiscoveryRedis.client.set(
            ToastVars.server.name,
            encodedPayload,
            options = SetOptions(
                previousKeyHandling = SetOptions.PreviousKeyHandling.OVERRIDE,
                expire = SetOptions.ExpireOption.ExpiresInSeconds(10),
            ),
        )
    }
}