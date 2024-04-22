package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.extensions.stripColors
import com.github.kennarddh.mindustry.genesis.standard.extensions.stripFooMessageInvisibleCharacters
import com.github.kennarddh.mindustry.genesis.standard.extensions.stripGlyphs
import com.github.kennarddh.mindustry.toast.common.extensions.preventDiscordPings
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerChatGameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerJoinGameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerLeaveGameEvent
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.handlers.users.PlayerDisconnected
import kotlinx.datetime.Clock
import mindustry.game.EventType

class GameEventsHandler : Handler {
    @EventHandler
    suspend fun onPlayerJoin(event: EventType.PlayerJoin) {
        val player = event.player

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.player.join",
            GameEvent(
                ToastVars.server,
                Clock.System.now(),
                PlayerJoinGameEvent(player.plainName(), player.uuid())
            )
        )
    }

    @EventHandler
    suspend fun onPlayerDisconnected(event: PlayerDisconnected) {
        if (!event.player.con.hasConnected) return

        val player = event.player

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.player.leave",
            GameEvent(
                ToastVars.server,
                Clock.System.now(),
                PlayerLeaveGameEvent(player.plainName(), player.uuid())
            )
        )
    }

    @EventHandler
    suspend fun onPlayerChat(event: EventType.PlayerChatEvent) {
        val player = event.player

        // Ignore client command
        if (event.message.startsWith(Genesis.commandRegistry.clientPrefix)) return

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.player.chat",
            GameEvent(
                ToastVars.server, Clock.System.now(),
                PlayerChatGameEvent(
                    player.plainName(), player.uuid(),
                    event.message
                        .stripFooMessageInvisibleCharacters()
                        .stripGlyphs()
                        .stripColors()
                        .preventDiscordPings()
                )
            )
        )
    }
}