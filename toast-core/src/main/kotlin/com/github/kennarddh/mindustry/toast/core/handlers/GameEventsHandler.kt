package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerJoinGameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerLeaveGameEvent
import kennarddh.genesis.core.events.annotations.EventHandler
import kennarddh.genesis.core.handlers.Handler
import mindustry.game.EventType

class GameEventsHandler : Handler() {
    @EventHandler
    fun onPlayerJoin(event: EventType.PlayerJoin) {
        val player = event.player

        Messenger.publishGameEvent(GameEvent(PlayerJoinGameEvent(player.name, player.uuid())))
    }

    @EventHandler
    fun onPlayerLeave(event: EventType.PlayerLeave) {
        val player = event.player

        Messenger.publishGameEvent(GameEvent(PlayerLeaveGameEvent(player.name, player.uuid())))
    }
}