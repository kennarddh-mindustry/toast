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

class DiscoveryHandler : Handler() {
    private lateinit var serverStart: Instant
    private var publicIP: String? = null

    override suspend fun onInit() {
        serverStart = Clock.System.now()
    }

    @TimerTask(1f, 5f)
    suspend fun onUpdateDiscovery() {
        val uptime = Clock.System.now() - serverStart

        if (publicIP == null) {
            publicIP = try {
                URL("http://checkip.amazonaws.com").readText().trim('\n')
            } catch (err: IOException) {
                null
            }
        }

        val host = "${publicIP}:${ToastVars.port}"

        val payload = DiscoveryPayload(
            Clock.System.now(),
            Entities.players.keys.map { it.name }.toTypedArray(),
            (60 / Time.delta).toInt(),
            uptime,
            Vars.state.map.name(),
            Vars.state.isPaused,
            if (publicIP == null) null else host
        )

        DiscoveryRedis.post(ToastVars.server, payload)
    }
}