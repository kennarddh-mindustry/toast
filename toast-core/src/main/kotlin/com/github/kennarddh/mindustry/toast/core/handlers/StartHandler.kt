package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerStartGameEvent
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kotlinx.datetime.Clock
import mindustry.game.EventType

class StartHandler : Handler() {
    @EventHandler
    fun onLoad(event: EventType.ServerLoadEvent) {
        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server, Clock.System.now().toEpochMilliseconds(),
                ServerStartGameEvent()
            )
        )
    }
}