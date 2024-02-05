package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.extensions.stripFooMessageInvisibleCharacters
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.*
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import mindustry.game.EventType
import java.time.Instant

class GameEventsHandler : Handler() {
    override fun onDispose() {
        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server, Instant.now().toEpochMilli(),
                ServerStopGameEvent()
            )
        )
    }

    @EventHandler
    fun onPlayerJoin(event: EventType.PlayerJoin) {
        val player = event.player

        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server,
                Instant.now().toEpochMilli(),
                PlayerJoinGameEvent(player.name, player.uuid())
            )
        )
    }

    @EventHandler
    fun onPlayerLeave(event: EventType.PlayerLeave) {
        val player = event.player

        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server,
                Instant.now().toEpochMilli(),
                PlayerLeaveGameEvent(player.name, player.uuid())
            )
        )
    }

    @EventHandler
    fun onPlayerChat(event: EventType.PlayerChatEvent) {
        val player = event.player

        // Ignore client command
        if (event.message.startsWith(Genesis.commandRegistry.clientPrefix)) return

        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server, Instant.now().toEpochMilli(),
                PlayerChatGameEvent(player.name, player.uuid(), event.message.stripFooMessageInvisibleCharacters())
            )
        )
    }
}