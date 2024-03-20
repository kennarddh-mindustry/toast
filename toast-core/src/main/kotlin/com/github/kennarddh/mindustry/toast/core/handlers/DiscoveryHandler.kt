package com.github.kennarddh.mindustry.toast.core.handlers

import arc.util.Time
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.timers.annotations.TimerTask
import com.github.kennarddh.mindustry.toast.common.discovery.DiscoveryPayload
import com.github.kennarddh.mindustry.toast.common.discovery.DiscoveryRedis
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mindustry.Vars
import java.io.IOException
import java.net.URL

class DiscoveryHandler : Handler {
    private lateinit var serverStart: Instant
    private lateinit var publicIP: String

    override suspend fun onInit() {
        serverStart = Clock.System.now()
    }

    @TimerTask(1f, 5f)
    suspend fun onUpdateDiscovery() {
        val uptime = Clock.System.now() - serverStart

        if (!::publicIP.isInitialized) {
            try {
                publicIP = URL("http://checkip.amazonaws.com").readText().trim('\n')
            } catch (err: IOException) {
                // Ignore and try again later if publicIP can't be initialized
            }
        }

        val host = "${publicIP}:${ToastVars.port}"

        val payload = DiscoveryPayload(
            Clock.System.now(),
            Entities.players.keys.map { it.plainName() }.toTypedArray(),
            (60 / Time.delta).toInt(),
            uptime,
            Vars.state.map.plainName(),
            Vars.state.isPaused,
            if (::publicIP.isInitialized) host else null
        )

        DiscoveryRedis.post(ToastVars.server, payload)
    }
}