package com.github.kennarddh.mindustry.toast.discord.listeners

import com.github.kennarddh.mindustry.toast.common.Server
import com.github.kennarddh.mindustry.toast.common.discovery.DiscoveryRedis
import com.github.kennarddh.mindustry.toast.common.extensions.toDisplayString
import com.github.kennarddh.mindustry.toast.discord.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import kotlin.time.Duration.Companion.seconds

object DiscoveryHandler : ListenerAdapter() {
    lateinit var serverListMessage: Message

    override fun onReady(event: ReadyEvent) {
        if (
            !System.getenv("ENABLE_DEV_SERVER_LIST").toBoolean() &&
            jda.selfUser.idLong != DiscordConstant.PRODUCTION_TOAST_BOT_USER_ID
        ) return

        CoroutineScopes.Main.launch {
            while (true) {
                val message = buildString {
                    appendLine("Last update <t:${Clock.System.now().toEpochMilliseconds() / 1000}:R>")
                    appendLine("Should update every 10 seconds.")

                    appendLine()

                    Server.entries.forEach {
                        val discoveryPayload = DiscoveryRedis.get(it)

                        if (discoveryPayload == null) {
                            appendLine("${it.displayName}: Offline")

                            return@forEach
                        }

                        appendLine("${it.displayName}: Online")

                        // Prevent exposing developers ip
                        if (jda.selfUser.idLong == DiscordConstant.PRODUCTION_TOAST_BOT_USER_ID)
                            appendLine("\tIP: ${discoveryPayload.host}")

                        appendLine("\tUptime: ${discoveryPayload.uptime.toDisplayString()}")
                        appendLine("\tTPS: ${discoveryPayload.tps}")
                        appendLine("\tGame mode: ${it.gameMode.name}")
                        appendLine("\tMap: ${discoveryPayload.map}")
                        appendLine("\tIs Paused: ${if (discoveryPayload.isPaused) ":green_square:" else ":red_square:"}")
                        appendLine("\tPlayers Count: ${discoveryPayload.players.size}")
                        appendLine("\tPlayers: ${discoveryPayload.players.joinToString(", ")}")
                    }
                }.trimEnd('\n')

                if (System.getenv("SERVER_LIST_MESSAGE_ID") == null) {
                    serverListChannel.sendMessage(message).queue()
                } else {
                    if (::serverListMessage.isInitialized) {
                        serverListMessage.editMessage(message).queue()
                    } else {
                        try {
                            serverListChannel.retrieveMessageById(System.getenv("SERVER_LIST_MESSAGE_ID").toLong())
                                .queue {
                                    serverListMessage = it

                                    it.editMessage(message).queue()
                                }
                        } catch (error: Exception) {
                            Logger.error("Cannot get server list message", error)
                        }
                    }
                }

                delay(10.seconds)
            }
        }
    }
}