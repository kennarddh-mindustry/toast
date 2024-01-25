package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerChatGameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerJoinGameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerLeaveGameEvent
import com.github.kennarddh.mindustry.toast.common.stripFooMessageInvisibleCharacters
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kennarddh.genesis.core.events.annotations.EventHandler
import kennarddh.genesis.core.handlers.Handler
import mindustry.game.EventType
import java.time.Instant

class GameEventsHandler : Handler() {
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

        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server, Instant.now().toEpochMilli(),
                PlayerChatGameEvent(player.name, player.uuid(), event.message.stripFooMessageInvisibleCharacters())
            )
        )
    }
}